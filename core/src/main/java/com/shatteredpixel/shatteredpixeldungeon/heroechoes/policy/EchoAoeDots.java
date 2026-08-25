package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blizzard;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ConfusionGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Inferno;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.StenchGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.plants.Blindweed;
import com.shatteredpixel.shatteredpixeldungeon.plants.Fadeleaf;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Icecap;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.plants.Rotberry;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sorrowmoss;
import com.shatteredpixel.shatteredpixeldungeon.plants.Stormvine;
import com.watabou.utils.PathFinder;

/**
 * Harmful area blobs for echo policy (fire / frost / gas / …).
 * {@link EchoBoss} treats these as impassable when pathing, and leave-step
 * picks a clear neighbour toward the hero unless positioning wants
 * {@code KEEP_DISTANCE}.
 */
public final class EchoAoeDots {

	public static final String STATUS = "aoe_dot";

	/** Exit-scoring weights; see {@code exitScore}. Each strictly outranks the next. */
	private static final int POCKET_WEIGHT = 10000;
	private static final int DISTANCE_WEIGHT = 100;

	@SuppressWarnings("unchecked")
	private static final Class<? extends Blob>[] GAS_BLOBS = new Class[] {
			ToxicGas.class,
			CorrosiveGas.class,
			ParalyticGas.class,
			ConfusionGas.class,
			StenchGas.class,
	};

	private EchoAoeDots() {
	}

	public static boolean isAoeDotAt(Char ch, int cell) {
		if (ch == null || Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) {
			return false;
		}
		if (harmful(ch, cell, Fire.class, Burning.class)) {
			return true;
		}
		if (harmful(ch, cell, Inferno.class, Burning.class)) {
			return true;
		}
		if (harmful(ch, cell, Freezing.class, Freezing.class)) {
			return true;
		}
		if (harmful(ch, cell, Blizzard.class, Freezing.class)) {
			return true;
		}
		if (harmful(ch, cell, ToxicGas.class, ToxicGas.class)) {
			return true;
		}
		if (harmful(ch, cell, CorrosiveGas.class, CorrosiveGas.class)) {
			return true;
		}
		if (harmful(ch, cell, ParalyticGas.class, ParalyticGas.class)) {
			return true;
		}
		if (harmful(ch, cell, ConfusionGas.class, ConfusionGas.class)) {
			return true;
		}
		if (harmful(ch, cell, StenchGas.class, StenchGas.class)) {
			return true;
		}
		if (harmful(ch, cell, Electricity.class, Electricity.class)) {
			return true;
		}
		return false;
	}

	/**
	 * Cells policy movement must refuse: current AoE DoT, next-tick gas growth,
	 * or a harmful plant.
	 */
	public static boolean isAoeHazardForPath(Char ch, int cell) {
		return isAoeHazardForPath(ch, cell, true);
	}

	/**
	 * As {@link #isAoeHazardForPath(Char, int)}, but the predicted gas-growth
	 * ring can be waived.
	 * <p>
	 * The ring around a dense cloud is often the whole corridor, so treating it
	 * as impassable can leave the echo with no legal step at all — it then
	 * stands still and keeps taking the damage it was trying to avoid. Callers
	 * try the strict mask first and fall back to {@code avoidPredictedGas =
	 * false}. Cells that are harmful <em>now</em>, and harmful plants, are never
	 * waived.
	 */
	public static boolean isAoeHazardForPath(Char ch, int cell, boolean avoidPredictedGas) {
		return isAoeHazardForPath(ch, cell, avoidPredictedGas, true);
	}

	/**
	 * As {@link #isAoeHazardForPath(Char, int, boolean)}, but harmful plants can
	 * be waived too.
	 * <p>
	 * Only for an echo with no way to burn the plant and no way to fight at
	 * range: refusing the cell then means refusing the fight, and it stands in
	 * the corridor forever. Cells that are harmful <em>now</em> are still never
	 * waived — a plant is one hit on the way through, a fire is every turn.
	 */
	public static boolean isAoeHazardForPath(
			Char ch, int cell, boolean avoidPredictedGas, boolean avoidHarmfulPlants) {
		if (ch == null || Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) {
			return true;
		}
		if (isAoeDotAt(ch, cell)) {
			return true;
		}
		if (avoidPredictedGas && isPredictedGasAt(ch, cell)) {
			return true;
		}
		return avoidHarmfulPlants && isHarmfulPlantAt(cell);
	}

	public static boolean isHarmfulPlantAt(int cell) {
		if (Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) {
			return false;
		}
		Plant plant = Dungeon.level.plants.get(cell);
		if (plant == null) {
			return false;
		}
		return plant instanceof Firebloom
				|| plant instanceof Sorrowmoss
				|| plant instanceof Blindweed
				|| plant instanceof Stormvine
				|| plant instanceof Icecap
				|| plant instanceof Fadeleaf
				|| plant instanceof Rotberry;
	}

