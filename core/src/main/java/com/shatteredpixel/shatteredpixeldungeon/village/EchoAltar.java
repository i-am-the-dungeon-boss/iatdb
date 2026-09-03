package com.shatteredpixel.shatteredpixeldungeon.village;

/**
 * The shape of the echo altar: one disc at the middle of the village, quartered
 * by a walkway, with a raised dais and the throne at its centre.
 *
 * <p>Pure geometry and nothing else. Three unrelated callers need the same
 * answers and would otherwise each carry their own copy — the level paints
 * terrain from it, the altar's tilemaps mask their tiles with it, and
 * {@link VillageFigurePlacement} stands the echo bosses on it. Keeping the
 * arithmetic here means a change to the disc moves all three together, and it
 * can be tested without building a level at all.
 *
 * <p>Coordinates are village cell coordinates, so this only makes sense against
 * a map of {@code VillageLevel.SIZE}.
 */
public final class EchoAltar {

	public static final int CENTRE_X = 16;
	public static final int CENTRE_Y = 16;

	/**
	 * Squared radius of the disc. 42 is 6.5 squared rounded down, which lands on
	 * a 13-wide disc with clean, symmetrical corners — a true 6.5 radius leaves
	 * single stray cells sticking out of the diagonals.
	 */
	private static final int RADIUS_SQ = 42;

	/**
	 * How far the disc reaches along an axis, and the bounding square that
	 * follows from it. The quadrant tilemaps are rectangles and have to cover the
	 * disc exactly, so the bound is derived here rather than written out again
	 * beside a radius it has to agree with.
	 */
	public static final int DISC_REACH = 6;
	public static final int DISC_LEFT = CENTRE_X - DISC_REACH;
	public static final int DISC_TOP = CENTRE_Y - DISC_REACH;
	public static final int DISC_SPAN = DISC_REACH * 2 + 1;

	/** The dais is the square of cells within this reach of the centre. */
	public static final int DAIS_REACH = 2;

	/** Bounding square of the dais together with the four step treads. */
	public static final int DAIS_LEFT = CENTRE_X - DAIS_REACH - 1;
	public static final int DAIS_TOP = CENTRE_Y - DAIS_REACH - 1;
	public static final int DAIS_SPAN = (DAIS_REACH + 1) * 2 + 1;

	/**
	 * The throne's seat, dead centre of the disc. Walkable: the deepest echo is
	 * meant to be found sitting in the chair rather than standing in front of it.
	 */
	public static final int THRONE_SEAT_X = CENTRE_X;
	public static final int THRONE_SEAT_Y = CENTRE_Y;

	/** Not part of the altar. */
	public static final int NONE = 0;
	public static final int SEWERS = 1;
	public static final int PRISON = 2;
	public static final int CAVES = 3;
	public static final int CITY = 4;

	/**
	 * Where the five echo bosses stand, shallowest first — the order
	 * {@code VillageFigures.apply} hands out posts in, so the array is indexed by
	 * depth band and not by geometry. The four quadrant posts happen to read
	 * clockwise from the north-west as well, which is a coincidence of the depth
	 * order and not something to "correct" into geometric order.
	 */
	public static final int[] POST_X = { 13, 19, 19, 13, THRONE_SEAT_X };
	public static final int[] POST_Y = { 13, 13, 19, 19, THRONE_SEAT_Y };

	private EchoAltar() {
	}

	public static boolean inDisc(int x, int y) {
		int dx = x - CENTRE_X;
		int dy = y - CENTRE_Y;
		return dx * dx + dy * dy <= RADIUS_SQ;
	}

	/** The raised centre carrying the throne. */
	public static boolean inDais(int x, int y) {
		return Math.abs(x - CENTRE_X) <= DAIS_REACH
				&& Math.abs(y - CENTRE_Y) <= DAIS_REACH;
	}

	/**
	 * The walkway quartering the disc. Deliberately left unpainted by the altar's
	 * tilemaps: the disc is laid as {@code EMPTY_SP}, which the village's own city
	 * tileset already draws as carpet, so the cross costs nothing to render.
	 */
	public static boolean onCross(int x, int y) {
		return inDisc(x, y) && !inDais(x, y)
				&& (x == CENTRE_X || y == CENTRE_Y);
	}

	/**
	 * Basins set into the paving, one to a quarter, each holding that region's
	 * own water.
	 *
	 * <p>Two by two rather than round: these are worked stone basins in a built
	 * plaza, so a square edge is the honest shape, and it is also the only one
	 * that survives being drawn as a flat overlay — the sheet's water tiles are
	 * the translucent edges of a scrolling bed, and there is one bed per level.
	 *
	 * <p>The offsets are wider than they are tall because the disc is not: at
	 * five cells out along both axes at once the corner falls outside it.
	 */
	public static final int BASIN_SPAN = 2;
	private static final int BASIN_NEAR_X = 4;
	private static final int BASIN_NEAR_Y = 3;

	public static boolean inBasin(int x, int y) {
		int adx = Math.abs(x - CENTRE_X);
		int ady = Math.abs(y - CENTRE_Y);
		return adx >= BASIN_NEAR_X && adx < BASIN_NEAR_X + BASIN_SPAN
				&& ady >= BASIN_NEAR_Y && ady < BASIN_NEAR_Y + BASIN_SPAN;
	}

	/** Left edge of a region's basin. */
	public static int basinLeft(int region) {
		return isWestern(region) ? CENTRE_X - BASIN_NEAR_X - BASIN_SPAN + 1
				: CENTRE_X + BASIN_NEAR_X;
	}

	/** Top edge of a region's basin. */
	public static int basinTop(int region) {
		return isNorthern(region) ? CENTRE_Y - BASIN_NEAR_Y - BASIN_SPAN + 1
				: CENTRE_Y + BASIN_NEAR_Y;
	}

	private static boolean isWestern(int region) {
		return region == SEWERS || region == CITY;
	}

	private static boolean isNorthern(int region) {
		return region == SEWERS || region == PRISON;
	}

	/** Which region's ground a cell shows, or {@link #NONE} off the quarters. */
	public static int quadrantOf(int x, int y) {
		if (!inDisc(x, y) || inDais(x, y) || onCross(x, y)) {
			return NONE;
		}
		int dx = x - CENTRE_X;
		int dy = y - CENTRE_Y;
		if (dy < 0) {
			return dx < 0 ? SEWERS : PRISON;
		}
		return dx < 0 ? CITY : CAVES;
	}

	/**
	 * The single step up onto the dais at the head of each approach. Four cells,
	 * one where each arm of the walkway meets the raised centre.
	 */
	public static boolean isTread(int x, int y) {
		int dx = x - CENTRE_X;
		int dy = y - CENTRE_Y;
		if (dx == 0) {
			return Math.abs(dy) == DAIS_REACH + 1;
		}
		if (dy == 0) {
			return Math.abs(dx) == DAIS_REACH + 1;
		}
		return false;
	}

	public static boolean isThroneSeat(int x, int y) {
		return x == THRONE_SEAT_X && y == THRONE_SEAT_Y;
	}
}
