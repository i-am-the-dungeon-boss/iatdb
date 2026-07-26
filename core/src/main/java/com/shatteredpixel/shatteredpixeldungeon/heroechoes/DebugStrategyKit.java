package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStamina;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfFear;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfShock;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact hero debug-start loadout (+ matching local policy) for exercising
 * kite, blink escape, finish/shield gates — without flooding the player bag.
 * Arena floor drops and echo arsenal keep their own full catalogs.
 */
public final class DebugStrategyKit {

	/** Small stacks so a fight can spend a few without dumping dozens. */
	public static final int POTION_STACK = 2;

	private DebugStrategyKit() {
	}

	public static List<Item> createItems() {
		Potion.initColors();
		// Potion.identify → setKnown needs a live Dungeon.hero (tests often have none).
		Hero previous = Dungeon.hero;
		boolean stubbed = previous == null || !previous.isAlive();
		if (stubbed) {
			Hero stub = new Hero();
			stub.HP = stub.HT = 1;
			Dungeon.hero = stub;
		}
		try {
			List<Item> items = new ArrayList<>();
			items.add(stackPotion(new PotionOfHealing()));
			items.add(stackPotion(new PotionOfShielding()));
			items.add(stackPotion(new PotionOfHaste()));
			items.add(stackPotion(new PotionOfStamina()));
			items.add(stackPotion(new PotionOfInvisibility()));
			items.add(stackPotion(new PotionOfFrost()));
			items.add(stackPotion(new PotionOfParalyticGas()));
			items.add(stackPotion(new PotionOfLiquidFlame()));
			items.add(stackPotion(new PotionOfCorrosiveGas()));
			items.add(prepare(new StoneOfBlink()));
			items.add(prepare(new StoneOfFear()));
			items.add(prepare(new StoneOfShock()));
			items.add(charged(new WandOfFireblast()));
			items.add(charged(new WandOfMagicMissile()));
			items.add(charged(new WandOfBlastWave()));
			return items;
		} finally {
			if (stubbed) {
				Dungeon.hero = previous;
			}
		}
	}

