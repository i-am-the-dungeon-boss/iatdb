package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoPlayerSessionTest {

	@AfterEach
	void cleanup() {
		EchoPlayerSession.resetForTests();
	}

	@Test
	@DisplayName("device id is created once and reused")
	void deviceIdCreatedOnceAndReused() {
		String first = EchoPlayerSession.deviceId();
		String second = EchoPlayerSession.deviceId();

		Assertions.assertThat(first).isNotBlank().hasSizeGreaterThanOrEqualTo(16);
		Assertions.assertThat(second).isEqualTo(first);
	}

	@Test
	@DisplayName("persists jwt username and credentials flag")
	void persistsSessionFields() {
		EchoPlayerSession.applyAuthResponse("jwt-token", "HeroName", false, null, 0L);

		Assertions.assertThat(EchoPlayerSession.jwt()).isEqualTo("jwt-token");
		Assertions.assertThat(EchoPlayerSession.username()).isEqualTo("HeroName");
		Assertions.assertThat(EchoPlayerSession.hasCredentials()).isFalse();
		Assertions.assertThat(EchoPlayerSession.hasSession()).isTrue();

		EchoPlayerSession.reloadForTests();
		Assertions.assertThat(EchoPlayerSession.jwt()).isEqualTo("jwt-token");
		Assertions.assertThat(EchoPlayerSession.username()).isEqualTo("HeroName");
	}

	@Test
	@DisplayName("holds the mute the server sent, across a reload, so it need not be asked for again")
	void persistsMuteAcrossReload() {
		long until = System.currentTimeMillis() + 3_600_000L;
		EchoPlayerSession.applyAuthResponse("jwt-token", "HeroName", false, null, until);

		Assertions.assertThat(EchoPlayerSession.mutedUntil()).isEqualTo(until);

		EchoPlayerSession.reloadForTests();
		Assertions.assertThat(EchoPlayerSession.mutedUntil()).isEqualTo(until);
	}

	@Test
	@DisplayName("treats an absent mute as not muted")
	void absentMuteIsNotMuted() {
		EchoPlayerSession.applyAuthResponse("jwt-token", "HeroName", false, null, 0L);

		Assertions.assertThat(EchoPlayerSession.mutedUntil()).isZero();
	}

	@Test
	@DisplayName("drops the mute along with the session it came with")
	void clearSessionDropsMute() {
		EchoPlayerSession.applyAuthResponse(
				"jwt-token", "HeroName", false, null, System.currentTimeMillis() + 60_000L);

		EchoPlayerSession.clearSession();

		Assertions.assertThat(EchoPlayerSession.mutedUntil()).isZero();
	}

	@Test
	@DisplayName("clearSession keeps device id")
	void clearSessionKeepsDeviceId() {
		String deviceId = EchoPlayerSession.deviceId();
		EchoPlayerSession.applyAuthResponse("jwt-token", "HeroName", true, "a@b.c", 0L);
		EchoPlayerSession.clearSession();

		Assertions.assertThat(EchoPlayerSession.hasSession()).isFalse();
		Assertions.assertThat(EchoPlayerSession.deviceId()).isEqualTo(deviceId);
	}
}
