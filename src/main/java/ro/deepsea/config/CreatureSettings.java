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

    /** Daca se scrie numele creaturii langa patrat. */
    public boolean showLabel = true;

    /**
     * UNTIL_DEAD = patratul ramane cat timp creatura (sau creaturile, la double hook) e in viata,
     * plus {@link #afterDeathMs} dupa ce moare.
     * TIMED = patratul ramane exact {@link #durationMs}, indiferent de mob.
     */
    public String mode = "UNTIL_DEAD";

    /** Durata in modul TIMED. In modul UNTIL_DEAD e folosita doar daca mobul nu e gasit. */
    public int durationMs = 6000;

    /** Cat timp ramane patratul dupa ce ultimul mob urmarit a murit. */
    public int afterDeathMs = 1500;

    public boolean isUntilDead() {
        return !"TIMED".equalsIgnoreCase(mode);
    }

    public void toggleMode() {
        mode = isUntilDead() ? "TIMED" : "UNTIL_DEAD";
    }

    public static CreatureSettings defaultsFor(String name) {
        CreatureSettings settings = new CreatureSettings();
        var creature = SeaCreatureDatabase.byName(name);
        settings.color = creature != null ? creature.defaultColor() : 0xFFFFFFFF;
        return settings;
    }
}
