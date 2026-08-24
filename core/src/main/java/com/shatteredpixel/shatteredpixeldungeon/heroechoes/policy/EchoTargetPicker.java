package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.PathFinder;

/**
 * Picks an aim cell for throws/zaps without UI CellSelector (see
 * {@code hero-echoes/docs/features/echo-policy.md} § "Phase 3 — Execute" for the targeting
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

		boolean blastMitigated = status.isSafeFor(EchoPolicyHazards.FIRE_AOE)
				|| status.isSafeFor(EchoPolicyHazards.PAYOFF_AOE);

		// Aim is on the enemy itself unless a splash the echo cannot shrug off would
		// catch the echo too. A shot a wall stops short of never reaches the enemy,
		// so it is no aim at all.
		if (!aoeHazard || blastMitigated || !splashCovers(level, focus, boss.pos)) {
			return reachesUnobstructed(boss.pos, focus) ? focus : -1;
		}

		// Echo stands in the blast: the only shot worth firing is one that bursts
		// where the splash still covers the enemy but no longer covers the echo.
		int best = -1;
		int bestScore = Integer.MIN_VALUE;
		for (int i = 0; i < PathFinder.NEIGHBOURS9.length; i++) {
			int cell = focus + PathFinder.NEIGHBOURS9[i];
			if (cell < 0 || cell >= level.length() || level.solid[cell])
				continue;
			// Where the throw/zap actually bursts, which is short of the aim cell
			// when a wall or another body intercepts it.
			int burst = new Ballistica(boss.pos, cell, Ballistica.PROJECTILE).collisionPos;
			if (!splashCovers(level, burst, focus) || splashCovers(level, burst, boss.pos))
				continue;
			int score = level.distance(cell, boss.pos);
			if (score > bestScore) {
				bestScore = score;
				best = cell;
			}
		}
		return best;
	}

	/** Whether a splash centred on {@code blast} reaches {@code cell}. */
	private static boolean splashCovers(Level level, int blast, int cell) {
		return level.distance(blast, cell) <= 1;
	}

	/**
	 * Whether a throw/zap aimed at {@code to} gets there at all: only terrain is
	 * checked, since a body in the line is the thing being shot at (or worth
	 * hitting on the way).
	 */
	private static boolean reachesUnobstructed(int from, int to) {
		Ballistica path = new Ballistica(from, to, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID);
		return path.collisionPos == to;
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
			if (EchoAoeDots.isAoeHazardForPath(boss, cell)) {
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
