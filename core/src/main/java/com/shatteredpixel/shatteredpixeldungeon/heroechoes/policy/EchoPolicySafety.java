package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlast;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Things the playbook must not get a vote on.
 * <p>
 * The generated playbook owns <em>with what</em>: which items serve a role, in
 * what preference order, and the numbers behind those choices (see
 * {@code hero-echoes/src/lib/echo/policy.ts}). It cannot own anything that
 * depends on the live board, because it is generated once, off the device,
 * from a kit card — it does not know where the echo is standing this turn, what
 * the hero is immune to right now, or what a thrown item would land on.
 * <p>
 * Everything in that second set lives client-side, and this class is where it
 * lives. Membership test: <em>would a wrong answer here be the playbook's fault
 * or the board's?</em> If the board's, it belongs here. Current members:
 * <ul>
 * <li>{@link #withoutSelfBlast} — an item whose blast covers the echo itself at
 * this range is dropped from the pick list. Range is a per-turn fact.</li>
 * <li>{@link #withoutParalyticGas} — stun items are dropped while the hero sits
 * under the shared stun lockout. Immunity is a per-turn fact.</li>
 * <li>{@code EchoBoss.forceStalledDoor} — <em>when</em> a door that keeps
 * shutting in the echo's face comes down. The playbook is the thing causing the
 * dance, so it can never be the thing that ends it; it only names the items via
 * {@code capabilities.DOOR_BREAK}, and the turn count is
 * {@code tuning.door_force_turns}.</li>
 * </ul>
 * New rules of this shape go here rather than becoming another one-off, and
 * their backend counterpart stays a knob or an item list — never a reaction.
 */
public final class EchoPolicySafety {

	private static final String[] SETUP_CC_STUN_ITEMS = {
			"PotionOfParalyticGas",
			"PotionOfFrost",
			"PotionOfSnapFreeze",
			"FlashBangBomb",
	};

	/** Bombs and blast stones detonate over {@code NEIGHBOURS9} — one cell out. */
	private static final int BLAST_RADIUS = 1;

	private EchoPolicySafety() {
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
		return withItems(cap, kept);
	}

	/**
	 * Copy of {@code cap} without items that would put their own blast on the
	 * echo. {@code targetDistance} is the echo's distance to the cell the item
	 * would be aimed at, so the same capability is safe at range and unsafe at
	 * point blank — which is exactly what the playbook cannot know.
	 *
	 * @param echoHero the kit the ids are read out of; ids it does not hold are
	 *                 left in place, since the resolver drops them anyway
	 */
	public static JSONObject withoutSelfBlast(JSONObject cap, Hero echoHero, int targetDistance) {
		if (cap == null) {
			return null;
		}
		JSONArray items = cap.optJSONArray("items");
		if (items == null || targetDistance > BLAST_RADIUS) {
			return cap;
		}
		JSONArray kept = new JSONArray();
		for (int i = 0; i < items.length(); i++) {
			String id = items.optString(i, "");
			if (!isSelfBlasting(id, echoHero)) {
				kept.put(id);
			}
		}
		return withItems(cap, kept);
	}

	/**
	 * The item is asked what it is rather than matched against a name list, so
	 * a new bomb subclass is covered the day it is added.
	 */
	private static boolean isSelfBlasting(String id, Hero echoHero) {
		if (id == null || id.isEmpty() || id.startsWith("*")) {
			return false;
		}
		Item item = EchoInventory.find(echoHero, id);
		return item instanceof Bomb || item instanceof StoneOfBlast;
	}

	/** Same capability, different pick list. */
	private static JSONObject withItems(JSONObject cap, JSONArray items) {
		JSONObject copy = new JSONObject();
		copy.put("pick", cap.optString("pick", "FIRST_LEGAL"));
		copy.put("items", items);
		String hazard = cap.optString("hazard", "");
		if (!hazard.isEmpty()) {
			copy.put("hazard", hazard);
		}
		String spell = cap.optString("spell", "");
		if (!spell.isEmpty()) {
			copy.put("spell", spell);
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