	/**
	 * True when {@link Blob#evolve()} gas diffusion would put volume on
	 * {@code cell} next tick (read-only; does not mutate blob state).
	 */
	public static boolean isPredictedGasAt(Char ch, int cell) {
		if (ch == null || Dungeon.level == null) {
			return false;
		}
		Level level = Dungeon.level;
		if (cell < 0 || cell >= level.length() || level.solid[cell]) {
			return false;
		}
		for (int g = 0; g < GAS_BLOBS.length; g++) {
			Class<? extends Blob> blobClass = GAS_BLOBS[g];
			if (ch.isImmune(blobClass)) {
				continue;
			}
			Blob blob = level.blobs.get(blobClass);
			if (blob == null || blob.volume <= 0 || blob.cur == null) {
				continue;
			}
			if (predictedGasVolume(blob, cell, level) > 0) {
				return true;
			}
		}
		return false;
	}

	/** One-step Blob.evolve gas value at cell (orthogonal average − 1). */
	static int predictedGasVolume(Blob blob, int cell, Level level) {
		int[] cur = blob.cur;
		if (cur == null || cell < 0 || cell >= cur.length || level.solid[cell]) {
			return 0;
		}
		int width = level.width();
		int x = cell % width;
		int y = cell / width;
		int count = 1;
		int sum = cur[cell];
		if (x > 0 && !level.solid[cell - 1]) {
			sum += cur[cell - 1];
			count++;
		}
		if (x < width - 1 && !level.solid[cell + 1]) {
			sum += cur[cell + 1];
			count++;
		}
		if (y > 0 && !level.solid[cell - width]) {
			sum += cur[cell - width];
			count++;
		}
		if (y < level.height() - 1 && !level.solid[cell + width]) {
			sum += cur[cell + width];
			count++;
		}
		return sum >= count ? (sum / count) - 1 : 0;
	}

	private static boolean harmful(
			Char ch, int cell, Class<? extends Blob> blob, Class<?> immunity) {
		return Blob.volumeAt(cell, blob) > 0 && !ch.isImmune(immunity);
	}

	/**
	 * True when standing in a hazard and at least one <em>clear</em> adjacent
	 * step exists — the readiness of the {@code LEAVE_AOE} role.
	 * <p>
	 * Deliberately excludes the crossing tier of {@link #bestEscape}: the
	 * playbook's trapped rules key on {@code role_not_ready: LEAVE_AOE} to spend
	 * a Purity or shoot from inside the cloud, and those beat walking through
	 * more of the blob. Crossing is the backstop for when none of them can act.
	 */
	public static boolean canLeave(EchoBoss boss) {
		return bestExit(boss, -1, false) >= 0;
	}

	/** True when {@code cell} is only unsafe because gas is about to spread onto it. */
	static boolean isPredictedGasOnly(Char ch, int cell) {
		return !isAoeDotAt(ch, cell) && !isHarmfulPlantAt(cell) && isPredictedGasAt(ch, cell);
	}

	/**
	 * Best adjacent cell clear of path hazards, or {@code -1}.
	 *
	 * @param enemyPos hero cell for distance scoring; ignored when &lt; 0
	 * @param kite     prefer maximizing distance to {@code enemyPos}
	 */
	public static int bestExit(EchoBoss boss, int enemyPos, boolean kite) {
		int strict = bestExit(boss, enemyPos, kite, true);
		if (strict >= 0) {
			return strict;
		}
		// Better to stand on a tile gas is about to reach than to stay in the fire.
		return bestExit(boss, enemyPos, kite, false);
	}

	/**
	 * Best step out of the hazard, falling back to crossing hazardous ground
	 * when no clear neighbour exists.
	 * <p>
	 * Separate from {@link #bestExit} because it must not decide role readiness:
	 * this is the last thing tried before the echo would stand in the blob and
	 * take another tick, not a step the playbook can ask for.
	 */
	public static int bestEscape(EchoBoss boss, int enemyPos, boolean kite) {
		int exit = bestExit(boss, enemyPos, kite);
		return exit >= 0 ? exit : bestCrossing(boss, enemyPos, kite);
	}

