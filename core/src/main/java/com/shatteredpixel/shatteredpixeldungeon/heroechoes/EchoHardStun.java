package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TimeStasis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;

/**
 * Echo-fight hard-stun cap, shared immunity grant, and evasion helpers.
 * SPD buff / {@link Hero} call these instead of growing more
 * {@code instanceof Hero || EchoBoss} checks.
 */
public final class EchoHardStun {

	public static final float MAX_DURATION = 3f;

	private static final String LANDED_TURNS = "echo_landed_turns";

	private EchoHardStun() {
	}

	public static boolean isCombatant(Char ch) {
		return ch instanceof Hero || ch instanceof EchoBoss;
	}

	public static boolean appliesTo(Char ch) {
		return isCombatant(ch) && Dungeon.isEchoBossActive();
	}

	public static float capHardStun(Char target, Buff buff, float durationAfterResist) {
		if (appliesTo(target) && isHardStunFlavour(buff)) {
			return Math.min(durationAfterResist, MAX_DURATION);
		}
		return durationAfterResist;
	}

	public static boolean isHardStunFlavour(Buff buff) {
		return buff instanceof Paralysis
				|| buff instanceof Frost
				|| buff instanceof TimeStasis;
	}

	public static boolean isHardStun(Buff buff) {
		return isHardStunFlavour(buff)
				|| buff instanceof MagicalSleep
				|| buff instanceof TimekeepersHourglass.timeStasis;
	}

	/**
	 * True when a second hard stun would chain onto an already-locked combatant.
	 */
	public static boolean rejectChainedHardStun(Char target, Buff incoming) {
		return appliesTo(target) && isHardStun(incoming) && target.paralysed > 0;
	}

	public static void recordFlavourLanded(Buff buff) {
		if (buff instanceof Paralysis) {
			((Paralysis) buff).recordEchoLandedTurns();
		} else if (buff instanceof Frost) {
			((Frost) buff).recordEchoLandedTurns();
		} else if (buff instanceof TimeStasis) {
			((TimeStasis) buff).recordEchoLandedTurns();
		}
	}

	public static void grantImmunityOnDetach(Char ch, float landedTurns) {
		if (!appliesTo(ch) || landedTurns <= 0f) {
			return;
		}
		Buff.prolong(ch, Paralysis.Immunity.class, landedTurns);
	}

	public static void clearGuaranteedHitIfUnstunned(Char ch) {
		if (ch == null || ch.paralysed > 0) {
			return;
		}
		GuaranteedHitTracker tracker = ch.buff(GuaranteedHitTracker.class);
		if (tracker != null) {
			tracker.detach();
		}
	}

	public static void storeLandedTurns(Bundle bundle, float landedTurns) {
		bundle.put(LANDED_TURNS, landedTurns);
	}

	public static float restoreLandedTurns(Bundle bundle) {
		if (bundle.contains(LANDED_TURNS)) {
			return bundle.getFloat(LANDED_TURNS);
		}
		return 0f;
	}

	public static boolean unseenAdjacentDoor(Char defender, Char attacker) {
		if (defender == null || attacker == null || Dungeon.level == null) {
			return false;
		}
		Level level = Dungeon.level;
		if (!level.adjacent(defender.pos, attacker.pos)) {
			return false;
		}
		boolean[] fov = defender.fieldOfView;
		if (fov != null && attacker.pos >= 0 && attacker.pos < fov.length && fov[attacker.pos]) {
			return false;
		}
		if (isDoor(level, attacker.pos) || isDoor(level, defender.pos)) {
			return true;
		}
		int width = level.width();
		int ax = attacker.pos % width;
		int ay = attacker.pos / width;
		int dx = defender.pos % width;
		int dy = defender.pos / width;
		if (ax != dx && ay != dy) {
			return isDoor(level, ay * width + dx) || isDoor(level, dy * width + ax);
		}
		return false;
	}

	private static boolean isDoor(Level level, int cell) {
		if (cell < 0 || cell >= level.length()) {
			return false;
		}
		int t = level.map[cell];
		return t == Terrain.DOOR || t == Terrain.OPEN_DOOR;
	}

	/**
	 * First hit while stunned is a guaranteed hit. Lives on the on-stage char;
	 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss}
	 * copies it onto the kit for {@link Hero#defenseSkill}.
	 */
	public static class GuaranteedHitTracker extends FlavourBuff {
		{
			actPriority = HERO_PRIO + 1;
		}

		@Override
		public boolean act() {
			if (target == null || target.paralysed <= 0) {
				detach();
			} else {
				spend(TICK);
			}
			return true;
		}
	}
}
