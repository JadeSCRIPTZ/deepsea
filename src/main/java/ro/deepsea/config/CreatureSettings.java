package ro.deepsea.config;

import ro.deepsea.data.SeaCreatureDatabase;

/**
 * Setarile unei singure creaturi. Salvate in {@code config/deepsea.json}.
 */
public class CreatureSettings {

    /** Daca apare sau nu patratul cand prinzi creatura. */
    public boolean enabled = false;

    /** Culoarea patratului, ARGB (Alpha in byte-ul cel mai semnificativ). */
    public int color = 0xFFFFFFFF;

    /** Cat timp ramane patratul pe ecran, in milisecunde. */
    public int durationMs = 5000;

    /** Daca se scrie numele creaturii langa patrat. */
    public boolean showLabel = true;

    public static CreatureSettings defaultsFor(String name) {
        CreatureSettings settings = new CreatureSettings();
        var creature = SeaCreatureDatabase.byName(name);
        settings.color = creature != null ? creature.defaultColor() : 0xFFFFFFFF;
        return settings;
    }
}
