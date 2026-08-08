package com.shatteredpixel.shatteredpixeldungeon.items.bombs;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;

/**
 * Same-package bridge so Echo throw adapters can light-throw without editing
 * upstream Hero {@link Bomb#execute} / {@code cast} flow.
 */
public final class BombEchoBridge {

	private BombEchoBridge() {
	}

	/**
	 * Echo always light-throws (like {@link #AC_LIGHTTHROW}). Hero lights only when
	 * {@link #execute} already set {@link #lightingFuse} for LIGHTTHROW — plain
	 * THROW must land unlit.
	 */
	public static void markEchoLightThrow(Bomb bomb) {
		if (bomb == null) {
			return;
		}
		Bomb.lightingFuse = true;
		bomb.igniteWhenThrown = true;
	}

	/**
	 * Missile VFX can delay {@link Item#onThrow}; re-assert light-fuse so a
	 * cleared static {@link Bomb#lightingFuse} mid-flight still yields a live
	 * bomb when this throw was meant to ignite.
	 */
	public static void reassertLightFuse(Bomb bomb) {
		if (bomb != null && bomb.igniteWhenThrown) {
			Bomb.lightingFuse = true;
		}
	}

	/**
	 * Mark the detached (possibly split) bomb so ignite survives static clears.
	 * Hook after detach and before {@link Item#onThrow} so subclasses can mark
	 * the specific thrown instance (important for stack splits).
	 */
	public static void markDetachedIgnite(Bomb source, Item thrown) {
		if (source != null && thrown instanceof Bomb && source.igniteWhenThrown) {
			((Bomb) thrown).igniteWhenThrown = true;
		}
	}
}
