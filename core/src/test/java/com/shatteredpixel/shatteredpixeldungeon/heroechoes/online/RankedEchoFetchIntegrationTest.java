package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
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

@ExtendWith(GdxTestExtension.class)
class RankedEchoFetchIntegrationTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
		EchoOnlineSettings.resetForTests();
		Dungeon.echoPlayMode = EchoPlayMode.NONE;
	}

	@Test
	@DisplayName("ranked boss prefetch fetches echo from backend instead of local storage")
	void rankedPrefetchFetchesFromBackend() throws Exception {
		Echo onlineEcho = EchoTestSupport.warriorEchoWithData(5);
		onlineEcho.echoId = "ranked-online-5";
		String fetchJson = rankedFetchJson(onlineEcho);
		Assertions.assertThat(EchoWireCodec.decodeEchoFetch(fetchJson).echo.hasCombatData()).isTrue();

		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(200, fetchJson);

		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		Echo localEcho = EchoTestSupport.warriorEchoWithData(5);
		localEcho.echoId = "local-ranked-should-not-be-used";
		EchoStorage local = new EchoStorage();
		local.save(localEcho);

		CompositeEchoLookup.setEchoLookupForTests(new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local));

		boolean found = Dungeon.prefetchEchoBossForDepth(5);

		Assertions.assertThat(found).isTrue();
		Assertions.assertThat(transport.requests).hasSize(1);
		Assertions.assertThat(transport.requests.get(0).method).isEqualTo("GET");
		Assertions.assertThat(transport.requests.get(0).url).contains("/v1/echoes/5");
		Assertions.assertThat(Dungeon.getPendingEcho()).isNotNull();
		Assertions.assertThat(Dungeon.getPendingEcho().echoId).isEqualTo("ranked-online-5");
		Assertions.assertThat(Dungeon.getPendingEcho().hasCombatData()).isTrue();
		Assertions.assertThat(Dungeon.isEchoBossActive()).isTrue();
	}

	@Test
	@DisplayName("ranked boss prefetch does not fall back to local echoes when backend misses")
	void rankedPrefetchDoesNotUseLocalStorage() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(404, "{\"error\":\"missing\"}");

		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		EchoStorage local = new EchoStorage();
		local.save(EchoTestSupport.warriorEchoWithData(5));

		CompositeEchoLookup.setEchoLookupForTests(new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local));

		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isFalse();
		Assertions.assertThat(transport.requests).hasSize(1);
		Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
		Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
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
