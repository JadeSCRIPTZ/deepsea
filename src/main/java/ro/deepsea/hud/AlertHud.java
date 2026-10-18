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

        int x = config.x;
        int y = config.y;
        int size = Math.max(4, config.boxSize);

        for (ActiveAlert alert : alerts) {
            // fara fade: patratul ramane la opacitate maxima pana expira, apoi dispare
            int argb = 0xFF000000 | (alert.color & 0xFFFFFF);

            drawSquare(graphics, x, y, size, argb);

            if (alert.label) {
                int textY = y + (size - 8) / 2;
                graphics.text(minecraft.font, alert.creature.name(), x + size + 6, textY, argb, config.textShadow);
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
