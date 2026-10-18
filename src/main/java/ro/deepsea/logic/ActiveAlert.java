package ro.deepsea.logic;

import net.minecraft.world.phys.Vec3;
import ro.deepsea.data.SeaCreature;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * O alerta activa: creatura prinsa, cum arata, si mobii urmariti in lume.
 */
public final class ActiveAlert {

    public final SeaCreature creature;
    public final int color;
    public final boolean label;
    public final boolean untilDead;
    public final int durationMs;
    public final int afterDeathMs;
    public final long startedAt;

    /** Cati mobi trebuie gasiti (2 la double hook). Poate creste daca prinzi din nou aceeasi creatura. */
    public int expected;
    public long findDeadline;

    /** Id-urile entitatilor urmarite (mob + nametag). */
    public final Set<Integer> entityIds = new LinkedHashSet<>();

    public boolean foundAny;
    public long allDeadAt = -1;
    public Vec3 lastPos;

    public ActiveAlert(
        SeaCreature creature,
        int color,
        boolean label,
        boolean untilDead,
        int durationMs,
        int afterDeathMs,
        int expected
    ) {
        this.creature = creature;
        this.color = color;
        this.label = label;
        this.untilDead = untilDead;
        this.durationMs = Math.max(500, durationMs);
        this.afterDeathMs = Math.max(0, afterDeathMs);
        this.expected = Math.max(1, expected);
        this.startedAt = System.currentTimeMillis();
        this.findDeadline = this.startedAt + 4000L;
    }

    public boolean visible(long now) {
        if (!untilDead) {
            return now < startedAt + durationMs;
        }
        if (foundAny) {
            if (allDeadAt < 0) {
                return true; // macar un mob e inca in viata
            }
            return now < allDeadAt + afterDeathMs;
        }
        // nu am gasit mobul (poate a murit instant sau e prea departe): fallback pe timp
        return now < findDeadline + durationMs;
    }

    /** Pentru fade-out: cand expira. */
    public long expiresAt() {
        if (!untilDead) {
            return startedAt + durationMs;
        }
        if (foundAny && allDeadAt >= 0) {
            return allDeadAt + afterDeathMs;
        }
        return findDeadline + durationMs;
    }
}
