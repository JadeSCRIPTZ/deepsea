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

    /** 0 = COMMON ... 5 = MYTHIC (pentru sortare). */
    public int rarityRank() {
        return switch (rarity) {
            case "UNCOMMON" -> 1;
            case "RARE" -> 2;
            case "EPIC" -> 3;
            case "LEGENDARY" -> 4;
            case "MYTHIC" -> 5;
            default -> 0;
        };
    }

    /** Culoarea numelui in GUI (aleasa sa se citeasca bine pe fundal inchis). */
    public int rarityColor() {
        return switch (rarity) {
            case "UNCOMMON" -> 0xFF55FF55;
            case "RARE" -> 0xFF6F8FFF;
            case "EPIC" -> 0xFFB65CFF;
            case "LEGENDARY" -> 0xFFFFAA00;
            case "MYTHIC" -> 0xFFFF66FF;
            default -> 0xFFE0E0E0;
        };
    }

    public String rarityLabel() {
        return switch (rarity) {
            case "UNCOMMON" -> "Uncommon";
            case "RARE" -> "Rare";
            case "EPIC" -> "Epic";
            case "LEGENDARY" -> "Legendary";
            case "MYTHIC" -> "Mythic";
            default -> "Common";
        };
    }
}
