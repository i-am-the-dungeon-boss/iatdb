package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.SentryCrashReporting;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoClientTest.FakeEchoHttpTransport;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
@DisplayName("Server mute refresh")
class ServerMuteRefreshTest {

	private static final long NOW = 1_700_000_000_000L;

	@AfterEach
	void cleanup() {
		ServerMuteRefresh.resetForTests();
		EchoPlayerSession.resetForTests();
		SentryCrashReporting.resetReporter();
	}

	private static void session(long mutedUntil) {
		EchoPlayerSession.applyAuthResponse("jwt", "Ann", false, null, mutedUntil);
	}

	private static EchoClient clientReturning(String body) {
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(200, body);
		return new EchoClient("https://echo.test", "secret-key", transport);
	}

	@Test
	@DisplayName("asks nothing while the mute is still running")
	void quietWhileMuted() {
		session(NOW + 60_000L);

		Assertions.assertThat(ServerMuteRefresh.claim(NOW)).isFalse();
	}

	@Test
	@DisplayName("asks nothing when there is no mute to check on")
	void quietWithoutAMute() {
		session(0L);

		Assertions.assertThat(ServerMuteRefresh.claim(NOW)).isFalse();
	}

	@Test
	@DisplayName("asks nothing without a session, since there is nobody to ask about")
	void quietWithoutASession() {
		Assertions.assertThat(ServerMuteRefresh.claim(NOW)).isFalse();
	}

	@Test
	@DisplayName("asks once the deadline is reached, because it may have been extended")
	void asksWhenTheDeadlineIsReached() {
		session(NOW);

		Assertions.assertThat(ServerMuteRefresh.claim(NOW)).isTrue();
	}

	@Test
	@DisplayName("claims the check so a per-frame caller cannot start a second one")
	void claimsOnlyOnce() {
		session(NOW - 1L);
		Assertions.assertThat(ServerMuteRefresh.claim(NOW)).isTrue();

		Assertions.assertThat(ServerMuteRefresh.claim(NOW)).isFalse();
	}

	@Test
	@DisplayName("adopts a mute the admin extended, without a reconnect")
	void adoptsAnExtendedMute() {
		session(NOW - 1L);
		long extended = System.currentTimeMillis() + 3_600_000L;

		ServerMuteRefresh.refresh(clientReturning(
				"{\"username\":\"Ann\",\"has_credentials\":false,\"muted_until\":" + extended + "}"));

		Assertions.assertThat(EchoPlayerSession.mutedUntil()).isEqualTo(extended);
	}

	@Test
	@DisplayName("drops a mute the admin cleared")
	void dropsAClearedMute() {
		session(NOW - 1L);

		ServerMuteRefresh.refresh(
				clientReturning("{\"username\":\"Ann\",\"has_credentials\":false}"));

		Assertions.assertThat(EchoPlayerSession.mutedUntil()).isZero();
	}

	@Test
	@DisplayName("does not ask again about a deadline the server has already answered for")
	void asksOnlyOncePerDeadline() {
		session(NOW - 1L);
		long extended = System.currentTimeMillis() + 3_600_000L;
		ServerMuteRefresh.claim(NOW);
		ServerMuteRefresh.refresh(clientReturning(
				"{\"username\":\"Ann\",\"has_credentials\":false,\"muted_until\":" + extended + "}"));

		// Far enough ahead that the retry throttle has long since lapsed.
		Assertions.assertThat(ServerMuteRefresh.claim(NOW + 3_600_000L)).isFalse();
	}

	@Test
	@DisplayName("checks an extended deadline again in its turn, once that one runs out too")
	void rechecksAnExtendedDeadline() {
		session(NOW - 1L);
		ServerMuteRefresh.claim(NOW);
		ServerMuteRefresh.refresh(clientReturning(
				"{\"username\":\"Ann\",\"has_credentials\":false,\"muted_until\":" + (NOW + 1_000L) + "}"));

		Assertions.assertThat(ServerMuteRefresh.claim(NOW + 2_000L)).isTrue();
	}

	@Test
	@DisplayName("retries later when the server could not be reached, rather than giving up")
	void retriesAfterAFailure() {
		session(NOW - 1L);
		FakeEchoHttpTransport transport = new FakeEchoHttpTransport();
		transport.enqueue(500, "");
		ServerMuteRefresh.claim(NOW);

		ServerMuteRefresh.refresh(new EchoClient("https://echo.test", "secret-key", transport));

		Assertions.assertThat(ServerMuteRefresh.claim(NOW + 1_000L)).isFalse();
		Assertions.assertThat(ServerMuteRefresh.claim(NOW + 120_000L)).isTrue();
	}
}
