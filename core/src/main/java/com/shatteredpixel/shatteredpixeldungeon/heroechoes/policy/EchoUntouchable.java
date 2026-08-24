package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;

import java.util.Set;

/**
 * The "hero cannot be hurt right now" ladder: what the echo does instead of
 * standing still.
 * <p>
 * The non-obvious fact behind the ordering: {@code Barrier} is a
 * {@link ShieldBuff}, so incoming damage runs through
 * {@code ShieldBuff.absorbDamage} — <em>hitting a shield drains it</em>, unlike
 * hitting {@link Invulnerability}. Attacking is therefore the fastest way to
 * remove a Barrier, which is why the shield branch prefers "shield up and keep
 * swinging" over running, and why running is time-boxed. Do not "fix" this into
 * a longer disengage.
 * <p>
 * Everything here is a Java floor under the wire-supplied playbook. Policies
 * arrive over the network ({@code EchoWireCodec}) and may be stale; a stale
 * playbook must not be able to turn a disengage back into a stand-still. JSON
 * keeps control over <em>which</em> buff or attack; Java guarantees the rung and
 * guarantees the turn is never wasted.
 */
public final class EchoUntouchable {

	/** What the echo does about an untouchable hero, most preferred first. */
	public enum Stance {
		/** Nothing worth reacting to; ordinary combat. */
		NONE,
		/** Shield up and keep fighting — the hero's Barrier drains as we hit it. */
		SHIELD_UP,
		/** Back off for a bounded number of turns. */
		RUN,
		/** Cornered: spend the window on self-buffs and control. */
		PREP,
		/** Nothing left to do but attack. */
		FIGHT
	}

	/**
	 * Barrier decay below 20 shielding is exponential (half-life
	 * {@code 20*ln2 ~= 13.9} turns) and never actually finishes;
	 * {@code HoldFast.buffDecayFactor} at 3 talent points stops it outright, so
	 * an uncapped disengage against a camping Warrior is an infinite loop. Seven
	 * turns is half a half-life (~30% of the shield gone) and bounds the stall.
	 */
	public static final int MAX_DISENGAGE_TURNS = 7;

	/**
	 * Prep roles in the order the Java floor spends them, derived from
	 * {@link EchoRole#prepRank()} so the membership set and the ordering cannot
	 * disagree.
	 * <p>
	 * {@code KNOCKBACK} leads deliberately — when cornered it achieves the
	 * distance the RUN rung could not. {@code SETUP_CC} is next: freezing an
	 * untouchable hero converts the window into free hits once it ends.
	 */
	public static final String[] PREP_ORDER = prepOrder();

	private static String[] prepOrder() {
		EchoRole[] all = EchoRole.values();
		int count = 0;
		for (int i = 0; i < all.length; i++) {
			if (all[i].isPrep()) {
				count++;
			}
		}
		String[] order = new String[count];
		// Ranks are dense and start at 1, so place each role straight into its slot.
		for (int i = 0; i < all.length; i++) {
			if (all[i].isPrep()) {
				order[all[i].prepRank() - 1] = all[i].id();
			}
		}
		return order;
	}

	private EchoUntouchable() {
	}

	/**
	 * Trickle Barrier sources (Protective Shadows, Satiated Spells, Liquid
	 * Willpower, Transfusion, Hallowed Ground) all cap below 10% of hero HT;
	 * every burst source (Potion of Shielding, Cleanse, Power of Many, Seal
	 * Shard, Bless) starts at 20% or more. There is nothing in between, so 20%
	 * of HT separates "a trickle to fight through" from "a real window".
	 */
	public static int bigShield(Char hero) {
		if (hero == null || hero.HT <= 0) {
			return 10;
		}
		return Math.max(10, Math.round(0.20f * hero.HT));
	}

	/**
	 * Sum of every active shield that goes away once spent, whether it decays on
	 * a timer ({@code Barrier}), counts down ({@code Blocking.BlockBuff}) or is
	 * tied to an ability window.
	 * <p>
	 * Shields that recharge instead of detaching — the warrior's Broken Seal,
	 * {@code Berserk}, {@code AscendedForm} — are excluded on purpose: there is
	 * no window to wait out, so backing off would mean backing off forever.
	 * Those are fought through as usual.
	 * <p>
	 * Note this reads each buff's own {@code shielding()};
	 * {@link Char#shielding()} sums every {@code ShieldBuff} and cannot tell the
	 * two kinds apart.
	 */
	public static int temporaryShielding(Char ch) {
		if (ch == null) {
			return 0;
		}
		int total = 0;
		for (Buff buff : ch.buffs()) {
			if (!(buff instanceof ShieldBuff)) {
				continue;
			}
			ShieldBuff shield = (ShieldBuff) buff;
			if (shield.shielding() > 0 && shield.detachesAtZero()) {
				total += shield.shielding();
			}
		}
		return total;
	}

	/** Any temporary shield at all, however small — the {@code temp_shield} status. */
	public static boolean hasTemporaryShield(Char ch) {
		return temporaryShielding(ch) > 0;
	}

	/** Ankh glow / Potion of Invulnerability: hits are genuinely wasted. */
	public static boolean isInvulnerable(Char ch, Class<?> src) {
		if (ch == null || !ch.isAlive()) {
			return false;
		}
		return ch.isInvulnerable(src) || ch.buff(Invulnerability.class) != null;
	}

	/**
	 * A decaying shield large enough to be worth reacting to. Below the
	 * threshold the echo simply fights through it — attacking drains it.
	 */
	public static boolean hasBigTemporaryShield(Char ch) {
		if (ch == null || !ch.isAlive()) {
			return false;
		}
		return temporaryShielding(ch) >= bigShield(ch);
	}

