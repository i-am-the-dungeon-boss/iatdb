package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineService;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineSettings;
import com.watabou.utils.Bundle;

import java.io.File;

/** Shared fixtures and cleanup for hero-echoes workflow tests. */
public final class EchoTestSupport {

	public static final String ECHOES_DIR = "echoes";
	public static final String LEADERBOARD_FILE = "leaderboard.json";
	public static final int TEST_GAME_VERSION = 846;

	private EchoTestSupport() {}

	public static void resetWorkflowState() {
		deleteRecursively(new File("echoes"));
		deleteRecursively(new File("echoes-solo"));
		deleteRecursively(new File("echoes-ranked"));
		new File("leaderboard.json").delete();
		new File("leaderboard-solo.json").delete();
		new File("leaderboard-ranked.json").delete();
		Dungeon.hero = null;
		Dungeon.setEchoLookup(null);
		Dungeon.resetEchoStateForTests();
		GamesInProgress.clearSlotCache();
		GamesInProgress.selectedEchoPlayMode = EchoPlayMode.NONE;
		DebugSettings.resetForTests();
		DebugSettings.setWeakEchoSnapshots(false);
		SPDSettings.echoesWeakSnapshots(false);
		SPDSettings.playerName("");
		EchoOnlineSettings.resetForTests();
		EchoOnlineService.resetForTests();
	}

	public static Echo warriorEcho(int depth) {
		return Echo.create(
				depth,
				TEST_GAME_VERSION,
				12345L,
				"WARRIOR",
				6,
				28,
				30,
				null
		);
	}

	/** Echo with bundled hero data — required for echo boss spawn and combat. */
	public static Echo warriorEchoWithData(int depth) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return Echo.create(
				depth,
				TEST_GAME_VERSION,
				12345L,
				"WARRIOR",
				6,
				28,
				30,
				bundleHero(hero)
		);
	}

	public static Echo echoWithVersion(int depth, int gameVersion) {
		Echo snap = warriorEcho(depth);
		snap.gameVersion = gameVersion;
		return snap;
	}

	public static int countEchoFiles() {
		File dir = new File(EchoPlayModePaths.echoesDir());
		if (!dir.exists()) return 0;
		String[] files = dir.list();
		return files == null ? 0 : files.length;
	}

	public static void deleteRecursively(File file) {
		if (file == null || !file.exists()) return;
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) deleteRecursively(child);
			}
		}
		//noinspection ResultOfMethodCallIgnored
		file.delete();
	}

	public static boolean bundleHasHeroData(Bundle bundle) {
		return bundle != null && bundle.contains("echo_data");
	}

	public static Bundle bundleHero(Hero hero) {
		return EchoHeroSnapshot.captureFromHero(hero);
	}
}
