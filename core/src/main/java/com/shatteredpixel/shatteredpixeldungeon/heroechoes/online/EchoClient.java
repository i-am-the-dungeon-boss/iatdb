package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoFightResult;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoLeaderboardEntry;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.SentryCrashReporting;
import com.watabou.utils.DeviceCompat;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
				new JavaEchoHttpTransport());
	}

	public boolean checkHealth() throws Exception {
		EchoHttpResponse response = transport.send(new EchoHttpRequest(
				"GET",
				baseUrl + "/health",
				jsonHeaders(false, false),
				null));
		return isHealthy(response.statusCode, response.body);
	}

	static boolean isHealthy(int statusCode, String body) {
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

	/**
	 * Ranked echo lookup. Outcomes:
	 * <ul>
	 * <li>200 + valid body → {@link EchoLookupStatus#FOUND}</li>
	 * <li>200 + bad/incomplete body → {@link EchoLookupFailureKind#DECODE}</li>
	 * <li>404 → {@link EchoLookupStatus#NOT_FOUND} (empty pool)</li>
	 * <li>other HTTP → {@link EchoLookupFailureKind#SERVER}</li>
	 * <li>transport throw → {@link EchoLookupFailureKind#NETWORK}</li>
	 * </ul>
	 */
	public EchoLookupOutcome fetchEcho(int depth) {
		String url = baseUrl + "/v1/echoes/" + depth + easyModeQuery();
		logFetch("GET " + url);
		try {
			EchoHttpResponse response = transport.send(new EchoHttpRequest(
					"GET",
					url,
					jsonHeaders(false, false),
					null));

			if (response.statusCode == 200) {
				try {
					EchoFetchResult decoded = EchoWireCodec.decodeEchoFetch(response.body);
					logFetch("depth=" + depth + " status=200 FOUND echo_id="
							+ (decoded.echo != null ? decoded.echo.echoId : "?"));
					return EchoLookupOutcome.found(decoded);
				} catch (Exception corruptBody) {
					logFetch("depth=" + depth + " status=200 DECODE: " + corruptBody.getMessage());
					SentryCrashReporting.report(corruptBody);
					return EchoLookupOutcome.error(EchoLookupFailureKind.DECODE);
				}
			}
			if (response.statusCode == 404) {
				logFetch("depth=" + depth + " status=404 NOT_FOUND");
				return EchoLookupOutcome.notFound();
			}
			logFetch("depth=" + depth + " status=" + response.statusCode + " SERVER");
			SentryCrashReporting.report(new EchoHttpException(response.statusCode, response.body));
			return EchoLookupOutcome.error(EchoLookupFailureKind.SERVER, response.statusCode);
		} catch (Exception networkError) {
			logFetch("depth=" + depth + " NETWORK: " + networkError.getMessage());
			SentryCrashReporting.report(networkError);
			return EchoLookupOutcome.error(EchoLookupFailureKind.NETWORK);
		}
	}

	/**
	 * Asks the backend to generate a fight policy for a local (solo) echo.
	 * Returns null on any failure; callers must treat that as an error (no
	 * local-policy fallback).
	 */
	public EchoPolicy fetchEchoPolicy(Echo echo) {
		String echoId = echo != null ? echo.echoId : null;
		logFetch("POST policy echo_id=" + echoId);
		try {
			EchoHttpResponse response = transport.send(new EchoHttpRequest(
					"POST",
					baseUrl + "/v1/echoes/policy",
					jsonHeaders(true, true),
					EchoWireCodec.encodeEchoPolicyRequest(EchoPolicyInput.fromEcho(echo))));
			if (response.statusCode != 200) {
				logFetch("policy echo_id=" + echoId + " status=" + response.statusCode + " FAIL");
				SentryCrashReporting.report(new EchoHttpException(response.statusCode, response.body));
				return null;
			}
			EchoPolicy policy = EchoWireCodec.decodeEchoPolicyResponse(response.body);
			logFetch("policy echo_id=" + echoId + " status=200"
					+ (policy != null && policy.isSupported() ? " OK" : " UNSUPPORTED"));
			return policy;
		} catch (Exception failure) {
			logFetch("policy echo_id=" + echoId + " ERROR: " + failure.getMessage());
			SentryCrashReporting.report(failure);
			return null;
		}
	}

	private static void logFetch(String message) {
		DeviceCompat.log("EchoFetch", message);
	}

	/**
	 * Validates the current Bearer JWT. Updates cached profile fields on success.
	 *
	 * @return true when the token is accepted by the server
	 */
	public boolean fetchMe() throws Exception {
		EchoHttpResponse response = transport.send(new EchoHttpRequest(
				"GET",
				baseUrl + "/v1/auth/me",
				jsonHeaders(true, true),
				null));
		if (response.statusCode == 401 || response.statusCode == 403) {
			return false;
		}
		ensureSuccess(response);
		JSONObject json = new JSONObject(response.body);
		String name = json.optString("username", EchoPlayerSession.username());
		boolean credentials = json.optBoolean("has_credentials", false);
		String linkedEmail = json.has("email") ? json.optString("email", "") : "";
		// Keep existing JWT; only refresh profile metadata.
		EchoPlayerSession.applyAuthResponse(EchoPlayerSession.jwt(), name, credentials, linkedEmail);
		return true;
	}

	/**
	 * Registers or re-authenticates this device. Persists session on success.
	 *
	 * @return true if a session is available afterward
	 */
	public boolean authenticateDevice(String deviceId, String username) throws Exception {
		JSONObject body = new JSONObject();
		body.put("device_id", deviceId);
		if (username != null && !username.isBlank()) {
			body.put("username", username.trim());
		}
		EchoHttpResponse response = transport.send(new EchoHttpRequest(
				"POST",
				baseUrl + "/v1/auth/device",
				jsonHeaders(true, false),
				body.toString()));
		if (response.statusCode != 200 && response.statusCode != 201) {
			throw new EchoHttpException(response.statusCode, response.body);
		}
		applySessionJson(response.body);
		return EchoPlayerSession.hasSession();
	}

	public void changeUsername(String username) throws Exception {
		JSONObject body = new JSONObject();
		body.put("username", username != null ? username.trim() : "");
		EchoHttpResponse response = transport.send(new EchoHttpRequest(
				"PATCH",
				baseUrl + "/v1/auth/username",
				jsonHeaders(true, true),
				body.toString()));
		ensureSuccess(response);
		applySessionJson(response.body);
	}

	public void setCredentials(String email, String password) throws Exception {
		JSONObject body = new JSONObject();
		body.put("email", email != null ? email.trim() : "");
		body.put("password", password != null ? password : "");
		EchoHttpResponse response = transport.send(new EchoHttpRequest(
				"PUT",
				baseUrl + "/v1/auth/credentials",
				jsonHeaders(true, true),
				body.toString()));
		ensureSuccess(response);
		applySessionJson(response.body);
	}

	public void uploadEcho(Echo echo) throws Exception {
		String body = EchoWireCodec.encodeEchoUpload(echo, SOURCE_CLIENT);
		EchoHttpResponse response = transport.send(new EchoHttpRequest(
				"POST",
				baseUrl + "/v1/echoes",
				jsonHeaders(true, true),
				body));
		ensureSuccess(response);
	}

	public void postLeaderboardResult(EchoFightResult result) throws Exception {
		String body = EchoWireCodec.encodeLeaderboardResult(result);
		EchoHttpResponse response = transport.send(new EchoHttpRequest(
				"POST",
				baseUrl + "/v1/leaderboard/results",
				jsonHeaders(true, true),
				body));
		ensureSuccess(response);
	}

	public List<EchoLeaderboardEntry> fetchLeaderboard(int depth, int limit) throws Exception {
		String url = baseUrl + "/v1/leaderboard/" + depth + "?limit=" + limit + easyModeQueryAmp();
		EchoHttpResponse response = transport.send(new EchoHttpRequest(
				"GET",
				url,
				jsonHeaders(false, false),
				null));
		if (response.statusCode == 200) {
			return EchoWireCodec.decodeLeaderboardEntries(response.body);
		}
		return List.of();
	}

	private static String easyModeQuery() {
		return "?easy_mode=" + Dungeon.easyMode;
	}

	private static String easyModeQueryAmp() {
		return "&easy_mode=" + Dungeon.easyMode;
	}

	private void applySessionJson(String body) throws Exception {
		JSONObject json = new JSONObject(body);
		String token = json.optString("token", "");
		String name = json.optString("username", "");
		boolean credentials = json.optBoolean("has_credentials", false);
		String linkedEmail = json.has("email") ? json.optString("email", "") : "";
		EchoPlayerSession.applyAuthResponse(token, name, credentials, linkedEmail);
	}

	private Map<String, String> jsonHeaders(boolean includeApiKey, boolean includeBearer) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Accept", "application/json");
		if (includeApiKey && !apiKey.isEmpty()) {
			headers.put("X-API-Key", apiKey);
		}
		if (includeBearer) {
			String token = EchoPlayerSession.jwt();
			if (!token.isEmpty()) {
				headers.put("Authorization", "Bearer " + token);
			}
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

}
