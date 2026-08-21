package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;

/**
 * Where the standing figures go.
 *
 * <p>Fixed and deterministic rather than scattered: the depth posts read as a
 * row along the waterfront, in depth order, so a player can learn where to look
 * for depth 15; the mentions gather round the well, which is the one landmark in
 * the middle of town. Both are asserted by a test rather than eyeballed, because
 * the village's terrain, its four villagers and its path all have prior claims
 * on those cells and none of them are obvious from a coordinate.
 */
public final class VillageFigurePlacement {

	/** One per depth the site tracks. */
	public static final int DEPTH_POSTS = 5;
	/** One per honorable mention. */
	public static final int MENTION_POSTS = 5;

	/**
	 * The promenade: below the bank at y = 4, above the path's top at y = 6.
	 * Column 8 is the path down to the dungeon and is left out of the run.
	 */
	private static final int PROMENADE_Y = 5;
	private static final int[] PROMENADE_X = { 2, 4, 6, 10, 12 };

	/** Round the well at (16, 14), clear of the sage who stands at (17, 15). */
	private static final int[][] WELL_CLUSTER = {
			{ 16, 12 },
			{ 14, 13 },
			{ 18, 13 },
			{ 14, 16 },
			{ 18, 16 }
	};

	private VillageFigurePlacement() {
	}

	/** The five waterfront spots, shallowest first. */
	public static int[] depthPosts(VillageLevel level) {
		int[] cells = new int[PROMENADE_X.length];
		for (int i = 0; i < PROMENADE_X.length; i++) {
			cells[i] = level.cell(PROMENADE_X[i], PROMENADE_Y);
		}
		return cells;
	}

	/** The five spots round the well. */
	public static int[] mentionPosts(VillageLevel level) {
		int[] cells = new int[WELL_CLUSTER.length];
		for (int i = 0; i < WELL_CLUSTER.length; i++) {
			cells[i] = level.cell(WELL_CLUSTER[i][0], WELL_CLUSTER[i][1]);
		}
		return cells;
	}

	/**
	 * Whether a figure can be put down here. Terrain is fixed and already
	 * asserted, but the villagers, the player and other figures are not, so a
	 * spot can be taken at the moment the figures arrive.
	 */
	public static boolean isFree(VillageLevel level, int cell) {
		if (cell < 0 || cell >= level.length()) {
			return false;
		}
		if (!level.passable[cell] || level.solid[cell]) {
			return false;
		}
		return Actor.findChar(cell) == null;
	}
}
