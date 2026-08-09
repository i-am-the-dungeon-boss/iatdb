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
		if (ch == null || Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) {
			return true;
		}
		if (isAoeDotAt(ch, cell)) {
			return true;
		}
		if (isPredictedGasAt(ch, cell)) {
			return true;
		}
		return isHarmfulPlantAt(cell);
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
	 * True when standing in a hazard and at least one safe adjacent step exists.
	 */
	public static boolean canLeave(EchoBoss boss) {
		return bestExit(boss, -1, false) >= 0;
	}

	/**
	 * Best adjacent cell clear of path hazards, or {@code -1}.
	 *
	 * @param enemyPos hero cell for distance scoring; ignored when &lt; 0
	 * @param kite     prefer maximizing distance to {@code enemyPos}
	 */
	public static int bestExit(EchoBoss boss, int enemyPos, boolean kite) {
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
			if (!level.insideMap(cell) || !boss.policyCellPathable(cell)) {
				continue;
			}
			if (isAoeHazardForPath(boss, cell)) {
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

	private static int exitScore(EchoBoss boss, int cell, int enemyPos, boolean kite, Level level) {
		int score = 0;
		// Prefer cells that still have a clear neighbour (avoid 1-tile pockets).
		if (hasClearNeighbour(boss, cell, level)) {
			score += 1000;
		}
		// Prefer clearance from gas / growth.
		score += 10 * gasClearance(boss, cell, level);
		if (enemyPos >= 0 && level.insideMap(enemyPos)) {
			int dist = level.distance(cell, enemyPos);
			score += kite ? dist : -dist;
		}
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
			if (!isAoeHazardForPath(boss, n)) {
				return true;
			}
		}
		return false;
	}

	private static int gasClearance(EchoBoss boss, int cell, Level level) {
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
