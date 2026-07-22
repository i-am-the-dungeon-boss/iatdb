package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import org.json.JSONArray;

import java.util.HashSet;
import java.util.Set;

/**
 * Single place for echo-hero inventory queries used by policy status + role
 * execution.
 */
public final class EchoInventory {

	private EchoInventory() {
	}

	/** Item class simple names that are currently usable (e.g. charged wands). */
	public static Set<String> availableIds(Hero echoHero) {
		Set<String> ids = new HashSet<>();
		if (echoHero == null)
			return ids;
		for (Item item : echoHero.belongings) {
			if (item instanceof Wand && ((Wand) item).curCharges <= 0) {
				continue;
			}
			if (item instanceof MagesStaff && !((MagesStaff) item).canZap()) {
				continue;
			}
			ids.add(itemId(item));
		}
		return ids;
	}

	/** Total quantity of items whose class simple name equals {@code itemId}. */
	public static int count(Hero echoHero, String itemId) {
		if (echoHero == null || itemId == null || itemId.isEmpty())
			return 0;
		int n = 0;
		for (Item item : echoHero.belongings) {
			if (itemId.equals(itemId(item))) {
				n += Math.max(1, item.quantity());
			}
		}
		return n;
	}

	/** Sum of {@link #count} across every id in {@code itemIds}. */
	public static int countMatching(Hero echoHero, JSONArray itemIds) {
		if (echoHero == null || itemIds == null)
			return 0;
		int n = 0;
		for (int i = 0; i < itemIds.length(); i++) {
			n += count(echoHero, itemIds.optString(i, ""));
		}
		return n;
	}

	/** First backpack/equipped item with the given class simple name, or null. */
	public static Item find(Hero echoHero, String itemId) {
		if (echoHero == null || itemId == null || itemId.isEmpty())
			return null;
		for (Item item : echoHero.belongings) {
			if (itemId.equals(itemId(item))) {
				return item;
			}
		}
		return null;
	}

	private static String itemId(Item item) {
		return item.getClass().getSimpleName();
	}
}
