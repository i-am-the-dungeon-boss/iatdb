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
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
	}

	@Test
	@DisplayName("ranked mode fetches echo online")
	void rankedModeFetchesOnline() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(200, "{"
				+ "\"echo_id\":\"online-1\","
				+ "\"depth\":5,"
				+ "\"game_version\":\"0.0.1\","
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
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		EchoStorage local = new EchoStorage();
		local.save(EchoTestSupport.warriorEcho(5));

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local);

		Optional<EchoFetchResult> result = lookup.findEchoForDepth(5);

		Assertions.assertThat(result).isPresent();
		Assertions.assertThat(result.get().echo.echoId).isEqualTo("online-1");
		Assertions.assertThat(result.get().policy).isNotNull();
		Assertions.assertThat(transport.requests).hasSize(1);
	}

	@Test
	@DisplayName("ranked mode returns empty when fetch response omits policy")
	void rankedModeReturnsEmptyWithoutPolicy() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(200, "{"
				+ "\"echo_id\":\"online-1\","
				+ "\"depth\":5,"
				+ "\"game_version\":\"0.0.1\","
				+ "\"hero_class\":\"MAGE\","
				+ "\"lvl\":6,"
				+ "\"hp\":20,"
				+ "\"ht\":30,"
				+ "\"timestamp\":1,"
				+ "\"game_seed\":9,"
				+ "\"echo_data_base64\":\"dGVzdA==\""
				+ "}");

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				new EchoStorage());

		Assertions.assertThat(lookup.findEchoForDepth(5)).isEmpty();
	}

	@Test
	@DisplayName("ranked mode returns empty when online fetch misses")
	void rankedModeReturnsEmptyOnMiss() throws Exception {
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
				local);

		Assertions.assertThat(lookup.findEchoForDepth(5)).isEmpty();
	}

	@Test
	@DisplayName("ranked mode returns empty when online fetch throws")
	void rankedModeReturnsEmptyOnThrow() throws Exception {
		EchoHttpTransport transport = request -> {
			throw new RuntimeException("network down");
		};

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		EchoStorage local = new EchoStorage();
		local.save(EchoTestSupport.warriorEcho(5));

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local);

		Assertions.assertThat(lookup.findEchoForDepth(5)).isEmpty();
	}

	@Test
	@DisplayName("non-ranked mode never fetches online even when backend is configured")
	void nonRankedNeverFetchesOnline() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(200, "{"
				+ "\"echo_id\":\"online-1\","
				+ "\"depth\":5,"
				+ "\"game_version\":\"0.0.1\","
				+ "\"hero_class\":\"MAGE\","
				+ "\"lvl\":6,"
				+ "\"hp\":20,"
				+ "\"ht\":30,"
				+ "\"timestamp\":1,"
				+ "\"game_seed\":9,"
				+ "\"echo_data_base64\":\"dGVzdA==\""
				+ "}");

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		Dungeon.echoPlayMode = EchoPlayMode.NONE;

		Echo localEcho = EchoTestSupport.warriorEcho(5);
		EchoStorage local = new EchoStorage();
		local.save(localEcho);

		CompositeEchoLookup lookup = new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local);

		Optional<EchoFetchResult> result = lookup.findEchoForDepth(5);

		Assertions.assertThat(result).isPresent();
		Assertions.assertThat(result.get().echo.echoId).isEqualTo(localEcho.echoId);
		Assertions.assertThat(result.get().policy).isNotNull();
		Assertions.assertThat(transport.requests).isEmpty();
	}

	@Test
	@DisplayName("solo mode fetches echo locally and never calls online")
	void soloModeFetchesLocally() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(200, "{"
				+ "\"echo_id\":\"online-1\","
				+ "\"depth\":5,"
				+ "\"game_version\":\"0.0.1\","
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
				local);

		Optional<EchoFetchResult> result = lookup.findEchoForDepth(5);

		Assertions.assertThat(result).isPresent();
		Assertions.assertThat(result.get().echo.echoId).isEqualTo(localEcho.echoId);
		Assertions.assertThat(result.get().policy).isNotNull();
		Assertions.assertThat(transport.requests).isEmpty();
	}
}
