package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.Bundle;

/** Debug helpers for weakening captured echo snapshots during testing. */
public final class EchoSnapshotDebug {

	public static final int WEAK_LEVEL = 10;
	public static final int WEAK_HT = 200;
	public static final int WEAK_STR = 10;

	private EchoSnapshotDebug() {
	}

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
		// Always full health — current HP tracks max HT.
		echo.ht = WEAK_HT;
		echo.hp = WEAK_HT;

		if (echo.echoData == null) {
			return;
		}

		try {
			// Must go through EchoHeroSnapshot so Dungeon.quickslot / ActionIndicator stay
			// intact.
			Hero hero = EchoHeroSnapshot.restoreHero(echo);
			if (hero == null) {
				return;
			}
			hero.lvl = WEAK_LEVEL;
			hero.HT = WEAK_HT;
			hero.HP = WEAK_HT;
			hero.STR = WEAK_STR;

			Bundle data = new Bundle();
			hero.storeInBundle(data);
			echo.echoData = data;
		} catch (Throwable ignored) {
		}
	}
}
