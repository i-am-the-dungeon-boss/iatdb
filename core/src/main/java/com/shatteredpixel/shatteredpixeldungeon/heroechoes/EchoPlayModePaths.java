package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;

/**
 * File-system namespaces for ranked vs solo hero-echoes data,
 * with an optional easy-mode suffix so easy and normal pools stay separate.
 */
public final class EchoPlayModePaths {

	private static final String ECHOES_BASE = "echoes";
	private static final String LEADERBOARD_BASE = "leaderboard";

	private EchoPlayModePaths() {
	}

	public static EchoPlayMode storageMode() {
		if (Dungeon.echoPlayMode != null) {
			return Dungeon.echoPlayMode;
		}
		if (GamesInProgress.selectedEchoPlayMode != null) {
			return GamesInProgress.selectedEchoPlayMode;
		}
		return EchoPlayMode.SOLO;
	}

	public static String easySuffix() {
		return Dungeon.easyMode ? "-easy" : "";
	}

	public static String gameFolderSuffix(EchoPlayMode mode) {
		if (mode == EchoPlayMode.RANKED) {
			return "-ranked";
		}
		if (mode == EchoPlayMode.DEBUG) {
			return "-debug";
		}
		return "-solo";
	}

	public static String gameFolderSuffix() {
		return gameFolderSuffix(storageMode());
	}

	public static String echoesDir(EchoPlayMode mode) {
		if (mode == null) {
			mode = EchoPlayMode.SOLO;
		}
		return ECHOES_BASE + gameFolderSuffix(mode) + easySuffix();
	}

	public static String echoesDir() {
		return echoesDir(storageMode());
	}

	public static String leaderboardFile(EchoPlayMode mode) {
		if (mode == null) {
			mode = EchoPlayMode.SOLO;
		}
		return LEADERBOARD_BASE + gameFolderSuffix(mode) + easySuffix() + ".json";
	}

	public static String leaderboardFile() {
		return leaderboardFile(storageMode());
	}

	public static boolean persistsLeaderboardLocally(EchoPlayMode mode) {
		return mode != EchoPlayMode.RANKED && mode != EchoPlayMode.DEBUG;
	}

	public static boolean persistsLeaderboardLocally() {
		return persistsLeaderboardLocally(storageMode());
	}
}
