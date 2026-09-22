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
	private static final int RADIUS_SQ = 20;

	/**
	 * How far the disc reaches along an axis, and the bounding square that
	 * follows from it. The quadrant tilemaps are rectangles and have to cover the
	 * disc exactly, so the bound is derived here rather than written out again
	 * beside a radius it has to agree with.
	 */
	public static final int DISC_REACH = 4;
	public static final int DISC_LEFT = CENTRE_X - DISC_REACH;
	public static final int DISC_TOP = CENTRE_Y - DISC_REACH;
	public static final int DISC_SPAN = DISC_REACH * 2 + 1;

	/** The dais is the square of cells within this reach of the centre. */
	public static final int DAIS_REACH = 1;

	/** Bounding square of the dais, which is the platform and nothing else. */
	public static final int DAIS_LEFT = CENTRE_X - DAIS_REACH;
	public static final int DAIS_TOP = CENTRE_Y - DAIS_REACH;
	public static final int DAIS_SPAN = DAIS_REACH * 2 + 1;

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
	public static final int[] POST_X = { 14, 18, 18, 14, THRONE_SEAT_X };
	public static final int[] POST_Y = { 14, 14, 18, 18, THRONE_SEAT_Y };

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
	 * The walkway quartering the disc. Left unpainted by the altar's own
	 * tilemaps, because the village's paving skin covers it instead: a path
	 * that changed texture at the edge of the plaza would read as two roads
	 * meeting rather than as one road arriving at the altar.
	 */
	public static boolean onCross(int x, int y) {
		return inDisc(x, y) && !inDais(x, y)
				&& (x == CENTRE_X || y == CENTRE_Y);
	}

	/**
	 * Basins set into the paving, one to a quarter, each holding that region's
	 * own water — and each cut to its own shape.
	 *
	 * <p>Four identical squares said nothing about the four places they stand
	 * for, so the shape carries the region as much as the water does: the sewers
	 * get a straight sluice, the prison a walled tank, the caves a stream that
	 * wanders, the city a formal pool with a spur off one end.
	 *
	 * <p>No cell may have water on all four sides: {@code stitchWaterTile} would
	 * hand back the bare {@code WATER} index for it, which the terrain layer
	 * skips outright, and the pool would be drawn with a hole in the middle.
	 *
	 * <p>Offsets are from the centre, unsigned — the same shape is mirrored into
	 * whichever corner its region owns. They are wider than they are tall
	 * because the disc is: five cells out along both axes at once already falls
	 * outside it. Nothing here may land on the dais, on an arm of the walkway,
	 * or on a post, all of which {@code EchoAltarTest} checks.
	 */
	private static final int[][] BASIN_DX = {
			{}, // NONE
			{ 4, 3, 2 }, // SEWERS — a sluice along the outer arc
			{ 4, 3, 3, 2 }, // PRISON — a squared-off tank
			{ 1, 2, 2, 3, 3 }, // CAVES  — a stream working its way out
			{ 1, 1, 1, 2, 2, 3 } // CITY   — a formal pool with a spur
	};

	private static final int[][] BASIN_DY = {
			{}, // NONE
			{ 1, 1, 1 }, // SEWERS
			{ 2, 2, 3, 3 }, // PRISON
			{ 4, 4, 3, 3, 2 }, // CAVES
			{ 2, 3, 4, 4, 3, 3 } // CITY
	};

	public static boolean inBasin(int x, int y) {
		int region = quadrantOf(x, y);
		if (region == NONE) {
			return false;
		}
		int adx = Math.abs(x - CENTRE_X);
		int ady = Math.abs(y - CENTRE_Y);
		for (int i = 0; i < BASIN_DX[region].length; i++) {
			if (BASIN_DX[region][i] == adx && BASIN_DY[region][i] == ady) {
				return true;
			}
		}
		return false;
	}

	/** How many cells of water a region's basin holds. */
	public static int basinSize(int region) {
		return BASIN_DX[region].length;
	}

	/** Left edge of the box a region's basin fits in. */
	public static int basinLeft(int region) {
		return isWestern(region)
				? CENTRE_X - max(BASIN_DX[region])
				: CENTRE_X + min(BASIN_DX[region]);
	}

	/** Top edge of the box a region's basin fits in. */
	public static int basinTop(int region) {
		return isNorthern(region)
				? CENTRE_Y - max(BASIN_DY[region])
				: CENTRE_Y + min(BASIN_DY[region]);
	}

	/** Width of the box a region's basin fits in. */
	public static int basinWidth(int region) {
		return max(BASIN_DX[region]) - min(BASIN_DX[region]) + 1;
	}

	/** Height of the box a region's basin fits in. */
	public static int basinHeight(int region) {
		return max(BASIN_DY[region]) - min(BASIN_DY[region]) + 1;
	}

	private static int min(int[] values) {
		if (values.length == 0) {
			throw new IllegalArgumentException("no basin for that region");
		}
		int least = values[0];
		for (int i = 1; i < values.length; i++) {
			if (values[i] < least) {
				least = values[i];
			}
		}
		return least;
	}

	private static int max(int[] values) {
		if (values.length == 0) {
			throw new IllegalArgumentException("no basin for that region");
		}
		int most = values[0];
		for (int i = 1; i < values.length; i++) {
			if (values[i] > most) {
				most = values[i];
			}
		}
		return most;
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

	public static boolean isThroneSeat(int x, int y) {
		return x == THRONE_SEAT_X && y == THRONE_SEAT_Y;
	}
}
