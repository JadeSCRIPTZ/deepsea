package ro.deepsea.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ro.deepsea.DeepSea;
import ro.deepsea.config.CreatureSettings;
import ro.deepsea.data.SeaCreature;
import ro.deepsea.data.SeaCreatureDatabase;
import ro.deepsea.logic.AlertManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GUI-ul principal, deschis cu {@code /dpsea}.
 *
 * Fiecare rand = o creatura:
 * [ON] Nume | #RRGGBB | 6.0s | +1.5s | MOB | N
 *                         durata  dupa    mod   nume
 *                                  moarte
 */
public final class DeepSeaScreen extends Screen {

    private static final int[] PALETTE = {
        0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF,
        0xFF5555FF, 0xFFFF55FF, 0xFFFF8000, 0xFFFFFFFF, 0xFF808080
    };
    private static final int[] DURATIONS = {2000, 3000, 4000, 5000, 6000, 8000, 10000, 15000, 20000};
    private static final int[] AFTER_DEATH = {0, 1000, 2000, 3000, 5000, 8000, 10000};

    private static final int ROW_LEFT = -158;
    private static final int ROW_RIGHT = 158;

    private final List<SeaCreature> filtered = new ArrayList<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private String query = "";
    private int page = 0;
    private int perPage = 6;
    private EditBox searchBox;

    public DeepSeaScreen() {
        super(Component.literal("DeepSea"));
    }

