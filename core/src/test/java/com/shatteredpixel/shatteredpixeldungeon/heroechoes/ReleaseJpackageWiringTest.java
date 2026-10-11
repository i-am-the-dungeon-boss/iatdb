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
 * Guards the store release list: Play AAB (full game) and Steam demo zips.
 */
class ReleaseJpackageWiringTest {

	@Test
	@DisplayName("Play AAB prepareRelease omits -Pdemo; Steam package passes -Pdemo")
	void playAabOmitsDemoAndSteamPassesDemo() throws IOException {
		String play = liveSource(readSource("scripts/release/Build-PlayAab.ps1"));
		String steam = liveSource(readSource("scripts/release/Build-SteamDemo.ps1"));
		String orchestrator = readSource("scripts/release.ps1");

		Assertions.assertThat(play).contains("@('prepareRelease')");
		Assertions.assertThat(play).doesNotContain("-Pdemo");
		Assertions.assertThat(play).doesNotContain("-PwithJpackage");
		Assertions.assertThat(steam).contains("@('packageSteamDemo', '-Pdemo')");
		Assertions.assertThat(readSource("scripts/release/paused.ps1")).contains("-PwithJpackage");
		Assertions.assertThat(orchestrator).contains("paused.ps1");
		Assertions.assertThat(liveSource(orchestrator)).doesNotContain("-PwithJpackage");
	}

	@Test
	@DisplayName("prepareRelease copies a Play AAB and refuses -Pdemo")
	void prepareReleaseCopiesPlayAab() throws IOException {
		String source = readSource("build.gradle");
		int task = source.indexOf("tasks.register('prepareRelease')");
		int steam = source.indexOf("tasks.register('packageSteamDemo')");

		Assertions.assertThat(task).isGreaterThanOrEqualTo(0);
		Assertions.assertThat(steam).isGreaterThan(task);
		String body = liveSource(source.substring(task, steam));
		Assertions.assertThat(body).contains(":android:bundleRelease");
		Assertions.assertThat(body).contains("-android.aab");
		Assertions.assertThat(body).contains("must not be run with -Pdemo");
		Assertions.assertThat(body).doesNotContain("assembleRelease");
		Assertions.assertThat(body).doesNotContain(".apk");
		Assertions.assertThat(body).doesNotContain("createIPA");
		Assertions.assertThat(body).doesNotContain("jpackage");
		Assertions.assertThat(source).contains("assembleRelease");
		Assertions.assertThat(source).contains("createIPA");
	}

	@Test
	@DisplayName("packageSteamDemo requires -Pdemo and zips Windows or Linux")
	void packageSteamDemoRequiresDemoFlag() throws IOException {
		String source = readSource("build.gradle");
		int steam = source.indexOf("tasks.register('packageSteamDemo')");

		Assertions.assertThat(steam).isGreaterThanOrEqualTo(0);
		String body = source.substring(steam);
		Assertions.assertThat(body).contains("requires -Pdemo");
		Assertions.assertThat(body).contains("steam-demo-");
		Assertions.assertThat(body).contains(":desktop:jpackageImage");
	}

	@Test
	@DisplayName("release.ps1 starts the other OS Steam demo build before local prepareRelease")
	void releaseStartsSteamDemoBeforeLocalBuild() throws IOException {
		String source = liveSource(readSource("scripts/release.ps1"));
		int start = source.indexOf("Start-SteamDemoBuilds");
		int tests = source.indexOf("GradleArgs @('test')");
		int play = source.indexOf("Build-PlayAab -Gradlew");
		int steam = source.indexOf("Build-SteamDemoLocal -Gradlew");
		int complete = source.indexOf("Complete-SteamDemoBuilds");

		Assertions.assertThat(start).isGreaterThanOrEqualTo(0);
		Assertions.assertThat(tests).isGreaterThan(start);
		Assertions.assertThat(play).isGreaterThan(tests);
		Assertions.assertThat(steam).isGreaterThan(play);
		Assertions.assertThat(complete).isGreaterThan(steam);
		Assertions.assertThat(readSource("scripts/release/Fetch-SteamDemo.ps1")).contains("steam-demo.yml");
	}

	@Test
	@DisplayName("release.ps1 creates the GitHub Release with notes only")
	void githubReleaseHasNoBinaryAssets() throws IOException {
		String raw = readSource("scripts/release/Publish-GitHubRelease.ps1");
		String source = liveSource(raw);

		Assertions.assertThat(source).contains("'release', 'create'");
		Assertions.assertThat(source).doesNotContain("gh release upload");
		Assertions.assertThat(source).doesNotContain("$ghArgs += @($assets)");
		Assertions.assertThat(source).doesNotContain("$assets.Add(");
		Assertions.assertThat(raw).contains("gh release upload");
		Assertions.assertThat(liveSource(readSource("scripts/release.ps1"))).contains("Publish-GitHubRelease");
	}

	@Test
	@DisplayName("release list is Play AAB plus the Windows Steam demo; Linux zip is paused")
	void releaseListMatchesStores() throws IOException {
		String doc = readSource("docs/release/prepare-release.md");
		String steamRaw = readSource("scripts/release/Build-SteamDemo.ps1");
		String steam = liveSource(steamRaw);

		Assertions.assertThat(doc).contains("iatdb-<version>-android.aab");
		Assertions.assertThat(doc).contains("iatdb-<version>-steam-demo-windows.zip");
		Assertions.assertThat(doc).contains("Linux/SteamOS zip is paused");
		Assertions.assertThat(steam).contains("@('windows')");
		Assertions.assertThat(steam).doesNotContain("@('windows', 'linux')");
		Assertions.assertThat(steamRaw).contains("@('windows', 'linux')");
	}

	/** Drops full-line # and // comments, and PowerShell <# #> blocks. */
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
