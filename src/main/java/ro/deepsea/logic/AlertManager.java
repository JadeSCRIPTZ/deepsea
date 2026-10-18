package ro.deepsea.logic;

import ro.deepsea.DeepSea;
import ro.deepsea.config.CreatureSettings;
import ro.deepsea.data.SeaCreature;
import ro.deepsea.data.SeaCreatureDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Tine lista alertelor active (ce trebuie desenat acum pe ecran).
 */
public final class AlertManager {

    /** O alerta: creatura, culoarea, momentul startului si durata. */
    public record Alert(SeaCreature creature, int color, long startMs, long durationMs, boolean label) {

        public boolean expired(long now) {
            return now - startMs >= durationMs;
        }
    }

    private static final List<Alert> ACTIVE = new ArrayList<>();

    private AlertManager() {
    }

    public static void onCatch(SeaCreature creature, CreatureSettings settings) {
        long now = System.currentTimeMillis();

        // daca aceeasi creatura a fost prinsa de doua ori foarte repede, nu dublam patratul
        ACTIVE.removeIf(alert -> alert.creature().name().equals(creature.name()) && now - alert.startMs() < 250);

        boolean label = settings.showLabel && DeepSea.config().showLabel;
        ACTIVE.add(0, new Alert(creature, settings.color, now, Math.max(500, settings.durationMs), label));

        int max = Math.max(1, DeepSea.config().maxAlerts);
        while (ACTIVE.size() > max) {
            ACTIVE.remove(ACTIVE.size() - 1);
        }
    }

    /** Alertele inca valide, cea mai recenta prima. */
    public static List<Alert> active() {
        long now = System.currentTimeMillis();
        ACTIVE.removeIf(alert -> alert.expired(now));
        return new ArrayList<>(ACTIVE);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    /** Afiseaza cateva alerte de test (cele mai rare creaturi din baza de date). */
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
            CreatureSettings settings = new CreatureSettings();
            settings.color = creature.defaultColor();
            settings.durationMs = 6000;
            onCatch(creature, settings);
        }
    }
}
