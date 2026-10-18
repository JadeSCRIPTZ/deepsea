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
import ro.deepsea.config.DeepSeaConfig;
import ro.deepsea.data.SeaCreature;
import ro.deepsea.data.SeaCreatureDatabase;
import ro.deepsea.logic.AlertManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GUI-ul principal, deschis cu {@code /dpsea}: lista cautabila a tuturor creaturilor,
 * cu pornit/oprit, culoare si durata pentru fiecare.
 */
public final class DeepSeaScreen extends Screen {

    private static final int[] PALETTE = {
        0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF,
        0xFF5555FF, 0xFFFF55FF, 0xFFFF8000, 0xFFFFFFFF, 0xFF808080
    };
    private static final int[] DURATIONS = {2000, 3000, 4000, 5000, 6000, 8000, 10000, 15000};

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
        perPage = Math.max(3, (this.height - 96) / 20);
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
        int buttonWidth = Math.min(84, Math.max(56, (this.width - 24) / 5 - 4));
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

        this.addRenderableWidget(Button.builder(Component.literal("Muta HUD"), button ->
            Minecraft.getInstance().setScreen(new HudEditScreen(this))
        ).bounds(startX + (buttonWidth + 4) * 2, bottom, buttonWidth, 20).build());

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

        for (int index = first; index < last; index++) {
            SeaCreature creature = this.filtered.get(index);
            CreatureSettings settings = DeepSea.config().settings(creature.name());

            Button toggle = this.addRenderableWidget(Button.builder(
                Component.literal((settings.enabled ? "§a[ON] " : "§8[OFF] ") + creature.name()),
                button -> {
                    settings.enabled = !settings.enabled;
                    DeepSea.config().save();
                    this.rebuildRows();
                }
            ).bounds(this.width / 2 - 150, y, 150, 18).build());
            this.rowWidgets.add(toggle);

            Button colorButton = this.addRenderableWidget(Button.builder(
                Component.literal(String.format("#%06X", settings.color & 0xFFFFFF)),
                button -> {
                    settings.color = nextColor(settings.color);
                    DeepSea.config().save();
                    this.rebuildRows();
                }
            ).bounds(this.width / 2 + 4, y, 72, 18).build());
            this.rowWidgets.add(colorButton);

            Button durationButton = this.addRenderableWidget(Button.builder(
                Component.literal(String.format(Locale.ROOT, "%.1fs", settings.durationMs / 1000.0)),
                button -> {
                    settings.durationMs = nextDuration(settings.durationMs);
                    DeepSea.config().save();
                    this.rebuildRows();
                }
            ).bounds(this.width / 2 + 80, y, 44, 18).build());
            this.rowWidgets.add(durationButton);

            Button labelButton = this.addRenderableWidget(Button.builder(
                Component.literal(settings.showLabel ? "Nume" : "Fara"),
                button -> {
                    settings.showLabel = !settings.showLabel;
                    DeepSea.config().save();
                    this.rebuildRows();
                }
            ).bounds(this.width / 2 + 128, y, 24, 18).build());
            this.rowWidgets.add(labelButton);

            y += 20;
        }
    }

    private static int nextColor(int current) {
        for (int index = 0; index < PALETTE.length; index++) {
            if (PALETTE[index] == current) {
                return PALETTE[(index + 1) % PALETTE.length];
            }
        }
        return PALETTE[0];
    }

    private static int nextDuration(int current) {
        for (int index = 0; index < DURATIONS.length; index++) {
            if (DURATIONS[index] == current) {
                return DURATIONS[(index + 1) % DURATIONS.length];
            }
        }
        return DURATIONS[3];
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.text(this.font, "DeepSea §7— alerte sea creature", this.width / 2 - 150, 8, 0xFFFFFFFF, true);
        graphics.text(
            this.font,
            "§7Pagina §f" + (this.page + 1) + "§7/§f" + (this.maxPage() + 1) + " §8(" + this.filtered.size() + " creaturi)",
            this.width / 2 - 80, 48, 0xFFFFFFFF, true
        );
        graphics.text(
            this.font,
            "§8Click pe nume = pornit/oprit · culoare · durata · nume",
            this.width / 2 - 150, this.height - 40, 0xFFFFFFFF, true
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
