package com.shatteredpixel.shatteredpixeldungeon.items.wands;

/**
 * Same-package bridge for Echo wand adapters to reach protected charge APIs
 * without exposing Echo overloads on {@link Wand}.
 */
public final class WandBridge {

	private WandBridge() {
	}

	/**
	 * Decrement charges without {@link Wand#wandUsed()} hero turn / talent side
	 * effects (EchoBoss AI).
	 */
	public static void spendCharges(Wand wand) {
		wand.curCharges = Math.max(0, wand.curCharges - (wand.cursed ? 1 : wand.chargesPerCast()));
	}
}
