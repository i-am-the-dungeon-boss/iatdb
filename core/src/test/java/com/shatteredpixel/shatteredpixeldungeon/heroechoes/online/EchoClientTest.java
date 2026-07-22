package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoFightResult;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ExtendWith(GdxTestExtension.class)
public class EchoClientTest {

	@AfterEach
	void cleanup() {
		Dungeon.easyMode = false;
		EchoOnlineSettings.resetForTests();
		EchoPlayerSession.resetForTests();
	}

	@Test
	@DisplayName("fetchEcho returns decoded echo and policy on 200")
	void fetchEchoReturnsResult() throws Exception {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		echo.echoId = "5-99";
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(200, EchoTestSupport.fetchResponseJson(echo, EchoPolicy.fallback()));

		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		EchoLookupOutcome result = client.fetchEcho(5);

		Assertions.assertThat(result.isFound()).isTrue();
		Assertions.assertThat(result.result.echo.echoId).isEqualTo("5-99");
		Assertions.assertThat(result.result.echo.hasCombatData()).isTrue();
		Assertions.assertThat(result.result.policy.isSupported()).isTrue();
		Assertions.assertThat(transport.requests.get(0).url).isEqualTo("https://echo.test/v1/echoes/5?easy_mode=false");
	}

	@Test
	@DisplayName("checkHealth returns true when health endpoint responds ok")
	void checkHealthReturnsTrueOnOk() throws Exception {
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(200, "{\"status\":\"ok\"}");

		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		Assertions.assertThat(client.checkHealth()).isTrue();
		Assertions.assertThat(transport.requests.get(0).url).isEqualTo("https://echo.test/health");
	}

	@Test
	@DisplayName("checkHealth returns false when health endpoint is down")
	void checkHealthReturnsFalseOnFailure() throws Exception {
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(503, "{\"status\":\"down\"}");

		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		Assertions.assertThat(client.checkHealth()).isFalse();
	}

	@Test
	@DisplayName("fetchEcho returns NOT_FOUND on 404")
	void fetchEchoReturnsNotFoundOn404() {
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(404, "{\"error\":\"missing\"}");

		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		Assertions.assertThat(client.fetchEcho(5).isNotFound()).isTrue();
	}

	@Test
	@DisplayName("fetchEcho returns DECODE when 200 body omits required fields")
	void fetchEchoReturnsDecodeWhenBodyIncomplete() throws Exception {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(200, EchoWireCodec.encodeEchoUpload(echo, "test-client"));

		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		EchoLookupOutcome outcome = client.fetchEcho(5);
		Assertions.assertThat(outcome.isError()).isTrue();
		Assertions.assertThat(outcome.failureKind).isEqualTo(EchoLookupFailureKind.DECODE);
	}

	@Test
	@DisplayName("fetchEcho returns DECODE when 200 body is not JSON")
	void fetchEchoReturnsDecodeWhenBodyMalformed() {
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(200, "not-json");

		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		EchoLookupOutcome outcome = client.fetchEcho(5);
		Assertions.assertThat(outcome.isError()).isTrue();
		Assertions.assertThat(outcome.failureKind).isEqualTo(EchoLookupFailureKind.DECODE);
	}

	@Test
	@DisplayName("fetchEcho returns ERROR on 503")
	void fetchEchoReturnsErrorOn503() {
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(503, "{\"error\":\"busy\"}");

		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		EchoLookupOutcome outcome = client.fetchEcho(5);
		Assertions.assertThat(outcome.isError()).isTrue();
		Assertions.assertThat(outcome.failureKind).isEqualTo(EchoLookupFailureKind.SERVER);
		Assertions.assertThat(outcome.httpStatus).isEqualTo(503);
	}