	/**
	 * Last resort: every neighbour is hazardous too — an Infernal Brew seeds the
	 * echo's whole 3x3 — so there is no clean step to make.
	 * <p>
	 * Standing still is not the safe option it looks like: the blob under the
	 * echo ticks every turn, and refusing to move is how it dies in place. One
	 * tile of hazard crossed toward open ground costs a single extra tick and
	 * ends the exposure; the tier therefore leads on "does this tile touch clear
	 * ground", then on how thin the hazard on it is, and only then on
	 * positioning.
	 *
	 * @return adjacent hazard cell worth crossing, or {@code -1} when the echo is
	 *         walled in or not standing in a hazard at all
	 */
	private static int bestCrossing(EchoBoss boss, int enemyPos, boolean kite) {
		if (boss == null || Dungeon.level == null || !isAoeDotAt(boss, boss.pos)) {
			return -1;
		}
		Level level = Dungeon.level;
		int best = -1;
		int bestScore = Integer.MIN_VALUE;
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			int cell = boss.pos + PathFinder.NEIGHBOURS8[i];
			if (!level.insideMap(cell) || !boss.policyCellPathableIgnoringAoe(cell)) {
				continue;
			}
			int score = 0;
			if (hasClearNeighbour(boss, cell, level)) {
				score += POCKET_WEIGHT;
			}
			// Thinner hazard first: a tile the blob is about to leave beats its core.
			score += DISTANCE_WEIGHT * -hazardDepth(boss, cell);
			if (enemyPos >= 0 && level.insideMap(enemyPos)) {
				int dist = level.distance(cell, enemyPos);
				score += kite ? dist : -dist;
			}
			if (best < 0 || score > bestScore || (score == bestScore && cell < best)) {
				best = cell;
				bestScore = score;
			}
		}
		return best;
	}

	/**
	 * How thick the harmful blob cover on {@code cell} is, as the count of
	 * hazardous cells in its 3x3 including itself. A cheap stand-in for "how
	 * long would the echo keep taking ticks here" that needs no blob-specific
	 * volume maths.
	 */
	private static int hazardDepth(Char ch, int cell) {
		Level level = Dungeon.level;
		int depth = isAoeDotAt(ch, cell) ? 1 : 0;
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			int n = cell + PathFinder.NEIGHBOURS8[i];
			if (level.insideMap(n) && isAoeDotAt(ch, n)) {
				depth++;
			}
		}
		return depth;
	}

	private static int bestExit(
			EchoBoss boss, int enemyPos, boolean kite, boolean avoidPredictedGas) {
		if (boss == null || Dungeon.level == null) {
			return -1;
		}
		if (!isAoeDotAt(boss, boss.pos)) {
			return -1;
		}
		Level level = Dungeon.level;
		int best = -1;
		int bestScore = Integer.MIN_VALUE;
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			int cell = boss.pos + PathFinder.NEIGHBOURS8[i];
			if (!level.insideMap(cell) || !boss.policyCellPathable(cell, avoidPredictedGas)) {
				continue;
			}
			if (isAoeHazardForPath(boss, cell, avoidPredictedGas)) {
				continue;
			}
			int score = exitScore(boss, cell, enemyPos, kite, level);
			if (best < 0 || score > bestScore || (score == bestScore && cell < best)) {
				best = cell;
				bestScore = score;
			}
		}
		return best;
	}

	/**
	 * Lexicographic exit preference, widest term first:
	 * <ol>
	 * <li>not a one-tile pocket still ringed by hazard,</li>
	 * <li>the positioning intent — away from the hero when kiting, toward when
	 * closing,</li>
	 * <li>clearance from gas and its predicted growth ring.</li>
	 * </ol>
	 * The weights keep each term strictly above the next, so clearance breaks
	 * ties but can never override where the echo is trying to stand.
	 */
	private static int exitScore(EchoBoss boss, int cell, int enemyPos, boolean kite, Level level) {
		int score = 0;
		if (hasClearNeighbour(boss, cell, level)) {
			score += POCKET_WEIGHT;
		}
		if (enemyPos >= 0 && level.insideMap(enemyPos)) {
			int dist = level.distance(cell, enemyPos);
			score += DISTANCE_WEIGHT * (kite ? dist : -dist);
		}
		score += gasClearance(boss, cell, level);
		return score;
	}

	private static boolean hasClearNeighbour(EchoBoss boss, int cell, Level level) {
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			int n = cell + PathFinder.NEIGHBOURS8[i];
			if (!level.insideMap(n) || n == boss.pos) {
				continue;
			}
			if (!level.passable[n] || level.solid[n]) {
				continue;
			}
			if (!isAoeHazardForPath(boss, n, false)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Count of {@code cell}'s neighbours clear of current or predicted gas.
	 * Public so other movement scoring — e.g. {@link EchoBoss}'s retreat-step
	 * choice — can use the same tiebreak this class uses for exit scoring.
	 */
	public static int gasClearance(EchoBoss boss, int cell, Level level) {
		int best = 0;
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			int n = cell + PathFinder.NEIGHBOURS8[i];
			if (!level.insideMap(n)) {
				continue;
			}
			if (isAoeDotAt(boss, n) || isPredictedGasAt(boss, n)) {
				continue;
			}
			best++;
		}
		return best;
	}
}
