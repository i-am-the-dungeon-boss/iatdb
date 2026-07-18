package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineSync;

/**
 * Decides when a boss victory should persist a hero echo and performs the save.
 */
public final class EchoCaptureTrigger {

	private EchoCaptureTrigger() {}

	public static boolean shouldCapture(int depth, boolean heroAlive) {
		return heroAlive
				&& com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider.isBossDepth(depth);
	}

	/** Called from boss {@code die()} when the player wins on a boss floor. */
	public static void onBossDefeated() {
		captureBossVictory(
				Dungeon.hero,
				Dungeon.depth,
				new EchoStorage()
		);
	}

	public static void captureBossVictory(Hero hero, int depth, EchoStorage storage) {
		if (!shouldCapture(depth, hero != null && hero.isAlive())) {
			return;
		}
		if (storage == null) {
			return;
		}
        Echo echo = Echo.fromHero(
				hero,
				depth,
				com.watabou.noosa.Game.version,
				Dungeon.seed
		);
		EchoSnapshotDebug.applyIfEnabled(echo);
		if (Dungeon.echoPlayMode != EchoPlayMode.RANKED) {
			storage.save(echo);
		}
		EchoOnlineSync.instance().uploadEchoAsync(echo);
	}

	public static void saveEcho(Echo echo, int depth, EchoStorage storage) {
		if (!shouldCapture(depth, echo != null) || storage == null || echo == null) {
			return;
		}
		echo.depth = depth;
		storage.save(echo);
	}
}
