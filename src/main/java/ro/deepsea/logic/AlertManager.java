package ro.deepsea.logic;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ro.deepsea.DeepSea;
import ro.deepsea.config.CreatureSettings;
import ro.deepsea.data.SeaCreature;
import ro.deepsea.data.SeaCreatureDatabase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tine lista alertelor active si urmareste mobii din lume: patratul ramane pe ecran
 * cat timp creatura (sau creaturile, la double hook) sunt in viata.
 */
public final class AlertManager {

    private static final List<ActiveAlert> ACTIVE = new ArrayList<>();
    private static final List<SeaCreature> BY_NAME_LENGTH = new ArrayList<>();
    private static final Map<String, List<String>> CHAINS = new HashMap<>();

    /** Raza in care cautam mobul dupa ce am primit mesajul de catch. */
    private static final double SEARCH_RADIUS = 16.0;

    /** Cat timp dupa moarte mai cautam "copii" (ex. Magma Slug -> Baby Magma Slug). */
    private static final long CHAIN_WINDOW_MS = 2500L;

    private static int ticks;

    static {
        BY_NAME_LENGTH.addAll(SeaCreatureDatabase.all());
        BY_NAME_LENGTH.sort(Comparator.comparingInt((SeaCreature c) -> c.name().length()).reversed());
        CHAINS.put("Magma Slug", List.of("Baby Magma Slug"));
    }

    private AlertManager() {
    }

    /** Reimprospateaza lista sortata dupa ce baza de date s-a incarcat. */
    public static void reloadDatabase() {
        BY_NAME_LENGTH.clear();
        BY_NAME_LENGTH.addAll(SeaCreatureDatabase.all());
        BY_NAME_LENGTH.sort(Comparator.comparingInt((SeaCreature c) -> c.name().length()).reversed());
    }

    public static void onCatch(SeaCreature creature, CreatureSettings settings, int expected) {
        long now = System.currentTimeMillis();

        for (Iterator<ActiveAlert> iterator = ACTIVE.iterator(); iterator.hasNext(); ) {
            ActiveAlert alert = iterator.next();
            if (!alert.creature.name().equals(creature.name())) {
                continue;
            }
            // MOB: aceeasi creatura prinsa din nou cat timp inca traieste -> adaugam la alerta existenta
            if (alert.untilDead && alert.allDeadAt < 0 && alert.visible(now)) {
                alert.expected += expected;
                alert.findDeadline = now + 4000L;
                return;
            }
            // TIM: o captura noua reporneste cronometrul (altfel alerta veche expira prea devreme)
            if (!alert.untilDead) {
                iterator.remove();
            }
        }

        boolean label = settings.showLabel && DeepSea.config().showLabel;
        ACTIVE.add(0, new ActiveAlert(
            creature,
            settings.color,
            label,
            settings.isUntilDead(),
            settings.durationMs,
            settings.afterDeathMs,
            expected
        ));

        int max = Math.max(1, DeepSea.config().maxAlerts);
        while (ACTIVE.size() > max) {
            ACTIVE.remove(ACTIVE.size() - 1);
        }
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            // deconectat / in meniu: nu lasam alerte vechi sa apara la reconectare
            ACTIVE.clear();
            return;
        }
        if (minecraft.player == null) {
            return;
        }
        if (ACTIVE.isEmpty()) {
            return;
        }
        if (++ticks % 3 != 0) {
            return;
        }

