package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.DeviceCompat;

/** Debug-only toggles for quick testing in INDEV / desktop:debug builds. */
public final class DebugSettings {

	public static final int START_DEPTH = 5;
	public static final int START_LEVEL = 50;
	public static final int START_STR = 50;

	private static Boolean debugBuildOverride;
	private static Boolean debugStartOverride;
	private static Boolean weakEchoSnapshotsOverride;

	private DebugSettings() {}

	public static boolean isDebugBuild() {
		if (debugBuildOverride != null) {
			return debugBuildOverride;
		}
		return DeviceCompat.isDebug();
	}

	public static void setDebugBuildOverride(Boolean value) {
		debugBuildOverride = value;
	}

	public static boolean debugStart() {
		if (!isDebugBuild()) {
			return false;
		}
		if (debugStartOverride != null) {
			return debugStartOverride;
		}
		return SPDSettings.debugStart();
	}

	public static void setDebugStart(boolean value) {
		debugStartOverride = value;
		SPDSettings.debugStart(value);
	}

	public static boolean weakEchoSnapshots() {
		if (!isDebugBuild()) {
			return false;
		}
		if (weakEchoSnapshotsOverride != null) {
			return weakEchoSnapshotsOverride;
		}
		return SPDSettings.echoesWeakSnapshots();
	}

	public static void setWeakEchoSnapshots(boolean value) {
		weakEchoSnapshotsOverride = value;
		SPDSettings.echoesWeakSnapshots(value);
	}

	public static void applyDebugStart() {
		if (!debugStart()) {
			return;
		}

		if (START_DEPTH > 1) {
			Dungeon.depth = START_DEPTH;
			Statistics.deepestFloor = Math.max(Statistics.deepestFloor, Dungeon.depth - 1);
		}

		Hero hero = Dungeon.hero;
		if (hero == null) {
			return;
		}

		if (START_LEVEL > 1) {
			hero.debugSetLevel(START_LEVEL);
		}

		if (START_STR > Hero.STARTING_STR) {
			hero.STR = START_STR;
		}
	}

	public static void resetForTests() {
		debugBuildOverride = null;
		debugStartOverride = null;
		weakEchoSnapshotsOverride = null;
	}
}
