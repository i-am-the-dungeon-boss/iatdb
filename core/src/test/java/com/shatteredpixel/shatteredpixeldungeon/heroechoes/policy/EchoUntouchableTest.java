package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoUntouchable.Stance;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * The untouchable ladder as a pure decision table — no {@code Dungeon}, no
 * level, no live fight. The live-fight half lives in
 * {@code EchoShieldResponseTest}.
 */
class EchoUntouchableTest {

	private static Set<String> ready(String... roles) {
		Set<String> set = new HashSet<>();
		for (String role : roles) {
			set.add(role);
		}
		return set;
	}

	/**
	 * The ladder at an unshielded echo on half health — inside the shield band,
	 * so these cases turn only on the rung logic and not on the shield window.
	 */
	private static Stance stance(
			boolean invulnerable,
			boolean bigTemporaryShield,
			Set<String> ready,
			boolean canStepAway,
			int disengageTurns) {
		return EchoUntouchable.stanceFor(
				invulnerable, bigTemporaryShield, ready, canStepAway, disengageTurns, false, 0.5f);
	}

	@Test
	@DisplayName("a hero that is neither invulnerable nor big-shielded is not reacted to")
	void nothingWorthReactingToIsStanceNone() {
		Assertions.assertThat(stance(false, false, ready("BLINK"), true, 0))
				.isEqualTo(Stance.NONE);
	}

	@Test
	@DisplayName("a big decaying shield with shield kit: shield up rather than back off")
	void shieldUpBeatsRun() {
		Assertions.assertThat(
				stance(false, true, ready("SHIELD_SELF", "BLINK"), true, 0))
				.isEqualTo(Stance.SHIELD_UP);
	}

	@Test
	@DisplayName("an echo already carrying a Barrier does not drink a second one")
	void shieldedEchoDoesNotStack() {
		Assertions.assertThat(EchoUntouchable.stanceFor(
				false, true, ready("SHIELD_SELF", "BLINK"), true, 0, true, 0.5f))
				.isEqualTo(Stance.RUN);
	}

	@Test
	@DisplayName("a healthy echo does not spend a shield potion it does not need yet")
	void healthyEchoDoesNotShield() {
		Assertions.assertThat(EchoUntouchable.stanceFor(
				false, true, ready("SHIELD_SELF", "BLINK"), true, 0, false, 0.9f))
				.isEqualTo(Stance.RUN);
	}

	@Test
	@DisplayName("a nearly dead echo heals rather than shielding")
	void dyingEchoDoesNotShield() {
		Assertions.assertThat(EchoUntouchable.stanceFor(
				false, true, ready("SHIELD_SELF", "BLINK"), true, 0, false, 0.2f))
				.isEqualTo(Stance.RUN);
	}

	@Test
	@DisplayName("the shield band runs from 35% to 65% HP, inclusive at both edges")
	void shieldWindowEdges() {
		Assertions.assertThat(EchoUntouchable.worthShielding(false, EchoUntouchable.SHIELD_HP_MAX))
				.as("at the 65% upper edge").isTrue();
		Assertions.assertThat(EchoUntouchable.worthShielding(false, EchoUntouchable.SHIELD_HP_MIN))
				.as("at the 35% lower edge").isTrue();
		Assertions.assertThat(EchoUntouchable.worthShielding(false, 0.66f))
				.as("just above the band").isFalse();
		Assertions.assertThat(EchoUntouchable.worthShielding(false, 0.34f))
				.as("just below the band").isFalse();
		Assertions.assertThat(EchoUntouchable.worthShielding(true, 0.5f))
				.as("mid-band but already shielded").isFalse();
	}

	@Test
	@DisplayName("with no room to run, an unworthy shield still falls through to prep")
	void unworthyShieldFallsThroughToPrep() {
		Assertions.assertThat(EchoUntouchable.stanceFor(
				false, true, ready("SHIELD_SELF", "SETUP_CC"), false, 0, true, 0.5f))
				.isEqualTo(Stance.PREP);
	}

	@Test
	@DisplayName("invulnerability cannot be out-shielded, so shield kit does not stop the run")
	void invulnerabilityIgnoresShieldKit() {
		Assertions.assertThat(
				stance(true, false, ready("SHIELD_SELF", "KEEP_DISTANCE"), true, 0))
				.isEqualTo(Stance.RUN);
	}

	@Test
	@DisplayName("a disengage role beats prep while the cap has room")
	void runBeatsPrep() {
		Assertions.assertThat(
				stance(true, false, ready("BLINK", "HASTE"), false, 6))
				.isEqualTo(Stance.RUN);
	}

	@Test
	@DisplayName("an open cell to back into is enough to run, with no disengage role declared")
	void geometryAloneIsEnoughToRun() {
		Assertions.assertThat(stance(true, false, ready("MELEE"), true, 0))
				.isEqualTo(Stance.RUN);
	}

