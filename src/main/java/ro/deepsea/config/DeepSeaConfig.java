package ro.deepsea.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ro.deepsea.DeepSea;
import ro.deepsea.data.SeaCreatureDatabase;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configul modului: pozitia HUD-ului (mutata cu mouse-ul) + setarile fiecarei creaturi.
 */
public final class DeepSeaConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Pozitia patratului pe ecran (coordonate GUI, ca la minecraft screen). */
    public int x = 10;
    public int y = 10;

    /** Latura patratului, in pixeli GUI. */
    public int boxSize = 22;

    /** Spatiul dintre alertele afisate simultan. */
    public int gap = 6;

    /** Cate alerte pot fi afisate simultan. */
    public int maxAlerts = 5;

    /** Daca numele creaturii se scrie langa patrat (global). */
    public boolean showLabel = true;

    /** Umbra la text. */
    public boolean textShadow = true;

    /** Setarile fiecarei creaturi, cheia = numele creaturii. */
    public Map<String, CreatureSettings> creatures = new LinkedHashMap<>();

    public static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("deepsea.json");
    }

    public static DeepSeaConfig load() {
        DeepSeaConfig config = null;
        try {
            Path path = file();
            if (Files.exists(path)) {
                config = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), DeepSeaConfig.class);
            }
        } catch (Exception exception) {
            DeepSea.LOGGER.error("Could not read {}", file(), exception);
        }

        if (config == null) {
            config = new DeepSeaConfig();
        }
        if (config.creatures == null) {
            config.creatures = new LinkedHashMap<>();
        }

        for (var creature : SeaCreatureDatabase.all()) {
            config.creatures.computeIfAbsent(creature.name(), CreatureSettings::defaultsFor);
        }
        config.save();
        return config;
    }

    public void save() {
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            DeepSea.LOGGER.error("Could not save {}", file(), exception);
        }
    }

    public CreatureSettings settings(String name) {
        return creatures.computeIfAbsent(name, CreatureSettings::defaultsFor);
    }
}
