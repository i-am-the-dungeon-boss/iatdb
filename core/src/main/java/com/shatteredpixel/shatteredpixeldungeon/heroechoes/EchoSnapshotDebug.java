package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.Bundle;

/** Debug helpers for weakening captured echo snapshots during testing. */
public final class EchoSnapshotDebug {

	public static final int WEAK_LEVEL = 1;
	public static final int WEAK_HP = 1;
	public static final int WEAK_HT = 5;
	public static final int WEAK_STR = 10;

	private EchoSnapshotDebug() {}

	public static void applyIfEnabled(Echo echo) {
		if (DebugSettings.weakEchoSnapshots()) {
			weaken(echo);
		}
	}

	public static void weaken(Echo echo) {
		if (echo == null) {
			return;
		}

		echo.lvl = WEAK_LEVEL;
		echo.hp = WEAK_HP;
		echo.ht = WEAK_HT;

		if (echo.echoData == null) {
			return;
		}

		try {
			Hero hero = new Hero();
			hero.live();
			hero.restoreFromBundle(echo.echoData);
			hero.lvl = WEAK_LEVEL;
			hero.HP = WEAK_HP;
			hero.HT = WEAK_HT;
			hero.STR = WEAK_STR;

			Bundle data = new Bundle();
			hero.storeInBundle(data);
			echo.echoData = data;
		} catch (Throwable ignored) {
		}
	}
}
