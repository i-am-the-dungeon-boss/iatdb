package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.debug.DebugStrategyKit;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(GdxTestExtension.class)
class EchoPolicyMatcherTest {

	@Test
	@DisplayName("blind_defense_ranged reaction picks RANGED when enemy is invisible and out of LOS")
	void blindDefenseRangedReactionPicksRanged() {
		JSONObject root = basePolicyJson();
		JSONObject when = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("enemy_in_los", false))
				.put(new JSONObject().put("enemy_status", "invisible"))
				.put(new JSONObject().put("role_ready", "RANGED")));
		root.put("reactions", new JSONArray().put(
				reaction("blind_defense_ranged", 100, "RANGED", when)));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.enemyInLos(false)
				.distance(2)
				.enemyStatuses(set("invisible"))
				.rolesReady(set("RANGED", "CLOSE_IN", "MELEE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("RANGED");
		Assertions.assertThat(choice.layer).isEqualTo("reactions");
	}

	@Test
	@DisplayName("blind_defense_ranged reaction is skipped while enemy is in LOS")
	void blindDefenseRangedSkippedWhenVisible() {
		JSONObject root = basePolicyJson();
		JSONObject when = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("enemy_in_los", false))
				.put(new JSONObject().put("enemy_status", "invisible"))
				.put(new JSONObject().put("role_ready", "RANGED")));
		root.put("reactions", new JSONArray().put(
				reaction("blind_defense_ranged", 100, "RANGED", when)));
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("reactions").put("default"))
				.put("default_roles", new JSONArray().put("MELEE")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.enemyInLos(true)
				.enemyStatuses(set("invisible"))
				.rolesReady(set("RANGED", "MELEE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("MELEE");
		Assertions.assertThat(choice.layer).isEqualTo("default");
	}

	@Test
	@DisplayName("blind_defense_ranged reaction is skipped when occluded but not invisible")
	void blindDefenseRangedSkippedWhenOnlyOccluded() {
		JSONObject root = basePolicyJson();
		JSONObject when = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("enemy_in_los", false))
				.put(new JSONObject().put("enemy_status", "invisible"))
				.put(new JSONObject().put("role_ready", "RANGED")));
		root.put("reactions", new JSONArray().put(
				reaction("blind_defense_ranged", 100, "RANGED", when)));
		root.put("selection", new JSONObject()
				.put("order", new JSONArray()
						.put("reactions").put("recipes").put("positioning")
						.put("matchups").put("default"))
				.put("default_roles", new JSONArray().put("CLOSE_IN")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.enemyInLos(false)
				.distance(2)
				.rolesReady(set("RANGED", "CLOSE_IN"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("CLOSE_IN");
		Assertions.assertThat(choice.layer).isNotEqualTo("reactions");
	}

	@Test
	@DisplayName("higher-priority reaction wins when its when matches")
	void reactionsWinByPriority() {
		JSONObject root = basePolicyJson();
		JSONArray reactions = new JSONArray();
		reactions.put(reaction("setup_cc", 80, "SETUP_CC",
				new JSONObject().put("role_ready", "SETUP_CC")));
		JSONObject finishWhen = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("enemy_hp_below", 0.15))
				.put(new JSONObject().put("enemy_shield_below", 0.25))
				.put(new JSONObject().put("distance_lte", 1)));
		reactions.put(reaction("finish_him", 110, "FINISHER", finishWhen));
		root.put("reactions", reactions);
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.selfHpRatio(0.8f)
				.enemyHpRatio(0.10f)
				.enemyShieldRatio(0f)
				.distance(1)
				.rolesReady(set("FINISHER", "SETUP_CC", "MELEE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("FINISHER");
		Assertions.assertThat(choice.layer).isEqualTo("reactions");
	}

	@Test
	@DisplayName("finish_him skips when enemy barrier is high even if HP is critical")
	void finishHimSkipsHighShield() {
		JSONObject root = basePolicyJson();
		JSONObject finishWhen = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("enemy_hp_below", 0.15))
				.put(new JSONObject().put("enemy_shield_below", 0.25))
				.put(new JSONObject().put("distance_lte", 1)));
		root.put("reactions", new JSONArray()
				.put(reaction("finish_him", 110, "FINISHER", finishWhen)));
		root.put("selection", new JSONObject()
				.put("order", new JSONArray()
						.put("reactions").put("recipes").put("positioning")
						.put("matchups").put("default"))
				.put("default_roles", new JSONArray().put("MELEE")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.enemyHpRatio(0.10f)
				.enemyShieldRatio(0.5f)
				.distance(1)
				.rolesReady(set("FINISHER", "MELEE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice.useRole).isEqualTo("MELEE");
		Assertions.assertThat(choice.layer).isEqualTo("default");
	}

	@Test
	@DisplayName("without RANGED ready, melee_adjacent is chosen over KEEP_DISTANCE")
	void kiteStepSkipsWithoutRanged() {
		EchoPolicy policy = DebugStrategyKit.policy();

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.selfHpRatio(0.8f)
				.enemyHpRatio(0.8f)
				.distance(1)
				.enemyInLos(true)
				.selfClass("MAGE")
				.selfStatuses(set("stamina"))
				.rolesReady(set("KEEP_DISTANCE", "CLOSE_IN", "MELEE", "WAIT", "HOLD"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice.useRole).isEqualTo("MELEE");
		Assertions.assertThat(choice.layer).isEqualTo("reactions");
	}

	@Test
	@DisplayName("kite_step picks KEEP_DISTANCE when RANGED is ready with kite edge")
	void kiteStepWithRangedReady() {
		EchoPolicy policy = DebugStrategyKit.policy();

		// No LOS so ranged_poke (priority 74) does not outrank kite_step (73).
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.selfHpRatio(0.8f)
				.enemyHpRatio(0.8f)
				.distance(1)
				.enemyInLos(false)
				.selfClass("MAGE")
				.selfStatuses(set("stamina"))
				.rolesReady(set("KEEP_DISTANCE", "CLOSE_IN", "MELEE", "RANGED", "WAIT", "HOLD"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice.useRole).isEqualTo("KEEP_DISTANCE");
		Assertions.assertThat(choice.layer).isEqualTo("reactions");
	}

	@Test
	@DisplayName("positioning emits KEEP_DISTANCE when closer than ideal")
	void positioningWhenTooClose() {
		JSONObject root = basePolicyJson();
		root.put("reactions", new JSONArray());
		root.put("recipes", new JSONArray());
		root.put("positioning", new JSONObject()
				.put("MAGE", new JSONObject()
						.put("ideal_distance", 3)
						.put("if_closer", "KEEP_DISTANCE")
						.put("if_closer_require_role", "RANGED")
						.put("if_farther", "CLOSE_IN")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.selfHpRatio(0.8f)
				.enemyHpRatio(0.8f)
				.distance(1)
				.selfClass("MAGE")
				.enemyClass("WARRIOR")
				.rolesReady(set("KEEP_DISTANCE", "MELEE", "RANGED"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice.useRole).isEqualTo("KEEP_DISTANCE");
		Assertions.assertThat(choice.layer).isEqualTo("positioning");
	}

	@Test
	@DisplayName("positioning skips KEEP_DISTANCE when if_closer_require_role is not ready")
	void positioningSkipsKeepDistanceWithoutRequiredRole() {
		JSONObject root = basePolicyJson();
		root.put("reactions", new JSONArray());
		root.put("recipes", new JSONArray());
		root.put("positioning", new JSONObject()
				.put("MAGE", new JSONObject()
						.put("ideal_distance", 3)
						.put("if_closer", "KEEP_DISTANCE")
						.put("if_closer_require_role", "RANGED")
						.put("if_farther", "CLOSE_IN")));
		root.put("selection", new JSONObject()
				.put("order", new JSONArray()
						.put("reactions").put("recipes").put("positioning")
						.put("matchups").put("default"))
				.put("default_roles", new JSONArray().put("MELEE")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.distance(1)
				.selfClass("MAGE")
				.rolesReady(set("KEEP_DISTANCE", "MELEE", "CLOSE_IN"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice.useRole).isEqualTo("MELEE");
		Assertions.assertThat(choice.layer).isEqualTo("default");
	}

	@Test
	@DisplayName("positioning still CLOSE_IN when far even if if_closer_require_role is not ready")
	void positioningCloseInIgnoresCloserRequireRole() {
		JSONObject root = basePolicyJson();
		root.put("reactions", new JSONArray());
		root.put("recipes", new JSONArray());
		root.put("positioning", new JSONObject()
				.put("MAGE", new JSONObject()
						.put("ideal_distance", 3)
						.put("if_closer", "KEEP_DISTANCE")
						.put("if_closer_require_role", "RANGED")
						.put("if_farther", "CLOSE_IN")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.distance(5)
				.selfClass("MAGE")
				.rolesReady(set("KEEP_DISTANCE", "CLOSE_IN", "MELEE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice.useRole).isEqualTo("CLOSE_IN");
		Assertions.assertThat(choice.layer).isEqualTo("positioning");
	}

	@Test
	@DisplayName("wantsKeepDistance is true for ranged ideal spacing even without if_closer")
	void wantsKeepDistanceFromIdealSpacing() {
		JSONObject root = basePolicyJson();
		root.put("positioning", new JSONObject()
				.put("MAGE", new JSONObject()
						.put("ideal_distance", 3)
						.put("if_farther", "CLOSE_IN")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.distance(1)
				.selfClass("MAGE")
				.rolesReady(set("KEEP_DISTANCE", "RANGED", "MELEE"))
				.build();

		Assertions.assertThat(EchoPolicyMatcher.wantsKeepDistance(policy, status)).isTrue();
	}

	@Test
	@DisplayName("wantsKeepDistance is false for melee ideal spacing without if_closer")
	void wantsKeepDistanceFalseForMeleeIdeal() {
		JSONObject root = basePolicyJson();
		root.put("positioning", new JSONObject()
				.put("WARRIOR", new JSONObject()
						.put("ideal_distance", 1)
						.put("if_farther", "CLOSE_IN")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.distance(1)
				.selfClass("WARRIOR")
				.rolesReady(set("KEEP_DISTANCE", "MELEE"))
				.build();

		Assertions.assertThat(EchoPolicyMatcher.wantsKeepDistance(policy, status)).isFalse();
	}

	@Test
	@DisplayName("at ideal distance positioning falls through to default_roles")
	void positioningFallsThroughAtIdeal() {
		JSONObject root = basePolicyJson();
		root.put("reactions", new JSONArray());
		root.put("recipes", new JSONArray());
		root.put("positioning", new JSONObject()
				.put("DEFAULT", new JSONObject()
						.put("ideal_distance", 1)
						.put("if_farther", "CLOSE_IN")));
		root.put("selection", new JSONObject()
				.put("order", new JSONArray()
						.put("reactions").put("recipes").put("positioning")
						.put("matchups").put("default"))
				.put("default_roles", new JSONArray().put("RANGED").put("MELEE")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.selfHpRatio(0.8f)
				.enemyHpRatio(0.8f)
				.distance(1)
				.rolesReady(set("RANGED", "MELEE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice.useRole).isEqualTo("RANGED");
		Assertions.assertThat(choice.layer).isEqualTo("default");
	}

	@Test
	@DisplayName("recipes evaluate the step at the current recipe index")
	void recipesEvaluateCurrentStep() {
		JSONObject root = basePolicyJson();
		root.put("reactions", new JSONArray());
		root.put("recipes", new JSONArray().put(new JSONObject()
				.put("id", "gas_then_ignite")
				.put("priority", 75)
				.put("steps", new JSONArray()
						.put(new JSONObject()
								.put("when", new JSONObject().put("role_ready", "SETUP_CC"))
								.put("do", new JSONObject().put("use_role", "SETUP_CC")))
						.put(new JSONObject()
								.put("when", new JSONObject().put("role_ready", "PAYOFF_AOE"))
								.put("do", new JSONObject().put("use_role", "PAYOFF_AOE"))))));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.selfHpRatio(0.8f)
				.enemyHpRatio(0.8f)
				.distance(2)
				.rolesReady(set("SETUP_CC", "PAYOFF_AOE", "MELEE"))
				.build();

		EchoPolicyChoice first = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());
		Assertions.assertThat(first.useRole).isEqualTo("SETUP_CC");

		java.util.Map<String, Integer> steps = new java.util.HashMap<>();
		steps.put("gas_then_ignite", 1);
		EchoPolicyChoice second = EchoPolicyMatcher.choose(policy, status, steps);
		Assertions.assertThat(second.useRole).isEqualTo("PAYOFF_AOE");
	}

	@Test
	@DisplayName("positioning emits CLOSE_IN when farther than ideal")
	void positioningWhenTooFar() {
		JSONObject root = basePolicyJson();
		root.put("reactions", new JSONArray());
		root.put("recipes", new JSONArray());
		root.put("positioning", new JSONObject()
				.put("DEFAULT", new JSONObject()
						.put("ideal_distance", 1)
						.put("if_farther", "CLOSE_IN")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.distance(3)
				.rolesReady(set("CLOSE_IN", "MELEE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice.useRole).isEqualTo("CLOSE_IN");
		Assertions.assertThat(choice.layer).isEqualTo("positioning");
	}

	@Test
	@DisplayName("matchups prefer_roles picks the first ready role")
	void matchupsPreferRoles() {
		JSONObject root = basePolicyJson();
		root.put("reactions", new JSONArray());
		root.put("recipes", new JSONArray());
		root.put("positioning", new JSONObject());
		root.put("matchups", new JSONObject()
				.put("DEFAULT", new JSONObject()
						.put("prefer_roles", new JSONArray().put("RANGED").put("MELEE"))));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.distance(2)
				.rolesReady(set("RANGED", "MELEE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice.useRole).isEqualTo("RANGED");
		Assertions.assertThat(choice.layer).isEqualTo("matchups");
	}

	@Test
	@DisplayName("skips reaction when role is not ready")
	void skipsReactionWhenRoleNotReady() {
		JSONObject root = basePolicyJson();
		root.put("reactions", new JSONArray().put(reaction(
				"finish_him", 110, "FINISHER",
				new JSONObject().put("enemy_hp_below", 0.15))));
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("reactions").put("default"))
				.put("default_roles", new JSONArray().put("MELEE")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.enemyHpRatio(0.10f)
				.distance(1)
				.rolesReady(set("MELEE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice.useRole).isEqualTo("MELEE");
		Assertions.assertThat(choice.layer).isEqualTo("default");
	}

	@Test
	@DisplayName("rules escape hatch matches after selection order misses")
	void rulesEscapeHatch() {
		JSONObject root = basePolicyJson();
		root.put("reactions", new JSONArray());
		root.put("recipes", new JSONArray());
		root.put("positioning", new JSONObject());
		root.put("matchups", new JSONObject());
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("reactions").put("default"))
				.put("default_roles", new JSONArray()));
		root.put("rules", new JSONArray().put(reaction(
				"wait_rule", 1, "WAIT",
				new JSONObject().put("role_ready", "WAIT"))));
		root.put("capabilities", root.getJSONObject("capabilities")
				.put("WAIT", cap("*wait")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.rolesReady(set("WAIT"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("WAIT");
		Assertions.assertThat(choice.layer).isEqualTo("rules");
	}

	@Test
	@DisplayName("melee_adjacent beats default RANGED when adjacent without kite edge")
	void meleeAdjacentBeatsDefaultRangedWhenAdjacent() {
		JSONObject root = basePolicyJson();
		JSONObject pokeWhen = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("distance_gte", 2))
				.put(new JSONObject().put("role_ready", "RANGED")));
		JSONObject kiteWhen = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("distance_lte", 1))
				.put(new JSONObject().put("any", new JSONArray()
						.put(new JSONObject().put("self_status", "haste"))
						.put(new JSONObject().put("self_speed_gt_enemy", true))))
				.put(new JSONObject().put("role_ready", "KEEP_DISTANCE"))
				.put(new JSONObject().put("role_ready", "RANGED")));
		JSONObject meleeWhen = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("distance_lte", 1))
				.put(new JSONObject().put("enemy_status_none", new JSONArray().put("invisible")))
				.put(new JSONObject().put("role_ready", "MELEE")));
		root.put("reactions", new JSONArray()
				.put(reaction("ranged_poke", 74, "RANGED", pokeWhen))
				.put(reaction("kite_step", 73, "KEEP_DISTANCE", kiteWhen))
				.put(reaction("melee_adjacent", 72, "MELEE", meleeWhen)));
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("reactions").put("default"))
				.put("default_roles", new JSONArray().put("RANGED").put("MELEE")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.distance(1)
				.selfSpeedGtEnemy(false)
				.rolesReady(set("RANGED", "MELEE", "KEEP_DISTANCE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice.useRole).isEqualTo("MELEE");
		Assertions.assertThat(choice.layer).isEqualTo("reactions");
	}

	@Test
	@DisplayName("melee_adjacent skips when hero is invisible so blind defense can use RANGED")
	void meleeAdjacentSkipsWhenHeroInvisible() {
		JSONObject root = basePolicyJson();
		JSONObject blindWhen = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("enemy_in_los", false))
				.put(new JSONObject().put("enemy_status", "invisible"))
				.put(new JSONObject().put("role_ready", "RANGED")));
		JSONObject meleeWhen = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("distance_lte", 1))
				.put(new JSONObject().put("enemy_status_none", new JSONArray().put("invisible")))
				.put(new JSONObject().put("role_ready", "MELEE")));
		root.put("reactions", new JSONArray()
				.put(reaction("blind_defense_ranged", 100, "RANGED", blindWhen))
				.put(reaction("melee_adjacent", 72, "MELEE", meleeWhen)));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.distance(1)
				.enemyInLos(false)
				.enemyStatuses(set("invisible"))
				.rolesReady(set("RANGED", "MELEE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice.useRole).isEqualTo("RANGED");
		Assertions.assertThat(choice.layer).isEqualTo("reactions");
	}

	@Test
	@DisplayName("kite_step wins over melee_adjacent when self is faster than enemy")
	void kiteStepWinsWhenSelfFasterThanEnemy() {
		JSONObject root = basePolicyJson();
		JSONObject kiteWhen = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("distance_lte", 1))
				.put(new JSONObject().put("any", new JSONArray()
						.put(new JSONObject().put("self_status", "haste"))
						.put(new JSONObject().put("self_speed_gt_enemy", true))))
				.put(new JSONObject().put("role_ready", "KEEP_DISTANCE"))
				.put(new JSONObject().put("role_ready", "RANGED")));
		JSONObject meleeWhen = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("distance_lte", 1))
				.put(new JSONObject().put("enemy_status_none", new JSONArray().put("invisible")))
				.put(new JSONObject().put("role_ready", "MELEE")));
		root.put("reactions", new JSONArray()
				.put(reaction("kite_step", 73, "KEEP_DISTANCE", kiteWhen))
				.put(reaction("melee_adjacent", 72, "MELEE", meleeWhen)));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.distance(1)
				.selfSpeedGtEnemy(true)
				.rolesReady(set("RANGED", "MELEE", "KEEP_DISTANCE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice.useRole).isEqualTo("KEEP_DISTANCE");
		Assertions.assertThat(choice.layer).isEqualTo("reactions");
	}

	@Test
	@DisplayName("unsupported policy yields no choice")
	void unsupportedPolicyYieldsNull() {
		EchoPolicy policy = EchoPolicy.fromJson("{"
				+ "\"policy_schema_version\":\"0.0.1\""
				+ "}");

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(
				policy,
				new EchoPolicyStatus.Builder().rolesReady(set("MELEE")).build(),
				Collections.emptyMap());

		Assertions.assertThat(choice).isNull();
	}

	private static JSONObject basePolicyJson() {
		return new JSONObject()
				.put("policy_schema_version", "0.0.1")
				.put("capabilities", new JSONObject()
						.put("FINISHER", cap("*melee"))
						.put("SETUP_CC", cap("PotionOfParalyticGas"))
						.put("PAYOFF_AOE", cap("PotionOfLiquidFlame"))
						.put("KEEP_DISTANCE", cap("*move_further"))
						.put("CLOSE_IN", cap("*move_closer"))
						.put("RANGED", cap("MagesStaff"))
						.put("MELEE", cap("*melee")))
				.put("reactions", new JSONArray())
				.put("recipes", new JSONArray())
				.put("positioning", new JSONObject())
				.put("matchups", new JSONObject().put("DEFAULT", new JSONObject()))
				.put("selection", new JSONObject()
						.put("order", new JSONArray()
								.put("reactions").put("recipes").put("positioning")
								.put("matchups").put("default"))
						.put("default_roles", new JSONArray().put("MELEE").put("WAIT")))
				.put("tuning", new JSONObject().put("terrain_near_tiles", 3));
	}

	private static JSONObject cap(String item) {
		return new JSONObject()
				.put("pick", "FIRST_LEGAL")
				.put("items", new JSONArray().put(item));
	}

	private static JSONObject reaction(String id, int priority, String role, JSONObject when) {
		return new JSONObject()
				.put("id", id)
				.put("priority", priority)
				.put("when", when)
				.put("do", new JSONObject().put("use_role", role));
	}

	private static Set<String> set(String... values) {
		return new HashSet<>(Arrays.asList(values));
	}
}
