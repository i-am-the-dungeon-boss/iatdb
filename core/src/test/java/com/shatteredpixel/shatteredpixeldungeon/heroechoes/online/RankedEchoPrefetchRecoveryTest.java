package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPrefetchUserChoice;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoStorage;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(GdxTestExtension.class)
class RankedEchoPrefetchRecoveryTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
		EchoOnlineSettings.resetForTests();
		CompositeEchoLookup.rankedRetryDelayMs = 0L;
		Dungeon.echoPlayMode = EchoPlayMode.NONE;
	}

	@Test
	@DisplayName("abort after ranked ERROR keeps ranked mode and returns the error")
	void abortKeepsRankedAndReturnsError() {
		CompositeEchoLookup.rankedRetryDelayMs = 0L;
		EchoHttpTransport transport = request -> {
			throw new RuntimeException("network down");
		};

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		CompositeEchoLookup.setEchoLookupForTests(new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				new EchoStorage()));

		EchoLookupOutcome outcome = Dungeon.prefetchEchoBossWithRankedRecovery(
				5, failed -> EchoPrefetchUserChoice.ABORT);

		Assertions.assertThat(outcome.isError()).isTrue();
		Assertions.assertThat(Dungeon.echoPlayMode).isEqualTo(EchoPlayMode.RANKED);
		Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
	}

	@Test
	@DisplayName("user Retry after ranked ERROR runs another fetch cycle")
	void userRetryRunsAnotherFetchCycle() throws Exception {
		CompositeEchoLookup.rankedRetryDelayMs = 0L;
		AtomicInteger prompts = new AtomicInteger();
		Echo onlineEcho = EchoTestSupport.warriorEchoWithData(5);
		onlineEcho.echoId = "after-retry";
		String fetchJson = rankedFetchJson(onlineEcho);

		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		// first cycle: 2 ERROR attempts
		transport.enqueue(503, "{}");
		transport.enqueue(503, "{}");
		// second cycle after user Retry: success on first attempt
		transport.enqueue(200, fetchJson);

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		CompositeEchoLookup.setEchoLookupForTests(new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				new EchoStorage()));

		EchoLookupOutcome outcome = Dungeon.prefetchEchoBossWithRankedRecovery(5, failed -> {
			prompts.incrementAndGet();
			Assertions.assertThat(failed.failureKind).isEqualTo(EchoLookupFailureKind.SERVER);
			Assertions.assertThat(failed.httpStatus).isEqualTo(503);
			return EchoPrefetchUserChoice.RETRY;
		});

		Assertions.assertThat(prompts.get()).isEqualTo(1);
		Assertions.assertThat(outcome.isFound()).isTrue();
		Assertions.assertThat(Dungeon.getPendingEcho().echoId).isEqualTo("after-retry");
		Assertions.assertThat(transport.requests).hasSize(3);
		Assertions.assertThat(Dungeon.echoPlayMode).isEqualTo(EchoPlayMode.RANKED);
	}

	@Test
	@DisplayName("solo mode ERROR prompts for retry or abort")
	void soloErrorPromptsForRetryOrAbort() {
		AtomicInteger prompts = new AtomicInteger();
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoLookupOutcome.error());
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;

		EchoLookupOutcome outcome = Dungeon.prefetchEchoBossWithRankedRecovery(5, failed -> {
			prompts.incrementAndGet();
			return EchoPrefetchUserChoice.ABORT;
		});

		Assertions.assertThat(prompts.get()).isEqualTo(1);
		Assertions.assertThat(outcome.isError()).isTrue();
		Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
	}

	@Test
	@DisplayName("user Retry after solo ERROR runs another fetch cycle")
	void soloUserRetryRunsAnotherFetchCycle() throws Exception {
		CompositeEchoLookup.rankedRetryDelayMs = 0L;
		AtomicInteger prompts = new AtomicInteger();
		Echo localEcho = EchoTestSupport.warriorEchoWithData(5);
		localEcho.echoId = "solo-local";
		String policyJson = "{"
				+ "\"echo_policy\":{"
				+ "\"policy_schema_version\":\"0.0.1\","
				+ "\"capabilities\":{\"MELEE\":{\"pick\":\"FIRST_LEGAL\",\"items\":[\"*melee\"]}},"
				+ "\"reactions\":[],"
				+ "\"recipes\":[],"
				+ "\"selection\":{\"order\":[\"default\"],\"default_roles\":[\"MELEE\"]}"
				+ "},"
				+ "\"base_policy_version\":\"0.0.1\""
				+ "}";

		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		// first cycle: 2 auto policy attempts fail
		transport.enqueue(503, "{}");
		transport.enqueue(503, "{}");
		// second cycle after user Retry: success
		transport.enqueue(200, policyJson);

		EchoOnlineSettings.setBackendUrl("https://echo.test");
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		EchoStorage local = new EchoStorage();
		local.save(localEcho);

		CompositeEchoLookup.setEchoLookupForTests(new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local));

		EchoLookupOutcome outcome = Dungeon.prefetchEchoBossWithRankedRecovery(5, failed -> {
			prompts.incrementAndGet();
			return EchoPrefetchUserChoice.RETRY;
		});

		Assertions.assertThat(prompts.get()).isEqualTo(1);
		Assertions.assertThat(outcome.isFound()).isTrue();
		Assertions.assertThat(Dungeon.getPendingEcho().echoId).isEqualTo("solo-local");
		Assertions.assertThat(transport.requests).hasSize(3);
		Assertions.assertThat(Dungeon.echoPlayMode).isEqualTo(EchoPlayMode.SOLO);
	}

	private static String rankedFetchJson(Echo echo) throws Exception {
		JSONObject json = new JSONObject(EchoWireCodec.encodeEchoUpload(echo, "test-client"));
		json.put("echo_policy", new JSONObject()
				.put("policy_schema_version", "0.0.1")
				.put("capabilities", new JSONObject()
						.put("MELEE", new JSONObject()
								.put("pick", "FIRST_LEGAL")
								.put("items", new JSONArray().put("*melee"))))
				.put("reactions", new JSONArray())
				.put("recipes", new JSONArray())
				.put("selection", new JSONObject()
						.put("order", new JSONArray().put("default"))
						.put("default_roles", new JSONArray().put("MELEE"))));
		return json.toString();
	}
}
