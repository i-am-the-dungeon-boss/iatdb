package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
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
		if ("StoneOfBlink".equals(itemId)) {
			return pickBlinkAway(boss);
		}
		Hero enemy = Dungeon.hero;
		Level level = Dungeon.level;
		if (enemy == null || level == null)
			return -1;

		// Visible: live cell. Cloaked: up to two last-seen guesses. Merely
		// occluded (out of LOS, not invisible): no aim — do not burn kit.
		int focus;
		if (status.enemyInLos) {
			focus = enemy.pos;
		} else if (status.enemyStatuses.contains("invisible")
				&& boss.blindDefenseShotsLeft() > 0) {
			focus = boss.lastSeenEnemyPos();
		} else {
			return -1;
		}
		if (focus < 0 || focus >= level.length()) {
			return -1;
		}

		if (!aoeHazard) {
			return focus;
		}

		// Prefer a neighbour of the focus whose blast does not include the echo.
		int best = -1;
		int bestScore = Integer.MIN_VALUE;
		for (int i = 0; i < PathFinder.NEIGHBOURS9.length; i++) {
			int cell = focus + PathFinder.NEIGHBOURS9[i];
			if (cell < 0 || cell >= level.length() || level.solid[cell])
				continue;
			boolean harmsEcho = level.distance(cell, boss.pos) <= 1
					&& !status.isSafeFor(EchoPolicyHazards.FIRE_AOE)
					&& !status.isSafeFor(EchoPolicyHazards.PAYOFF_AOE);
			if (harmsEcho)
				continue;
			int score = level.distance(cell, boss.pos);
			if (score > bestScore) {
				bestScore = score;
				best = cell;
			}
		}
		if (best >= 0)
			return best;

		// Allow focus cell only when already mitigated.
		if (status.isSafeFor(EchoPolicyHazards.FIRE_AOE)
				|| status.isSafeFor(EchoPolicyHazards.PAYOFF_AOE)) {
			return focus;
		}
		return -1;
	}

	/**
	 * Stone of Blink land cell: throwable empty tile farther from the hero than
	 * the echo currently stands. Returns -1 when no such cell exists.
	 */
	public static int pickBlinkAway(EchoBoss boss) {
		Hero enemy = Dungeon.hero;
		Level level = Dungeon.level;
		if (boss == null || enemy == null || level == null) {
			return -1;
		}
		int curDist = level.distance(boss.pos, enemy.pos);
		int best = -1;
		int bestEnemyDist = curDist;
		int bestTravel = -1;
		for (int cell = 0; cell < level.length(); cell++) {
			if (cell == boss.pos) {
				continue;
			}
			if (!level.passable[cell] || level.solid[cell]) {
				continue;
			}
			if (Actor.findChar(cell) != null) {
				continue;
			}
			if (EchoAoeDots.isAoeDotAt(boss, cell)) {
				continue;
			}
			int enemyDist = level.distance(cell, enemy.pos);
			if (enemyDist <= curDist) {
				continue;
			}
			Ballistica path = new Ballistica(boss.pos, cell, Ballistica.PROJECTILE);
			if (path.collisionPos != cell) {
				continue;
			}
			int travel = level.distance(boss.pos, cell);
			if (enemyDist > bestEnemyDist
					|| (enemyDist == bestEnemyDist && travel > bestTravel)
					|| (enemyDist == bestEnemyDist && travel == bestTravel && cell < best)) {
				best = cell;
				bestEnemyDist = enemyDist;
				bestTravel = travel;
			}
		}
		return best;
	}
}
