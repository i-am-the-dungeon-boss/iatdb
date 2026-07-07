package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EchoHealthTest {

	@Test
	@DisplayName("healthy response is 200 with status ok")
	void acceptsHealthyResponse() {
		Assertions.assertThat(EchoHealth.isHealthy(200, "{\"status\":\"ok\"}")).isTrue();
	}

	@Test
	@DisplayName("non-200 responses are unhealthy")
	void rejectsNon200() {
		Assertions.assertThat(EchoHealth.isHealthy(503, "{\"status\":\"ok\"}")).isFalse();
	}

	@Test
	@DisplayName("missing or invalid status is unhealthy")
	void rejectsInvalidBody() {
		Assertions.assertThat(EchoHealth.isHealthy(200, "{}")).isFalse();
		Assertions.assertThat(EchoHealth.isHealthy(200, "not-json")).isFalse();
		Assertions.assertThat(EchoHealth.isHealthy(200, null)).isFalse();
	}
}
