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

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

@ExtendWith(GdxTestExtension.class)
class CompositeEchoLookupTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
		EchoOnlineSettings.resetForTests();
		Dungeon.echoPlayMode = EchoPlayMode.NONE;
	}

	@Test
	@DisplayName("uses online echo when online mode is enabled and fetch succeeds")
	void usesOnlineEchoFirst() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(200, "{"
				+ "\"echo_id\":\"online-1\","
				+ "\"depth\":5,"
				+ "\"game_version\":846,"
				+ "\"hero_class\":\"MAGE\","
				+ "\"lvl\":6,"
				+ "\"hp\":20,"
				+ "\"ht\":30,"
				+ "\"timestamp\":1,"
				+ "\"game_seed\":9,"
				+ "\"echo_data_base64\":\"dGVzdA==\","
				+ "\"echo_policy\":{"
				+ "\"policy_schema_version\":1,"
				+ "\"rules\":[{\"when\":{},\"do\":{\"action\":\"MELEE_CHASE\"},\"priority\":0}]"
				+ "}"
				+ "}");

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");

		EchoStorage local = new EchoStorage();
		local.save(EchoTestSupport.warriorEcho(5));

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local
		);

		Optional<Echo> echo = lookup.findEchoForDepth(5);

		Assertions.assertThat(echo).isPresent();
		Assertions.assertThat(echo.get().echoId).isEqualTo("online-1");
		Assertions.assertThat(lookup.getLastFetchedPolicy()).isPresent();
	}

	@Test
	@DisplayName("falls back to local echo when online fetch misses outside ranked mode")
	void fallsBackToLocalEcho() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(404, "{}");

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		Dungeon.echoPlayMode = EchoPlayMode.NONE;

		Echo localEcho = EchoTestSupport.warriorEcho(5);
		EchoStorage local = new EchoStorage();
		local.save(localEcho);

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local
		);

		Optional<Echo> echo = lookup.findEchoForDepth(5);

		Assertions.assertThat(echo).isPresent();
		Assertions.assertThat(echo.get().echoId).isEqualTo(localEcho.echoId);
		Assertions.assertThat(lookup.getLastFetchedPolicy()).isEmpty();
	}

	@Test
	@DisplayName("solo mode skips online fetch even when backend is configured")
	void soloModeSkipsOnlineFetch() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(200, "{"
				+ "\"echo_id\":\"online-1\","
				+ "\"depth\":5,"
				+ "\"game_version\":846,"
				+ "\"hero_class\":\"MAGE\","
				+ "\"lvl\":6,"
				+ "\"hp\":20,"
				+ "\"ht\":30,"
				+ "\"timestamp\":1,"
				+ "\"game_seed\":9,"
				+ "\"echo_data_base64\":\"dGVzdA==\""
				+ "}");

		EchoOnlineSettings.setBackendUrl("https://echo.test");
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;

		Echo localEcho = EchoTestSupport.warriorEcho(5);
		EchoStorage local = new EchoStorage();
		local.save(localEcho);

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local
		);

		Optional<Echo> echo = lookup.findEchoForDepth(5);

		Assertions.assertThat(echo).isPresent();
		Assertions.assertThat(echo.get().echoId).isEqualTo(localEcho.echoId);
		Assertions.assertThat(transport.requests).isEmpty();
	}

	@Test
	@DisplayName("falls back to local echo when online fetch throws outside ranked mode")
	void fallsBackWhenOnlineFetchThrows() throws Exception {
		EchoHttpTransport transport = request -> {
			throw new RuntimeException("network down");
		};

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		Dungeon.echoPlayMode = EchoPlayMode.NONE;

		Echo localEcho = EchoTestSupport.warriorEcho(5);
		EchoStorage local = new EchoStorage();
		local.save(localEcho);

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local
		);

		Optional<Echo> echo = lookup.findEchoForDepth(5);

		Assertions.assertThat(echo).isPresent();
		Assertions.assertThat(echo.get().echoId).isEqualTo(localEcho.echoId);
	}

	@Test
	@DisplayName("ranked mode does not fall back to local echoes")
	void rankedModeSkipsLocalFallback() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(404, "{}");

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		EchoStorage local = new EchoStorage();
		local.save(EchoTestSupport.warriorEcho(5));

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local
		);

		Assertions.assertThat(lookup.findEchoForDepth(5)).isEmpty();
	}
}