	/**
	 * Local merged-style policy for the strategy kit (Mage/Huntress kite loop +
	 * universal finish/heal/shield gates).
	 */
	public static EchoPolicy policy() {
		JSONObject caps = new JSONObject();
		caps.put("HEAL", role("FIRST_LEGAL", "PotionOfShielding", "PotionOfHealing"));
		caps.put("CLEANSE_BURN", role("FIRST_LEGAL", "PotionOfFrost"));
		caps.put("HASTE", role("FIRST_LEGAL", "PotionOfHaste", "PotionOfStamina"));
		caps.put("INVIS", role("FIRST_LEGAL", "PotionOfInvisibility"));
		caps.put("SETUP_CC", role("FIRST_LEGAL", "PotionOfParalyticGas", "StoneOfShock"));
		caps.put("KNOCKBACK", role("FIRST_LEGAL", "WandOfBlastWave"));
		caps.put("BLINK", role("FIRST_LEGAL", "StoneOfBlink"));
		caps.put("FEAR", role("FIRST_LEGAL", "StoneOfFear"));
		caps.put("PAYOFF_AOE", role("MAX_DAMAGE", "PotionOfCorrosiveGas", "PotionOfLiquidFlame")
				.put("hazard", "fire_aoe"));
		caps.put("RANGED", role("MAX_DAMAGE",
				"WandOfFireblast", "WandOfBlastWave", "WandOfMagicMissile"));
		caps.put("FINISHER", role("MAX_DAMAGE",
				"WandOfFireblast", "WandOfBlastWave", "WandOfMagicMissile", "*melee"));
		caps.put("MELEE", role("FIRST_LEGAL", "*melee"));
		caps.put("KEEP_DISTANCE", role("FIRST_LEGAL", "*move_further"));
		caps.put("CLOSE_IN", role("FIRST_LEGAL", "*move_closer"));
		caps.put("LEAVE_AOE", role("FIRST_LEGAL", "*leave_aoe"));
		caps.put("WAIT", role("FIRST_LEGAL", "*wait"));

		JSONArray reactions = new JSONArray()
				.put(reaction("finish_him", 110, "FINISHER", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("enemy_hp_below", 0.15))
						.put(new JSONObject().put("enemy_shield_below", 0.25))
						.put(new JSONObject().put("any", new JSONArray()
								.put(new JSONObject().put("distance_lte", 1))
								.put(new JSONObject().put("role_ready", "RANGED")))))))
				.put(reaction("leave_aoe_dot", 109, "LEAVE_AOE", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("self_status", "aoe_dot"))
						.put(new JSONObject().put("role_ready", "LEAVE_AOE")))))
				.put(reaction("blink_escape", 106, "BLINK", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("self_hp_below", 0.35))
						.put(new JSONObject().put("distance_lte", 2))
						.put(new JSONObject().put("role_ready", "BLINK"))), "blink_cell"))
				.put(reaction("heal_when_hurt", 105, "HEAL", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("self_hp_below", 0.35))
						.put(new JSONObject().put("role_ready", "HEAL")))))
				.put(reaction("haste_disengage", 103, "HASTE", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("self_hp_below", 0.4))
						.put(new JSONObject().put("distance_lte", 2))
						.put(new JSONObject().put("role_ready", "HASTE")))))
				.put(reaction("invis_escape", 102, "INVIS", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("self_hp_below", 0.3))
						.put(new JSONObject().put("distance_lte", 2))
						.put(new JSONObject().put("role_ready", "INVIS")))))
				.put(reaction("kite_blink", 86, "BLINK", kiteEdgeWhen("BLINK"), "blink_cell"))
				.put(reaction("kite_knockback", 85, "KNOCKBACK", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("distance_lte", 1))
						.put(new JSONObject().put("role_ready", "KNOCKBACK"))), "enemy_cell"))
				.put(reaction("kite_cc", 84, "SETUP_CC", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("distance_lte", 1))
						.put(new JSONObject().put("enemy_status_none",
								new JSONArray().put("paralysed").put("frozen")))
						.put(new JSONObject().put("role_ready", "SETUP_CC"))), "enemy_cell"))
				.put(reaction("kite_fear", 83, "FEAR", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("distance_lte", 1))
						.put(new JSONObject().put("enemy_status_none", new JSONArray().put("terror")))
						.put(new JSONObject().put("role_ready", "FEAR"))), "enemy_cell"))
				.put(reaction("setup_cc", 80, "SETUP_CC", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("enemy_status_none",
								new JSONArray().put("paralysed").put("frozen")))
						.put(new JSONObject().put("distance_gte", 2))
						.put(new JSONObject().put("role_ready", "SETUP_CC"))), "enemy_cell"))
				.put(reaction("haste_kite", 78, "HASTE", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("self_hp_above", 0.4))
						.put(new JSONObject().put("distance_lte", 3))
						.put(new JSONObject().put("distance_gte", 2))
						.put(new JSONObject().put("self_status_none", "haste"))
						.put(new JSONObject().put("role_ready", "HASTE")))))
				.put(reaction("standalone_payoff_aoe", 76, "PAYOFF_AOE", new JSONObject().put("all",
						new JSONArray()
								.put(new JSONObject().put("role_ready", "PAYOFF_AOE"))
								.put(new JSONObject().put("self_safe_for", "PAYOFF_AOE"))
								.put(new JSONObject().put("distance_gte", 2))
								.put(new JSONObject().put("enemy_shield_below", 0.25))),
						"enemy_cell"))
				.put(reaction("ranged_poke", 74, "RANGED", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("distance_gte", 2))
						.put(new JSONObject().put("enemy_in_los", true))
						.put(new JSONObject().put("role_ready", "RANGED"))), "enemy_cell"))
				.put(reaction("kite_step", 73, "KEEP_DISTANCE", kiteEdgeWhen("KEEP_DISTANCE")));

		JSONObject positioning = new JSONObject()
				.put("MAGE", new JSONObject().put("ideal_distance", 3).put("if_farther", "CLOSE_IN"))
				.put("HUNTRESS", new JSONObject().put("ideal_distance", 3).put("if_farther", "CLOSE_IN"))
				.put("DEFAULT", new JSONObject().put("ideal_distance", 1).put("if_farther", "CLOSE_IN"));

		JSONObject root = new JSONObject();
		root.put("policy_schema_version", EchoPolicy.supportedSchemaVersion());
		root.put("capabilities", caps);
		root.put("reactions", reactions);
		root.put("recipes", new JSONArray());
		root.put("positioning", positioning);
		root.put("matchups", new JSONObject());
		root.put("selection", new JSONObject()
				.put("order", new JSONArray()
						.put("reactions").put("recipes").put("positioning")
						.put("matchups").put("default"))
				.put("default_roles", new JSONArray().put("RANGED").put("MELEE").put("WAIT")));
		root.put("tuning", new JSONObject()
				.put("aggression", 0.55)
				.put("finish_hp", 0.15)
				.put("retreat_hp", 0.3));
		return EchoPolicy.fromJson(root);
	}

	private static JSONObject kiteEdgeWhen(String role) {
		return new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("distance_lte", 1))
				.put(new JSONObject().put("any", new JSONArray()
						.put(new JSONObject().put("self_status", "haste"))
						.put(new JSONObject().put("self_status", "stamina"))
						.put(new JSONObject().put("enemy_status_any",
								new JSONArray().put("paralysed").put("frozen").put("terror")))))
				.put(new JSONObject().put("role_ready", role))
				.put(new JSONObject().put("role_ready", "RANGED")));
	}

	private static JSONObject role(String pick, String... items) {
		JSONArray arr = new JSONArray();
		for (String item : items) {
			arr.put(item);
		}
		return new JSONObject().put("pick", pick).put("items", arr);
	}

	private static JSONObject reaction(String id, int priority, String role, JSONObject when) {
		return reaction(id, priority, role, when, null);
	}

	private static JSONObject reaction(
			String id, int priority, String role, JSONObject when, String target) {
		JSONObject dof = new JSONObject().put("use_role", role);
		if (target != null) {
			dof.put("target", target);
		}
		return new JSONObject()
				.put("id", id)
				.put("priority", priority)
				.put("when", when)
				.put("do", dof);
	}

	private static Potion stackPotion(Potion potion) {
		potion.identify();
		potion.quantity(POTION_STACK);
		return potion;
	}

	private static Item prepare(Item item) {
		item.identify();
		item.quantity(1);
		return item;
	}

	private static Wand charged(Wand wand) {
		wand.identify();
		wand.curCharges = wand.maxCharges;
		wand.curChargeKnown = true;
		return wand;
	}
}
