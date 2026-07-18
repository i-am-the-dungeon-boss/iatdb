package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoFetchResult;

import java.util.Optional;

/**
 * Boss-depth helpers and the echo lookup contract used for hero-boss
 * replacement.
 */
public final class EchoReplacementDecider {

    public static final int[] BOSS_DEPTHS = new int[] { 5, 10, 15, 20, 25 };

    private EchoReplacementDecider() {
    }

    public static boolean isBossDepth(int depth) {
        for (int d : BOSS_DEPTHS) {
            if (d == depth)
                return true;
        }
        return false;
    }

    public interface EchoLookup {
        Optional<EchoFetchResult> findEchoForDepth(int depth);
    }
}