	@Test
	@DisplayName("fetchEcho returns ERROR when transport throws")
	void fetchEchoReturnsErrorOnThrow() {
		EchoHttpTransport transport = request -> {
			throw new RuntimeException("network down");
		};

		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		EchoLookupOutcome outcome = client.fetchEcho(5);
		Assertions.assertThat(outcome.isError()).isTrue();
		Assertions.assertThat(outcome.failureKind).isEqualTo(EchoLookupFailureKind.NETWORK);
	}

	@Test
	@DisplayName("fetchEchoPolicy posts policy_input and returns decoded policy")
	void fetchEchoPolicyReturnsPolicy() {
		EchoPlayerSession.applyAuthResponse("policy-jwt", "Hero", false, null);
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(200, "{"
				+ "\"echo_policy\":{"
				+ "\"policy_schema_version\":\"0.0.1\","
				+ "\"capabilities\":{\"RANGED\":{\"pick\":\"MAX_DAMAGE\",\"items\":[\"MagesStaff\"]},"
				+ "\"MELEE\":{\"pick\":\"FIRST_LEGAL\",\"items\":[\"*melee\"]}},"
				+ "\"reactions\":[],"
				+ "\"recipes\":[],"
				+ "\"selection\":{\"order\":[\"default\"],\"default_roles\":[\"RANGED\",\"MELEE\"]}"
				+ "},"
				+ "\"base_policy_version\":\"0.0.1\""
				+ "}");

		EchoClient client = new EchoClient("https://echo.test", "secret", transport);
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		EchoPolicy policy = client.fetchEchoPolicy(echo);

		Assertions.assertThat(policy).isNotNull();
		Assertions.assertThat(policy.isSupported()).isTrue();
		Assertions.assertThat(policy.root().getJSONObject("capabilities").has("RANGED")).isTrue();
		EchoHttpRequest request = transport.requests.get(0);
		Assertions.assertThat(request.method).isEqualTo("POST");
		Assertions.assertThat(request.url).isEqualTo("https://echo.test/v1/echoes/policy");
		Assertions.assertThat(request.body).contains("\"hero_class\":\"WARRIOR\"");
		Assertions.assertThat(request.body).contains("\"items\":");
		Assertions.assertThat(request.body).contains("\"talents\":");
		Assertions.assertThat(request.headers.get("X-API-Key")).isEqualTo("secret");
		Assertions.assertThat(request.headers.get("Authorization")).isEqualTo("Bearer policy-jwt");
	}

	@Test
	@DisplayName("fetchEchoPolicy returns null when request fails")
	void fetchEchoPolicyReturnsNullOnFailure() {
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(503, "{}");

		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		Assertions.assertThat(client.fetchEchoPolicy(EchoTestSupport.warriorEchoWithData(5))).isNull();
	}

	@Test
	@DisplayName("uploadEcho fails on unauthorized responses")
	void uploadEchoFailsOnUnauthorized() {
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(401, "{\"detail\":\"Unauthorized\"}");
		Echo echo = EchoTestSupport.warriorEchoWithData(5);

		EchoClient client = new EchoClient("https://echo.test", "secret-key", transport);

		Assertions.assertThatThrownBy(() -> client.uploadEcho(echo))
				.isInstanceOf(EchoHttpException.class)
				.hasMessageContaining("401");
	}

	@Test
	@DisplayName("uploadEcho posts JSON with API key header")
	void uploadEchoPostsWithApiKey() throws Exception {
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(201, "{}");
		Echo echo = EchoTestSupport.warriorEchoWithData(5);

		EchoClient client = new EchoClient("https://echo.test", "secret-key", transport);
		client.uploadEcho(echo);

		EchoHttpRequest request = transport.requests.get(0);
		Assertions.assertThat(request.method).isEqualTo("POST");
		Assertions.assertThat(request.url).isEqualTo("https://echo.test/v1/echoes");
		Assertions.assertThat(request.headers.get("X-API-Key")).isEqualTo("secret-key");
		Assertions.assertThat(request.body).contains(echo.echoId);
	}

