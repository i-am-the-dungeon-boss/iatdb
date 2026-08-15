package com.shatteredpixel.shatteredpixeldungeon.utils;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.IntegrityReportQueue;

/**
 * Whether the run being played came from save data the game did not write, and what to do
 * about it.
 * <p>
 * {@link SaveChecksum} says what one file's bytes mean and {@link SaveFiles} says what a
 * slot's history is; this is the policy on top of both — what a verdict costs the run, and
 * whether anybody is told. It lives here rather than in {@code Dungeon} because none of it
 * is about the dungeon: it outlives the level, the hero and the run, and mixing it in with
 * the run's own statics made it look like one of them.
 * <p>
 * Nothing here ever reaches the player. A modified run keeps playing exactly as it was; it
 * simply stops producing anything the rest of the world is asked to believe.
 */
public final class SaveIntegrity {

	// set when a save file's contents no longer match what we wrote. Sticky for the life
	// of the run: it is re-written into every later save, and clearing it on disk breaks
	// the checksum, which sets it again.
	private static boolean modified;

	// session-scoped, so continuing a run that is already known to be modified stays quiet
	private static boolean reported;

	private SaveIntegrity() {
	}

	/** A run starting or a slot being loaded: nothing is known about it yet. */
	public static void reset() {
		modified = false;
		reported = false;
	}

	public static boolean isModified() {
		return modified;
	}

	/**
	 * Marks the run without filing anything.
	 * <p>
	 * For the cases where the detection is not news: the debug pause menu, and a slot that
	 * was already reported earlier in this session.
	 */
	public static void mark() {
		modified = true;
	}

	/**
	 * Records what reading a save file said about it. A file whose bytes no longer match
	 * what we wrote marks the run for the rest of its life, and nothing about that reaches
	 * the player.
	 * <p>
	 * An unsigned file is only innocent on a slot this install has never written; see
	 * {@link SaveFiles} for why that cannot be decided from the file itself.
	 *
	 * @param seed the run's seed, needed to adopt a slot that predates checksums
	 */
	public static void note(SaveChecksum.State state, int slot, long seed, String reason) {
		if (state == SaveChecksum.State.OK) {
			return;
		}
		if (state == SaveChecksum.State.UNSIGNED && !SaveFiles.isKnown(slot)) {
			// genuinely predates checksums — adopt the whole slot, quietly and once
			SaveFiles.adopt(slot, seed);
			return;
		}

		// FLAGGED is a mark we wrote ourselves on an earlier load, so it is not news
		boolean report = state != SaveChecksum.State.FLAGGED && !reported;
		mark();

		if (report) {
			reported = true;
			IntegrityReportQueue.recordCurrentRun(reason);
		}
	}
}
