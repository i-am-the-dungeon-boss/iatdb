/*
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
 */

package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Guards release.ps1: native desktop zip (jpackage) is always part of prepareRelease.
 */
class ReleaseJpackageWiringTest {

	@Test
	@DisplayName("release.ps1 always passes -PwithJpackage to prepareRelease")
	void releaseAlwaysPassesWithJpackage() throws IOException {
		String source = readSource("scripts/release.ps1");
		int prepare = source.indexOf("@('prepareRelease'");
		int withJpackage = source.indexOf("'-PwithJpackage'", prepare);
		int conditional = source.indexOf("if ($WithJpackage)", prepare);

		Assertions.assertThat(prepare).isGreaterThanOrEqualTo(0);
		Assertions.assertThat(withJpackage).isGreaterThan(prepare);
		Assertions.assertThat(conditional).isLessThan(0);
		Assertions.assertThat(source).doesNotContain("[switch] $WithJpackage");
	}

	private static String readSource(String relativePath) throws IOException {
		Path file = findRepoFile(relativePath);
		return Files.readString(file, StandardCharsets.UTF_8);
	}

	private static Path findRepoFile(String relativePath) {
		Path dir = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			Path candidate = dir.resolve(relativePath);
			if (Files.isRegularFile(dir.resolve("settings.gradle")) && Files.isRegularFile(candidate)) {
				return candidate;
			}
			dir = dir.getParent();
		}
		throw new AssertionError("Could not find " + relativePath + " from " + Paths.get("").toAbsolutePath());
	}
}
