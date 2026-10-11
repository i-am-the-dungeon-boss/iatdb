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
	@DisplayName("release.ps1 does not start or download the paused iOS IPA")
	void releaseDoesNotFetchPausedIosIpa() throws IOException {
		String raw = readSource("scripts/release/paused.ps1");
		String live = liveSource(readSource("scripts/release.ps1"));

		Assertions.assertThat(raw).contains("Start-UnsignedIosIpaViaActions");
		Assertions.assertThat(raw).contains("ios-unsigned.ipa");
		Assertions.assertThat(live).doesNotContain("Start-UnsignedIosIpaViaActions");
		Assertions.assertThat(live).doesNotContain("Complete-UnsignedIosIpaViaActions");
		Assertions.assertThat(live).doesNotContain("Get-UnsignedIosIpaViaActions");
		Assertions.assertThat(live).doesNotContain("ios-unsigned.ipa");
	}

	private static String liveSource(String source) {
		StringBuilder out = new StringBuilder();
		boolean block = false;
		for (String line : source.split("\n", -1)) {
			String trimmed = line.trim();
			if (block) {
				if (trimmed.contains("#>")) {
					block = false;
				}
				continue;
			}
			if (trimmed.startsWith("<#")) {
				if (!trimmed.contains("#>")) {
					block = true;
				}
				continue;
			}
			if (trimmed.startsWith("#") || trimmed.startsWith("//")) {
				continue;
			}
			out.append(line).append('\n');
		}
		return out.toString();
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