	@Test
	@DisplayName("uploadEcho includes Bearer token when session present")
	void uploadEchoIncludesBearerWhenSessionPresent() throws Exception {
		EchoPlayerSession.applyAuthResponse("player-jwt", "Hero", false, null);
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(201, "{}");
		Echo echo = EchoTestSupport.warriorEchoWithData(5);

		EchoClient client = new EchoClient("https://echo.test", "secret-key", transport);
		client.uploadEcho(echo);

		Assertions.assertThat(transport.requests.get(0).headers.get("Authorization"))
				.isEqualTo("Bearer player-jwt");
	}

	@Test
	@DisplayName("authenticateDevice persists session from response")
	void authenticateDevicePersistsSession() throws Exception {
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(201, "{"
				+ "\"token\":\"new-jwt\","
				+ "\"exp\":123,"
				+ "\"username\":\"Named\","
				+ "\"has_credentials\":false"
				+ "}");

		EchoClient client = new EchoClient("https://echo.test", "secret-key", transport);
		boolean ok = client.authenticateDevice("device-0123456789ab", "Named");

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(EchoPlayerSession.jwt()).isEqualTo("new-jwt");
		Assertions.assertThat(EchoPlayerSession.username()).isEqualTo("Named");
		Assertions.assertThat(transport.requests.get(0).url).endsWith("/v1/auth/device");
		Assertions.assertThat(transport.requests.get(0).headers.get("Authorization")).isNull();
	}

	@Test
	@DisplayName("postLeaderboardResult posts fight outcome JSON")
	void postsLeaderboardResult() throws Exception {
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(201, "{}");

		EchoClient client = new EchoClient("https://echo.test", "secret", transport);
		client.postLeaderboardResult(new EchoFightResult(
				"5-1", true, 5, 1L, "0.0.1", "MAGE", 30, 10, 12));

		Assertions.assertThat(transport.requests.get(0).url).endsWith("/v1/leaderboard/results");
		Assertions.assertThat(transport.requests.get(0).body).contains("\"boss_win\":true");
	}

	@Test
	@DisplayName("fetchLeaderboard returns decoded entries on 200")
	void fetchLeaderboardReturnsEntries() throws Exception {
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(200, "[{"
				+ "\"rank\":1,"
				+ "\"echo_id\":\"5-9\","
				+ "\"boss_win\":true,"
				+ "\"depth\":5,"
				+ "\"player_class\":\"ROGUE\","
				+ "\"damage_dealt\":55,"
				+ "\"damage_taken\":8,"
				+ "\"turns\":14,"
				+ "\"win_rate_proxy\":1"
				+ "}]");

		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		var entries = client.fetchLeaderboard(5, 10);

		Assertions.assertThat(entries).hasSize(1);
		Assertions.assertThat(entries.get(0).echoId).isEqualTo("5-9");
		Assertions.assertThat(transport.requests.get(0).url)
				.contains("/v1/leaderboard/5?limit=10&easy_mode=false");
	}

	@Test
	@DisplayName("fetchEcho requests easy_mode query from dungeon flag")
	void fetchEchoRequestsEasyModeQuery() {
		Dungeon.easyMode = true;
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(404, "");

		EchoClient client = new EchoClient("https://echo.test", "secret", transport);
		client.fetchEcho(5);

		Assertions.assertThat(transport.requests.get(0).url)
				.isEqualTo("https://echo.test/v1/echoes/5?easy_mode=true");
	}

	public static final class FakeEchoHttpTransport implements EchoHttpTransport {

		public final List<EchoHttpRequest> requests = new CopyOnWriteArrayList<>();
		final List<EchoHttpResponse> responses = new CopyOnWriteArrayList<>();

		public void enqueue(int status, String body) {
			responses.add(new EchoHttpResponse(status, body));
		}

		@Override
		public EchoHttpResponse send(EchoHttpRequest request) {
			requests.add(request);
			if (responses.isEmpty()) {
				return new EchoHttpResponse(500, "");
			}
			return responses.remove(0);
		}
	}
}
