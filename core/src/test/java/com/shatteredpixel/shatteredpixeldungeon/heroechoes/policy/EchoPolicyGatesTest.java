package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfSnapFreeze;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Hard gates that must fail closed regardless of what the generated policy
 * asks for: hero purity blocks blob potions, hero invulnerability / timed
 * shield blocks damage roles.
 */
@ExtendWith(GdxTestExtension.class)
class EchoPolicyGatesTest {

	@Test
	@DisplayName("hero with BlobImmunity is sensed as purity and unreadies SETUP_CC and PAYOFF_AOE")
	void purityHardGatesBlobRoles() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);
		Buff.affect(hero, BlobImmunity.class, 20f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.enemyStatuses).contains("purity");
		Assertions.assertThat(status.isRoleReady("SETUP_CC")).isFalse();
		Assertions.assertThat(status.isRoleReady("PAYOFF_AOE")).isFalse();
	}

	@Test
	@DisplayName("purity does not unready non-blob roles such as MELEE")
	void purityLeavesNonBlobRolesReady() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);
		Buff.affect(hero, BlobImmunity.class, 20f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.isRoleReady("MELEE")).isTrue();
	}

	@Test
	@DisplayName("3-turn Paralysis.Immunity is not purity and leaves PAYOFF_AOE ready")
	void paralysisImmunityIsNotPurity() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);
		Buff.affect(hero, Paralysis.Immunity.class, 3f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.enemyStatuses).contains("paralysis_immunity");
		Assertions.assertThat(status.enemyStatuses).doesNotContain("purity");
		Assertions.assertThat(status.isRoleReady("PAYOFF_AOE")).isTrue();
	}

	@Test
	@DisplayName("SETUP_CC stays ready under paralysis lockout when a non-gas item is available")
	void paralysisImmunityKeepsNonGasSetupCc() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);
		Buff.affect(hero, Paralysis.Immunity.class, 3f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.isRoleReady("SETUP_CC")).isTrue();
	}

	@Test
	@DisplayName("SETUP_CC is not ready under paralysis lockout when ParalyticGas is the only item")
	void paralysisImmunityUnreadiesGasOnlySetupCc() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("SETUP_CC", EchoTestSupport.capability("PotionOfParalyticGas"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		giveEchoItem(boss, new PotionOfParalyticGas());
		Buff.affect(hero, Paralysis.Immunity.class, 3f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.isRoleReady("SETUP_CC")).isFalse();
	}

	@Test
	@DisplayName("hero Invulnerability unreadies MELEE, RANGED and PAYOFF_AOE")
	void invulnerabilityHardGatesDamageRoles() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);
		Buff.affect(hero, Invulnerability.class, 3f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.enemyStatuses).contains("invulnerable");
		Assertions.assertThat(status.isRoleReady("MELEE")).isFalse();
		Assertions.assertThat(status.isRoleReady("PAYOFF_AOE")).isFalse();
	}

	@Test
	@DisplayName("a decaying Barrier unreadies damage roles but leaves escape roles ready")
	void timedShieldHardGatesDamageRoles() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);
		Buff.affect(hero, Barrier.class).setShield(10);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.enemyStatuses).contains("temp_shield", "damage_immune");
		Assertions.assertThat(status.isRoleReady("MELEE")).isFalse();
		Assertions.assertThat(status.isRoleReady("KEEP_DISTANCE")).isTrue();
		Assertions.assertThat(status.isRoleReady("WAIT")).isTrue();
	}

	@Test
	@DisplayName("a fixed-duration Blocking shield also counts as temporary")
	void blockingShieldIsTemporary() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);
		Buff.affect(hero, Blocking.BlockBuff.class).setShield(5);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.enemyStatuses).contains("temp_shield", "damage_immune");
		Assertions.assertThat(status.isRoleReady("MELEE")).isFalse();
	}

	@Test
	@DisplayName("a permanent recharging shield is fought through as usual")
	void permanentShieldDoesNotDisengage() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);
		// The warrior's Broken Seal shield recharges instead of going away, so
		// waiting it out would mean waiting forever.
		BrokenSeal.WarriorShield seal = Buff.affect(hero, BrokenSeal.WarriorShield.class);
		seal.setShield(8);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(hero.shielding()).isGreaterThan(0);
		Assertions.assertThat(seal.detachesAtZero()).isFalse();
		Assertions.assertThat(status.enemyStatuses).doesNotContain("damage_immune", "temp_shield");
		Assertions.assertThat(status.isRoleReady("MELEE")).isTrue();
	}

	@Test
	@DisplayName("an empty Barrier alongside a permanent shield is not a temporary shield")
	void emptyBarrierIsNotATemporaryShield() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);
		Buff.affect(hero, Barrier.class).setShield(0);
		Buff.affect(hero, BrokenSeal.WarriorShield.class).setShield(8);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(hero.shielding()).isGreaterThan(0);
		Assertions.assertThat(status.enemyStatuses).doesNotContain("damage_immune");
		Assertions.assertThat(status.isRoleReady("MELEE")).isTrue();
	}

	@Test
	@DisplayName("damage roles are ready again once invulnerability expires")
	void damageRolesReadyWithoutInvuln() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.enemyStatuses).doesNotContain("invulnerable");
		Assertions.assertThat(status.isRoleReady("MELEE")).isTrue();
		Assertions.assertThat(status.isRoleReady("PAYOFF_AOE")).isTrue();
	}

	@Test
	@DisplayName("SETUP_CC pick walks past ParalyticGas under the paralysis lockout")
	void setupCcSkipsParalyticGasUnderLockout() {
		JSONObject cap = new JSONObject()
				.put("pick", "FIRST_LEGAL")
				.put("items", new org.json.JSONArray()
						.put("PotionOfParalyticGas")
						.put("PotionOfSnapFreeze"));
		EchoPolicyStatus lockedOut = new EchoPolicyStatus.Builder()
				.enemyStatuses(java.util.Set.of("paralysis_immunity"))
				.build();
		java.util.Set<String> available =
				java.util.Set.of("PotionOfParalyticGas", "PotionOfSnapFreeze");

		JSONObject narrowed = EchoRoleExecutor.capForEnemy("SETUP_CC", cap, lockedOut);

		Assertions.assertThat(EchoRoleResolver.resolveItemId(narrowed, available))
				.isEqualTo("PotionOfSnapFreeze");
	}

	@Test
	@DisplayName("SETUP_CC still prefers ParalyticGas when the hero is not locked out")
	void setupCcKeepsParalyticGasWithoutLockout() {
		JSONObject cap = new JSONObject()
				.put("pick", "FIRST_LEGAL")
				.put("items", new org.json.JSONArray()
						.put("PotionOfParalyticGas")
						.put("PotionOfSnapFreeze"));
		EchoPolicyStatus plain = new EchoPolicyStatus.Builder().build();
		java.util.Set<String> available =
				java.util.Set.of("PotionOfParalyticGas", "PotionOfSnapFreeze");

		JSONObject narrowed = EchoRoleExecutor.capForEnemy("SETUP_CC", cap, plain);

		Assertions.assertThat(EchoRoleResolver.resolveItemId(narrowed, available))
				.isEqualTo("PotionOfParalyticGas");
	}

	private static EchoPolicy blobPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("SETUP_CC", EchoTestSupport.capability("PotionOfSnapFreeze"))
				.put("PAYOFF_AOE", EchoTestSupport.capability("PotionOfLiquidFlame"))
				.put("MELEE", EchoTestSupport.capability("*melee"))
				.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
				.put("WAIT", EchoTestSupport.capability("*wait")));
	}

	private static EchoBoss bossWithBlobKit(Hero hero, EchoPolicy policy) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		giveEchoItem(boss, new PotionOfSnapFreeze());
		giveEchoItem(boss, new PotionOfLiquidFlame());
		return boss;
	}

	private static void giveEchoItem(
			EchoBoss boss, com.shatteredpixel.shatteredpixeldungeon.items.Item item) {
		item.identify();
		item.collect(boss.getEchoHero().belongings.backpack);
	}
}
