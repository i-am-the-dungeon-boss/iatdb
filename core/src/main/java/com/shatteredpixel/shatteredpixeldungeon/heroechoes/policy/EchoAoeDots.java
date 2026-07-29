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
import com.watabou.utils.PathFinder;

/**
 * Harmful area blobs for echo policy (fire / frost / gas / …).
 * {@link EchoBoss} treats these as impassable when pathing, and leave-step
 * picks a clear neighbour toward the hero unless positioning wants
 * {@code KEEP_DISTANCE}.
 */
public final class EchoAoeDots {

	public static final String STATUS = "aoe_dot";

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
	 * Best adjacent cell clear of AoE hazards, or {@code -1}.
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
			if (isAoeDotAt(boss, cell)) {
				continue;
			}
			int score;
			if (enemyPos < 0 || !level.insideMap(enemyPos)) {
				score = 0;
			} else {
				int dist = level.distance(cell, enemyPos);
				score = kite ? dist : -dist;
			}
			if (best < 0 || score > bestScore || (score == bestScore && cell < best)) {
				best = cell;
				bestScore = score;
			}
		}
		return best;
	}
}
