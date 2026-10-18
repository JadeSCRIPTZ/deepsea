package ro.deepsea.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ro.deepsea.DeepSea;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Baza de date cu sea creature-uri, incarcata din resursa bundled
 * {@code assets/deepsea/sea_creatures.json}.
 */
public final class SeaCreatureDatabase {

    private static final List<SeaCreature> ALL = new ArrayList<>();
    private static final Map<String, SeaCreature> BY_MESSAGE = new LinkedHashMap<>();
    private static final Map<String, SeaCreature> BY_NAME = new LinkedHashMap<>();

    private SeaCreatureDatabase() {
    }

    public static void load() {
        ALL.clear();
        BY_MESSAGE.clear();
        BY_NAME.clear();

        String path = "/assets/deepsea/sea_creatures.json";
        try (InputStream in = SeaCreatureDatabase.class.getResourceAsStream(path)) {
            if (in == null) {
                DeepSea.LOGGER.error("Missing resource {}", path);
                return;
            }
            JsonObject root = JsonParser.parseReader(
                new InputStreamReader(in, StandardCharsets.UTF_8)
            ).getAsJsonObject();

            for (JsonElement element : root.getAsJsonArray("creatures")) {
                JsonObject object = element.getAsJsonObject();
                SeaCreature creature = new SeaCreature(
                    object.get("name").getAsString(),
                    object.get("message").getAsString(),
                    object.get("rarity").getAsString(),
                    object.get("xp").getAsInt(),
                    object.get("island").getAsString()
                );
                ALL.add(creature);
                BY_MESSAGE.put(creature.message(), creature);
                BY_NAME.put(creature.name().toLowerCase(Locale.ROOT), creature);
            }
        } catch (Exception exception) {
            DeepSea.LOGGER.error("Could not load sea creature database", exception);
        }
    }

    public static SeaCreature byMessage(String message) {
        return BY_MESSAGE.get(message);
    }

    public static SeaCreature byName(String name) {
        return BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    public static List<SeaCreature> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static int size() {
        return ALL.size();
    }
}
