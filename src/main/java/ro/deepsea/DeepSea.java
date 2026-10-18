package ro.deepsea;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.deepsea.command.DeepSeaCommand;
import ro.deepsea.config.DeepSeaConfig;
import ro.deepsea.data.SeaCreature;
import ro.deepsea.data.SeaCreatureDatabase;
import ro.deepsea.hud.AlertHud;
import ro.deepsea.logic.AlertManager;

/**
 * DeepSea — alerte vizuale pentru Sea Creature-urile prinse in Hypixel SkyBlock.
 */
public final class DeepSea implements ClientModInitializer {

    public static final String MOD_ID = "deepsea";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Mesajul de double hook vine inaintea mesajului de catch. */
    private static final String DOUBLE_HOOK = "It's a Double Hook!";
    private static final long DOUBLE_HOOK_WINDOW_MS = 1500L;

    private static DeepSeaConfig config = new DeepSeaConfig();
    private static long lastDoubleHookMs = 0L;
    private static boolean announced = false;

    public static DeepSeaConfig config() {
        return config;
    }

    @Override
    public void onInitializeClient() {
        SeaCreatureDatabase.load();
        AlertManager.reloadDatabase();
        config = DeepSeaConfig.load();
        LOGGER.info("DeepSea loaded with {} sea creatures", SeaCreatureDatabase.size());

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "alerts"), new AlertHud());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AlertManager.tick();
            if (!announced && client.player != null) {
                announced = true;
                client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§b[DeepSea] §7activat. Scrie §f/dpsea§7 pentru setari."
                ));
            }
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!overlay) {
                try {
                    handleChatLine(message.getString());
                } catch (Throwable throwable) {
                    LOGGER.error("DeepSea chat handling failed", throwable);
                }
            }
            return true;
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LOGGER.info("Registering /dpsea command");
            DeepSeaCommand.register(dispatcher);
        });
    }

    /**
     * Mesajul de la Hypixel vine ca text simplu si este comparat EXACT cu mesajele
     * din baza de date (dupa ce scoatem codurile de culoare).
     */
    private static void handleChatLine(String raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        String plain = ChatFormatting.stripFormatting(raw).trim();

        if (plain.startsWith(DOUBLE_HOOK)) {
            lastDoubleHookMs = System.currentTimeMillis();
            return;
        }

        SeaCreature creature = SeaCreatureDatabase.byMessage(plain);
        if (creature == null) {
            return;
        }
        var settings = config.settings(creature.name());
        if (!settings.enabled) {
            return;
        }

        boolean doubleHook = System.currentTimeMillis() - lastDoubleHookMs <= DOUBLE_HOOK_WINDOW_MS;
        lastDoubleHookMs = 0L;

        AlertManager.onCatch(creature, settings, doubleHook ? 2 : 1);
    }
}
