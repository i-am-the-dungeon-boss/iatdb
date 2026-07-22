package com.shatteredpixel.shatteredpixeldungeon.services.updates;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EchoUpdatesVersionTest {

	@Test
	@DisplayName("equal version names do not require update")
	void equalNamesDoNotRequireUpdate() {
		Assertions.assertThat(EchoUpdates.requiresUpdate("0.0.1", "0.0.1")).isFalse();
	}

	@Test
	@DisplayName("ignores -INDEV suffix when comparing version names")
	void ignoresIndevSuffix() {
		Assertions.assertThat(EchoUpdates.requiresUpdate("0.0.1", "0.0.1-INDEV")).isFalse();
	}

	@Test
	@DisplayName("requires update when remote version differs from local")
	void requiresUpdateWhenVersionsDiffer() {
		Assertions.assertThat(EchoUpdates.requiresUpdate("0.0.2", "0.0.1")).isTrue();
		Assertions.assertThat(EchoUpdates.requiresUpdate("0.0.10", "0.0.9")).isTrue();
		Assertions.assertThat(EchoUpdates.requiresUpdate("0.0.1", "0.0.2")).isTrue();
	}

	@Test
	@DisplayName("empty remote version does not require update")
	void emptyRemoteDoesNotRequireUpdate() {
		Assertions.assertThat(EchoUpdates.requiresUpdate("", "0.0.1")).isFalse();
		Assertions.assertThat(EchoUpdates.requiresUpdate(null, "0.0.1")).isFalse();
	}

	@Test
	@DisplayName("uses backend update_url when present")
	void usesBackendUpdateUrl() {
		Assertions.assertThat(EchoUpdates.resolveUpdateUrl("https://example.com/dl"))
				.isEqualTo("https://example.com/dl");
	}

	@Test
	@DisplayName("falls back to GitHub latest release when update_url missing")
	void fallsBackToGitHubLatest() {
		Assertions.assertThat(EchoUpdates.resolveUpdateUrl(null))
				.isEqualTo(EchoUpdates.DEFAULT_UPDATE_URL);
		Assertions.assertThat(EchoUpdates.resolveUpdateUrl("  "))
				.isEqualTo(EchoUpdates.DEFAULT_UPDATE_URL);
	}
}
