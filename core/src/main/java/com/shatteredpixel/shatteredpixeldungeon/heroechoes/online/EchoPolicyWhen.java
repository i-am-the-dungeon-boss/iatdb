package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

/** Evaluates policy `when` trees against {@link EchoPolicyStatus}. */
public final class EchoPolicyWhen {

	private EchoPolicyWhen() {
	}

	public static boolean matches(JSONObject when, EchoPolicyStatus status) {
		if (when == null || when.length() == 0) {
			return true;
		}
		if (when.has("all")) {
			JSONArray all = when.optJSONArray("all");
			if (all == null)
				return false;
			for (int i = 0; i < all.length(); i++) {
				if (!matches(all.optJSONObject(i), status))
					return false;
			}
			return true;
		}
		if (when.has("any")) {
			JSONArray any = when.optJSONArray("any");
			if (any == null)
				return false;
			for (int i = 0; i < any.length(); i++) {
				if (matches(any.optJSONObject(i), status))
					return true;
			}
			return false;
		}
		return matchesLeaves(when, status);
	}

	private static boolean matchesLeaves(JSONObject when, EchoPolicyStatus status) {
		Iterator<String> keys = when.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			if ("all".equals(key) || "any".equals(key))
				continue;
			if (!matchLeaf(key, when.opt(key), status))
				return false;
		}
		return true;
	}

	private static boolean matchLeaf(String key, Object raw, EchoPolicyStatus status) {
		if (raw == null || raw == JSONObject.NULL)
			return false;
		switch (key) {
			case "self_status":
				return status.selfStatuses.contains(String.valueOf(raw));
			case "enemy_status":
				return status.enemyStatuses.contains(String.valueOf(raw));
			case "enemy_status_any":
				return anyStatus(asStrings(raw), status.enemyStatuses);
			case "enemy_status_none":
				return !anyStatus(asStrings(raw), status.enemyStatuses);
			case "enemy_hp_below":
				return status.enemyHpRatio < asDouble(raw);
			case "enemy_hp_above":
				return status.enemyHpRatio > asDouble(raw);
			case "self_hp_below":
				return status.selfHpRatio < asDouble(raw);
			case "self_hp_above":
				return status.selfHpRatio > asDouble(raw);
			case "distance_lte":
				return status.distance <= asInt(raw);
			case "distance_gte":
				return status.distance >= asInt(raw);
			case "enemy_class":
				return status.enemyClass.equalsIgnoreCase(String.valueOf(raw));
			case "role_ready":
				return status.isRoleReady(String.valueOf(raw));
			case "wants_role":
				// Soft preference: true when role is ready (same as role_ready for now).
				return status.isRoleReady(String.valueOf(raw));
			case "terrain_near":
				return status.isTerrainNear(String.valueOf(raw));
			case "terrain_near_none":
				return !status.isTerrainNear(String.valueOf(raw));
			case "on_terrain":
				return status.onTerrain.equalsIgnoreCase(String.valueOf(raw));
			case "enemy_in_los":
				return status.enemyInLos == asBoolean(raw);
			case "self_safe_for":
				return status.isSafeFor(String.valueOf(raw));
			case "self_unsafe_for":
				return status.isUnsafeFor(String.valueOf(raw));
			default:
				// Unknown keys ignored for forward compatibility.
				return true;
		}
	}

	private static boolean anyStatus(String[] wanted, java.util.Set<String> have) {
		for (String s : wanted) {
			if (have.contains(s))
				return true;
		}
		return false;
	}

	private static String[] asStrings(Object raw) {
		if (raw instanceof JSONArray) {
			JSONArray arr = (JSONArray) raw;
			String[] out = new String[arr.length()];
			for (int i = 0; i < arr.length(); i++) {
				out[i] = String.valueOf(arr.opt(i));
			}
			return out;
		}
		return new String[] { String.valueOf(raw) };
	}

	private static double asDouble(Object raw) {
		if (raw instanceof Number)
			return ((Number) raw).doubleValue();
		try {
			return Double.parseDouble(String.valueOf(raw));
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	private static int asInt(Object raw) {
		if (raw instanceof Number)
			return ((Number) raw).intValue();
		try {
			return Integer.parseInt(String.valueOf(raw));
		} catch (NumberFormatException e) {
			return Integer.MIN_VALUE;
		}
	}

	private static boolean asBoolean(Object raw) {
		if (raw instanceof Boolean)
			return (Boolean) raw;
		return Boolean.parseBoolean(String.valueOf(raw));
	}
}
