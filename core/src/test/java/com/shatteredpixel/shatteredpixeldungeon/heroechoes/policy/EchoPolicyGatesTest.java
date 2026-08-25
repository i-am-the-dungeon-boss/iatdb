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
import org.json.JSONArray;
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
	@DisplayName("SETUP_CC stays ready under paralysis lockout when a non-stun item is available")
	void paralysisImmunityKeepsNonGasSetupCc() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("SETUP_CC", EchoTestSupport.capability("StoneOfShock"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		giveEchoItem(boss, new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfShock());
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
		// Retreating from a shielded hero is disabled (EchoUntouchable.stanceFor),
		// so the shield window is spent on prep rather than on backing off.
		Assertions.assertThat(status.untouchableStance).isNotEqualTo(EchoUntouchable.Stance.RUN);
	}

	@Test
	@DisplayName("a big fixed-duration Blocking shield also counts as temporary")
	void bigBlockingShieldAlsoCountsAsTemporary() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);
		Buff.affect(hero, Blocking.BlockBuff.class).setShield(12);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.enemyStatuses).contains("temp_shield", "damage_immune");
		Assertions.assertThat(status.isRoleReady("MELEE")).isFalse();
	}

	@Test
	@DisplayName("a small Blocking shield is fought through: hitting it is what drains it")
	void smallBlockingShieldDoesNotStopTheFight() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);
		// Below EchoUntouchable.bigShield: BlockBuff's 5-turn timer resets on every
		// proc, so there is no window to outlast — and ShieldBuff.absorbDamage
		// means swinging is the fastest way to remove it.
		Buff.affect(hero, Blocking.BlockBuff.class).setShield(5);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.enemyStatuses).contains("temp_shield");
		Assertions.assertThat(status.enemyStatuses).doesNotContain("damage_immune");
		Assertions.assertThat(status.untouchableStance).isEqualTo(EchoUntouchable.Stance.NONE);
		Assertions.assertThat(status.isRoleReady("MELEE")).isTrue();
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
	@DisplayName("PATH_THROUGH is a damage role: unready while disengaging from invulnerability")
	void pathThroughHardGatedByInvulnerability() {
		Hero hero = EchoTestSupport.warriorHero();
		// KEEP_DISTANCE gives the echo somewhere to go, so the stance is RUN and
		// damage roles are gated; with nothing to run to it would rightly attack.
		EchoPolicy policy = EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("PATH_THROUGH", EchoTestSupport.capability("WandOfDisintegration"))
				.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		// Offset 1: at offset 2 the boss sits on the 7x7 level's outer ring with no
		// interior cell to retreat into, which would read as cornered.
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		giveEchoItem(
				boss, new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration());
		Buff.affect(hero, Invulnerability.class, 3f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.isRoleReady("PATH_THROUGH")).isFalse();
	}

	@Test
	@DisplayName("KEEP_DISTANCE is ready in an open room")
	void keepDistanceReadyInOpenRoom() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.isRoleReady("KEEP_DISTANCE")).isTrue();
	}

	@Test
	@DisplayName("KEEP_DISTANCE is not ready when every farther cell is walled off")
	void keepDistanceNotReadyWhenCornered() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = blobPolicy();
		EchoBoss boss = bossWithBlobKit(hero, policy);
		wallOffEverySideExceptTowardHero(hero, boss);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.isRoleReady("KEEP_DISTANCE")).isFalse();
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
	@DisplayName("SETUP_CC is not ready under paralysis lockout when Frost is the only item")
	void paralysisImmunityUnreadiesFrostOnlySetupCc() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("SETUP_CC", EchoTestSupport.capability("PotionOfFrost"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		giveEchoItem(boss, new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost());
		Buff.affect(hero, Paralysis.Immunity.class, 3f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.isRoleReady("SETUP_CC")).isFalse();
	}

	@Test
	@DisplayName("SETUP_CC pick walks past stun items under the paralysis lockout")
	void setupCcSkipsParalyticGasUnderLockout() {
		JSONObject cap = new JSONObject()
				.put("pick", "FIRST_LEGAL")
				.put("items", new org.json.JSONArray()
						.put("PotionOfParalyticGas")
						.put("PotionOfSnapFreeze")
						.put("StoneOfShock"));
		EchoPolicyStatus lockedOut = new EchoPolicyStatus.Builder()
				.enemyStatuses(java.util.Set.of("paralysis_immunity"))
				.build();
		java.util.Set<String> available =
				java.util.Set.of("PotionOfParalyticGas", "PotionOfSnapFreeze", "StoneOfShock");

		JSONObject narrowed = EchoRoleExecutor.capForEnemy("SETUP_CC", cap, lockedOut);

		Assertions.assertThat(EchoRoleResolver.resolveItemId(narrowed, available))
				.isEqualTo("StoneOfShock");
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

	/**
	 * Walls every neighbour of {@code boss} that is not strictly closer to
	 * {@code hero}, so no adjacent step increases distance from the hero.
	 */
	private static void wallOffEverySideExceptTowardHero(Hero hero, EchoBoss boss) {
		com.shatteredpixel.shatteredpixeldungeon.levels.Level level =
				com.shatteredpixel.shatteredpixeldungeon.Dungeon.level;
		int current = level.distance(boss.pos, hero.pos);
		for (int i = 0; i < com.watabou.utils.PathFinder.NEIGHBOURS8.length; i++) {
			int cell = boss.pos + com.watabou.utils.PathFinder.NEIGHBOURS8[i];
			if (!level.insideMap(cell)) {
				continue;
			}
			if (level.distance(cell, hero.pos) > current) {
				level.map[cell] = com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WALL;
			}
		}
		level.buildFlagMaps();
	}

	@Test
	@DisplayName("invulnerable hero with an open room: matcher chooses the disengage ladder")
	void untouchableInOpenRoomDisengages() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = untouchableLadderPolicy();
		EchoBoss boss = untouchableLadderBoss(hero, policy, false);
		giveEchoItem(boss, new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink());
		giveEchoItem(boss, new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste());
		Buff.affect(hero, Invulnerability.class, 3f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		EchoPlan choice =
				EchoPolicyMatcher.choose(policy, status, new java.util.HashMap<>());

		Assertions.assertThat(status.isRoleReady("BLINK")).isTrue();
		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("BLINK");
	}

	@Test
	@DisplayName("invulnerable hero, echo cornered with prep kit: preps instead of idling")
	void untouchableWhileCorneredPrepsInstead() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = untouchableLadderPolicy();
		// Cornered, and deliberately no BLINK / KEEP_DISTANCE — only prep kit.
		EchoBoss boss = untouchableLadderBoss(hero, policy, true);
		giveEchoItem(boss, new PotionOfSnapFreeze());
		Buff.affect(hero, Invulnerability.class, 3f);
		// No FOV filled: enemy_in_los reads false, same as an occluded hero.

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		EchoPlan choice =
				EchoPolicyMatcher.choose(policy, status, new java.util.HashMap<>());

		Assertions.assertThat(status.isRoleReady("KEEP_DISTANCE")).isFalse();
		Assertions.assertThat(status.isRoleReady("BLINK")).isFalse();
		Assertions.assertThat(status.untouchableStance).isEqualTo(EchoUntouchable.Stance.PREP);
		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("SETUP_CC");
	}

	@Test
	@DisplayName("no_escape self status marks the cornered case for the playbook")
	void noEscapeSelfStatusMarksTheCorneredCase() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = untouchableLadderPolicy();
		EchoBoss boss = untouchableLadderBoss(hero, policy, true);
		Buff.affect(hero, Invulnerability.class, 3f);

		EchoPolicyStatus cornered = EchoPolicyStatusBuilder.build(boss, policy);
		Assertions.assertThat(cornered.selfStatuses).contains(EchoPolicyHazards.NO_ESCAPE);

		EchoBoss open = untouchableLadderBoss(hero, policy, false);
		Assertions.assertThat(EchoPolicyStatusBuilder.build(open, policy).selfStatuses)
				.doesNotContain(EchoPolicyHazards.NO_ESCAPE);
	}

	@Test
	@DisplayName("invulnerable hero with an escape: the matcher never returns a damage role")
	void untouchableNeverChoosesADamageRole() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = untouchableLadderPolicy();
		java.util.Set<String> damageRoles =
				java.util.Set.of("MELEE", "RANGED", "FINISHER", "PAYOFF_AOE");

		EchoBoss boss = untouchableLadderBoss(hero, policy, false);
		giveEchoItem(boss, new com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink());
		giveEchoItem(boss, new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste());
		giveEchoItem(boss, new PotionOfLiquidFlame());
		Buff.affect(hero, Invulnerability.class, 3f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		EchoPlan choice =
				EchoPolicyMatcher.choose(policy, status, new java.util.HashMap<>());

		Assertions.assertThat(status.untouchableStance).isEqualTo(EchoUntouchable.Stance.RUN);
		Assertions.assertThat(status.isRoleReady("MELEE")).isFalse();
		Assertions.assertThat(status.isRoleReady("RANGED")).isFalse();
		Assertions.assertThat(status.isRoleReady("FINISHER")).isFalse();
		Assertions.assertThat(status.isRoleReady("PAYOFF_AOE")).isFalse();
		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(damageRoles).doesNotContain(choice.useRole);
	}

	@Test
	@DisplayName("invulnerable, cornered and out of prep kit: the echo attacks anyway")
	void untouchableWithNowhereToRunAttacksAnyway() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = untouchableLadderPolicy();
		// No blink, no step away, no prep kit — only the damage roles.
		EchoBoss boss = untouchableLadderBoss(hero, policy, true);
		Buff.affect(hero, Invulnerability.class, 3f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		EchoPlan choice =
				EchoPolicyMatcher.choose(policy, status, new java.util.HashMap<>());

		Assertions.assertThat(status.untouchableStance).isEqualTo(EchoUntouchable.Stance.FIGHT);
		Assertions.assertThat(status.isRoleReady("MELEE")).isTrue();
		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(java.util.Set.of("MELEE", "RANGED", "FINISHER", "PAYOFF_AOE"))
				.contains(choice.useRole);
	}

	@Test
	@DisplayName("no shipped policy lists WAIT in default_roles")
	void defaultRolesNeverResolveToAWait() {
		JSONArray[] shipped = {
				EchoPolicy.fallback().root().getJSONObject("selection")
						.getJSONArray("default_roles"),
				com.shatteredpixel.shatteredpixeldungeon.heroechoes.debug.DebugStrategyKit.policy()
						.root().getJSONObject("selection").getJSONArray("default_roles")
		};
		for (JSONArray roles : shipped) {
			for (int i = 0; i < roles.length(); i++) {
				Assertions.assertThat(roles.optString(i, "")).isNotEqualTo("WAIT");
			}
		}
	}

	/**
	 * Minimal reaction ladder shaped like the real playbook: self-preservation,
	 * then the untouchable block, then normal combat. No attack reaction carries
	 * an {@code enemy_status_none: damage_immune} clause — Java's
	 * {@code allowedAgainstEnemy} hard gate is the only thing standing between
	 * them and firing, which is exactly the guarantee these tests exercise.
	 */
	private static EchoPolicy untouchableLadderPolicy() {
		JSONObject damageImmune = new JSONObject().put("enemy_status", "damage_immune");
		return EchoPolicy.fromJson(new JSONObject()
				.put("policy_schema_version", EchoTestSupport.TEST_GAME_VERSION)
				.put("capabilities", new JSONObject()
						.put("BLINK", EchoTestSupport.capability("StoneOfBlink"))
						.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
						.put("HASTE", EchoTestSupport.capability("PotionOfHaste"))
						.put("CLEAR_LOS", EchoTestSupport.capability("PotionOfLiquidFlame"))
						.put("MELEE", EchoTestSupport.capability("*melee"))
						.put("RANGED", EchoTestSupport.capability("SpiritBow"))
						.put("FINISHER", EchoTestSupport.capability("*melee"))
						.put("PAYOFF_AOE", EchoTestSupport.capability("PotionOfToxicGas"))
						.put("SETUP_CC", EchoTestSupport.capability("PotionOfSnapFreeze"))
						.put("HOLD", EchoTestSupport.capability("*wait")))
				.put("reactions", new JSONArray()
						.put(reaction("untouchable_blink", 99, damageImmune, "BLINK", null))
						.put(reaction("untouchable_step", 98, damageImmune, "KEEP_DISTANCE", null))
						.put(reaction("untouchable_haste", 97, damageImmune, "HASTE", null))
						// Cornered prep, mirroring the backend untouchable_prep_* block.
						.put(reaction(
								"untouchable_prep_cc", 89,
								new JSONObject().put("all", new JSONArray()
										.put(damageImmune)
										.put(new JSONObject().put("self_status", EchoPolicyHazards.NO_ESCAPE))
										.put(new JSONObject().put("role_ready", "SETUP_CC"))),
								"SETUP_CC", "enemy_cell"))
						// Stand-in for clear_bush_for_los: real los_blocked sensing is
						// separate future work, so this keys on enemy_in_los alone.
						.put(reaction(
								"clear_bush_for_los", 90,
								new JSONObject().put("enemy_in_los", false),
								"CLEAR_LOS", "bush_cell"))
						.put(reaction("finish_him", 110, new JSONObject(), "FINISHER", null))
						.put(reaction("ranged_poke", 74, new JSONObject(), "RANGED", "enemy_cell"))
						.put(reaction("melee_adjacent", 72, new JSONObject(), "MELEE", null))
						.put(reaction("payoff", 76, new JSONObject(), "PAYOFF_AOE", "enemy_cell")))
				.put("recipes", new JSONArray())
				.put("positioning", new JSONObject())
				.put("matchups", new JSONObject())
				.put("selection", new JSONObject()
						.put("order", new JSONArray()
								.put("reactions").put("recipes").put("positioning")
								.put("matchups").put("default"))
						.put("default_roles", new JSONArray().put("MELEE")))
				.put("tuning", new JSONObject()));
	}

	/**
	 * Boss with items for the damage roles the gate is supposed to unready
	 * (SpiritBow, a toxic potion), so the test proves the gate does something
	 * rather than the roles simply having no item. Escape kit (BLINK / HASTE)
	 * and prep kit (fire) are left to each call site, since which of those is
	 * present is what each scenario is testing. Walled off on every farther
	 * side if {@code cornered}.
	 */
	private static EchoBoss untouchableLadderBoss(Hero hero, EchoPolicy policy, boolean cornered) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		giveEchoItem(boss, new com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow());
		giveEchoItem(boss, new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas());
		if (cornered) {
			wallOffEverySideExceptTowardHero(hero, boss);
		}
		return boss;
	}

	private static JSONObject reaction(
			String id, int priority, JSONObject when, String useRole, String target) {
		JSONObject dof = new JSONObject().put("use_role", useRole);
		if (target != null) {
			dof.put("target", target);
		}
		return new JSONObject()
				.put("id", id)
				.put("priority", priority)
				.put("when", when)
				.put("do", dof);
	}

	private static EchoPolicy blobPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("SETUP_CC", EchoTestSupport.capability("PotionOfSnapFreeze"))
				.put("PAYOFF_AOE", EchoTestSupport.capability("PotionOfLiquidFlame"))
				.put("MELEE", EchoTestSupport.capability("*melee"))
				.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
				.put("HOLD", EchoTestSupport.capability("*wait")));
	}

	private static EchoBoss bossWithBlobKit(Hero hero, EchoPolicy policy) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		// Offset 1, not 2: Level.insideMap excludes the outer ring on the 7x7
		// test level, so a boss placed 2 cells out sits on that boundary and has
		// no interior cell left to retreat into — a false "cornered" reading.
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
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
