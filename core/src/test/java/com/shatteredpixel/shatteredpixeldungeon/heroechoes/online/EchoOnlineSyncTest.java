package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoFightResult;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

@ExtendWith(GdxTestExtension.class)
class EchoOnlineSyncTest {

	@AfterEach
	void cleanup() {
		EchoOnlineSettings.resetForTests();
	}

	@Test
	@DisplayName("does not call backend when online mode is disabled")
	void skipsWhenOffline() throws Exception {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		EchoOnlineSync sync = new EchoOnlineSync(new EchoClient("https://echo.test", "secret", transport));

		EchoOnlineSettings.setOnlineEnabled(false);
		sync.uploadEchoAsync(EchoTestSupport.warriorEcho(5));
		sync.postLeaderboardResultAsync(new EchoFightResult(
				"5-1", true, 5, 1L, 846, "MAGE", 10, 5, 8
		));

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

		sync.uploadEchoAsync(EchoTestSupport.warriorEcho(5));
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

		sync.postLeaderboardResultAsync(new EchoFightResult(
				"5-1", true, 5, 1L, 846, "MAGE", 10, 5, 8
		));
		sync.awaitBackgroundTasksForTests();

		Assertions.assertThat(transport.requests).hasSize(1);
		Assertions.assertThat(transport.requests.get(0).url).endsWith("/v1/leaderboard/results");
	}
}
