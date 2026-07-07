package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class EchoPolicyInterpreter {

	private EchoPolicyInterpreter() {}

	public static EchoPolicyAction interpret(EchoPolicy policy, EchoPolicyContext context) {
		if (policy == null || !policy.isSupported()) {
			return EchoPolicy.fallback().rules.get(0).action;
		}

		List<EchoPolicy.Rule> sorted = new ArrayList<>(policy.rules);
		sorted.sort(Comparator.comparingInt((EchoPolicy.Rule rule) -> rule.priority).reversed());

		for (EchoPolicy.Rule rule : sorted) {
			if (matches(rule.when, context)) {
				return rule.action;
			}
		}

		return EchoPolicy.fallback().rules.get(0).action;
	}

	public static EchoBoss.IntendedAction toIntendedAction(EchoPolicyAction action) {
		if (action == null) {
			return EchoBoss.IntendedAction.ATTACK;
		}
		switch (action.type) {
			case USE_ITEM:
				return EchoBoss.IntendedAction.HEAL;
			case KEEP_DISTANCE:
			case WAIT:
				return EchoBoss.IntendedAction.MOVE;
			case ZAP:
			case MELEE_CHASE:
			default:
				return EchoBoss.IntendedAction.ATTACK;
		}
	}

	private static boolean matches(JSONObject when, EchoPolicyContext context) {
		if (when == null || when.length() == 0) {
			return true;
		}

		Iterator<String> keys = when.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			if (!matchesKey(key, when, context)) {
				return false;
			}
		}
		return true;
	}

	private static boolean matchesKey(String key, JSONObject when, EchoPolicyContext context) {
		switch (key) {
			case "self_hp_below":
				return context.selfHpRatio() < (float) when.optDouble(key);
			case "self_hp_above":
				return context.selfHpRatio() > (float) when.optDouble(key);
			case "distance_gte":
				return context.distance() >= when.optInt(key);
			case "distance_lte":
				return context.distance() <= when.optInt(key);
			case "has_item": {
				String item = when.optString(key);
				return context.hasItem(item);
			}
			case "charges_gte": {
				String item = when.optString("has_item", "");
				return context.itemCharges(item) >= when.optInt(key);
			}
			case "class":
				return context.heroClass().equalsIgnoreCase(when.optString(key));
			case "hero_visible":
				return context.heroVisible() == when.optBoolean(key);
			default:
				return true;
		}
	}
}
