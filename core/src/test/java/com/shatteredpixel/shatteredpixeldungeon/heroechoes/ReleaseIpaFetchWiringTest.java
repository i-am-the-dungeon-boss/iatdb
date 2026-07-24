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
 * Guards release.ps1 IPA fetch: overlap Actions with local build, reuse
 * same-SHA runs.
 */
class ReleaseIpaFetchWiringTest {

	@Test
	@DisplayName("Fetch-UnsignedIosIpa splits start and complete so release can overlap CI")
	void fetchScriptSplitsStartAndComplete() throws IOException {
		String source = readSource("scripts/release/Fetch-UnsignedIosIpa.ps1");

		Assertions.assertThat(source).contains("function Start-UnsignedIosIpaViaActions");
		Assertions.assertThat(source).contains("function Complete-UnsignedIosIpaViaActions");
		Assertions.assertThat(source).contains("function Get-UnsignedIosIpaViaActions");
	}

	@Test
	@DisplayName("Start-UnsignedIosIpaViaActions reuses successful or in-progress run for same commit")
	void startReusesExistingRunForSameCommit() throws IOException {
		String source = readSource("scripts/release/Fetch-UnsignedIosIpa.ps1");
		int reuseCheck = source.indexOf("headSha -eq $CommitSha");
		if (reuseCheck < 0) {
			reuseCheck = source.indexOf("$_.headSha -eq $CommitSha");
		}
		int dispatch = source.indexOf("gh workflow run $workflow");

		Assertions.assertThat(reuseCheck).isGreaterThanOrEqualTo(0);
		Assertions.assertThat(dispatch).isGreaterThan(reuseCheck);
		Assertions.assertThat(source).contains("conclusion");
		Assertions.assertThat(source).contains("success");
	}

	@Test
	@DisplayName("release.ps1 starts ios-unsigned Actions before local tests and prepareRelease")
	void releaseStartsIosActionsBeforeLocalBuild() throws IOException {
		String source = readSource("scripts/release.ps1");
		int start = source.indexOf("Start-UnsignedIosIpaViaActions");
		int tests = source.indexOf("GradleArgs @('test')");
		int prepare = source.indexOf("'prepareRelease'");

		Assertions.assertThat(start).isGreaterThanOrEqualTo(0);
		Assertions.assertThat(tests).isGreaterThan(start);
		Assertions.assertThat(prepare).isGreaterThan(start);
	}

	@Test
	@DisplayName("release.ps1 completes ios-unsigned download after prepareRelease when IPA missing")
	void releaseCompletesIosDownloadAfterPrepareRelease() throws IOException {
		String source = readSource("scripts/release.ps1");
		int prepare = source.indexOf("'prepareRelease'");
		int complete = source.indexOf("Complete-UnsignedIosIpaViaActions");

		Assertions.assertThat(prepare).isGreaterThanOrEqualTo(0);
		Assertions.assertThat(complete).isGreaterThan(prepare);
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
