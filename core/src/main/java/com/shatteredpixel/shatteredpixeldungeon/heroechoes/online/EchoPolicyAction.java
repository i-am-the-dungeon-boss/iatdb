package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.json.JSONObject;

public final class EchoPolicyAction {

	public enum Type {
		MELEE_CHASE,
		KEEP_DISTANCE,
		USE_ITEM,
		ZAP,
		WAIT
	}

	public final Type type;
	public final String item;
	public final int range;

	public EchoPolicyAction(Type type, String item, int range) {
		this.type = type;
		this.item = item;
		this.range = range;
	}

	public static EchoPolicyAction fromJson(JSONObject json) {
		if (json == null) {
			return new EchoPolicyAction(Type.WAIT, null, 0);
		}
		String action = json.optString("action", "WAIT");
		Type type;
		try {
			type = Type.valueOf(action);
		} catch (IllegalArgumentException ex) {
			type = Type.WAIT;
		}
		return new EchoPolicyAction(
				type,
				json.optString("item", null),
				json.optInt("range", 0)
		);
	}
}
