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
		json.put("lvl", echo.lvl);
		json.put("hp", echo.hp);
		json.put("ht", echo.ht);
		json.put("game_seed", echo.gameSeed);
		json.put("timestamp", echo.timestamp > 0 ? echo.timestamp : System.currentTimeMillis());
		json.put("echo_data_base64", encodeEchoData(echo));
		json.put("source_client", sourceClient);
		JSONObject policyInput = EchoPolicyInput.fromEcho(echo).toJson();
		// Root echo already carries hero_class/lvl; nested kit must not duplicate them.
		policyInput.remove("hero_class");
		policyInput.remove("lvl");
		json.put("policy_input", policyInput);
		return json.toString();
	}

	public static String encodeEchoPolicyRequest(EchoPolicyInput input) {
		return input.toJson().toString();
	}

	public static EchoPolicy decodeEchoPolicyResponse(String json) {
		JSONObject root = new JSONObject(json);
		if (!root.has("echo_policy")) {
			throw new IllegalArgumentException("policy response requires echo_policy");
		}
		return EchoPolicy.fromJson(root.getJSONObject("echo_policy"));
	}

	/**
	 * Parses GET /v1/echoes/{depth} body. Required: echo_id, depth, game_version,
	 * echo_data_base64 (combat bundle), echo_policy (supported). Optional:
	 * user_name, kill_count.
	 */
	public static EchoFetchResult decodeEchoFetch(String json) throws Exception {
		JSONObject root = new JSONObject(json);

		Echo echo = parseCombatEcho(requireNonEmptyString(root, "echo_data_base64"));
		// API envelope fields win over values embedded in the bundle.
		echo.echoId = requireNonEmptyString(root, "echo_id");
		echo.depth = root.getInt("depth");
		echo.gameVersion = requireString(root, "game_version");
		echo.userName = Echo.resolveUserName(root.optString("user_name", null), echo.heroClass);
		echo.killCount = Math.max(0, root.optInt("kill_count", 0));

		EchoPolicy policy = parseSupportedPolicy(requireObject(root, "echo_policy"));
		return new EchoFetchResult(echo, policy);
	}

	public static String encodeLeaderboardResult(EchoFightResult result) {
		if (result.playerClass == null || result.playerClass.isEmpty()) {
			throw new IllegalArgumentException("leaderboard result requires player_class");
		}
		JSONObject json = new JSONObject();
		if (result.echoId != null) {
			json.put("echo_id", result.echoId);
		}
		json.put("boss_win", result.bossWin);
		json.put("depth", result.depth);
		json.put("game_version", result.gameVersion != null ? result.gameVersion : "");
		json.put("player_class", result.playerClass);
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
			String playerClass = row.optString("player_class", "");
			if (playerClass.isEmpty()) {
				continue;
			}
			entries.add(new EchoLeaderboardEntry(
					row.optInt("rank", i + 1),
					row.optString("echo_id", null),
					row.optBoolean("boss_win", false),
					row.optInt("depth", 0),
					playerClass,
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

	private static Echo parseCombatEcho(String echoDataBase64) {
		try {
			byte[] bytes = Base64.getDecoder().decode(echoDataBase64);
			Bundle fileBundle = Bundle.read(new ByteArrayInputStream(bytes));
			if (fileBundle == null || !fileBundle.contains(Echo.BUNDLE_KEY)) {
				throw new IllegalArgumentException("echo fetch response requires echo_data");
			}
			Echo echo = Echo.fromFileBundle(fileBundle);
			if (!echo.hasCombatData()) {
				throw new IllegalArgumentException("echo fetch response requires echo_data");
			}
			return echo;
		} catch (IllegalArgumentException required) {
			throw required;
		} catch (Exception decodeError) {
			throw new IllegalArgumentException("echo fetch response requires echo_data", decodeError);
		}
	}

	private static EchoPolicy parseSupportedPolicy(JSONObject policyJson) {
		EchoPolicy policy = EchoPolicy.fromJson(policyJson);
		if (!policy.isSupported()) {
			throw new IllegalArgumentException("echo fetch response requires supported echo_policy");
		}
		return policy;
	}

	private static String requireString(JSONObject root, String key) {
		if (!root.has(key) || root.isNull(key)) {
			throw new IllegalArgumentException("echo fetch response requires " + key);
		}
		Object raw = root.get(key);
		if (!(raw instanceof String)) {
			throw new IllegalArgumentException("echo fetch response requires " + key + " as string");
		}
		return (String) raw;
	}

	private static String requireNonEmptyString(JSONObject root, String key) {
		String value = requireString(root, key);
		if (value.isEmpty()) {
			throw new IllegalArgumentException("echo fetch response requires " + key);
		}
		return value;
	}

	private static JSONObject requireObject(JSONObject root, String key) {
		if (!root.has(key) || root.isNull(key)) {
			throw new IllegalArgumentException("echo fetch response requires " + key);
		}
		JSONObject value = root.optJSONObject(key);
		if (value == null) {
			throw new IllegalArgumentException("echo fetch response requires " + key + " as object");
		}
		return value;
	}

	private static String encodeEchoData(Echo echo) throws Exception {
		if (echo.echoData == null) {
			return "";
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Bundle.write(echo.toFileBundle(), out);
		return Base64.getEncoder().encodeToString(out.toByteArray());
	}
}
