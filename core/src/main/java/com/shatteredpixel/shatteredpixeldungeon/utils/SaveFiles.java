package com.shatteredpixel.shatteredpixeldungeon.utils;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.IOException;

/**
 * Reads and writes a save slot's files, and knows which of them this install wrote.
 * <p>
 * {@link SaveChecksum} covers the bytes of one file; this covers a slot: which key each
 * file is signed under, and whether an unsigned file in it is an older save or a rewritten
 * one. That distinction cannot come from the file — a save editor that decompresses,
 * edits and re-compresses produces exactly what a pre-checksum save looks like — so the
 * fact that the slot has been written before is kept in settings, outside the save folder,
 * where copying or deleting a save cannot reset it.
 */
public final class SaveFiles {

	// The game file is keyed with zeroes: its own seed lives inside it, so it cannot be
	// part of its own key. The floor files, which can, are keyed by the run they belong to.
	private static final long GAME_FILE_SEED = 0L;
	private static final int GAME_FILE_DEPTH = 0;
	private static final int GAME_FILE_BRANCH = 0;

	private static final String DEPTH_PREFIX = "depth";
	private static final String BRANCH_MARKER = "-branch";
	private static final String EXTENSION = ".dat";

	private SaveFiles() {
	}

	public static SaveChecksum.Result readGame(int slot) throws IOException {
		return SaveChecksum.read(GamesInProgress.gameFile(slot),
				GAME_FILE_SEED, GAME_FILE_DEPTH, GAME_FILE_BRANCH);
	}

	public static void writeGame(int slot, Bundle bundle, boolean marked) throws IOException {
		SaveChecksum.write(GamesInProgress.gameFile(slot), bundle,
				GAME_FILE_SEED, GAME_FILE_DEPTH, GAME_FILE_BRANCH, marked);
		remember(slot);
	}

	public static SaveChecksum.Result readLevel(int slot, long seed, int depth, int branch)
			throws IOException {
		return SaveChecksum.read(GamesInProgress.depthFile(slot, depth, branch), seed, depth, branch);
	}

	public static void writeLevel(int slot, Bundle bundle, long seed, int depth, int branch,
			boolean marked) throws IOException {
		SaveChecksum.write(GamesInProgress.depthFile(slot, depth, branch), bundle,
				seed, depth, branch, marked);
	}

	/** Whether this install has written a checksummed file for the slot before. */
	public static boolean isKnown(int slot) {
		return (SPDSettings.slotLayout() & slotBit(slot)) != 0;
	}

	private static void remember(int slot) {
		SPDSettings.slotLayout(SPDSettings.slotLayout() | slotBit(slot));
	}

	private static int slotBit(int slot) {
		return 1 << (slot & 31);
	}

	/**
	 * Signs everything in a slot that predates checksums, once, and remembers the slot.
	 * <p>
	 * Both halves matter. The game file is signed so that opening an old save and backing
	 * out to the title without playing does not come back to an unsigned file on a slot
	 * that is now known. The floor files are signed because {@code saveLevel} only ever
	 * rewrites the floor being left, so an old floor would otherwise read as rewritten the
	 * first time it is revisited.
	 */
	public static void adopt(int slot, long seed) {
		String folder = GamesInProgress.gameFolder(slot);

		adoptFile(GamesInProgress.gameFile(slot), GAME_FILE_SEED, GAME_FILE_DEPTH, GAME_FILE_BRANCH);

		for (String name : FileUtils.filesInDir(folder)) {
			int[] key = depthFileKey(name);
			if (key != null) {
				adoptFile(folder + "/" + name, seed, key[0], key[1]);
			}
		}

		remember(slot);
	}

	private static void adoptFile(String path, long seed, int depth, int branch) {
		try {
			SaveChecksum.Result file = SaveChecksum.read(path, seed, depth, branch);
			if (file.state == SaveChecksum.State.UNSIGNED) {
				SaveChecksum.write(path, file.bundle, seed, depth, branch, false);
			}
		} catch (IOException ignored) {
			// an unreadable file is the ordinary load path's problem, not this one's
		}
	}

	/** {depth, branch} for a {@code depthN.dat} / {@code depthN-branchM.dat} name, else null. */
	static int[] depthFileKey(String name) {
		if (!name.startsWith(DEPTH_PREFIX) || !name.endsWith(EXTENSION)) {
			return null;
		}

		String body = name.substring(DEPTH_PREFIX.length(), name.length() - EXTENSION.length());
		int split = body.indexOf(BRANCH_MARKER);
		String depth = split < 0 ? body : body.substring(0, split);
		String branch = split < 0 ? "0" : body.substring(split + BRANCH_MARKER.length());

		try {
			return new int[] { Integer.parseInt(depth), Integer.parseInt(branch) };
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
