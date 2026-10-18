package ro.deepsea.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ro.deepsea.DeepSea;
import ro.deepsea.config.CreatureSettings;
import ro.deepsea.data.SeaCreature;
import ro.deepsea.data.SeaCreatureDatabase;
import ro.deepsea.gui.DeepSeaScreen;
import ro.deepsea.gui.HudEditScreen;
import ro.deepsea.logic.AlertManager;

/**
 * {@code /dpsea} — deschide GUI-ul; subcomenzi pentru toggle/culoare/test.
 */
public final class DeepSeaCommand {

    private DeepSeaCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("dpsea")
            .executes(context -> openGui())
            .then(ClientCommands.literal("edit")
                .executes(context -> openEdit()))
            .then(ClientCommands.literal("test")
                .executes(context -> {
                    AlertManager.showTest();
                    context.getSource().sendFeedback(Component.literal("§b[DeepSea] §7Test afisat."));
                    return 1;
                }))
            .then(ClientCommands.literal("toggle")
                .then(ClientCommands.argument("nume", StringArgumentType.greedyString())
                    .executes(context -> toggle(
                        context.getSource(),
                        StringArgumentType.getString(context, "nume")
                    ))))
            .then(ClientCommands.literal("color")
                .then(ClientCommands.argument("hex", StringArgumentType.word())
                    .then(ClientCommands.argument("nume", StringArgumentType.greedyString())
                        .executes(context -> color(
                            context.getSource(),
                            StringArgumentType.getString(context, "hex"),
                            StringArgumentType.getString(context, "nume")
                        )))))
        );
    }

    private static int openGui() {
        Minecraft.getInstance().setScreen(new DeepSeaScreen());
        return 1;
    }

    private static int openEdit() {
        Minecraft.getInstance().setScreen(new HudEditScreen(null));
        return 1;
    }

    private static int toggle(FabricClientCommandSource source, String name) {
        SeaCreature creature = SeaCreatureDatabase.byName(name);
        if (creature == null) {
            source.sendError(Component.literal("§c[DeepSea] Creatura nu exista: " + name));
            return 0;
        }
        CreatureSettings settings = DeepSea.config().settings(creature.name());
        settings.enabled = !settings.enabled;
        DeepSea.config().save();
        source.sendFeedback(Component.literal(
            "§b[DeepSea] §f" + creature.name() + " §7-> " + (settings.enabled ? "§aON" : "§cOFF")
        ));
        return 1;
    }

    private static int color(FabricClientCommandSource source, String hex, String name) {
        SeaCreature creature = SeaCreatureDatabase.byName(name);
        if (creature == null) {
            source.sendError(Component.literal("§c[DeepSea] Creatura nu exista: " + name));
            return 0;
        }
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        int rgb;
        try {
            rgb = Integer.parseInt(cleaned, 16);
        } catch (NumberFormatException exception) {
            source.sendError(Component.literal("§c[DeepSea] Culoare invalida: " + hex + " (foloseste #RRGGBB)"));
            return 0;
        }
        CreatureSettings settings = DeepSea.config().settings(creature.name());
        settings.color = 0xFF000000 | (rgb & 0xFFFFFF);
        DeepSea.config().save();
        source.sendFeedback(Component.literal(
            "§b[DeepSea] §f" + creature.name() + " §7-> culoare " + String.format("#%06X", settings.color & 0xFFFFFF)
        ));
        return 1;
    }
}
