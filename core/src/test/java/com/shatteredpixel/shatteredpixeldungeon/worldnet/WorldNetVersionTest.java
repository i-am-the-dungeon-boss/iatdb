package com.shatteredpixel.shatteredpixeldungeon.worldnet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("World channel version comparison")
class WorldNetVersionTest {

	@Test
	@DisplayName("wants an update when the server expects a different build")
	void differentBuildsNeedAnUpdate() {
		assertThat(WorldNet.versionsDiffer("2.4.0", "2.3.0")).isTrue();
	}

	@Test
	@DisplayName("is content when the builds match")
	void matchingBuildsAreFine() {
		assertThat(WorldNet.versionsDiffer("2.4.0", "2.4.0")).isFalse();
	}

	@Test
	@DisplayName("gates nobody out on a server too old to state its version")
	void anUnstatedServerVersionGatesNobody() {
		assertThat(WorldNet.versionsDiffer("", "2.3.0")).isFalse();
		assertThat(WorldNet.versionsDiffer(null, "2.3.0")).isFalse();
	}

	@Test
	@DisplayName("gates nobody out when this build does not know its own version")
	void anUnknownInstalledVersionGatesNobody() {
		assertThat(WorldNet.versionsDiffer("2.4.0", "")).isFalse();
		assertThat(WorldNet.versionsDiffer("2.4.0", null)).isFalse();
	}

	@Test
	@DisplayName("gates nobody out over a build suffix on the same version")
	void aBuildSuffixIsNotADifferentBuild() {
		assertThat(WorldNet.versionsDiffer("0.0.13", "0.0.13-INDEV")).isFalse();
		assertThat(WorldNet.versionsDiffer("0.0.13-INDEV", "0.0.13")).isFalse();
	}

	@Test
	@DisplayName("still gates a suffixed build whose version differs")
	void aSuffixDoesNotExcuseAnOldBuild() {
		assertThat(WorldNet.versionsDiffer("0.0.14", "0.0.13-INDEV")).isTrue();
	}
}
