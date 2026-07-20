package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

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
	public static EchoPolicyChoice choose(
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
			EchoPolicyChoice choice = null;
			switch (layer) {
				case "reactions":
					choice = matchReactions(root.optJSONArray("reactions"), status);
					break;
				case "recipes":
					choice = matchRecipes(root.optJSONArray("recipes"), status, recipeSteps);
					break;
				case "positioning":
					choice = matchPositioning(root.optJSONObject("positioning"), status);
					break;
				case "matchups":
					choice = matchMatchups(root.optJSONObject("matchups"), status);
					break;
				case "default":
					choice = matchDefaultRoles(
							selection != null ? selection.optJSONArray("default_roles") : null,
							status);
					break;
				default:
					break;
			}
			if (choice != null) {
				return choice;
			}
		}

		// Optional escape-hatch rules[] after selection order.
		EchoPolicyChoice rules = matchReactions(root.optJSONArray("rules"), status);
		if (rules != null) {
			return new EchoPolicyChoice(rules.useRole, "rules", null);
		}
		return null;
	}

	private static EchoPolicyChoice matchReactions(JSONArray reactions, EchoPolicyStatus status) {
		if (reactions == null || reactions.length() == 0)
			return null;
		List<JSONObject> sorted = new ArrayList<>();
		for (int i = 0; i < reactions.length(); i++) {
			JSONObject r = reactions.optJSONObject(i);
			if (r != null)
				sorted.add(r);
		}
		sorted.sort(Comparator.comparingInt((JSONObject r) -> r.optInt("priority", 0)).reversed());

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
			return new EchoPolicyChoice(role, "reactions", null);
		}
		return null;
	}

	private static EchoPolicyChoice matchRecipes(
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
		sorted.sort(Comparator.comparingInt((JSONObject r) -> r.optInt("priority", 0)).reversed());

		Map<String, Integer> steps = recipeSteps != null ? recipeSteps : Collections.emptyMap();
		for (JSONObject recipe : sorted) {
			String id = recipe.optString("id", "");
			JSONArray stepArr = recipe.optJSONArray("steps");
			if (stepArr == null || stepArr.length() == 0)
				continue;
			int idx = steps.getOrDefault(id, 0);
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
			return new EchoPolicyChoice(role, "recipes", id);
		}
		return null;
	}

	private static EchoPolicyChoice matchPositioning(JSONObject positioning, EchoPolicyStatus status) {
		if (positioning == null)
			return null;
		JSONObject stance = positioning.optJSONObject(status.selfClass);
		if (stance == null)
			stance = positioning.optJSONObject(status.enemyClass);
		if (stance == null)
			stance = positioning.optJSONObject("DEFAULT");
		if (stance == null)
			return null;

		int ideal = stance.optInt("ideal_distance", 1);
		String role = null;
		if (status.distance < ideal) {
			role = optRole(stance, "if_closer");
		} else if (status.distance > ideal) {
			role = optRole(stance, "if_farther");
		} else {
			// At ideal: only HOLD if explicitly set; else fall through.
			role = optRole(stance, "if_at_ideal");
		}
		if (role == null || role.isEmpty() || !status.isRoleReady(role)) {
			return null;
		}
		return new EchoPolicyChoice(role, "positioning", null);
	}

	private static EchoPolicyChoice matchMatchups(JSONObject matchups, EchoPolicyStatus status) {
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
				return new EchoPolicyChoice(role, "matchups", null);
			}
		}
		return null;
	}

	private static EchoPolicyChoice matchDefaultRoles(JSONArray defaults, EchoPolicyStatus status) {
		if (defaults == null)
			return null;
		for (int i = 0; i < defaults.length(); i++) {
			String role = defaults.optString(i, "");
			if (!role.isEmpty() && status.isRoleReady(role)) {
				return new EchoPolicyChoice(role, "default", null);
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
}
