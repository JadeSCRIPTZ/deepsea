package ro.deepsea.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import ro.deepsea.DeepSea;
import ro.deepsea.config.DeepSeaConfig;
import ro.deepsea.hud.AlertHud;

/**
 * Ecranul in care misti patratul cu mouse-ul. Pozitia se salveaza automat la eliberarea click-ului.
 */
public final class HudEditScreen extends Screen {

    private final Screen parent;

    private boolean dragging;
    private double grabOffsetX;
    private double grabOffsetY;

    public HudEditScreen(Screen parent) {
        super(Component.literal("DeepSea - muta HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("Gata"), button -> this.onClose())
            .bounds(this.width / 2 - 106, this.height - 28, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("-"), button -> this.resize(-2))
            .bounds(this.width / 2 + 2, this.height - 28, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), button -> this.resize(2))
            .bounds(this.width / 2 + 26, this.height - 28, 20, 20).build());
    }

    private void resize(int delta) {
        DeepSeaConfig config = DeepSea.config();
        config.boxSize = Math.max(8, Math.min(80, config.boxSize + delta));
        config.save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        DeepSeaConfig config = DeepSea.config();
        int size = config.boxSize;

        // fundal intunecat usor, ca HUD-ul sa fie vizibil
        graphics.fill(0, 0, this.width, this.height, 0x50000000);

        AlertHud.drawSquare(graphics, config.x, config.y, size, 0xFFFFAA00);

        if (config.showLabel) {
            graphics.text(this.font, "Thunder", config.x + size + 6, config.y + (size - 8) / 2, 0xFFFFAA00, true);
        }

        // rama alba in jurul zonei de tras
        int left = config.x - 2;
        int top = config.y - 2;
        int right = config.x + size + (config.showLabel ? 80 : 2);
        int bottom = config.y + size + 2;
        graphics.fill(left, top, right, top + 1, 0xFFFFFFFF);
        graphics.fill(left, bottom - 1, right, bottom, 0xFFFFFFFF);
        graphics.fill(left, top, left + 1, bottom, 0xFFFFFFFF);
        graphics.fill(right - 1, top, right, bottom, 0xFFFFFFFF);

        graphics.text(this.font, "Trage patratul cu mouse-ul", this.width / 2 - 106, 12, 0xFFFFFFFF, true);
        graphics.text(
            this.font,
            "§7Pozitie: §f" + config.x + ", " + config.y + "   §7Marime: §f" + size,
            this.width / 2 - 106, 26, 0xFFFFFFFF, true
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            DeepSeaConfig config = DeepSea.config();
            int size = config.boxSize;
            int right = config.x + size + (config.showLabel ? 90 : 4);
            int bottom = config.y + size + 4;
            if (event.x() >= config.x - 4 && event.x() <= right
                && event.y() >= config.y - 4 && event.y() <= bottom) {
                this.dragging = true;
                this.grabOffsetX = event.x() - config.x;
                this.grabOffsetY = event.y() - config.y;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.dragging) {
            DeepSeaConfig config = DeepSea.config();
            config.x = (int) Math.max(0, Math.min(this.width - config.boxSize, event.x() - this.grabOffsetX));
            config.y = (int) Math.max(0, Math.min(this.height - config.boxSize, event.y() - this.grabOffsetY));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.dragging) {
            this.dragging = false;
            DeepSea.config().save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        DeepSea.config().save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
