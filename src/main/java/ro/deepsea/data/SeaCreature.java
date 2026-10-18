package ro.deepsea.data;

/**
 * O creatura marina, asa cum apare in baza de date extrasa din SkyHanni.
 *
 * @param name    numele afisat (ex. "Lord Jawbus")
 * @param message mesajul EXACT din chat care anunta captura
 * @param rarity  COMMON / UNCOMMON / RARE / EPIC / LEGENDARY / MYTHIC
 * @param xp      fishing experience acordat
 * @param island  varianta/insula din care face parte
 */
public record SeaCreature(String name, String message, String rarity, int xp, String island) {

    /** Culoarea implicita a patratului, in functie de raritate (ARGB). */
    public int defaultColor() {
        return switch (rarity) {
            case "UNCOMMON" -> 0xFF55FF55;
            case "RARE" -> 0xFF5555FF;
            case "EPIC" -> 0xFFAA00AA;
            case "LEGENDARY" -> 0xFFFFAA00;
            case "MYTHIC" -> 0xFFFF55FF;
            default -> 0xFFB0B0B0;
        };
    }
}
