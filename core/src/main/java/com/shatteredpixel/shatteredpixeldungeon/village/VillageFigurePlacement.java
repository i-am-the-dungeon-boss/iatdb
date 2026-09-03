package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;

/**
 * Where the standing figures go.
 *
 * <p>Fixed and deterministic rather than scattered: the depth posts stand on the
 * echo altar, one to a quarter and the deepest before the throne, so a player
 * can learn where to look for depth 15; the mentions gather round the well.
 * Both are asserted by a test rather than eyeballed, because the village's
 * terrain, its four villagers and its paths all have prior claims on those cells
 * and none of them are obvious from a coordinate.
 */
public final class VillageFigurePlacement {

	/** One per depth the site tracks. */
	public static final int DEPTH_POSTS = 5;
	/** One per honorable mention. */
	public static final int MENTION_POSTS = 5;

	/** The well the mentions gather round, on the north-west green. */
	private static final int WELL_X = 7;
	private static final int WELL_Y = 13;

	/**
	 * Where the mentions stand relative to the well. The well itself has moved
	 * off the middle of town to make room for the altar, but the ring around it
	 * is the arrangement players already know, so the offsets are kept exactly
	 * and only the origin moved.
	 */
	private static final int[][] WELL_CLUSTER = {
			{ 0, -2 },
			{ -2, -1 },
			{ 2, -1 },
			{ -2, 2 },
			{ 2, 2 }
	};

	private VillageFigurePlacement() {
	}

	/**
	 * The five spots on the echo altar, shallowest first — one per quarter of the
	 * disc, and the deepest before the throne.
	 */
	public static int[] depthPosts(VillageLevel level) {
		int[] cells = new int[DEPTH_POSTS];
		for (int i = 0; i < DEPTH_POSTS; i++) {
			cells[i] = level.cell(EchoAltar.POST_X[i], EchoAltar.POST_Y[i]);
		}
		return cells;
	}

	/** The five spots round the well. */
	public static int[] mentionPosts(VillageLevel level) {
		int[] cells = new int[WELL_CLUSTER.length];
		for (int i = 0; i < WELL_CLUSTER.length; i++) {
			cells[i] = level.cell(WELL_X + WELL_CLUSTER[i][0], WELL_Y + WELL_CLUSTER[i][1]);
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
