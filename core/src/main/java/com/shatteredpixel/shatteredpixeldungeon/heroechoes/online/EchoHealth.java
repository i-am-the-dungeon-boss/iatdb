package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.json.JSONObject;

public final class EchoHealth {

	private EchoHealth() {}

	public static boolean isHealthy(int statusCode, String body) {
		if (statusCode != 200 || body == null || body.isBlank()) {
			return false;
		}
		try {
			JSONObject json = new JSONObject(body);
			return "ok".equals(json.optString("status"));
		} catch (Exception ignored) {
			return false;
		}
	}
}
