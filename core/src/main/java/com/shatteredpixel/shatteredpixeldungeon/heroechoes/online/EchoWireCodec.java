package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoFightResult;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoLeaderboardEntry;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class EchoWireCodec {

	private EchoWireCodec() {
	}

	public static String encodeEchoUpload(Echo echo, String sourceClient) throws Exception {
		JSONObject json = new JSONObject();
		json.put("echo_id", echo.echoId);
		json.put("depth", echo.depth);
		json.put("game_version", gameVersionForUpload(echo));
		json.put("hero_class", echo.heroClass);
		if (echo.userName != null && !echo.userName.isEmpty()) {
			json.put("user_name", echo.userName);
		}
		json.put("lvl", echo.lvl);
		json.put("hp", echo.hp);
		json.put("ht", echo.ht);
		json.put("game_seed", echo.gameSeed);
		json.put("timestamp", echo.timestamp > 0 ? echo.timestamp : System.currentTimeMillis());
		json.put("echo_data_base64", encodeEchoData(echo));
		json.put("source_client", sourceClient);
		return json.toString();
	}

	public static String encodeEchoPolicyRequest(String heroClass, int lvl) {
		JSONObject json = new JSONObject();
		json.put("hero_class", heroClass != null && !heroClass.isEmpty() ? heroClass : "UNKNOWN");
		json.put("lvl", Math.max(0, lvl));
		return json.toString();
	}

	public static EchoPolicy decodeEchoPolicyResponse(String json) {
		JSONObject root = new JSONObject(json);
		if (!root.has("echo_policy")) {
			throw new IllegalArgumentException("policy response requires echo_policy");
		}
		EchoPolicy policy = EchoPolicy.fromJson(root.getJSONObject("echo_policy"));
		if (!policy.isSupported()) {
			throw new IllegalArgumentException("unsupported echo_policy");
		}
		return policy;
	}

	public static EchoFetchResult decodeEchoFetch(String json) throws Exception {
		JSONObject root = new JSONObject(json);
		Echo echo = new Echo();
		echo.echoId = root.getString("echo_id");
		echo.depth = root.getInt("depth");
		echo.gameVersion = readGameVersion(root);
		echo.heroClass = root.optString("hero_class", "UNKNOWN");
		echo.userName = root.optString("user_name", "");
		echo.lvl = root.optInt("lvl", 0);
		echo.hp = root.optInt("hp", 0);
		echo.ht = root.optInt("ht", 1);
		echo.timestamp = root.optLong("timestamp", 0L);
		echo.gameSeed = root.optLong("game_seed", 0L);
		Bundle fileBundle = null;
		try {
			fileBundle = decodeEchoData(root.optString("echo_data_base64", ""));
		} catch (Exception ignored) {
		}
		if (fileBundle != null && fileBundle.contains(Echo.BUNDLE_KEY)) {
			echo = Echo.fromFileBundle(fileBundle);
			echo.echoId = root.getString("echo_id");
			echo.depth = root.getInt("depth");
			echo.gameVersion = readGameVersion(root);
		}
		if (root.has("user_name")) {
			echo.userName = root.optString("user_name", "");
		}

		if (!root.has("echo_policy")) {
			throw new IllegalArgumentException("echo fetch response requires echo_policy");
		}
		EchoPolicy policy = EchoPolicy.fromJson(root.getJSONObject("echo_policy"));
		if (!policy.isSupported()) {
			policy = EchoPolicy.fallback();
		}

		return new EchoFetchResult(echo, policy);
	}

	public static String encodeLeaderboardResult(EchoFightResult result) {
		JSONObject json = new JSONObject();
		if (result.echoId != null) {
			json.put("echo_id", result.echoId);
		}
		json.put("boss_win", result.bossWin);
		json.put("depth", result.depth);
		json.put("game_version", result.gameVersion != null ? result.gameVersion : "");
		json.put("player_class", result.playerClass != null ? result.playerClass : "UNKNOWN");
		json.put("damage_dealt", result.damageDealt);
		json.put("damage_taken", result.damageTaken);
		json.put("turns", result.turns);
		json.put("timestamp", result.timestamp > 0 ? result.timestamp : System.currentTimeMillis());
		return json.toString();
	}

	public static List<EchoLeaderboardEntry> decodeLeaderboardEntries(String jsonBody) {
		List<EchoLeaderboardEntry> entries = new ArrayList<>();
		if (jsonBody == null || jsonBody.isEmpty()) {
			return entries;
		}
		JSONArray array = new JSONArray(jsonBody);
		for (int i = 0; i < array.length(); i++) {
			JSONObject row = array.getJSONObject(i);
			entries.add(new EchoLeaderboardEntry(
					row.optInt("rank", i + 1),
					row.optString("echo_id", null),
					row.optBoolean("boss_win", false),
					row.optInt("depth", 0),
					row.optString("player_class", "UNKNOWN"),
					row.optInt("damage_dealt", 0),
					row.optInt("damage_taken", 0),
					row.optInt("turns", 0),
					(float) row.optDouble("win_rate_proxy", 0)));
		}
		return entries;
	}

	private static String gameVersionForUpload(Echo echo) {
		if (echo.gameVersion != null && !echo.gameVersion.isEmpty()) {
			return echo.gameVersion;
		}
		return Game.version != null ? Game.version : "";
	}

	private static String readGameVersion(JSONObject root) {
		Object raw = root.opt("game_version");
		if (raw == null || raw == JSONObject.NULL) {
			return "";
		}
		if (raw instanceof Number) {
			return String.valueOf(((Number) raw).intValue());
		}
		return root.optString("game_version", "");
	}

	private static String encodeEchoData(Echo echo) throws Exception {
		if (echo.echoData == null) {
			return "";
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Bundle.write(echo.toFileBundle(), out);
		return Base64.getEncoder().encodeToString(out.toByteArray());
	}

	private static Bundle decodeEchoData(String encoded) throws Exception {
		if (encoded == null || encoded.isEmpty()) {
			return null;
		}
		byte[] bytes = Base64.getDecoder().decode(encoded);
		return Bundle.read(new ByteArrayInputStream(bytes));
	}
}
