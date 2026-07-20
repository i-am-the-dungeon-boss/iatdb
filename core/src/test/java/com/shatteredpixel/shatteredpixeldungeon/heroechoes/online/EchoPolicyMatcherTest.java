package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

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
	@DisplayName("higher-priority reaction wins when its when matches")
	void reactionsWinByPriority() {
		JSONObject root = basePolicyJson();
		JSONArray reactions = new JSONArray();
		reactions.put(reaction("setup_cc", 80, "SETUP_CC",
				new JSONObject().put("role_ready", "SETUP_CC")));
		JSONObject finishWhen = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("enemy_hp_below", 0.05))
				.put(new JSONObject().put("distance_lte", 1)));
		reactions.put(reaction("finish_him", 110, "FINISHER", finishWhen));
		root.put("reactions", reactions);
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.selfHpRatio(0.8f)
				.enemyHpRatio(0.04f)
				.distance(1)
				.rolesReady(set("FINISHER", "SETUP_CC", "MELEE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("FINISHER");
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
				new JSONObject().put("enemy_hp_below", 0.05))));
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("reactions").put("default"))
				.put("default_roles", new JSONArray().put("MELEE")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.enemyHpRatio(0.01f)
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
