package ro.deepsea.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import ro.deepsea.DeepSea;
import ro.deepsea.config.CreatureSettings;
import ro.deepsea.data.CreatureCategory;
import ro.deepsea.data.SeaCreature;
import ro.deepsea.data.SeaCreatureDatabase;
import ro.deepsea.logic.AlertManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GUI-ul principal, deschis cu {@code /dpsea}.
 *
 * <pre>
 * +-------------------------------------------------------------+
 * | DeepSea   12/89 active                     [ cauta...      ] |
 * +-----------+-----------------------------+-------------------+
 * | Categorii | Lista creaturilor           | Detalii creatura  |
 * | Toate     |  [] Lord Jawbus        ON   | ACTIV/INACTIV     |
 * | Apa       |  [] Thunder            OFF  | [] #FFAA00 Reset  |
 * | Lava      |  ...                        | paleta de culori  |
 * | ...       |                             | mod / durata / ...|
 * +-----------+-----------------------------+-------------------+
 * | Toate ON | Toate OFF            Muta HUD | Test | Gata       |
 * +-------------------------------------------------------------+
 * </pre>
 */
public final class DeepSeaScreen extends Screen {

    private static final int ROW_H = 14;
    private static final int SCROLL_W = 4;
    private static final int TITLE_H = 24;
    private static final int BOTTOM_H = 26;

    /** Paleta rapida (RGB). */
    private static final int[] PALETTE = {
        0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x00AA00, 0x55FFFF,
        0x00AAAA, 0x5555FF, 0xAA00AA, 0xFF55FF, 0xFFFFFF, 0x808080
    };
    private static final int[] DURATIONS = {
        500, 1000, 1500, 2000, 3000, 4000, 5000, 6000, 8000, 10000, 12000, 15000, 20000, 30000, 45000, 60000
    };
    private static final int[] AFTER_DEATH = {
        0, 500, 1000, 1500, 2000, 3000, 4000, 5000, 8000, 10000, 15000, 20000, 30000
    };

    private static final int C_PANEL = 0xF0121620;
    private static final int C_SUB = 0xA0000000;
    private static final int C_BORDER = 0xFF3A4256;
    private static final int C_ACCENT = 0xFF2F6DB5;
    private static final int C_HOVER = 0x28FFFFFF;
    private static final int C_TEXT = 0xFFE6E6E6;
    private static final int C_MUTED = 0xFF8C93A3;
    private static final int C_ON = 0xFF55FF55;
    private static final int C_HEX_OK = 0xFFE0E0E0;
    private static final int C_HEX_PARTIAL = 0xFFFFAA55;
    private static final int C_HEX_INVALID = 0xFFFF5555;

    /** Categoria aleasa ramane memorata cat timp jocul e deschis. */
    private static CreatureCategory lastCategory = CreatureCategory.ALL;

    // ---- stare ----
    private CreatureCategory category = lastCategory;
    private String query = "";
    private SeaCreature selected;
    private int scroll;
    private boolean draggingScrollbar;
    private boolean updatingHex;
    private final List<SeaCreature> visible = new ArrayList<>();

    // ---- layout (calculat in init) ----
    private int px, py, pw, ph;
    private int bodyTop, bodyBottom;
    private int sideX, sideW;
    private int listX, listW, rowsTop, visibleRows;
    private int detX, detW;
    private int colorY, paletteY, durY, afterY, paletteSize;

    // ---- widgeturi ----
    private EditBox searchBox;
    private EditBox hexBox;
    private Button toggleButton;
    private Button modeButton;
    private Button labelButton;
    private Button durMinus;
    private Button durPlus;
    private Button afterMinus;
    private Button afterPlus;

    public DeepSeaScreen() {
        super(Component.literal("DeepSea"));
    }

    // =====================================================================
    //  INIT / LAYOUT
    // =====================================================================