	/**
	 * Upper edge of the band where drinking a Barrier is the right spend.
	 * <p>
	 * A shield is pre-emptive mitigation — it absorbs damage not yet taken — so
	 * it is worth most while the echo is still healthy enough to fight behind
	 * it. Above this the echo does not need one yet and should not burn a
	 * limited potion stack on it.
	 */
	public static final float SHIELD_HP_MAX = 0.65f;

	/**
	 * Lower edge of that band. Below this a Barrier buys a couple of hits and
	 * the echo dies anyway; a heal is the correct spend instead, which is what
	 * the playbook's {@code heal_when_hurt} does at the same threshold.
	 */
	public static final float SHIELD_HP_MIN = 0.35f;

	/**
	 * True when spending a Barrier-granting item is worthwhile right now:
	 * inside the HP band, and not already carrying one.
	 * <p>
	 * Mirrors the {@code SHIELD_HP_WINDOW} + {@code self_shielded} clauses on
	 * the backend's {@code untouchable_shield_up} reaction. Both layers need it
	 * because the playbook only governs the matcher — {@link Stance#SHIELD_UP}
	 * also drives {@code EchoBoss.forcedUntouchableAct}, which fires when
	 * matching produced nothing at all and so cannot be gated by JSON.
	 */
	public static boolean worthShielding(boolean alreadyShielded, float selfHpRatio) {
		return !alreadyShielded && selfHpRatio <= SHIELD_HP_MAX && selfHpRatio >= SHIELD_HP_MIN;
	}

	/**
	 * The rung, as a pure function of the observations — no {@code Dungeon},
	 * so it is directly unit-testable.
	 *
	 * @param preGateReady    roles ready <em>before</em> this stance's own gate is
	 *                        applied; {@code virtualRoleFeasible} has already
	 *                        answered "is there anywhere to run?" for
	 *                        {@code BLINK} / {@code KEEP_DISTANCE}
	 * @param canStepAway     some adjacent cell is legal and strictly farther from
	 *                        the hero. Independent of the playbook on purpose: the
	 *                        Java floor steps away with {@code policyStepFurther},
	 *                        so a policy that never declared a {@code KEEP_DISTANCE}
	 *                        capability can still disengage
	 * @param alreadyShielded the echo is carrying its own temporary shield, so a
	 *                        second one would stack Barrier on Barrier
	 * @param selfHpRatio     the echo's own HP ratio, checked against
	 *                        {@link #worthShielding}
	 */
	public static Stance stanceFor(
			boolean invulnerable,
			boolean bigTemporaryShield,
			Set<String> preGateReady,
			boolean canStepAway,
			int disengageTurns,
			boolean alreadyShielded,
			float selfHpRatio) {
		if (!invulnerable && !bigTemporaryShield) {
			return Stance.NONE;
		}
		Set<String> ready = preGateReady != null ? preGateReady : java.util.Collections.<String>emptySet();
		// Invulnerability cannot be out-shielded — only outlasted (3 turns).
		// The worthShielding check is what stops the floor re-drinking a Barrier
		// every turn of the window: without it SHIELD_UP holds for as long as the
		// hero stays untouchable, which is the longest stretch of the fight.
		// Falling through instead lands on RUN / PREP, which cost no consumable.
		if (!invulnerable
				&& ready.contains(EchoRole.SHIELD_SELF.id())
				&& worthShielding(alreadyShielded, selfHpRatio)) {
			return Stance.SHIELD_UP;
		}
		boolean canRun = canStepAway || hasAnyDisengage(ready);
		if (disengageTurns < MAX_DISENGAGE_TURNS && canRun) {
			return Stance.RUN;
		}
		if (hasAny(ready, PREP_ORDER)) {
			return Stance.PREP;
		}
		return Stance.FIGHT;
	}

	private static boolean hasAnyDisengage(Set<String> ready) {
		for (String role : ready) {
			if (EchoRole.isDisengageRole(role)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasAny(Set<String> ready, String[] roles) {
		for (int i = 0; i < roles.length; i++) {
			if (ready.contains(roles[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * True when {@code stance} forbids {@code role} this turn. Only the two
	 * disengage rungs gate anything: closing in or holding ground would undo the
	 * retreat, and damage roles are pointless against the window.
	 * {@code SHIELD_UP} and {@code FIGHT} gate nothing at all — that is the
	 * whole point of them.
	 */
	public static boolean gatesRole(Stance stance, String role) {
		if (stance != Stance.RUN && stance != Stance.PREP) {
			return false;
		}
		return EchoRole.isDamageRole(role)
				|| EchoRole.CLOSE_IN.id().equals(role)
				|| EchoRole.HOLD.id().equals(role);
	}

	/**
	 * First {@link #PREP_ORDER} entry that is ready and not already spent in
	 * this untouchable window, as a Java-layer plan.
	 *
	 * @return null when the echo has no unused prep left
	 */
	public static EchoPlan firstReadyPrep(EchoPolicyStatus status, Set<String> alreadyUsed) {
		if (status == null) {
			return null;
		}
		for (int i = 0; i < PREP_ORDER.length; i++) {
			String role = PREP_ORDER[i];
			if (alreadyUsed != null && alreadyUsed.contains(role)) {
				continue;
			}
			if (status.isRoleReady(role)) {
				return EchoPlan.resolve(role, "java_untouchable", null, status);
			}
		}
		return null;
	}
}
