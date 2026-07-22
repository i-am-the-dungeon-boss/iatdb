package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
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
import java.util.List;

/**
 * Debug helper: fill an echo kit with potions, scrolls, wands, and throwables,
 * then install a policy that spends them one per turn via FIRST_LEGAL.
 * Potions are split into drink vs throw roles by type.
 */
public final class DebugEchoArsenal {

	/** Non-potion arsenal (scrolls / wands / stones / missiles / bombs). */
	public static final String ROLE = "ARSENAL";
	public static final String ROLE_DRINK = "DRINK";
	public static final String ROLE_THROW = "THROW";

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

	/** Drop prior arsenal copies so re-grant stays at 1 use each. */
	static void clearArsenalItems(Hero kit) {
		if (kit == null || kit.belongings == null || kit.belongings.backpack == null) {
			return;
		}
		kit.belongings.backpack.items.removeIf(DebugEchoArsenal::isPolicyUsable);
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
			} else {
				otherIds.put(id);
			}
		}

		JSONObject caps = new JSONObject();
		if (drinkIds.length() > 0) {
			caps.put(ROLE_DRINK, new JSONObject()
					.put("pick", "FIRST_LEGAL")
					.put("items", drinkIds));
		}
		if (throwIds.length() > 0) {
			caps.put(ROLE_THROW, new JSONObject()
					.put("pick", "FIRST_LEGAL")
					.put("items", throwIds)
					.put("hazard", "aoe"));
		}
		if (otherIds.length() > 0) {
			caps.put(ROLE, new JSONObject()
					.put("pick", "FIRST_LEGAL")
					.put("items", otherIds));
		}

		JSONArray defaults = new JSONArray();
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

		JSONObject root = new JSONObject();
		root.put("policy_schema_version", EchoPolicy.supportedSchemaVersion());
		root.put("capabilities", caps);
		root.put("reactions", new JSONArray());
		root.put("recipes", new JSONArray());
		root.put("positioning", new JSONObject());
		root.put("matchups", new JSONObject());
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("default"))
				.put("default_roles", defaults));
		root.put("tuning", new JSONObject());
		return new EchoPolicy(root);
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
				|| item instanceof Runestone
				|| item instanceof MissileWeapon
				|| item instanceof Bomb;
	}

	/** Gas / shatter / brew potions — thrown at the enemy. */
	public static boolean isThrowPotion(Potion potion) {
		if (potion == null) {
			return false;
		}
		if (potion instanceof com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.Brew) {
			return true;
		}
		Potion.initColors();
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
	}
}