        long now = System.currentTimeMillis();
        for (ActiveAlert alert : ACTIVE) {
            if (!alert.untilDead) {
                continue;
            }
            scanForMobs(minecraft, alert, now);
            updateDead(minecraft, alert, now);
            handleChain(minecraft, alert, now);
        }
        ACTIVE.removeIf(alert -> !alert.visible(now));
    }

    public static List<ActiveAlert> visible() {
        long now = System.currentTimeMillis();
        ACTIVE.removeIf(alert -> !alert.visible(now));
        return new ArrayList<>(ACTIVE);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static void showTest() {
        List<SeaCreature> all = SeaCreatureDatabase.all();
        if (all.isEmpty()) {
            return;
        }
        List<SeaCreature> picks = new ArrayList<>();
        for (String rarity : new String[]{"MYTHIC", "LEGENDARY", "EPIC"}) {
            all.stream().filter(c -> c.rarity().equals(rarity)).findFirst().ifPresent(picks::add);
        }
        if (picks.isEmpty()) {
            picks.add(all.get(0));
        }
        for (SeaCreature creature : picks) {
            boolean label = DeepSea.config().showLabel;
            ACTIVE.add(0, new ActiveAlert(
                creature,
                creature.defaultColor(),
                label,
                false,
                6000,
                0,
                1
            ));
        }
    }

    private static void scanForMobs(Minecraft minecraft, ActiveAlert alert, long now) {
        if (alert.entityIds.size() >= alert.expected) {
            return;
        }
        long extra = alert.foundAny ? 2000L : 0L;
        if (now > alert.findDeadline + extra) {
            return;
        }

        AABB box = minecraft.player.getBoundingBox().inflate(SEARCH_RADIUS);
        List<Entity> entities = minecraft.level.getEntities(minecraft.player, box, entity -> entity instanceof LivingEntity);
        String wanted = alert.creature.name();

        for (Entity entity : entities) {
            if (isTracked(entity.getId())) {
                continue;
            }
            SeaCreature found = matchName(cleanName(entity));
            if (found == null || !found.name().equals(wanted)) {
                continue;
            }
            alert.entityIds.add(entity.getId());
            alert.foundAny = true;
            alert.lastPos = entity.position();
            if (alert.entityIds.size() >= alert.expected) {
                return;
            }
        }
    }

    private static void updateDead(Minecraft minecraft, ActiveAlert alert, long now) {
        if (alert.entityIds.isEmpty()) {
            return;
        }
        Iterator<Integer> iterator = alert.entityIds.iterator();
        while (iterator.hasNext()) {
            Entity entity = minecraft.level.getEntity(iterator.next());
            boolean gone = entity == null
                || entity.isRemoved()
                || (entity instanceof LivingEntity living && living.isDeadOrDying());
            if (gone) {
                iterator.remove();
            } else {
                alert.lastPos = entity.position();
            }
        }
        if (alert.foundAny && alert.entityIds.isEmpty() && alert.allDeadAt < 0) {
            alert.allDeadAt = now;
        }
    }

    /** Unele creaturi se spawneaza in mai multe bucati dupa moarte (ex. Magma Slug -> Baby Magma Slug). */
    private static void handleChain(Minecraft minecraft, ActiveAlert alert, long now) {
        if (alert.allDeadAt < 0 || now - alert.allDeadAt > CHAIN_WINDOW_MS) {
            return;
        }
        Vec3 position = alert.lastPos;
        if (position == null) {
            return;
        }
        List<String> extra = CHAINS.getOrDefault(alert.creature.name(), List.of());
        if (extra.isEmpty()) {
            return;
        }

        AABB box = new AABB(
            position.x - 6.0, position.y - 6.0, position.z - 6.0,
            position.x + 6.0, position.y + 6.0, position.z + 6.0
        );

        for (Entity entity : minecraft.level.getEntities(minecraft.player, box, e -> e instanceof LivingEntity)) {
            if (isTracked(entity.getId())) {
                continue;
            }
            SeaCreature found = matchName(cleanName(entity));
            if (found == null || !extra.contains(found.name())) {
                continue;
            }
            alert.entityIds.add(entity.getId());
            alert.allDeadAt = -1;
            alert.foundAny = true;
            alert.lastPos = entity.position();
            alert.expected = Math.max(alert.expected, alert.entityIds.size());
            return;
        }
    }

    private static boolean isTracked(int entityId) {
        for (ActiveAlert alert : ACTIVE) {
            if (alert.entityIds.contains(entityId)) {
                return true;
            }
        }
        return false;
    }

    private static String cleanName(Entity entity) {
        if (!entity.hasCustomName()) {
            return null;
        }
        var custom = entity.getCustomName();
        if (custom == null) {
            return null;
        }
        String text = ChatFormatting.stripFormatting(custom.getString()).trim();
        return text.isEmpty() ? null : text;
    }

    /** Potrivire exacta pe nume, apoi "contine" (nametag-ul poate avea si alte detalii). */
    private static SeaCreature matchName(String name) {
        if (name == null) {
            return null;
        }
        SeaCreature direct = SeaCreatureDatabase.byName(name);
        if (direct != null) {
            return direct;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (SeaCreature creature : BY_NAME_LENGTH) {
            if (lower.contains(creature.name().toLowerCase(Locale.ROOT))) {
                return creature;
            }
        }
        return null;
    }

    /** Pentru debug: cate entitati urmarim acum. */
    public static Set<Integer> trackedIds() {
        Set<Integer> ids = new HashSet<>();
        for (ActiveAlert alert : ACTIVE) {
            ids.addAll(alert.entityIds);
        }
        return ids;
    }
}
