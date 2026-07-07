package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;

import java.util.Optional;

/**
 * Decides whether a boss floor should use a hero echo instead of the default boss.
 */
public final class EchoReplacementDecider {

    public static final int[] BOSS_DEPTHS = new int[]{5, 10, 15, 20, 25};

    private EchoReplacementDecider() {}

    public static boolean isBossDepth(int depth) {
        for (int d : BOSS_DEPTHS) {
            if (d == depth) return true;
        }
        return false;
    }

    public interface EchoLookup {
        Optional<Echo> findEchoForDepth(int depth);
    }

    public static boolean shouldUseEchoBoss(int depth, EchoLookup lookup) {
        if (!isBossDepth(depth)) return false;
        if (lookup == null) return false;
        try {
            return lookup.findEchoForDepth(depth)
                    .filter(Echo::hasCombatData)
                    .isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}
