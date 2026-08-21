package com.shatteredpixel.shatteredpixeldungeon.services.updates;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Version name comparison")
class VersionNamesTest {

	@Test
	@DisplayName("identical names match")
	void identicalNamesMatch() {
		assertThat(VersionNames.differ("0.0.13", "0.0.13")).isFalse();
	}

	@Test
	@DisplayName("a build suffix is not a different version")
	void buildSuffixesAreIgnored() {
		assertThat(VersionNames.differ("0.0.13", "0.0.13-INDEV")).isFalse();
		assertThat(VersionNames.differ("0.0.13-INDEV", "0.0.13")).isFalse();
		assertThat(VersionNames.differ("0.0.13-INDEV", "0.0.13-BETA")).isFalse();
	}

	@Test
	@DisplayName("different version cores differ, suffix or not")
	void differentCoresDiffer() {
		assertThat(VersionNames.differ("0.0.14", "0.0.13-INDEV")).isTrue();
		assertThat(VersionNames.differ("0.0.13-INDEV", "0.0.14")).isTrue();
		assertThat(VersionNames.differ("0.1.0", "0.0.13")).isTrue();
	}

	@Test
	@DisplayName("missing trailing components count as zero")
	void missingComponentsAreZero() {
		assertThat(VersionNames.differ("1.0", "1.0.0")).isFalse();
		assertThat(VersionNames.differ("1.0", "1.0.1")).isTrue();
	}

	@Test
	@DisplayName("surrounding whitespace is not a version difference")
	void whitespaceIsIgnored() {
		assertThat(VersionNames.differ(" 0.0.13 ", "0.0.13-INDEV")).isFalse();
	}
}
