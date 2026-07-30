package com.shatteredpixel.shatteredpixeldungeon.heroechoes.debug;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.InventoryStone;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Debug helper: fill an echo kit with potions, scrolls, wands, and throwables,
 * then install a policy that spends them one per turn via FIRST_LEGAL.
 * Lit bombs use {@link #ROLE_BOMB} first (LIGHTTHROW via {@link Bomb#throwAs});
 * other throwables use THROW; potions split drink vs throw.
 * Class armor abilities are a separate button via
 * {@link #grantArmorAbilityAll()}.
 */
public final class DebugEchoArsenal {

	/** Non-throw arsenal (scrolls / wands / inventory stones). */
	public static final String ROLE = "ARSENAL";
	public static final String ROLE_DRINK = "DRINK";
	public static final String ROLE_THROW = "THROW";
	/**
	 * Lit bomb throws ({@link Bomb#throwAs} / LIGHTTHROW) — prioritized over plain
	 * THROW.
	 */
	public static final String ROLE_BOMB = "BOMB";
	/** Class armor ability only — installed by {@link #grantArmorAbility}. */
	public static final String ROLE_ARMOR = "ARMOR";

	private DebugEchoArsenal() {
	}

	/**
	 * Grants arsenal items to every living echo boss and switches them to a
	 * cycle-through policy. Debug builds only.
	 *
	 * @return number of echo bosses updated
	 */
	public static int grantAndCycleAll() {
		if (!DebugSettings.isDebugBuild() || Dungeon.level == null) {
			return 0;
		}
		int updated = 0;
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob instanceof EchoBoss && mob.isAlive()) {
				grantAndCycle((EchoBoss) mob);
				updated++;
			}
		}
		return updated;
	}

	public static void grantAndCycle(EchoBoss boss) {
		if (boss == null || boss.getEchoHero() == null) {
			throw new IllegalArgumentException("echo boss requires a kit hero");
		}
		Hero kit = boss.getEchoHero();
		clearArsenalItems(kit);
		List<Item> items = usableItems();
		for (Item item : items) {
			forceCollect(kit, item);
		}
		boss.replacePolicy(cyclePolicy(items));
		boss.state = boss.HUNTING;
		if (Dungeon.hero != null) {
			boss.aggro(Dungeon.hero);
		}
	}

	/**
	 * Equips charged class armor, rotates the kit armor ability, and installs a
	 * policy that only uses that ClassArmor. Debug builds only via
	 * {@link #grantArmorAbilityAll()}.
	 *
	 * @return display name of the ability now assigned
	 */
	public static String grantArmorAbility(EchoBoss boss) {
		if (boss == null || boss.getEchoHero() == null) {
			throw new IllegalArgumentException("echo boss requires a kit hero");
		}
		Hero kit = boss.getEchoHero();
		ClassArmor armor = ensureClassArmor(kit);
		cycleArmorAbility(kit);
		boss.replacePolicy(armorAbilityPolicy(armor));
		boss.state = boss.HUNTING;
		if (Dungeon.hero != null) {
			boss.aggro(Dungeon.hero);
		}
		return kit.armorAbility != null ? kit.armorAbility.name() : "";
	}

	/**
	 * Grants armor ability to every living echo boss. Debug builds only.
	 *
	 * @return number of echo bosses updated
	 */
	public static int grantArmorAbilityAll() {
		if (!DebugSettings.isDebugBuild() || Dungeon.level == null) {
			return 0;
		}
		int updated = 0;
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob instanceof EchoBoss && mob.isAlive()) {
				grantArmorAbility((EchoBoss) mob);
				updated++;
			}
		}
		return updated;
	}

	/** Policy that only light-uses the equipped class armor ability. */
	static EchoPolicy armorAbilityPolicy(ClassArmor armor) {
		if (armor == null) {
			throw new IllegalArgumentException("class armor required");
		}
		JSONArray items = new JSONArray().put(armor.getClass().getSimpleName());
		JSONObject caps = new JSONObject()
				.put(ROLE_ARMOR, new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", items));
		JSONObject root = new JSONObject();
		root.put("policy_schema_version", EchoPolicy.supportedSchemaVersion());
		root.put("capabilities", caps);
		root.put("reactions", new JSONArray());
		root.put("recipes", new JSONArray());
		root.put("positioning", new JSONObject());
		root.put("matchups", new JSONObject());
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("default"))
				.put("default_roles", new JSONArray().put(ROLE_ARMOR).put("WAIT")));
		root.put("tuning", new JSONObject());
		return new EchoPolicy(root);
	}

	/**
	 * Equips charged class armor for the kit's hero class (creates one if needed).
	 */
	static ClassArmor ensureClassArmor(Hero kit) {
		if (kit == null) {
			throw new IllegalArgumentException("echo kit hero required");
		}
		ClassArmor armor;
		if (kit.belongings.armor instanceof ClassArmor) {
			armor = (ClassArmor) kit.belongings.armor;
		} else {
			Armor base = kit.belongings.armor instanceof Armor
					? (Armor) kit.belongings.armor
					: new ClothArmor();
			armor = ClassArmor.upgrade(kit, base);
			kit.belongings.armor = armor;
			armor.activate(kit);
		}
		armor.charge = 100f;
		armor.identify();
		return armor;
	}

	/**
	 * Advances {@link Hero#armorAbility} through the class's armor abilities so
	 * repeated arsenal grants exercise each skill.
	 */
	static void cycleArmorAbility(Hero kit) {
		if (kit == null || kit.heroClass == null) {
			return;
		}
		ArmorAbility[] options = kit.heroClass.armorAbilities();
		if (options == null || options.length == 0) {
			return;
		}
		int next = 0;
		if (kit.armorAbility != null) {
			for (int i = 0; i < options.length; i++) {
				if (options[i].getClass() == kit.armorAbility.getClass()) {
					next = (i + 1) % options.length;
					break;
				}
			}
		}
		kit.armorAbility = options[next];
	}

	/** Drop prior arsenal copies so re-grant stays at 1 use each. */
	static void clearArsenalItems(Hero kit) {
		if (kit == null || kit.belongings == null || kit.belongings.backpack == null) {
			return;
		}
		Iterator<Item> it = kit.belongings.backpack.items.iterator();
		while (it.hasNext()) {
			if (isPolicyUsable(it.next())) {
				it.remove();
			}
		}
	}

	public static List<Item> usableItems() {
		List<Item> usable = new ArrayList<>();
		for (Item item : DebugArenaItems.createAll()) {
			if (isPolicyUsable(item)) {
				prepare(item);
				usable.add(item);
			}
		}
		return usable;
	}

	public static EchoPolicy cyclePolicy(List<Item> items) {
		JSONArray drinkIds = new JSONArray();
		JSONArray bombIds = new JSONArray();
		JSONArray throwIds = new JSONArray();
		JSONArray otherIds = new JSONArray();
		for (Item item : items) {
			String id = item.getClass().getSimpleName();
			if (item instanceof Potion) {
				if (isThrowPotion((Potion) item)) {
					throwIds.put(id);
				} else if (isDrinkPotion((Potion) item)) {
					drinkIds.put(id);
				}
			} else if (item instanceof Bomb) {
				bombIds.put(id);
			} else if (isThrowable(item)) {
				throwIds.put(id);
			} else {
				otherIds.put(id);
			}
		}

		// Temporarily disabled — door-break spam while tuning invis / fight flow.
		// JSONArray doorBreakIds = new JSONArray();
		// for (Item item : items) {
		// if (EchoPolicyMatcher.isDoorBreakItem(item)) {
		// doorBreakIds.put(item.getClass().getSimpleName());
		// }
		// }

		JSONObject caps = new JSONObject();
		if (bombIds.length() > 0) {
			// Point aim at hero — lit bombs land on the target cell (LIGHTTHROW).
			caps.put(ROLE_BOMB, new JSONObject()
					.put("pick", "FIRST_LEGAL")
					.put("items", bombIds));
		}
		if (drinkIds.length() > 0) {
			caps.put(ROLE_DRINK, new JSONObject()
					.put("pick", "FIRST_LEGAL")
					.put("items", drinkIds));
		}
		if (throwIds.length() > 0) {
			// No AOE hazard: point throwables (stones, hammers, darts) must aim at
			// the hero. A role-wide hazard makes EchoTargetPicker pick a neighbour
			// cell (often SW of the hero) and miss.
			caps.put(ROLE_THROW, new JSONObject()
					.put("pick", "FIRST_LEGAL")
					.put("items", throwIds));
		}
		if (otherIds.length() > 0) {
			caps.put(ROLE, new JSONObject()
					.put("pick", "FIRST_LEGAL")
					.put("items", otherIds));
		}
		// if (doorBreakIds.length() > 0) {
		// caps.put("DOOR_BREAK", new JSONObject()
		// .put("pick", "FIRST_LEGAL")
		// .put("items", doorBreakIds));
		// }

		JSONArray defaults = new JSONArray();
		if (bombIds.length() > 0) {
			defaults.put(ROLE_BOMB);
		}
		if (drinkIds.length() > 0) {
			defaults.put(ROLE_DRINK);
		}
		if (throwIds.length() > 0) {
			defaults.put(ROLE_THROW);
		}
		if (otherIds.length() > 0) {
			defaults.put(ROLE);
		}
		defaults.put("WAIT");

		JSONArray reactions = new JSONArray();
		// Temporarily disabled — door-break spam while tuning invis / fight flow.
		// if (doorBreakIds.length() > 0) {
		// reactions.put(doorBreakReaction());
		// }
		if (bombIds.length() > 0) {
			reactions.put(blindDefenseReaction(ROLE_BOMB, 101));
		}
		if (otherIds.length() > 0) {
			reactions.put(blindDefenseReaction(ROLE, 100));
		}
		if (throwIds.length() > 0) {
			reactions.put(blindDefenseReaction(ROLE_THROW, 99));
		}

		JSONObject root = new JSONObject();
		root.put("policy_schema_version", EchoPolicy.supportedSchemaVersion());
		root.put("capabilities", caps);
		root.put("reactions", reactions);
		root.put("recipes", new JSONArray());
		root.put("positioning", new JSONObject());
		root.put("matchups", new JSONObject());
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("reactions").put("default"))
				.put("default_roles", defaults));
		root.put("tuning", new JSONObject());
		return new EchoPolicy(root);
	}

	private static JSONObject doorBreakReaction() {
		return new JSONObject()
				.put("id", "door_break")
				.put("priority", 101)
				.put("when", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("door_stalling", true))
						.put(new JSONObject().put("role_ready", "DOOR_BREAK"))))
				.put("do", new JSONObject().put("use_role", "DOOR_BREAK").put("target", "door_cell"));
	}

	private static JSONObject blindDefenseReaction(String role, int priority) {
		return new JSONObject()
				.put("id", "blind_defense_" + role.toLowerCase(java.util.Locale.ROOT))
				.put("priority", priority)
				.put("when", new JSONObject().put("all", new JSONArray()
						.put(new JSONObject().put("enemy_in_los", false))
						.put(new JSONObject().put("enemy_status", "invisible"))
						.put(new JSONObject().put("role_ready", role))))
				.put("do", new JSONObject().put("use_role", role).put("target", "enemy_cell"));
	}

	/**
	 * Arsenal is potions/scrolls (1 each), wands (1 charge), and
	 * throwables/stones (1 unit).
	 */
	static boolean isPolicyUsable(Item item) {
		if (item instanceof Potion) {
			return isDrinkPotion((Potion) item) || isThrowPotion((Potion) item);
		}
		return item instanceof Scroll
				|| item instanceof Wand
				|| item instanceof InventoryStone
				|| isThrowable(item);
	}

	/**
	 * Non-potion items the executor throws at a cell: missiles, bombs, and
	 * throwable runestones (not inventory stones).
	 */
	static boolean isThrowable(Item item) {
		if (item == null) {
			return false;
		}
		return item instanceof MissileWeapon
				|| item instanceof Bomb
				|| (item instanceof Runestone && !(item instanceof InventoryStone));
	}

	/** Gas / shatter / brew potions — thrown at the enemy. */
	public static boolean isThrowPotion(Potion potion) {
		if (potion == null) {
			return false;
		}
		if (potion instanceof com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.Brew) {
			return true;
		}
		Potion.ensureColors();
		Item.clearCurrent();
		Hero previous = Dungeon.hero;
		boolean stubbed = previous == null || !previous.isAlive();
		if (stubbed) {
			Hero stub = new Hero();
			stub.HP = stub.HT = 1;
			Dungeon.hero = stub;
		}
		try {
			potion.identify();
			return Potion.AC_THROW.equals(potion.defaultAction());
		} finally {
			if (stubbed) {
				Dungeon.hero = previous;
			}
		}
	}

	/** Buff / heal potions — drunk on the echo body (includes dual-mode choose). */
	public static boolean isDrinkPotion(Potion potion) {
		if (potion == null || isHeroOnlyDrink(potion)) {
			return false;
		}
		return !isThrowPotion(potion);
	}

	private static boolean isHeroOnlyDrink(Potion potion) {
		return potion instanceof PotionOfStrength
				|| potion instanceof PotionOfExperience
				|| potion instanceof ElixirOfMight
				|| potion instanceof PotionOfMindVision
				|| potion instanceof PotionOfMagicalSight;
	}

	private static void prepare(Item item) {
		Item.clearCurrent();
		item.identify();
		// One of each: quantity for stackables/throwables, charges for wands.
		// Finite-durability missiles also get one throw of durability left.
		item.quantity(1);
		if (item instanceof Wand) {
			Wand wand = (Wand) item;
			wand.curCharges = 1;
			wand.curChargeKnown = true;
		}
		if (item instanceof MissileWeapon) {
			limitMissileToOneThrow((MissileWeapon) item);
		}
	}

	/**
	 * Missiles keep durability uses even at qty 1 — clamp to a single throw
	 * when durability applies. Infinite-use missiles (e.g. Dart) stay qty-limited.
	 */
	private static void limitMissileToOneThrow(MissileWeapon missile) {
		float perUse = missile.durabilityPerUse();
		if (perUse <= 0f) {
			return;
		}
		try {
			java.lang.reflect.Field durability = MissileWeapon.class.getDeclaredField("durability");
			durability.setAccessible(true);
			durability.setFloat(missile, perUse);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("failed to limit missile durability", e);
		}
	}

	/** Bypasses bag capacity — debug only. */
	static void forceCollect(Hero hero, Item item) {
		if (hero == null || item == null) {
			return;
		}
		prepare(item);
		hero.belongings.backpack.items.add(item);
		// Wand.collect normally attaches Charger; keep that for ward energy caps.
		if (item instanceof Wand) {
			((Wand) item).charge(hero);
		}
	}
}
