package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoStorage;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

@ExtendWith(GdxTestExtension.class)
class CompositeEchoLookupTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
		EchoOnlineSettings.resetForTests();
		CompositeEchoLookup.rankedRetryDelayMs = 0L;
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
	}

	@Test
	@DisplayName("ranked mode fetches echo online")
	void rankedModeFetchesOnline() throws Exception {
		Echo online = EchoTestSupport.warriorEchoWithData(5);
		online.echoId = "online-1";
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(200, EchoTestSupport.fetchResponseJson(online, EchoPolicy.fallback()));

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		EchoStorage local = new EchoStorage();
		local.save(EchoTestSupport.warriorEchoWithData(5));

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local);

		EchoLookupOutcome result = lookup.findEchoForDepth(5);

		Assertions.assertThat(result.isFound()).isTrue();
		Assertions.assertThat(result.result.echo.echoId).isEqualTo("online-1");
		Assertions.assertThat(result.result.policy).isNotNull();
		Assertions.assertThat(transport.requests).hasSize(1);
	}

	@Test
	@DisplayName("ranked mode retries DECODE when fetch response is corrupt")
	void rankedModeRetriesDecodeOnCorruptBody() throws Exception {
		Echo online = EchoTestSupport.warriorEchoWithData(5);
		String corrupt = EchoWireCodec.encodeEchoUpload(online, "test-client");
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(200, corrupt);
		transport.enqueue(200, corrupt);

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				new EchoStorage());

		EchoLookupOutcome outcome = lookup.findEchoForDepth(5);
		Assertions.assertThat(outcome.isError()).isTrue();
		Assertions.assertThat(outcome.failureKind).isEqualTo(EchoLookupFailureKind.DECODE);
		Assertions.assertThat(transport.requests).hasSize(CompositeEchoLookup.RANKED_ATTEMPTS);
	}

	@Test
	@DisplayName("ranked mode returns NOT_FOUND when online fetch misses")
	void rankedModeReturnsNotFoundOnMiss() {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(404, "{}");

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		EchoStorage local = new EchoStorage();
		local.save(EchoTestSupport.warriorEchoWithData(5));

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local);

		Assertions.assertThat(lookup.findEchoForDepth(5).isNotFound()).isTrue();
		Assertions.assertThat(transport.requests).hasSize(1);
	}

	@Test
	@DisplayName("ranked mode returns ERROR when online fetch keeps failing")
	void rankedModeReturnsErrorOnThrow() {
		CompositeEchoLookup.rankedRetryDelayMs = 0L;
		EchoHttpTransport transport = request -> {
			throw new RuntimeException("network down");
		};

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		EchoStorage local = new EchoStorage();
		local.save(EchoTestSupport.warriorEchoWithData(5));

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local);

		Assertions.assertThat(lookup.findEchoForDepth(5).isError()).isTrue();
	}

	@Test
	@DisplayName("ranked mode auto-retries ERROR then returns FOUND")
	void rankedModeAutoRetriesThenFound() throws Exception {
		CompositeEchoLookup.rankedRetryDelayMs = 0L;
		Echo online = EchoTestSupport.warriorEchoWithData(5);
		online.echoId = "online-retry";
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(503, "{}");
		transport.enqueue(200, EchoTestSupport.fetchResponseJson(online, EchoPolicy.fallback()));

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				new EchoStorage());

		EchoLookupOutcome result = lookup.findEchoForDepth(5);

		Assertions.assertThat(result.isFound()).isTrue();
		Assertions.assertThat(result.result.echo.echoId).isEqualTo("online-retry");
		Assertions.assertThat(transport.requests).hasSize(2);
	}

	@Test
	@DisplayName("solo mode keeps local policy when backend policy fetch fails")
	void soloModeKeepsLocalPolicyWhenBackendPolicyFetchFails() {
		EchoHttpTransport transport = request -> {
			throw new RuntimeException("network down");
		};

		EchoOnlineSettings.setBackendUrl("https://echo.test");
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;

		Echo localEcho = EchoTestSupport.warriorEchoWithData(5);
		EchoStorage local = new EchoStorage();
		local.save(localEcho);

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local);

		EchoLookupOutcome result = lookup.findEchoForDepth(5);

		Assertions.assertThat(result.isFound()).isTrue();
		Assertions.assertThat(result.result.echo.echoId).isEqualTo(localEcho.echoId);
		Assertions.assertThat(result.result.policy).isNotNull();
	}

	@Test
	@DisplayName("non-ranked mode never fetches online even when backend is configured")
	void nonRankedNeverFetchesOnline() {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(200, "{\"unused\":true}");

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.NONE;

		Echo localEcho = EchoTestSupport.warriorEchoWithData(5);
		EchoStorage local = new EchoStorage();
		local.save(localEcho);

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local);

		EchoLookupOutcome result = lookup.findEchoForDepth(5);

		Assertions.assertThat(result.isFound()).isTrue();
		Assertions.assertThat(result.result.echo.echoId).isEqualTo(localEcho.echoId);
		Assertions.assertThat(result.result.policy).isNotNull();
		Assertions.assertThat(transport.requests).isEmpty();
	}

	@Test
	@DisplayName("solo mode uses local echo and requests policy from backend when configured")
	void soloModeRequestsPolicyFromBackend() {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(200, "{"
				+ "\"echo_policy\":{"
				+ "\"policy_schema_version\":\"0.0.1\","
				+ "\"capabilities\":{\"MELEE\":{\"pick\":\"FIRST_LEGAL\",\"items\":[\"*melee\"]}},"
				+ "\"reactions\":[],"
				+ "\"recipes\":[],"
				+ "\"selection\":{\"order\":[\"default\"],\"default_roles\":[\"MELEE\"]}"
				+ "},"
				+ "\"base_policy_version\":\"0.0.1\""
				+ "}");

		EchoOnlineSettings.setBackendUrl("https://echo.test");
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;

		Echo localEcho = EchoTestSupport.warriorEchoWithData(5);
		EchoStorage local = new EchoStorage();
		local.save(localEcho);

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local);

		EchoLookupOutcome result = lookup.findEchoForDepth(5);

		Assertions.assertThat(result.isFound()).isTrue();
		Assertions.assertThat(result.result.echo.echoId).isEqualTo(localEcho.echoId);
		Assertions.assertThat(result.result.policy.isSupported()).isTrue();
		Assertions.assertThat(result.result.policy.root().has("capabilities")).isTrue();
		Assertions.assertThat(transport.requests).hasSize(1);
		Assertions.assertThat(transport.requests.get(0).url).endsWith("/v1/echoes/policy");
	}

	@Test
	@DisplayName("solo mode keeps local policy when backend is not configured")
	void soloModeKeepsLocalPolicyWhenUnconfigured() {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(200, "{\"echo_policy\":{\"policy_schema_version\":\"0.0.1\",\"capabilities\":{}}}");

		EchoOnlineSettings.resetForTests();
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;

		Echo localEcho = EchoTestSupport.warriorEchoWithData(5);
		EchoStorage local = new EchoStorage();
		local.save(localEcho);

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local);

		EchoLookupOutcome result = lookup.findEchoForDepth(5);

		Assertions.assertThat(result.isFound()).isTrue();
		Assertions.assertThat(result.result.echo.echoId).isEqualTo(localEcho.echoId);
		Assertions.assertThat(result.result.policy).isNotNull();
		Assertions.assertThat(transport.requests).isEmpty();
	}
}
