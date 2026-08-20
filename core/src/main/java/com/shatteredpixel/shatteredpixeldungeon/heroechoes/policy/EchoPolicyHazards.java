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
	/** 3-turn {@code Paralysis.Immunity} — shared hard-stun lockout in the echo fight. */
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
	/**
	 * The echo wanted to disengage from an untouchable hero and could not — no
	 * blink landing and no adjacent cell farther from the hero. JSON-visible
	 * projection of {@link EchoUntouchable.Stance#PREP} /
	 * {@link EchoUntouchable.Stance#FIGHT}.
	 */
	public static final String NO_ESCAPE = "no_escape";
	/** The echo is carrying its own temporary shield right now. */
	public static final String SELF_SHIELDED = "self_shielded";

	// Role ids, sourced from EchoRole so there is exactly one spelling of each.
	// Prefer EchoRole directly in new code; these remain for existing call sites.
	public static final String PAYOFF_AOE = EchoRole.PAYOFF_AOE.id();
	public static final String SETUP_CC = EchoRole.SETUP_CC.id();
	public static final String MELEE = EchoRole.MELEE.id();
	public static final String RANGED = EchoRole.RANGED.id();
	public static final String FINISHER = EchoRole.FINISHER.id();
	public static final String WEAPON_ABILITY = EchoRole.WEAPON_ABILITY.id();
	public static final String ARMOR_ABILITY = EchoRole.ARMOR_ABILITY.id();
	public static final String CLEAR_LOS = EchoRole.CLEAR_LOS.id();
	public static final String CLEAR_PLANT = EchoRole.CLEAR_PLANT.id();
	public static final String PATH_THROUGH = EchoRole.PATH_THROUGH.id();
	public static final String BLIND = EchoRole.BLIND.id();
	public static final String CLOSE_IN = EchoRole.CLOSE_IN.id();
	public static final String KEEP_DISTANCE = EchoRole.KEEP_DISTANCE.id();
	public static final String BLINK = EchoRole.BLINK.id();
	/** Strategy-only hold: legal solely via the matcher's {@code if_at_ideal}. */
	public static final String HOLD = EchoRole.HOLD.id();
	public static final String KNOCKBACK = EchoRole.KNOCKBACK.id();
	public static final String FEAR = EchoRole.FEAR.id();
	/** Barrier-granting kit the echo turns on itself; distinct from ARCANE_ARMOR. */
	public static final String SHIELD_SELF = EchoRole.SHIELD_SELF.id();

	private static final String[] SETUP_CC_STUN_ITEMS = {
			"PotionOfParalyticGas",
			"PotionOfFrost",
			"PotionOfSnapFreeze",
			"FlashBangBomb",
	};

	private EchoPolicyHazards() {
	}

	/** Roles whose whole point is seeding a blob on the hero. */
	public static boolean isBlobRole(String role) {
		return EchoRole.isBlobRole(role);
	}

	/**
	 * Roles that spend the turn trying to damage the hero. Includes
	 * {@code PATH_THROUGH}: its items (Disintegration, Fireblast, Dragon's
	 * Breath) are aimed at {@code enemy_cell} specifically to pierce a soft
	 * blocker and still hit the hero, so it is an attack, not just a bypass.
	 */
	public static boolean isDamageRole(String role) {
		return EchoRole.isDamageRole(role);
	}

	/** Roles whose whole point is putting distance between echo and hero. */
	public static boolean isDisengageRole(String role) {
		return EchoRole.isDisengageRole(role);
	}

	/** Roles the echo can usefully spend a wasted window on — see EchoUntouchable.PREP_ORDER. */
	public static boolean isPrepRole(String role) {
		return EchoRole.isPrepRole(role);
	}

	/**
	 * Copy of {@code cap} without SETUP_CC stun items (gas, frost, flashbang,
	 * snap freeze), used while the hero sits under the shared stun lockout.
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
			if (!isSetupCcStunItem(id)) {
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

	private static boolean isSetupCcStunItem(String id) {
		for (int i = 0; i < SETUP_CC_STUN_ITEMS.length; i++) {
			if (SETUP_CC_STUN_ITEMS[i].equals(id)) {
				return true;
			}
		}
		return false;
	}
}
