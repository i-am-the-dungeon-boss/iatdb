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
		EchoPlayerSession.applyAuthResponse("jwt-token", "HeroName", false, null);

		Assertions.assertThat(EchoPlayerSession.jwt()).isEqualTo("jwt-token");
		Assertions.assertThat(EchoPlayerSession.username()).isEqualTo("HeroName");
		Assertions.assertThat(EchoPlayerSession.hasCredentials()).isFalse();
		Assertions.assertThat(EchoPlayerSession.hasSession()).isTrue();

		EchoPlayerSession.reloadForTests();
		Assertions.assertThat(EchoPlayerSession.jwt()).isEqualTo("jwt-token");
		Assertions.assertThat(EchoPlayerSession.username()).isEqualTo("HeroName");
	}

	@Test
	@DisplayName("clearSession keeps device id")
	void clearSessionKeepsDeviceId() {
		String deviceId = EchoPlayerSession.deviceId();
		EchoPlayerSession.applyAuthResponse("jwt-token", "HeroName", true, "a@b.c");
		EchoPlayerSession.clearSession();

		Assertions.assertThat(EchoPlayerSession.hasSession()).isFalse();
		Assertions.assertThat(EchoPlayerSession.deviceId()).isEqualTo(deviceId);
	}
}
