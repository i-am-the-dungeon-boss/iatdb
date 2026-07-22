package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.GameMath;

/** Debug-only toggles for quick testing in INDEV / desktop:debug builds. */
public final class DebugSettings {

	/** Floors 1–26 for debug start (includes ascending depth). */
	public static final int MIN_START_DEPTH = 1;
	public static final int MAX_START_DEPTH = 26;
	/**
	 * Default: floor before the fourth boss so descending triggers a real echo
	 * prefetch.
	 */
	public static final int DEFAULT_START_DEPTH = 19;
	public static final int START_LEVEL = 100;
	public static final int START_STR = 100;
	/**
	 * Passed to {@link Dungeon#switchLevel} to place the hero on the down stairs.
	 */
	public static final int START_AT_EXIT = -2;

	private static Boolean debugBuildOverride;
	private static Boolean debugStartOverride;
	private static Integer startDepthOverride;
	private static Boolean weakEchoSnapshotsOverride;

	private DebugSettings() {
	}

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

	public static int startDepth() {
		if (startDepthOverride != null) {
			return clampStartDepth(startDepthOverride);
		}
		return SPDSettings.debugStartDepth();
	}

	public static void setStartDepth(int depth) {
		int clamped = clampStartDepth(depth);
		startDepthOverride = clamped;
		SPDSettings.debugStartDepth(clamped);
	}

	public static int clampStartDepth(int depth) {
		return (int) GameMath.gate(MIN_START_DEPTH, depth, MAX_START_DEPTH);
	}

	/** Label for the debug start-depth slider, including the chosen floor. */
	public static String startDepthTitle(int depth) {
		return Messages.get("windows.wndsettings$uitab.debug_start_depth", clampStartDepth(depth));
	}

	/** True when a live debug run can jump floors (hero + level present). */
	public static boolean canJumpToFloor() {
		return isDebugBuild() && Dungeon.hero != null && Dungeon.level != null;
	}

	/** Title for the shared start/jump depth slider. */
	public static String depthSliderTitle(int depth) {
		int clamped = clampStartDepth(depth);
		if (canJumpToFloor()) {
			return Messages.get("windows.wndsettings$uitab.debug_jump_depth", clamped);
		}
		return startDepthTitle(clamped);
	}

	/**
	 * Slider is usable for jump in-run, or for start depth when quick-start is on.
	 */
	public static boolean depthSliderEnabled() {
		return canJumpToFloor() || debugStart();
	}

	/**
	 * Current slider value: live floor in-run, otherwise configured start depth.
	 */
	public static int depthSliderValue() {
		if (canJumpToFloor()) {
			return clampStartDepth(Dungeon.depth);
		}
		return startDepth();
	}

	/**
	 * Applies the depth slider: stores start depth always; in a live run also arms
	 * a
	 * RETURN jump (abandoning a sealed echo boss floor first when needed).
	 *
	 * @return true if a jump was armed
	 */
	public static boolean applyDepthSlider(int depth) {
		int clamped = clampStartDepth(depth);
		setStartDepth(clamped);
		if (!canJumpToFloor()) {
			return false;
		}
		if (Dungeon.depth == clamped && Dungeon.branch == 0) {
			return false;
		}
		if (Dungeon.shouldRetreatEchoBossOnContinue(Dungeon.level)) {
			Dungeon.abandonSealedEchoBossFloor();
		}
		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = clamped;
		InterlevelScene.returnBranch = 0;
		InterlevelScene.returnPos = START_AT_EXIT;
		return true;
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

		int depth = startDepth();
		if (depth > 1) {
			Dungeon.depth = depth;
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

	/**
	 * Hero spawn cell for a new run: exit stairs when debug start is on, otherwise
	 * default entrance placement ({@code -1}).
	 */
	public static int debugStartHeroPos() {
		return debugStart() ? START_AT_EXIT : -1;
	}

	public static void resetForTests() {
		// Force debug off in unit tests — do not fall through to
		// DeviceCompat.isDebug().
		debugBuildOverride = false;
		debugStartOverride = false;
		startDepthOverride = null;
		weakEchoSnapshotsOverride = false;
		SPDSettings.debugStart(false);
		SPDSettings.debugStartDepth(DEFAULT_START_DEPTH);
		SPDSettings.echoesWeakSnapshots(false);
	}
}
