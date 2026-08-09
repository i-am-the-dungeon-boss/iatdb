package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Shared hazard, status and role id strings used by status building, target
 * picking and the generated playbook. Keeping them here stops Java and the
 * sandbox mirror from drifting apart on a typo.
 */
public final class EchoPolicyHazards {

	public static final String FIRE_AOE = "fire_aoe";

	// Enemy status aliases.
	/** Potion of Purity / Mageroyal {@code BlobImmunity} — blocks every blob. */
	public static final String PURITY = "purity";
	/** 3-turn {@code Paralysis.Immunity} — blocks Paralysis + ParalyticGas only. */
	public static final String PARALYSIS_IMMUNITY = "paralysis_immunity";
	public static final String INVULNERABLE = "invulnerable";
	/** A shield that will run out on its own; excludes permanent recharging ones. */
	public static final String TEMP_SHIELD = "temp_shield";
	/** Aggregate: attacking is pointless right now, but the window will pass. */
	public static final String DAMAGE_IMMUNE = "damage_immune";

	// Self flags for blocked routes / sightlines.
	public static final String PLANT_BLOCKED = "plant_blocked";
	public static final String LOS_BLOCKED = "los_blocked";
	public static final String PATH_BLOCKED = "path_blocked";

	// Roles.
	public static final String PAYOFF_AOE = "PAYOFF_AOE";
	public static final String SETUP_CC = "SETUP_CC";
	public static final String MELEE = "MELEE";
	public static final String RANGED = "RANGED";
	public static final String FINISHER = "FINISHER";
	public static final String WEAPON_ABILITY = "WEAPON_ABILITY";
	public static final String ARMOR_ABILITY = "ARMOR_ABILITY";
	public static final String CLEAR_LOS = "CLEAR_LOS";
	public static final String CLEAR_PLANT = "CLEAR_PLANT";
	public static final String PATH_THROUGH = "PATH_THROUGH";
	public static final String BLIND = "BLIND";

	private static final String PARALYTIC_GAS = "PotionOfParalyticGas";

	private EchoPolicyHazards() {
	}

	/** Roles whose whole point is seeding a blob on the hero. */
	public static boolean isBlobRole(String role) {
		return SETUP_CC.equals(role) || PAYOFF_AOE.equals(role);
	}

	/**
	 * Roles that spend the turn trying to damage the hero. Includes
	 * {@code PATH_THROUGH}: its items (Disintegration, Fireblast, Dragon's
	 * Breath) are aimed at {@code enemy_cell} specifically to pierce a soft
	 * blocker and still hit the hero, so it is an attack, not just a bypass.
	 */
	public static boolean isDamageRole(String role) {
		return MELEE.equals(role)
				|| RANGED.equals(role)
				|| FINISHER.equals(role)
				|| WEAPON_ABILITY.equals(role)
				|| ARMOR_ABILITY.equals(role)
				|| PAYOFF_AOE.equals(role)
				|| PATH_THROUGH.equals(role);
	}

	/**
	 * Copy of {@code cap} without Potion of Paralytic Gas, used to ask whether
	 * {@code SETUP_CC} still has a legal item while the hero sits under the
	 * 3-turn paralysis lockout.
	 */
	public static JSONObject withoutParalyticGas(JSONObject cap) {
		if (cap == null) {
			return null;
		}
		JSONArray items = cap.optJSONArray("items");
		if (items == null) {
			return cap;
		}
		JSONArray kept = new JSONArray();
		for (int i = 0; i < items.length(); i++) {
			String id = items.optString(i, "");
			if (!PARALYTIC_GAS.equals(id)) {
				kept.put(id);
			}
		}
		JSONObject copy = new JSONObject();
		copy.put("pick", cap.optString("pick", "FIRST_LEGAL"));
		copy.put("items", kept);
		String hazard = cap.optString("hazard", "");
		if (!hazard.isEmpty()) {
			copy.put("hazard", hazard);
		}
		return copy;
	}
}
