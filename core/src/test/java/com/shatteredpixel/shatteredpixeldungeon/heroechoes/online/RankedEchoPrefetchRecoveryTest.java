package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoCaptureTrigger;
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

import java.io.File;
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
	@DisplayName("continue solo after ranked ERROR uses solo-local echo when available")
	void continueSoloUsesLocalEcho() {
		CompositeEchoLookup.rankedRetryDelayMs = 0L;
		EchoHttpTransport transport = request -> {
			throw new RuntimeException("network down");
		};

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");

		// Seed solo storage before the ranked run fails.
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Echo localEcho = EchoTestSupport.warriorEchoWithData(5);
		localEcho.echoId = "local-after-solo";
		EchoStorage local = new EchoStorage();
		local.save(localEcho);

		Dungeon.echoPlayMode = EchoPlayMode.RANKED;
		CompositeEchoLookup.setEchoLookupForTests(new CompositeEchoLookup(
				new EchoClient("https://echo.test", "secret", transport),
				local));

		EchoLookupOutcome outcome = Dungeon.prefetchEchoBossWithRankedRecovery(
				5, failed -> EchoPrefetchUserChoice.CONTINUE_SOLO);

		Assertions.assertThat(outcome.isFound()).isTrue();
		Assertions.assertThat(Dungeon.echoPlayMode).isEqualTo(EchoPlayMode.SOLO);
		Assertions.assertThat(Dungeon.getPendingEcho().echoId).isEqualTo("local-after-solo");
	}

	@Test
	@DisplayName("continue solo after ranked ERROR still captures boss kill into echoes-solo")
	void continueSoloStillCapturesBossKillIntoEchoesSolo() {
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

		Dungeon.prefetchEchoBossWithRankedRecovery(
				5, failed -> EchoPrefetchUserChoice.CONTINUE_SOLO);

		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		Dungeon.depth = 5;

		EchoCaptureTrigger.onBossDefeated();

		Assertions.assertThat(Dungeon.echoPlayMode).isEqualTo(EchoPlayMode.SOLO);
		Assertions.assertThat(new File("echoes-solo/depth-5.dat")).exists();
		Assertions.assertThat(new File("echoes-ranked/depth-5.dat")).doesNotExist();
		Assertions.assertThat(new EchoStorage().loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION))
				.isPresent();
	}

	@Test
	@DisplayName("continue solo after ranked ERROR falls back when local echo missing")
	void continueSoloFallsBackWithoutLocalEcho() {
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
				5, failed -> EchoPrefetchUserChoice.CONTINUE_SOLO);

		Assertions.assertThat(outcome.isNotFound()).isTrue();
		Assertions.assertThat(Dungeon.echoPlayMode).isEqualTo(EchoPlayMode.SOLO);
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

	private static String rankedFetchJson(Echo echo) throws Exception {
		JSONObject json = new JSONObject(EchoWireCodec.encodeEchoUpload(echo, "test-client"));
		json.put("echo_policy", new JSONObject()
				.put("policy_schema_version", 1)
				.put("rules", new JSONArray()
						.put(new JSONObject()
								.put("when", new JSONObject())
								.put("do", new JSONObject().put("action", "MELEE_CHASE"))
								.put("priority", 0))));
		return json.toString();
	}

	@Test
	@DisplayName("solo mode ERROR does not prompt and leaves echo inactive")
	void soloErrorDoesNotPrompt() {
		AtomicInteger prompts = new AtomicInteger();
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoLookupOutcome.error());
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;

		EchoLookupOutcome outcome = Dungeon.prefetchEchoBossWithRankedRecovery(5, failed -> {
			prompts.incrementAndGet();
			return EchoPrefetchUserChoice.RETRY;
		});

		Assertions.assertThat(prompts.get()).isZero();
		Assertions.assertThat(outcome.isError()).isTrue();
		Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
	}
}
