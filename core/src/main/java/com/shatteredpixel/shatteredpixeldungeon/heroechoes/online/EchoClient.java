package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoFightResult;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoLeaderboardEntry;
import com.watabou.noosa.Game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EchoClient {

	private static final String SOURCE_CLIENT = "iatdb-desktop-1.0";

	private final String baseUrl;
	private final String apiKey;
	private final EchoHttpTransport transport;

	public EchoClient(String baseUrl, String apiKey, EchoHttpTransport transport) {
		this.baseUrl = trimTrailingSlash(baseUrl);
		this.apiKey = apiKey != null ? apiKey : "";
		this.transport = transport;
	}

	public static EchoClient createDefault() {
		return new EchoClient(
				EchoOnlineSettings.backendUrl(),
				EchoOnlineSettings.apiKey(),
				new JavaEchoHttpTransport()
		);
	}

	public boolean checkHealth() throws Exception {
		EchoHttpResponse response = transport.send(new EchoHttpRequest(
				"GET",
				baseUrl + "/health",
				jsonHeaders(false),
				null
		));
		return EchoHealth.isHealthy(response.statusCode, response.body);
	}

	public Optional<EchoFetchResult> fetchEcho(int depth, int gameVersion) throws Exception {
		String url = baseUrl + "/v1/echoes/" + depth + "?game_version=" + gameVersion;
		EchoHttpResponse response = transport.send(new EchoHttpRequest(
				"GET",
				url,
				jsonHeaders(false),
				null
		));

		if (response.statusCode == 200) {
			return Optional.of(EchoWireCodec.decodeEchoFetch(response.body));
		}
		return Optional.empty();
	}

	public void uploadEcho(Echo echo) throws Exception {
		String body = EchoWireCodec.encodeEchoUpload(echo, SOURCE_CLIENT);
		EchoHttpResponse response = transport.send(new EchoHttpRequest(
				"POST",
				baseUrl + "/v1/echoes",
				jsonHeaders(true),
				body
		));
		ensureSuccess(response);
	}

	public void postLeaderboardResult(EchoFightResult result) throws Exception {
		String body = EchoWireCodec.encodeLeaderboardResult(result);
		EchoHttpResponse response = transport.send(new EchoHttpRequest(
				"POST",
				baseUrl + "/v1/leaderboard/results",
				jsonHeaders(true),
				body
		));
		ensureSuccess(response);
	}

	public List<EchoLeaderboardEntry> fetchLeaderboard(int depth, int limit) throws Exception {
		String url = baseUrl + "/v1/leaderboard/" + depth + "?limit=" + limit;
		EchoHttpResponse response = transport.send(new EchoHttpRequest(
				"GET",
				url,
				jsonHeaders(false),
				null
		));
		if (response.statusCode == 200) {
			return EchoWireCodec.decodeLeaderboardEntries(response.body);
		}
		return List.of();
	}

	private Map<String, String> jsonHeaders(boolean includeApiKey) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Accept", "application/json");
		if (includeApiKey && !apiKey.isEmpty()) {
			headers.put("X-API-Key", apiKey);
		}
		return headers;
	}

	private static String trimTrailingSlash(String url) {
		if (url == null) {
			return "";
		}
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	private static void ensureSuccess(EchoHttpResponse response) throws EchoHttpException {
		if (response.statusCode >= 400) {
			throw new EchoHttpException(response.statusCode, response.body);
		}
	}

	public int currentGameVersion() {
		return Game.versionCode;
	}
}
