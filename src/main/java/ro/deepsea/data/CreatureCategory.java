package ro.deepsea.data;

import java.util.Set;

/**
 * Categoriile din GUI. Fiecare categorie grupeaza una sau mai multe "insule"/zone din baza de date.
 */
public enum CreatureCategory {
    ALL("Toate"),
    WATER("Apa", "WATER"),
    WATER_HOTSPOT("Hotspot apa", "WATER_HOTSPOT"),
    LAVA("Lava", "LAVA_CRIMSON_ISLE"),
    LAVA_HOTSPOT("Hotspot lava", "LAVA_HOTSPOT"),
    MINES("Mine & Cristale",
        "ABANDONED_QUARRY", "GOBLIN_BURROWS", "MAGMA_FIELDS", "WATER_CRYSTAL_HOLLOWS", "LAVA_PRECURSOR", "PLHLEGBLAST"),
    BAYOU("Backwater Bayou", "BACKWATER_BAYOU"),
    LOTUS("Lotus Atoll", "LOTUS_ATOLL"),
    MOONGLADE("Moonglade Marsh", "MOONGLADE_MARSH"),
    TORRHUS("Torrhus Canyon", "TORRHUS_CANYON"),
    SPOOKY("Spooky", "SPOOKY"),
    WINTER("Winter Island", "WINTER_ISLAND"),
    SHARK("Rechini", "SHARK"),
    OTHER("Diverse", "CARROT", "CHUMCAP", "OASIS", "PARK");

    private final String label;
    private final Set<String> islands;

    CreatureCategory(String label, String... islands) {
        this.label = label;
        this.islands = Set.of(islands);
    }

    public String label() {
        return label;
    }

    /** Categoria unei creaturi; zonele necunoscute ajung la "Diverse". */
    public static CreatureCategory of(SeaCreature creature) {
        for (CreatureCategory category : values()) {
            if (category != ALL && category.islands.contains(creature.island())) {
                return category;
            }
        }
        return OTHER;
    }

    public boolean matches(SeaCreature creature) {
        return this == ALL || of(creature) == this;
    }
}
