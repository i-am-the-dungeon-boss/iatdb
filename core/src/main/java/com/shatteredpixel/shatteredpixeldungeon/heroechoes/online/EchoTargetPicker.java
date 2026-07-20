package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.PathFinder;

/**
 * Picks an aim cell for throws/zaps without UI CellSelector (canvas targeting
 * rules).
 */
public final class EchoTargetPicker {

	private EchoTargetPicker() {
	}

	/**
	 * @return target cell, or -1 if none is legal/safe
	 */
	public static int pick(EchoBoss boss, EchoPolicyStatus status, String itemId, boolean aoeHazard) {
		Hero enemy = Dungeon.hero;
		Level level = Dungeon.level;
		if (enemy == null || level == null)
			return -1;

		if (!aoeHazard) {
			return enemy.pos;
		}

		// Prefer a neighbour of the hero whose blast does not include the echo.
		int best = -1;
		int bestScore = Integer.MIN_VALUE;
		for (int i = 0; i < PathFinder.NEIGHBOURS9.length; i++) {
			int cell = enemy.pos + PathFinder.NEIGHBOURS9[i];
			if (cell < 0 || cell >= level.length() || level.solid[cell])
				continue;
			boolean hitsHero = true; // NEIGHBOURS9 around hero always overlaps hero for potion splash
			boolean harmsEcho = level.distance(cell, boss.pos) <= 1
					&& !status.isSafeFor(EchoPolicyHazards.FIRE_AOE)
					&& !status.isSafeFor(EchoPolicyHazards.PAYOFF_AOE);
			if (!hitsHero || harmsEcho)
				continue;
			int score = level.distance(cell, boss.pos);
			if (score > bestScore) {
				bestScore = score;
				best = cell;
			}
		}
		if (best >= 0)
			return best;

		// Allow hero.pos only when already mitigated.
		if (status.isSafeFor(EchoPolicyHazards.FIRE_AOE)
				|| status.isSafeFor(EchoPolicyHazards.PAYOFF_AOE)) {
			return enemy.pos;
		}
		return -1;
	}
}