    @Override
    protected void init() {
        computeLayout();

        // --- cautare (in bara de sus) ---
        this.searchBox = new EditBox(this.font, px + pw - 4 - 130, py + 4, 130, 16, Component.literal("Cauta"));
        this.searchBox.setMaxLength(32);
        this.searchBox.setHint(Component.literal("Cauta creatura..."));
        this.searchBox.setValue(this.query);
        this.searchBox.setResponder(value -> {
            this.query = value;
            this.scroll = 0;
            this.rebuildVisible();
        });
        this.addRenderableWidget(this.searchBox);

        // --- panoul de detalii ---
        int y = bodyTop + 26;

        this.toggleButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (selected != null) {
                CreatureSettings s = current();
                s.enabled = !s.enabled;
                save();
                updateDetail();
            }
        }).bounds(detX, y, detW, 18).build());
        y += 21;

        colorY = y;
        int hexW = detW - 22 - 40 - 2;
        this.hexBox = new EditBox(this.font, detX + 22, y, hexW, 18, Component.literal("Culoare hex"));
        this.hexBox.setMaxLength(7);
        this.hexBox.setResponder(this::onHexChanged);
        this.addRenderableWidget(this.hexBox);
        this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
            if (selected != null) {
                applyColor(selected.defaultColor() & 0xFFFFFF);
            }
        }).bounds(detX + detW - 40, y, 40, 18).build());
        y += 21;

        paletteY = y;
        paletteSize = Math.max(6, (detW - (PALETTE.length - 1)) / PALETTE.length);
        y += paletteSize + 5;

        this.modeButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (selected != null) {
                current().toggleMode();
                save();
                updateDetail();
            }
        }).bounds(detX, y, detW, 18).build());
        y += 21;

        durY = y;
        this.durMinus = this.addRenderableWidget(Button.builder(Component.literal("-"), button -> stepDuration(-1))
            .bounds(detX + detW - 68, y, 18, 18).build());
        this.durPlus = this.addRenderableWidget(Button.builder(Component.literal("+"), button -> stepDuration(1))
            .bounds(detX + detW - 18, y, 18, 18).build());
        y += 21;

        afterY = y;
        this.afterMinus = this.addRenderableWidget(Button.builder(Component.literal("-"), button -> stepAfter(-1))
            .bounds(detX + detW - 68, y, 18, 18).build());
        this.afterPlus = this.addRenderableWidget(Button.builder(Component.literal("+"), button -> stepAfter(1))
            .bounds(detX + detW - 18, y, 18, 18).build());
        y += 21;

        this.labelButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (selected != null) {
                CreatureSettings s = current();
                s.showLabel = !s.showLabel;
                save();
                updateDetail();
            }
        }).bounds(detX, y, detW, 18).build());

        // --- bara de jos ---
        int by = py + ph - BOTTOM_H + 3;
        int bx = px + 4;
        this.addRenderableWidget(Button.builder(Component.literal("Toate ON"), button -> bulk(true))
            .bounds(bx, by, 62, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Toate OFF"), button -> bulk(false))
            .bounds(bx + 66, by, 66, 20).build());

        int rx = px + pw - 4;
        this.addRenderableWidget(Button.builder(Component.literal("Gata"), button -> this.onClose())
            .bounds(rx - 50, by, 50, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Test"), button -> {
            AlertManager.showTest();
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }).bounds(rx - 50 - 4 - 44, by, 44, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Muta HUD"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new HudEditScreen(this));
            }
        }).bounds(rx - 50 - 4 - 44 - 4 - 64, by, 64, 20).build());

        rebuildVisible();
        updateDetail();
    }

    private void computeLayout() {
        pw = Math.min(this.width - 8, 470);
        ph = Math.min(this.height - 8, 270);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;

        bodyTop = py + TITLE_H + 2;
        bodyBottom = py + ph - BOTTOM_H;

        boolean narrow = pw < 400;
        sideW = narrow ? 76 : 92;
        detW = narrow ? 120 : 138;
        sideX = px + 4;
        detX = px + pw - 4 - detW;
        listX = sideX + sideW + 4;
        listW = detX - 8 - listX;

        rowsTop = bodyTop + 16;
        visibleRows = Math.max(3, (bodyBottom - rowsTop - 2) / ROW_H);
    }

    // =====================================================================
    //  DATE / STARE
    // =====================================================================

    private CreatureSettings current() {
        return DeepSea.config().settings(selected.name());
    }

    private void save() {
        DeepSea.config().save();
    }

    private void rebuildVisible() {
        visible.clear();
        String needle = query.trim().toLowerCase(Locale.ROOT);
        for (SeaCreature creature : SeaCreatureDatabase.all()) {
            if (!needle.isEmpty()) {
                if (creature.name().toLowerCase(Locale.ROOT).contains(needle)) {
                    visible.add(creature);
                }
            } else if (category.matches(creature)) {
                visible.add(creature);
            }
        }
        // cele mai rare primele, apoi alfabetic
        visible.sort((a, b) -> {
            int diff = b.rarityRank() - a.rarityRank();
            return diff != 0 ? diff : a.name().compareToIgnoreCase(b.name());
        });

        clampScroll();
        if (!visible.isEmpty() && (selected == null || !visible.contains(selected))) {
            select(visible.get(0));
        }
    }

    private void select(SeaCreature creature) {
        this.selected = creature;
        updateDetail();
    }

    private int maxScroll() {
        return Math.max(0, visible.size() - visibleRows);
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    private void bulk(boolean enabled) {
        for (SeaCreature creature : visible) {
            DeepSea.config().settings(creature.name()).enabled = enabled;
        }
        save();
        updateDetail();
    }

    private void toggle(SeaCreature creature) {
        CreatureSettings s = DeepSea.config().settings(creature.name());
        s.enabled = !s.enabled;
        save();
        if (creature == selected) {
            updateDetail();
        }
    }

    // ---- culoare ----

    private static String toHex(int argb) {
        return String.format(Locale.ROOT, "%06X", argb & 0xFFFFFF);
    }

    private static Integer parseHex(String text) {
        String t = text.startsWith("#") ? text.substring(1) : text;
        if (t.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(t, 16);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void onHexChanged(String value) {
        if (updatingHex || selected == null) {
            return;
        }
        Integer rgb = parseHex(value);
        if (rgb != null) {
            current().color = 0xFF000000 | rgb;
            save();
            hexBox.setTextColor(C_HEX_OK);
        } else if (isHexPrefix(value)) {
            hexBox.setTextColor(C_HEX_PARTIAL); // inca scrii (sunt necesare 6 cifre)
        } else {
            hexBox.setTextColor(C_HEX_INVALID); // caractere care nu sunt cifre hex
        }
    }

    /** True daca textul (cu sau fara '#') contine doar cifre hex si are cel mult 6 caractere. */
    private static boolean isHexPrefix(String text) {
        String t = text.startsWith("#") ? text.substring(1) : text;
        return t.length() <= 6 && t.matches("[0-9a-fA-F]*");
    }

    private void applyColor(int rgb) {
        if (selected == null) {
            return;
        }
        current().color = 0xFF000000 | rgb;
        save();
        setHexText(toHex(rgb));
    }

    private void setHexText(String text) {
        updatingHex = true;
        hexBox.setValue(text);
        hexBox.setTextColor(C_HEX_OK);
        updatingHex = false;
    }

    // ---- durate ----

    private static int step(int[] values, int current, int direction) {
        int index = 0;
        int best = Integer.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            int distance = Math.abs(values[i] - current);
            if (distance < best) {
                best = distance;
                index = i;
            }
        }
        index = Math.max(0, Math.min(values.length - 1, index + direction));
        return values[index];
    }

    private void stepDuration(int direction) {
        if (selected != null) {
            CreatureSettings s = current();
            s.durationMs = step(DURATIONS, s.durationMs, direction);
            save();
        }
    }

    private void stepAfter(int direction) {
        if (selected != null) {
            CreatureSettings s = current();
            s.afterDeathMs = step(AFTER_DEATH, s.afterDeathMs, direction);
            save();
        }
    }

    private static String seconds(int ms) {
        return String.format(Locale.ROOT, "%.1fs", ms / 1000.0);
    }

    /** Sincronizeaza widgeturile din panoul de detalii cu creatura selectata. */
    private void updateDetail() {
        if (selected == null || hexBox == null) {
            return;
        }
        CreatureSettings s = current();
        toggleButton.setMessage(Component.literal(s.enabled ? "\u00A7aACTIV  (ON)" : "\u00A77INACTIV  (OFF)"));
        setHexText(toHex(s.color));
        modeButton.setMessage(Component.literal(s.isUntilDead() ? "Mod: pana moare" : "Mod: timp fix"));
        labelButton.setMessage(Component.literal(s.showLabel ? "Nume pe HUD: DA" : "Nume pe HUD: NU"));
        afterMinus.active = s.isUntilDead();
        afterPlus.active = s.isUntilDead();
    }

    // =====================================================================
    //  DESEN
    // =====================================================================

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static void swatch(GuiGraphicsExtractor g, int x, int y, int size, int rgb) {
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
        g.fill(x, y, x + size, y + size, 0xFF000000 | rgb);
    }

    private String fit(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "..";
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end)) + this.font.width(ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    private int categoryRowHeight() {
        return Math.max(10, Math.min(15, (bodyBottom - bodyTop - 4) / CreatureCategory.values().length));
    }

    /** Fundalul panourilor; desenat inainte de widgeturi, ca butoanele sa ramana deasupra. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        graphics.fill(px, py, px + pw, py + ph, C_PANEL);
        outline(graphics, px, py, pw, ph, C_BORDER);
        graphics.fill(px + 1, py + TITLE_H, px + pw - 1, py + TITLE_H + 1, C_BORDER);
        graphics.fill(px + 1, bodyBottom + 1, px + pw - 1, bodyBottom + 2, C_BORDER);

        int bodyH = bodyBottom - bodyTop;
        graphics.fill(sideX, bodyTop, sideX + sideW, bodyTop + bodyH, C_SUB);
        outline(graphics, sideX, bodyTop, sideW, bodyH, C_BORDER);
        graphics.fill(listX, bodyTop, listX + listW, bodyTop + bodyH, C_SUB);
        outline(graphics, listX, bodyTop, listW, bodyH, C_BORDER);
        graphics.fill(detX - 5, bodyTop, detX - 4, bodyBottom, C_BORDER);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        drawTitle(graphics);
        drawSidebar(graphics, mouseX, mouseY);
        drawList(graphics, mouseX, mouseY);
        drawDetail(graphics);
    }

    private void drawTitle(GuiGraphicsExtractor g) {
        int enabled = 0;
        for (SeaCreature creature : SeaCreatureDatabase.all()) {
            if (DeepSea.config().settings(creature.name()).enabled) {
                enabled++;
            }
        }
        g.text(this.font, "DeepSea", px + 8, py + 8, 0xFF55FFFF, true);
        g.text(this.font, enabled + "/" + SeaCreatureDatabase.size() + " active",
            px + 8 + this.font.width("DeepSea") + 8, py + 8, C_MUTED, false);
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        CreatureCategory[] categories = CreatureCategory.values();
        int[] total = new int[categories.length];
        int[] on = new int[categories.length];
        for (SeaCreature creature : SeaCreatureDatabase.all()) {
            boolean enabled = DeepSea.config().settings(creature.name()).enabled;
            int index = CreatureCategory.of(creature).ordinal();
            total[index]++;
            total[CreatureCategory.ALL.ordinal()]++;
            if (enabled) {
                on[index]++;
                on[CreatureCategory.ALL.ordinal()]++;
            }
        }

        int rowHeight = categoryRowHeight();
        for (int i = 0; i < categories.length; i++) {
            int y = bodyTop + 2 + i * rowHeight;
            boolean selectedRow = query.isBlank() && categories[i] == category;
            if (selectedRow) {
                g.fill(sideX + 1, y, sideX + sideW - 1, y + rowHeight, C_ACCENT);
            } else if (inside(mouseX, mouseY, sideX, y, sideW, rowHeight)) {
                g.fill(sideX + 1, y, sideX + sideW - 1, y + rowHeight, C_HOVER);
            }

            String count = on[i] + "/" + total[i];
            int countWidth = this.font.width(count);
            int textY = y + (rowHeight - 8) / 2;
            g.text(this.font, fit(categories[i].label(), sideW - countWidth - 12), sideX + 5, textY,
                selectedRow ? 0xFFFFFFFF : C_TEXT, false);
            g.text(this.font, count, sideX + sideW - 4 - countWidth, textY, on[i] > 0 ? C_ON : C_MUTED, false);
        }
    }

    private void drawList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        String header = query.isBlank()
            ? category.label() + "  (" + visible.size() + ")"
            : "Cautare: \"" + query.trim() + "\"  (" + visible.size() + ")";
        g.text(this.font, fit(header, listW - 10), listX + 5, bodyTop + 4, C_MUTED, false);

        int rowRight = listX + listW - SCROLL_W - 5;

        if (visible.isEmpty()) {
            String none = "Nicio creatura gasita";
            g.text(this.font, none, listX + (listW - this.font.width(none)) / 2, rowsTop + 20, C_MUTED, false);
            return;
        }

        for (int i = 0; i < visibleRows; i++) {
            int index = scroll + i;
            if (index >= visible.size()) {
                break;
            }
            SeaCreature creature = visible.get(index);
            CreatureSettings s = DeepSea.config().settings(creature.name());
            int y = rowsTop + i * ROW_H;

            if (creature == selected) {
                g.fill(listX + 1, y, rowRight + 2, y + ROW_H, C_ACCENT);
            } else if (inside(mouseX, mouseY, listX, y, rowRight - listX + 2, ROW_H)) {
                g.fill(listX + 1, y, rowRight + 2, y + ROW_H, C_HOVER);
            }

            swatch(g, listX + 6, y + 3, 8, s.color & 0xFFFFFF);

            int pillX = rowRight - 24;
            g.fill(pillX, y + 2, pillX + 24, y + ROW_H - 2, s.enabled ? 0xFF1E6B1E : 0xFF3A3A3A);
            String pill = s.enabled ? "ON" : "OFF";
            g.text(this.font, pill, pillX + (24 - this.font.width(pill)) / 2, y + 3,
                s.enabled ? 0xFFFFFFFF : 0xFF9A9A9A, false);

            g.text(this.font, fit(creature.name(), pillX - (listX + 19) - 4), listX + 19, y + 3,
                creature.rarityColor(), false);
        }

        // bara de scroll
        if (maxScroll() > 0) {
            int trackH = visibleRows * ROW_H;
            int trackX = listX + listW - SCROLL_W - 2;
            g.fill(trackX, rowsTop, trackX + SCROLL_W, rowsTop + trackH, 0x40FFFFFF);
            int thumbH = Math.max(12, trackH * visibleRows / visible.size());
            int thumbY = rowsTop + (trackH - thumbH) * scroll / maxScroll();
            g.fill(trackX, thumbY, trackX + SCROLL_W, thumbY + thumbH, 0xFFB8C2D8);
        }
    }

    private void drawDetail(GuiGraphicsExtractor g) {
        if (selected == null) {
            String hint = "Alege o creatura";
            g.text(this.font, hint, detX + (detW - this.font.width(hint)) / 2, bodyTop + 40, C_MUTED, false);
            return;
        }
        CreatureSettings s = current();

        g.text(this.font, fit(selected.name(), detW), detX, bodyTop + 4, selected.rarityColor(), true);
        g.text(this.font, selected.rarityLabel() + " - " + selected.xp() + " XP", detX, bodyTop + 15, C_MUTED, false);

        // culoarea curenta + paleta rapida
        swatch(g, detX + 1, colorY + 1, 16, s.color & 0xFFFFFF);
        for (int i = 0; i < PALETTE.length; i++) {
            int x = detX + i * (paletteSize + 1);
            swatch(g, x, paletteY, paletteSize, PALETTE[i]);
            if ((s.color & 0xFFFFFF) == PALETTE[i]) {
                outline(g, x - 1, paletteY - 1, paletteSize + 2, paletteSize + 2, 0xFFFFFFFF);
            }
        }

        // durata / dupa moarte
        String durationLabel = s.isUntilDead() ? "Rezerva" : "Durata";
        g.text(this.font, durationLabel, detX, durY + 5, C_TEXT, false);
        drawValue(g, seconds(s.durationMs), durY, true);

        g.text(this.font, "Dupa moarte", detX, afterY + 5, s.isUntilDead() ? C_TEXT : 0xFF5A6070, false);
        drawValue(g, seconds(s.afterDeathMs), afterY, s.isUntilDead());
    }

    private void drawValue(GuiGraphicsExtractor g, String text, int y, boolean active) {
        int areaX = detX + detW - 68 + 18;
        int areaW = 32;
        g.text(this.font, text, areaX + (areaW - this.font.width(text)) / 2, y + 5,
            active ? 0xFFFFFFFF : 0xFF5A6070, false);
    }

    // =====================================================================
    //  MOUSE
    // =====================================================================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }
        double mx = event.x();
        double my = event.y();

        // categorii
        CreatureCategory[] categories = CreatureCategory.values();
        int rowHeight = categoryRowHeight();
        for (int i = 0; i < categories.length; i++) {
            if (inside(mx, my, sideX, bodyTop + 2 + i * rowHeight, sideW, rowHeight)) {
                category = categories[i];
                lastCategory = category;
                scroll = 0;
                if (!query.isEmpty()) {
                    query = "";
                    searchBox.setValue("");
                }
                rebuildVisible();
                return true;
            }
        }

        // bara de scroll
        int trackX = listX + listW - SCROLL_W - 2;
        int trackH = visibleRows * ROW_H;
        if (maxScroll() > 0 && inside(mx, my, trackX - 2, rowsTop, SCROLL_W + 4, trackH)) {
            draggingScrollbar = true;
            scrollToMouse(my);
            return true;
        }

        // randuri
        int rowRight = listX + listW - SCROLL_W - 5;
        if (inside(mx, my, listX, rowsTop, rowRight - listX + 2, trackH)) {
            int index = scroll + (int) ((my - rowsTop) / ROW_H);
            if (index >= 0 && index < visible.size()) {
                SeaCreature creature = visible.get(index);
                select(creature);
                if (mx >= rowRight - 26 || doubleClick) {
                    toggle(creature);
                }
                return true;
            }
        }

        // paleta
        if (selected != null) {
            for (int i = 0; i < PALETTE.length; i++) {
                if (inside(mx, my, detX + i * (paletteSize + 1), paletteY, paletteSize, paletteSize)) {
                    applyColor(PALETTE[i]);
                    return true;
                }
            }
        }
        return false;
    }

    private void scrollToMouse(double my) {
        int trackH = visibleRows * ROW_H;
        int thumbH = Math.max(12, trackH * visibleRows / Math.max(1, visible.size()));
        double ratio = (my - rowsTop - thumbH / 2.0) / Math.max(1, trackH - thumbH);
        scroll = (int) Math.round(Math.max(0, Math.min(1, ratio)) * maxScroll());
        clampScroll();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScrollbar) {
            scrollToMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inside(mouseX, mouseY, listX, bodyTop, listW, bodyBottom - bodyTop)) {
            scroll -= (int) Math.signum(scrollY) * 3;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // =====================================================================

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        save();
        super.removed();
    }
}
