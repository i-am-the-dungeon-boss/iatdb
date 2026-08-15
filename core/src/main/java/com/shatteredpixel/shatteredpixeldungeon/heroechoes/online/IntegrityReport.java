package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/**
 * One record of a save file whose bytes no longer matched what the game wrote.
 * Queued locally and posted to the backend when a session is available; the
 * player is never told either happened.
 */
public class IntegrityReport implements Bundlable {

	public static final String REASON_GAME_FILE = "game_file";
	public static final String REASON_DEPTH_FILE = "depth_file";
	/** Triggered from the debug pause menu, so the backend can tell it from a real one. */
	public static final String REASON_DEBUG = "debug";

	private static final String REPORT_ID = "report_id";
	private static final String DETECTED_AT = "detected_at";
	private static final String REASON = "reason";
	private static final String HERO_CLASS = "hero_class";
	private static final String HERO_LEVEL = "hero_level";
	private static final String DEPTH = "depth";
	private static final String SEED = "seed";
	private static final String GAME_VERSION = "game_version";
	private static final String PLAY_MODE = "play_mode";
	private static final String EASY_MODE = "easy_mode";
	private static final String SAVE_SLOT = "save_slot";

	public String reportId = "";
	public long detectedAt;
	public String reason = "";
	public String heroClass = "";
	public int heroLevel;
	public int depth;
	public long seed;
	public String gameVersion = "";
	public String playMode = "";
	public boolean easyMode;
	public int saveSlot;

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(REPORT_ID, reportId);
		bundle.put(DETECTED_AT, detectedAt);
		bundle.put(REASON, reason);
		bundle.put(HERO_CLASS, heroClass);
		bundle.put(HERO_LEVEL, heroLevel);
		bundle.put(DEPTH, depth);
		bundle.put(SEED, seed);
		bundle.put(GAME_VERSION, gameVersion);
		bundle.put(PLAY_MODE, playMode);
		bundle.put(EASY_MODE, easyMode);
		bundle.put(SAVE_SLOT, saveSlot);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		reportId = bundle.getString(REPORT_ID);
		detectedAt = bundle.getLong(DETECTED_AT);
		reason = bundle.getString(REASON);
		heroClass = bundle.getString(HERO_CLASS);
		heroLevel = bundle.getInt(HERO_LEVEL);
		depth = bundle.getInt(DEPTH);
		seed = bundle.getLong(SEED);
		gameVersion = bundle.getString(GAME_VERSION);
		playMode = bundle.getString(PLAY_MODE);
		easyMode = bundle.getBoolean(EASY_MODE);
		saveSlot = bundle.getInt(SAVE_SLOT);
	}
}
