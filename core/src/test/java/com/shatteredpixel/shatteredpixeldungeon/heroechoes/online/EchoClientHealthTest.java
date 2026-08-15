package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Reachability is decided by the game-version payload; the client no longer calls /health. */
class EchoClientHealthTest {

	@Test
	@DisplayName("reachable response is 200 with a version name")
	void acceptsVersionResponse() {
		Assertions.assertThat(EchoClient.parseVersionName(200, "{\"version_name\":\"1.4.2\"}"))
				.isEqualTo("1.4.2");
	}

	@Test
	@DisplayName("non-200 responses are unreachable")
	void rejectsNon200() {
		Assertions.assertThat(EchoClient.parseVersionName(503, "{\"version_name\":\"1.4.2\"}")).isNull();
	}

	@Test
	@DisplayName("missing or invalid version name is unreachable")
	void rejectsInvalidBody() {
		Assertions.assertThat(EchoClient.parseVersionName(200, "{}")).isNull();
		Assertions.assertThat(EchoClient.parseVersionName(200, "not-json")).isNull();
		Assertions.assertThat(EchoClient.parseVersionName(200, null)).isNull();
	}

	@Test
	@DisplayName("whitespace-only body or version name is unreachable")
	void rejectsWhitespaceOnly() {
		Assertions.assertThat(EchoClient.parseVersionName(200, "   ")).isNull();
		Assertions.assertThat(EchoClient.parseVersionName(200, "{\"version_name\":\"  \"}")).isNull();
	}
}
