package ro.deepsea;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
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

    private static DeepSeaConfig config = new DeepSeaConfig();

    public static DeepSeaConfig config() {
        return config;
    }

    @Override
    public void onInitializeClient() {
        SeaCreatureDatabase.load();
        config = DeepSeaConfig.load();
        LOGGER.info("DeepSea loaded: {} sea creatures in database", SeaCreatureDatabase.all().size());

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "alerts"), new AlertHud());

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!overlay) {
                handleChatLine(message.getString());
            }
            return true;
        });

        ClientCommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess) -> DeepSeaCommand.register(dispatcher)
        );
    }

    /**
     * Mesajul de la Hypixel vine ca text simplu (fara culori de obicei, dar curatam oricum)
     * si este comparat EXACT cu mesajele din baza de date.
     */
    private static void handleChatLine(String raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        String plain = ChatFormatting.stripFormatting(raw).trim();
        SeaCreature creature = SeaCreatureDatabase.byMessage(plain);
        if (creature == null) {
            return;
        }
        var settings = config.settings(creature.name());
        if (!settings.enabled) {
            return;
        }
        AlertManager.onCatch(creature, settings);
    }
}
