package ro.deepsea.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import ro.deepsea.DeepSea;
import ro.deepsea.config.DeepSeaConfig;
import ro.deepsea.logic.ActiveAlert;
import ro.deepsea.logic.AlertManager;

import java.util.List;

/**
 * Deseneaza patratele colorate + numele creaturilor, la pozitia aleasa de utilizator.
 * Poate fi tras cu mouse-ul din {@code /dpsea edit}.
 */
public final class AlertHud implements HudElement {

    private static final int FADE_MS = 600;

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        List<ActiveAlert> alerts = AlertManager.visible();
        if (alerts.isEmpty()) {
            return;
        }

        DeepSeaConfig config = DeepSea.config();
        long now = System.currentTimeMillis();

        int x = config.x;
        int y = config.y;
        int size = Math.max(4, config.boxSize);

        for (ActiveAlert alert : alerts) {
            long remaining = alert.expiresAt() - now;
            int alpha = remaining < FADE_MS ? (int) Math.max(0, remaining * 255L / FADE_MS) : 255;
            int argb = (alpha << 24) | (alert.color & 0xFFFFFF);

            drawSquare(graphics, x, y, size, argb);

            if (alert.label) {
                int textColor = (Math.max(alpha, 70) << 24) | (alert.color & 0xFFFFFF);
                int textY = y + (size - 8) / 2;
                graphics.text(minecraft.font, alert.creature.name(), x + size + 6, textY, textColor, config.textShadow);
            }

            y += size + config.gap;
        }
    }

    /** Patrat umplut + rama neagra semi-transparenta, ca sa fie vizibil pe orice fundal. */
    public static void drawSquare(GuiGraphicsExtractor graphics, int x, int y, int size, int argb) {
        graphics.fill(x, y, x + size, y + size, argb);

        int frame = 0x90000000;
        graphics.fill(x - 1, y - 1, x + size + 1, y, frame);
        graphics.fill(x - 1, y + size, x + size + 1, y + size + 1, frame);
        graphics.fill(x - 1, y, x, y + size, frame);
        graphics.fill(x + size, y, x + size + 1, y + size, frame);
    }
}
