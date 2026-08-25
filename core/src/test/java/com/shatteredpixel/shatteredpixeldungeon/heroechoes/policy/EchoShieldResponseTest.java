package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Live-fight half of the untouchable ladder against a shielded hero. The test
 * warrior has HT 30, so {@link EchoUntouchable#bigShield} is 10.
 */
@ExtendWith(GdxTestExtension.class)
class EchoShieldResponseTest {

	@Test
	@DisplayName("a small decaying shield is fought through, because hitting it is what drains it")
	void smallDecayingShieldIsFoughtThrough() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = shieldPolicy();
		EchoBoss boss = shieldBoss(hero, policy, false);
		// Barrier is a ShieldBuff, so damage runs through ShieldBuff.absorbDamage:
		// swinging removes the shield faster than waiting for its decay.
		Buff.affect(hero, Barrier.class).setShield(4);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.enemyStatuses).contains(EchoPolicyHazards.TEMP_SHIELD);
		Assertions.assertThat(status.enemyStatuses).doesNotContain(EchoPolicyHazards.DAMAGE_IMMUNE);
		Assertions.assertThat(status.isRoleReady("MELEE")).isTrue();
	}

	@Test
	@DisplayName("a big shield plus shield kit: the echo shields up and keeps fighting")
	void bigShieldWithShieldKitShieldsUpAndKeepsFighting() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = shieldPolicy();
		EchoBoss boss = shieldBoss(hero, policy, false);
		giveEchoItem(boss, new PotionOfShielding());
		hurtIntoShieldBand(boss);
		Buff.affect(hero, Barrier.class).setShield(20);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		Assertions.assertThat(status.untouchableStance).isEqualTo(EchoUntouchable.Stance.SHIELD_UP);
		Assertions.assertThat(status.isRoleReady("MELEE")).isTrue();

		boolean spent = EchoRoleExecutor.execute(boss, policy, status,
				new EchoPlan(EchoPolicyHazards.SHIELD_SELF, "java_untouchable", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(EchoUntouchable.temporaryShielding(boss)).isGreaterThan(0);
		Assertions.assertThat(countItem(boss, PotionOfShielding.class)).isZero();
	}

	@Test
	@DisplayName("a full-health echo keeps its shield potion rather than pre-buffing")
	void fullHealthEchoDoesNotSpendAShield() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = shieldPolicy();
		EchoBoss boss = shieldBoss(hero, policy, false);
		giveEchoItem(boss, new PotionOfShielding());
		Buff.affect(hero, Barrier.class).setShield(20);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		// Above 65% HP a Barrier absorbs damage the echo was not going to take
		// anyway; the disengage ladder costs no consumable.
		Assertions.assertThat(status.untouchableStance).isNotEqualTo(EchoUntouchable.Stance.SHIELD_UP);
		Assertions.assertThat(countItem(boss, PotionOfShielding.class)).isEqualTo(1);
	}

	@Test
	@DisplayName("the echo does not stack a second shield while its own is above half")
	void doesNotStackASecondShieldWhileItsOwnIsAboveHalf() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = shieldPolicy();
		EchoBoss boss = shieldBoss(hero, policy, false);
		giveEchoItem(boss, new PotionOfShielding());
		giveEchoItem(boss, new PotionOfShielding());
		hurtIntoShieldBand(boss);
		Buff.affect(hero, Barrier.class).setShield(20);

		EchoPolicyStatus first = EchoPolicyStatusBuilder.build(boss, policy);
		EchoRoleExecutor.execute(boss, policy, first,
				new EchoPlan(EchoPolicyHazards.SHIELD_SELF, "java_untouchable", null));

		EchoPolicyStatus next = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(next.isRoleReady(EchoPolicyHazards.SHIELD_SELF)).isFalse();
		Assertions.assertThat(next.selfStatuses).contains(EchoPolicyHazards.SELF_SHIELDED);
		Assertions.assertThat(countItem(boss, PotionOfShielding.class)).isEqualTo(1);
	}

	@Test
	@DisplayName("once its own shield falls below half, the echo re-shields")
	void reShieldsOnceItsOwnShieldFallsBelowHalf() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = shieldPolicy();
		EchoBoss boss = shieldBoss(hero, policy, false);
		giveEchoItem(boss, new PotionOfShielding());
		giveEchoItem(boss, new PotionOfShielding());
		Buff.affect(hero, Barrier.class).setShield(20);

		EchoPolicyStatus first = EchoPolicyStatusBuilder.build(boss, policy);
		EchoRoleExecutor.execute(boss, policy, first,
				new EchoPlan(EchoPolicyHazards.SHIELD_SELF, "java_untouchable", null));
		// The peak is recorded by the next sense, as it is in a real fight.
		EchoPolicyStatusBuilder.build(boss, policy);
		int peak = EchoUntouchable.temporaryShielding(boss);
		// ShieldBuff.setShield only raises, so drain it the way the fight would.
		Buff.affect(boss, Barrier.class).absorbDamage(peak - (peak / 2 - 1));

		EchoPolicyStatus next = EchoPolicyStatusBuilder.build(boss, policy);
		Assertions.assertThat(next.isRoleReady(EchoPolicyHazards.SHIELD_SELF)).isTrue();

		EchoRoleExecutor.execute(boss, policy, next,
				new EchoPlan(EchoPolicyHazards.SHIELD_SELF, "java_untouchable", null));
		Assertions.assertThat(countItem(boss, PotionOfShielding.class)).isZero();
	}

	@Test
	@DisplayName("a fully spent shield resets the peak, so the next one is judged on its own")
	void selfShieldPeakResetsWhenTheShieldIsSpent() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = shieldPolicy();
		EchoBoss boss = shieldBoss(hero, policy, false);
		giveEchoItem(boss, new PotionOfShielding());
		Buff.affect(hero, Barrier.class).setShield(20);

		boss.noteSelfShield(40);
		boss.noteSelfShield(0);

		Assertions.assertThat(boss.hasMostlyIntactShield()).isFalse();
		Assertions.assertThat(
				EchoPolicyStatusBuilder.build(boss, policy).isRoleReady(EchoPolicyHazards.SHIELD_SELF))
				.isTrue();
	}

	@Test
	@DisplayName("a big shield with no shield kit: the echo fights through it instead of running")
	void bigShieldWithoutShieldKitFights() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = shieldPolicy();
		EchoBoss boss = shieldBoss(hero, policy, false);
		Buff.affect(hero, Barrier.class).setShield(20);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.untouchableStance).isEqualTo(EchoUntouchable.Stance.FIGHT);
		Assertions.assertThat(status.isRoleReady("MELEE")).isTrue();
	}

	@Test
	@DisplayName("running stops after seven turns even when the window never closes")
	void runStopsAfterSevenTurns() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = shieldPolicy();
		EchoBoss boss = shieldBoss(hero, policy, false);
		// Invulnerability, not a Barrier: retreating from a shielded hero is
		// disabled (see EchoUntouchable.stanceFor), so only the invulnerable rung
		// still exercises the disengage cap.
		Buff.affect(hero, Invulnerability.class, 100f);

		for (int turn = 0; turn < EchoUntouchable.MAX_DISENGAGE_TURNS; turn++) {
			EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
			Assertions.assertThat(status.untouchableStance)
					.as("turn " + turn).isEqualTo(EchoUntouchable.Stance.RUN);
			runTurn(boss);
		}

		EchoPolicyStatus after = EchoPolicyStatusBuilder.build(boss, policy);
		Assertions.assertThat(after.untouchableStance).isEqualTo(EchoUntouchable.Stance.FIGHT);
		Assertions.assertThat(after.isRoleReady("MELEE")).isTrue();
	}

	@Test
	@DisplayName("the disengage budget survives a save/load, so a reload cannot reset it")
	void disengageTurnsSurviveSaveAndLoad() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = shieldPolicy();
		EchoBoss boss = shieldBoss(hero, policy, false);
		Buff.affect(hero, Invulnerability.class, 100f);
		for (int turn = 0; turn < 4; turn++) {
			runTurn(boss);
		}

		Bundle bundle = new Bundle();
		boss.storeInBundle(bundle);
		EchoBoss restored = new EchoBoss();
		restored.restoreFromBundle(bundle);
		Assertions.assertThat(restored.disengageTurns()).isEqualTo(4);

		boss = shieldBoss(hero, policy, false);
		setDisengageTurns(boss, 4);
		for (int turn = 0; turn < 3; turn++) {
			Assertions.assertThat(EchoPolicyStatusBuilder.build(boss, policy).untouchableStance)
					.isEqualTo(EchoUntouchable.Stance.RUN);
			runTurn(boss);
		}
		Assertions.assertThat(EchoPolicyStatusBuilder.build(boss, policy).untouchableStance)
				.isEqualTo(EchoUntouchable.Stance.FIGHT);
	}

	@Test
	@DisplayName("invulnerability cannot be out-shielded: the echo runs and keeps its potion")
	void invulnerabilityRunsRegardlessOfShieldKit() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = shieldPolicy();
		EchoBoss boss = shieldBoss(hero, policy, false);
		giveEchoItem(boss, new PotionOfShielding());
		Buff.affect(hero, Invulnerability.class, 3f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.untouchableStance).isEqualTo(EchoUntouchable.Stance.RUN);
		Assertions.assertThat(countItem(boss, PotionOfShielding.class)).isEqualTo(1);
	}

	@Test
	@DisplayName("a small Blocking shield timer is not worth reacting to")
	void blockingShieldTimerIsNotWorthReactingTo() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = shieldPolicy();
		EchoBoss boss = shieldBoss(hero, policy, false);
		// round(powerMulti * (2 + buffedLvl)) with a 5-turn timer that resets on
		// every proc: small and self-refreshing, so there is no window to outlast.
		Buff.affect(hero, Blocking.BlockBuff.class).setShield(5);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.untouchableStance).isEqualTo(EchoUntouchable.Stance.NONE);
		Assertions.assertThat(status.isRoleReady("MELEE")).isTrue();
	}

	/** One RUN turn's worth of bookkeeping without driving the whole actor clock. */
	private static void runTurn(EchoBoss boss) {
		setDisengageTurns(boss, boss.disengageTurns() + 1);
	}

	private static void setDisengageTurns(EchoBoss boss, int turns) {
		try {
			java.lang.reflect.Field field = EchoBoss.class.getDeclaredField("disengageTurns");
			field.setAccessible(true);
			field.setInt(boss, turns);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static EchoPolicy shieldPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("MELEE", EchoTestSupport.capability("*melee"))
				.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
				.put(EchoPolicyHazards.SHIELD_SELF, EchoTestSupport.capability("PotionOfShielding")));
	}

	private static EchoBoss shieldBoss(Hero hero, EchoPolicy policy, boolean cornered) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		return boss;
	}

	/**
	 * Drops the echo to half health, inside {@code EchoUntouchable}'s 35–65%
	 * shield band. A shield is pre-emptive mitigation, so a full-HP echo does
	 * not spend one at all — see {@code fullHealthEchoDoesNotSpendAShield}.
	 */
	private static void hurtIntoShieldBand(EchoBoss boss) {
		boss.HP = Math.max(1, boss.HT / 2);
	}

	private static int countItem(EchoBoss boss, Class<? extends Item> type) {
		int n = 0;
		for (Item item : boss.getEchoHero().belongings) {
			if (type.isInstance(item)) {
				n += Math.max(1, item.quantity());
			}
		}
		return n;
	}

	private static void giveEchoItem(EchoBoss boss, Item item) {
		item.identify();
		item.collect(boss.getEchoHero().belongings.backpack);
	}
}
