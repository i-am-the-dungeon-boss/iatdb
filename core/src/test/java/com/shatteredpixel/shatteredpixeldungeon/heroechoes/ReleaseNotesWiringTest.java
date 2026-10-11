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
 * Guards generated GitHub Release notes content.
 */
class ReleaseNotesWiringTest {

	@Test
	@DisplayName("release notes link Google Play and Steam instead of sideload artifacts")
	void releaseNotesLinkStores() throws IOException {
		String raw = readSource("scripts/release/New-ReleaseNotes.ps1");
		String source = liveSource(raw);

		Assertions.assertThat(source).contains(
				"https://play.google.com/store/apps/details?id=com.dungeonboss.iatdb");
		Assertions.assertThat(source).contains("https://store.steampowered.com/app/5020980");
		Assertions.assertThat(source).doesNotContain("Sideloadly");
		Assertions.assertThat(source).doesNotContain("unsigned IPA");
		Assertions.assertThat(raw).contains("Sideloadly");
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