	@Test
	@DisplayName("with nowhere to run, the window is spent on prep")
	void corneredPreps() {
		Assertions.assertThat(stance(true, false, ready("HASTE"), false, 0))
				.isEqualTo(Stance.PREP);
	}

	@Test
	@DisplayName("nothing ready at all: attack anyway, never idle")
	void nothingLeftFights() {
		Assertions.assertThat(stance(true, false, ready("MELEE"), false, 0))
				.isEqualTo(Stance.FIGHT);
	}

	@Test
	@DisplayName("the seven-turn disengage cap forces the fight even with escape kit ready")
	void disengageCapForcesFight() {
		Set<String> ready = ready("BLINK", "KEEP_DISTANCE");
		Assertions.assertThat(
				stance(true, false, ready, false, EchoUntouchable.MAX_DISENGAGE_TURNS - 1))
				.isEqualTo(Stance.RUN);
		Assertions.assertThat(
				stance(true, false, ready, false, EchoUntouchable.MAX_DISENGAGE_TURNS))
				.isEqualTo(Stance.FIGHT);
	}

	@Test
	@DisplayName("past the cap, prep is still preferred to a bare fight")
	void capFallsThroughToPrepWhenPrepIsReady() {
		Assertions.assertThat(stance(
				true, false, ready("BLINK", "SETUP_CC"), false, EchoUntouchable.MAX_DISENGAGE_TURNS))
				.isEqualTo(Stance.PREP);
	}

	@Test
	@DisplayName("RUN and PREP gate damage roles plus CLOSE_IN and HOLD, and nothing else")
	void gateTable() {
		String[] gated = {
				"MELEE", "RANGED", "FINISHER", "PAYOFF_AOE", "PATH_THROUGH",
				"WEAPON_ABILITY", "ARMOR_ABILITY", "CLOSE_IN", "HOLD"
		};
		String[] allowed = {
				"KEEP_DISTANCE", "BLINK", "HEAL", "SETUP_CC", "SHIELD_SELF", "CLEAR_LOS"
		};
		for (Stance stance : new Stance[] { Stance.RUN, Stance.PREP }) {
			for (String role : gated) {
				Assertions.assertThat(EchoUntouchable.gatesRole(stance, role))
						.as(stance + " gates " + role).isTrue();
			}
			for (String role : allowed) {
				Assertions.assertThat(EchoUntouchable.gatesRole(stance, role))
						.as(stance + " allows " + role).isFalse();
			}
		}
		for (Stance stance : new Stance[] { Stance.NONE, Stance.SHIELD_UP, Stance.FIGHT }) {
			for (String role : gated) {
				Assertions.assertThat(EchoUntouchable.gatesRole(stance, role))
						.as(stance + " allows " + role).isFalse();
			}
		}
	}

	@Test
	@DisplayName("prep is spent in PREP_ORDER, skipping roles already used this window")
	void firstReadyPrepOrderAndSkipping() {
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.rolesReady(ready("HASTE", "SETUP_CC", "KNOCKBACK", "MELEE"))
				.build();

		EchoPolicyChoice first = EchoUntouchable.firstReadyPrep(status, new HashSet<String>());
		Assertions.assertThat(first).isNotNull();
		Assertions.assertThat(first.useRole).isEqualTo("KNOCKBACK");
		Assertions.assertThat(first.layer).isEqualTo("java_untouchable");

		Assertions.assertThat(
				EchoUntouchable.firstReadyPrep(status, ready("KNOCKBACK")).useRole)
				.isEqualTo("SETUP_CC");
		Assertions.assertThat(
				EchoUntouchable.firstReadyPrep(status, ready("KNOCKBACK", "SETUP_CC")).useRole)
				.isEqualTo("HASTE");
		Assertions.assertThat(
				EchoUntouchable.firstReadyPrep(status, ready("KNOCKBACK", "SETUP_CC", "HASTE")))
				.isNull();
	}

	@Test
	@DisplayName("bigShield floors at 10 for a frail hero and scales at 20% of HT")
	void bigShieldThreshold() {
		Assertions.assertThat(EchoUntouchable.bigShield(charWithHt(20))).isEqualTo(10);
		Assertions.assertThat(EchoUntouchable.bigShield(charWithHt(30))).isEqualTo(10);
		Assertions.assertThat(EchoUntouchable.bigShield(charWithHt(100))).isEqualTo(20);
		Assertions.assertThat(EchoUntouchable.bigShield(null)).isEqualTo(10);
	}

	private static Char charWithHt(int ht) {
		Char ch = new Char() {
		};
		ch.HT = ht;
		ch.HP = ht;
		return ch;
	}
}
