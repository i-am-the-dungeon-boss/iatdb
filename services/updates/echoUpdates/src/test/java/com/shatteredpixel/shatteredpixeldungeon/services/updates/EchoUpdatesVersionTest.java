package com.shatteredpixel.shatteredpixeldungeon.services.updates;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EchoUpdatesVersionTest {

	@Test
	@DisplayName("treats equal version names as up to date")
	void equalNamesAreUpToDate() {
		Assertions.assertThat(EchoUpdates.isRemoteNewer("0.0.1", "0.0.1")).isFalse();
	}

	@Test
	@DisplayName("ignores -INDEV suffix on the local version name")
	void ignoresIndevSuffix() {
		Assertions.assertThat(EchoUpdates.isRemoteNewer("0.0.1", "0.0.1-INDEV")).isFalse();
	}

	@Test
	@DisplayName("detects a newer remote version name")
	void detectsNewerRemote() {
		Assertions.assertThat(EchoUpdates.isRemoteNewer("0.0.2", "0.0.1")).isTrue();
		Assertions.assertThat(EchoUpdates.isRemoteNewer("0.0.10", "0.0.9")).isTrue();
	}

	@Test
	@DisplayName("does not treat an older remote as newer")
	void olderRemoteIsNotNewer() {
		Assertions.assertThat(EchoUpdates.isRemoteNewer("0.0.1", "0.0.2")).isFalse();
	}
}