    @Override
    protected void init() {
        rowWidgets.clear();
        this.perPage = Math.max(3, (this.height - 118) / 20);
        filterCreatures();

        this.searchBox = new EditBox(this.font, this.width / 2 - 110, 22, 220, 18, Component.literal("Cauta creatura"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue(this.query);
        this.searchBox.setResponder(value -> {
            this.query = value;
            this.page = 0;
            this.rebuildRows();
        });
        this.addRenderableWidget(this.searchBox);

        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            if (this.page > 0) {
                this.page--;
                this.rebuildRows();
            }
        }).bounds(this.width / 2 - 110, 44, 22, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            if (this.page < this.maxPage()) {
                this.page++;
                this.rebuildRows();
            }
        }).bounds(this.width / 2 + 88, 44, 22, 18).build());

        int bottom = this.height - 26;
        int buttonWidth = Math.min(84, Math.max(54, (this.width - 24) / 5 - 4));
        int startX = this.width / 2 - (buttonWidth * 5 + 16) / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Toate ON"), button -> {
            for (SeaCreature creature : this.filtered) {
                DeepSea.config().settings(creature.name()).enabled = true;
            }
            DeepSea.config().save();
            this.rebuildRows();
        }).bounds(startX, bottom, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Toate OFF"), button -> {
            for (SeaCreature creature : this.filtered) {
                DeepSea.config().settings(creature.name()).enabled = false;
            }
            DeepSea.config().save();
            this.rebuildRows();
        }).bounds(startX + buttonWidth + 4, bottom, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Muta HUD"), button -> {
            try {
                Minecraft.getInstance().setScreen(new HudEditScreen(this));
            } catch (Throwable throwable) {
                DeepSea.LOGGER.error("Could not open HUD editor", throwable);
            }
        }).bounds(startX + (buttonWidth + 4) * 2, bottom, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Test"), button -> {
            AlertManager.showTest();
            Minecraft.getInstance().setScreen(null);
        }).bounds(startX + (buttonWidth + 4) * 3, bottom, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Gata"), button -> this.onClose())
            .bounds(startX + (buttonWidth + 4) * 4, bottom, buttonWidth, 20).build());

        this.rebuildRows();
    }

    private void filterCreatures() {
        this.filtered.clear();
        String needle = this.query.toLowerCase(Locale.ROOT);
        for (SeaCreature creature : SeaCreatureDatabase.all()) {
            if (needle.isEmpty() || creature.name().toLowerCase(Locale.ROOT).contains(needle)) {
                this.filtered.add(creature);
            }
        }
        int maxPage = this.maxPage();
        if (this.page > maxPage) {
            this.page = maxPage;
        }
    }

    private int maxPage() {
        return Math.max(0, (this.filtered.size() - 1) / this.perPage);
    }

    private void rebuildRows() {
        for (AbstractWidget widget : this.rowWidgets) {
            this.removeWidget(widget);
        }
        this.rowWidgets.clear();

        this.filterCreatures();
        int first = this.page * this.perPage;
        int last = Math.min(this.filtered.size(), first + this.perPage);
        int y = 68;
        int left = this.width / 2 + ROW_LEFT;

        for (int index = first; index < last; index++) {
            SeaCreature creature = this.filtered.get(index);
            CreatureSettings settings = DeepSea.config().settings(creature.name());

            this.addRowButton(Button.builder(
                Component.literal((settings.enabled ? "§a[ON] " : "§8[OFF] ") + creature.name()),
                button -> {
                    settings.enabled = !settings.enabled;
                    DeepSea.config().save();
                    this.rebuildRows();
                }
            ).bounds(left, y, 104, 18).build());

            this.addRowButton(Button.builder(
                Component.literal(String.format("#%06X", settings.color & 0xFFFFFF)),
                button -> {
                    settings.color = nextColor(settings.color);
                    DeepSea.config().save();
                    this.rebuildRows();
                }
            ).bounds(left + 108, y, 62, 18).build());

            this.addRowButton(Button.builder(
                Component.literal(String.format(Locale.ROOT, "%.1fs", settings.durationMs / 1000.0)),
                button -> {
                    settings.durationMs = nextValue(DURATIONS, settings.durationMs, 5000);
                    DeepSea.config().save();
                    this.rebuildRows();
                }
            ).bounds(left + 174, y, 34, 18).build());

            this.addRowButton(Button.builder(
                Component.literal(String.format(Locale.ROOT, "+%.1fs", settings.afterDeathMs / 1000.0)),
                button -> {
                    settings.afterDeathMs = nextValue(AFTER_DEATH, settings.afterDeathMs, 2000);
                    DeepSea.config().save();
                    this.rebuildRows();
                }
            ).bounds(left + 212, y, 38, 18).build());

            this.addRowButton(Button.builder(
                Component.literal(settings.isUntilDead() ? "MOB" : "TIM"),
                button -> {
                    settings.toggleMode();
                    DeepSea.config().save();
                    this.rebuildRows();
                }
            ).bounds(left + 254, y, 34, 18).build());

            this.addRowButton(Button.builder(
                Component.literal(settings.showLabel ? "N" : "-"),
                button -> {
                    settings.showLabel = !settings.showLabel;
                    DeepSea.config().save();
                    this.rebuildRows();
                }
            ).bounds(left + 292, y, 24, 18).build());

            y += 20;
            if (y > this.height - 60) {
                break;
            }
        }
    }

    private void addRowButton(Button button) {
        this.rowWidgets.add(this.addRenderableWidget(button));
    }

    private static int nextColor(int current) {
        for (int index = 0; index < PALETTE.length; index++) {
            if (PALETTE[index] == current) {
                return PALETTE[(index + 1) % PALETTE.length];
            }
        }
        return PALETTE[0];
    }

    private static int nextValue(int[] values, int current, int fallback) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] == current) {
                return values[(index + 1) % values.length];
            }
        }
        return fallback;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int left = this.width / 2 + ROW_LEFT;

        graphics.text(this.font, "DeepSea §7— alerte sea creature", left, 8, 0xFFFFFFFF, true);

        String pageLabel = "§7Pagina §f" + (this.page + 1) + "§7/§f" + (this.maxPage() + 1)
            + " §8(" + this.filtered.size() + " creaturi)";
        graphics.text(this.font, pageLabel, this.width / 2 - 80, 48, 0xFFFFFFFF, true);

        graphics.text(
            this.font,
            "§8[ON] / #culoare / durata / +dupa moarte / MOB-TIM / nume",
            left, 62, 0xFFFFFFFF, true
        );
        graphics.text(
            this.font,
            "§8MOB = sta pana moare mobul (ex. si la double hook) · TIM = doar durata",
            left, this.height - 42, 0xFFFFFFFF, true
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
