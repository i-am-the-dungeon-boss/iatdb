package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;

/**
 * Resolves a hero echo boss before the destination level is generated.
 * Called from {@link com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene}
 * so the whole floor waits for fetch/lookup to finish.
 */
public final class EchoBossPrefetch {

	private EchoBossPrefetch() {}

	public static boolean shouldPrefetch(int depth, int branch) {
		return branch == 0 && EchoReplacementDecider.isBossDepth(depth);
	}

	/**
	 * Blocks until echo lookup completes. Sets {@link Dungeon#getPendingEcho()} when successful.
	 *
	 * @return true if an echo boss will replace the regional boss on this depth
	 */
	public static boolean prefetch(int depth) {
		return Dungeon.prefetchEchoBossForDepth(depth);
	}
}
