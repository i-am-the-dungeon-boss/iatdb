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

	/** The dais is the square of cells within this reach of the centre. */
	private static final int DAIS_REACH = 2;

	/** The throne's seat. Solid, so nobody ever stands on it. */
	public static final int THRONE_SEAT_X = 16;
	public static final int THRONE_SEAT_Y = 15;

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
	public static final int[] POST_Y = { 13, 13, 19, 19, 17 };

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
