package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EchoBackendProbeTest {

	@AfterEach
	void cleanup() {
		EchoBackendProbe.resetForTests();
		EchoOnlineSettings.resetForTests();
	}

	@Test
	@DisplayName("online is unavailable when backend URL is missing")
	void unavailableWithoutBackendUrl() {
		Assertions.assertThat(EchoBackendProbe.isOnlineReady()).isFalse();
		Assertions.assertThat(EchoBackendProbe.offlineMessageKey()).isEqualTo("offline_unconfigured");
	}

	@Test
	@DisplayName("online is unavailable when backend is configured but unreachable")
	void unavailableWhenUnreachable() {
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoBackendProbe.setReachableForTests(false);

		Assertions.assertThat(EchoBackendProbe.isOnlineReady()).isFalse();
		Assertions.assertThat(EchoBackendProbe.offlineMessageKey()).isEqualTo("offline_unreachable");
	}

	@Test
	@DisplayName("online is available when backend is configured and healthy")
	void availableWhenReachable() {
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoBackendProbe.setReachableForTests(true);

		Assertions.assertThat(EchoBackendProbe.isOnlineReady()).isTrue();
	}
}
