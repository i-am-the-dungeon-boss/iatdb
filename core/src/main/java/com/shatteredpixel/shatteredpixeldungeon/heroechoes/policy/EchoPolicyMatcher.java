package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Walks {@code selection.order}: reactions → recipes → positioning → matchups →
 * default.
 */
public final class EchoPolicyMatcher {

	private EchoPolicyMatcher() {
	}

	/**
	 * @param recipeSteps current step index per recipe id (mutated by caller after
	 *                    successful execute)
	 */
	public static EchoPlan choose(
			EchoPolicy policy,
			EchoPolicyStatus status,
			Map<String, Integer> recipeSteps) {
		JSONObject root = policy.root();
		JSONObject selection = root.optJSONObject("selection");
		JSONArray order = selection != null ? selection.optJSONArray("order") : null;
		if (order == null || order.length() == 0) {
			order = new JSONArray()
					.put("reactions").put("recipes").put("positioning")
					.put("matchups").put("default");
		}

		for (int i = 0; i < order.length(); i++) {
			String layer = order.optString(i, "");
			EchoPlan plan = null;
			switch (layer) {
				case "reactions":
					plan = matchReactions(root.optJSONArray("reactions"), status);
					break;
				case "recipes":
					plan = matchRecipes(root.optJSONArray("recipes"), status, recipeSteps);
					break;
				case "positioning":
					plan = matchPositioning(root.optJSONObject("positioning"), status);
					break;
				case "matchups":
					plan = matchMatchups(root.optJSONObject("matchups"), status);
					break;
				case "default":
					plan = matchDefaultRoles(
							selection != null ? selection.optJSONArray("default_roles") : null,
							status);
					break;
				default:
					break;
			}
			if (plan != null) {
				return plan;
			}
		}

		// Optional escape-hatch rules[] after selection order.
		EchoPlan rules = matchReactions(root.optJSONArray("rules"), status);
		if (rules != null) {
			return EchoPlan.resolve(rules.useRole, "rules", null, status);
		}
		return null;
	}

	private static EchoPlan matchReactions(JSONArray reactions, EchoPolicyStatus status) {
		if (reactions == null || reactions.length() == 0)
			return null;
		List<JSONObject> sorted = new ArrayList<>();
		for (int i = 0; i < reactions.length(); i++) {
			JSONObject r = reactions.optJSONObject(i);
			if (r != null)
				sorted.add(r);
		}
		sortByPriorityDescending(sorted);

		for (JSONObject r : sorted) {
			JSONObject when = r.optJSONObject("when");
			JSONObject dof = r.optJSONObject("do");
			if (dof == null)
				continue;
			String role = dof.optString("use_role", "");
			if (role.isEmpty() || !status.isRoleReady(role))
				continue;
			if (!EchoPolicyWhen.matches(when, status))
				continue;
			return EchoPlan.resolve(role, "reactions", null, status);
		}
		return null;
	}

	private static EchoPlan matchRecipes(
			JSONArray recipes,
			EchoPolicyStatus status,
			Map<String, Integer> recipeSteps) {
		if (recipes == null || recipes.length() == 0)
			return null;
		List<JSONObject> sorted = new ArrayList<>();
		for (int i = 0; i < recipes.length(); i++) {
			JSONObject r = recipes.optJSONObject(i);
			if (r != null)
				sorted.add(r);
		}
		sortByPriorityDescending(sorted);

		Map<String, Integer> steps = recipeSteps != null ? recipeSteps : Collections.emptyMap();
		for (JSONObject recipe : sorted) {
			String id = recipe.optString("id", "");
			JSONArray stepArr = recipe.optJSONArray("steps");
			if (stepArr == null || stepArr.length() == 0)
				continue;
			Integer stepIdx = steps.get(id);
			int idx = stepIdx != null ? stepIdx : 0;
			if (idx < 0 || idx >= stepArr.length())
				continue;
			JSONObject step = stepArr.optJSONObject(idx);
			if (step == null)
				continue;
			JSONObject dof = step.optJSONObject("do");
			if (dof == null)
				continue;
			String role = dof.optString("use_role", "");
			if (role.isEmpty() || !status.isRoleReady(role))
				continue;
			if (!EchoPolicyWhen.matches(step.optJSONObject("when"), status))
				continue;
			return EchoPlan.resolve(role, "recipes", id, status);
		}
		return null;
	}

