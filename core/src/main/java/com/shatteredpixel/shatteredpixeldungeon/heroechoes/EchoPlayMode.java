package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;

/**
 * How hero echoes are sourced for a run.
 */
public enum EchoPlayMode {
	NONE,
	RANKED,
	SOLO,
	/** Sandbox arena — debug builds only; never for release. */
	DEBUG;

	/** False for {@link #DEBUG} outside {@link DebugSettings#isDebugBuild()}. */
	public static boolean isAllowed(EchoPlayMode mode) {
		if (mode == null || mode == NONE) {
			return true;
		}
		if (mode == DEBUG) {
			return DebugSettings.isDebugBuild();
		}
		return true;
	}

	/**
	 * Coerces disallowed modes (e.g. DEBUG in release) to {@link #SOLO}.
	 */
	public static EchoPlayMode sanitize(EchoPlayMode mode) {
		if (mode == null) {
			return NONE;
		}
		return isAllowed(mode) ? mode : SOLO;
	}
}
