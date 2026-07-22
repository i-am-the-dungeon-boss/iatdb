package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoFightResult;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.SentryCrashReporting;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(GdxTestExtension.class)
class EchoOnlineSyncTest {

	@BeforeEach
	@AfterEach
	void cleanup() {
		EchoOnlineSettings.resetForTests();
		EchoPlayerSession.resetForTests();
		SentryCrashReporting.resetReporter();
	}

	@Test
	@DisplayName("does not recreate player from cached local name when session is missing")
	void doesNotRecreateWhenSessionMissing() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(201, "{}");
		EchoOnlineSync sync = new EchoOnlineSync(new EchoClient("https://echo.test", "secret", transport));

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		com.shatteredpixel.shatteredpixeldungeon.SPDSettings.playerName("CachedHero");

		Assertions.assertThat(EchoPlayerSession.hasSession()).isFalse();
		sync.uploadEchoAsync(EchoTestSupport.warriorEchoWithData(5));
		sync.awaitBackgroundTasksForTests();

		Assertions.assertThat(transport.requests).isEmpty();
	}

	@Test
	@DisplayName("does not call backend when online mode is disabled")
	void skipsWhenOffline() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		EchoOnlineSync sync = new EchoOnlineSync(new EchoClient("https://echo.test", "secret", transport));

		EchoOnlineSettings.setOnlineEnabled(false);
		sync.uploadEchoAsync(EchoTestSupport.warriorEcho(5));
		sync.postLeaderboardResultAsync(new EchoFightResult(
				"5-1", true, 5, 1L, "0.0.1", "MAGE", 10, 5, 8));

		Assertions.assertThat(transport.requests).isEmpty();
	}

	@Test
	@DisplayName("uploads echo asynchronously when online mode is enabled")
	void uploadsWhenOnline() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(201, "{}");
		EchoOnlineSync sync = new EchoOnlineSync(new EchoClient("https://echo.test", "secret", transport));

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		EchoPlayerSession.applyAuthResponse("jwt", "Hero", false, null);

		sync.uploadEchoAsync(EchoTestSupport.warriorEchoWithData(5));
		sync.awaitBackgroundTasksForTests();

		Assertions.assertThat(transport.requests).hasSize(1);
		Assertions.assertThat(transport.requests.get(0).url).endsWith("/v1/echoes");
	}

	@Test
	@DisplayName("posts leaderboard result asynchronously when online mode is enabled")
	void postsLeaderboardWhenOnline() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(201, "{}");
		EchoOnlineSync sync = new EchoOnlineSync(new EchoClient("https://echo.test", "secret", transport));

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoPlayerSession.applyAuthResponse("jwt", "Hero", false, null);

		sync.postLeaderboardResultAsync(new EchoFightResult(
				"5-1", true, 5, 1L, "0.0.1", "MAGE", 10, 5, 8));
		sync.awaitBackgroundTasksForTests();

		Assertions.assertThat(transport.requests).hasSize(1);
		Assertions.assertThat(transport.requests.get(0).url).endsWith("/v1/leaderboard/results");
	}

	@Test
	@DisplayName("uploadEchoAsync reports upload failure to Sentry")
	void uploadEchoAsyncReportsFailureToSentry() throws Exception {
		List<Throwable> captured = new ArrayList<>();
		SentryCrashReporting.setReporter(captured::add);

		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(500, "{\"error\":\"nope\"}");
		EchoOnlineSync sync = new EchoOnlineSync(new EchoClient("https://echo.test", "secret", transport));

		EchoOnlineSettings.setOnlineEnabled(true);
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		EchoPlayerSession.applyAuthResponse("jwt", "Hero", false, null);

		sync.uploadEchoAsync(EchoTestSupport.warriorEchoWithData(5));
		sync.awaitBackgroundTasksForTests();

		Assertions.assertThat(captured).hasSize(1);
		Assertions.assertThat(captured.get(0)).isInstanceOf(EchoHttpException.class);
		Assertions.assertThat(((EchoHttpException) captured.get(0)).statusCode).isEqualTo(500);
	}
}