	/**
	 * True when the echo prefers open space: active {@code KEEP_DISTANCE}
	 * positioning, or a ranged stance ({@code ideal_distance > 1}) even when
	 * step-away is gated to haste/CC reactions. Used by leave-AoE direction.
	 */
	public static boolean wantsKeepDistance(EchoPolicy policy, EchoPolicyStatus status) {
		if (policy == null || status == null) {
			return false;
		}
		JSONObject positioning = policy.root().optJSONObject("positioning");
		EchoPlan plan = matchPositioning(positioning, status);
		if (plan != null && "KEEP_DISTANCE".equals(plan.useRole)) {
			return true;
		}
		JSONObject stance = stanceFor(positioning, status);
		return stance != null && stance.optInt("ideal_distance", 1) > 1;
	}

	private static JSONObject stanceFor(JSONObject positioning, EchoPolicyStatus status) {
		if (positioning == null) {
			return null;
		}
		JSONObject stance = positioning.optJSONObject(status.selfClass);
		if (stance == null) {
			stance = positioning.optJSONObject(status.enemyClass);
		}
		if (stance == null) {
			stance = positioning.optJSONObject("DEFAULT");
		}
		return stance;
	}

	private static EchoPlan matchPositioning(JSONObject positioning, EchoPolicyStatus status) {
		JSONObject stance = stanceFor(positioning, status);
		if (stance == null)
			return null;

		int ideal = stance.optInt("ideal_distance", 1);
		String role = null;
		if (status.distance < ideal) {
			role = optRole(stance, "if_closer");
			String requireRole = stance.optString("if_closer_require_role", "");
			if (!requireRole.isEmpty() && !status.isRoleReady(requireRole)) {
				role = null;
			}
		} else if (status.distance > ideal) {
			role = optRole(stance, "if_farther");
		} else {
			// At ideal: only HOLD if explicitly set; else fall through.
			role = optRole(stance, "if_at_ideal");
		}
		if (role == null || role.isEmpty() || !status.isRoleReady(role)) {
			return null;
		}
		return EchoPlan.resolve(role, "positioning", null, status);
	}

	private static EchoPlan matchMatchups(JSONObject matchups, EchoPolicyStatus status) {
		if (matchups == null)
			return null;
		JSONObject entry = matchups.optJSONObject(status.enemyClass);
		if (entry == null)
			entry = matchups.optJSONObject(status.selfClass);
		if (entry == null)
			entry = matchups.optJSONObject("DEFAULT");
		if (entry == null)
			return null;

		JSONArray prefer = entry.optJSONArray("prefer_roles");
		if (prefer == null)
			return null;
		for (int i = 0; i < prefer.length(); i++) {
			String role = prefer.optString(i, "");
			if (!role.isEmpty() && status.isRoleReady(role)) {
				return EchoPlan.resolve(role, "matchups", null, status);
			}
		}
		return null;
	}

	private static EchoPlan matchDefaultRoles(JSONArray defaults, EchoPolicyStatus status) {
		if (defaults == null)
			return null;
		for (int i = 0; i < defaults.length(); i++) {
			String role = defaults.optString(i, "");
			if (!role.isEmpty() && status.isRoleReady(role)) {
				return EchoPlan.resolve(role, "default", null, status);
			}
		}
		return null;
	}

	private static String optRole(JSONObject stance, String key) {
		if (!stance.has(key) || stance.isNull(key))
			return null;
		String v = stance.optString(key, "");
		return v.isEmpty() ? null : v;
	}

	private static void sortByPriorityDescending(List<JSONObject> sorted) {
		Collections.sort(sorted, new Comparator<JSONObject>() {
			@Override
			public int compare(JSONObject a, JSONObject b) {
				return Integer.compare(b.optInt("priority", 0), a.optInt("priority", 0));
			}
		});
	}
}
