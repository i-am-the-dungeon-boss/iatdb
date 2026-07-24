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
 * Guards Sentry source-context upload wiring on each launcher module.
 */
class SentrySourceContextWiringTest {

	@Test
	@DisplayName("shared sentry gradle snippet enables source context with core sources")
	void sharedSnippetEnablesSourceContextWithCoreSources() throws IOException {
		String shared = readSource("gradle/sentry-source-context.gradle");

		Assertions.assertThat(shared).contains("includeSourceContext");
		Assertions.assertThat(shared).contains("SENTRY_AUTH_TOKEN");
		Assertions.assertThat(shared).doesNotContain("skipSentryUpload");
		Assertions.assertThat(shared).contains(".env");
		Assertions.assertThat(shared).contains("additionalSourceDirsForSourceContext");
		Assertions.assertThat(shared).contains("../core/src/main/java");
		Assertions.assertThat(shared).contains("../SPD-classes/src/main/java");
	}

	@Test
	@DisplayName("release tasks require Sentry auth token before upload with no skip option")
	void releaseTasksRequireSentryAuthTokenWithNoSkip() throws IOException {
		String source = readSource("build.gradle");

		Assertions.assertThat(source).contains("taskGraph.whenReady");
		Assertions.assertThat(source).contains("SENTRY_AUTH_TOKEN");
		Assertions.assertThat(source).contains("Release builds require SENTRY_AUTH_TOKEN");
		Assertions.assertThat(source).doesNotContain("skipSentryUpload");
	}

	@Test
	@DisplayName("release.ps1 loads .env and requires Sentry auth token with no skip option")
	void releaseScriptRequiresSentryAuthToken() throws IOException {
		String source = readSource("scripts/release.ps1");

		Assertions.assertThat(source).contains("SENTRY_AUTH_TOKEN");
		Assertions.assertThat(source).contains("Import-DotEnv");
		Assertions.assertThat(source).doesNotContain("SkipSentryUpload");
	}

	@Test
	@DisplayName("release.ps1 runs unit tests before loading .env")
	void releaseScriptTestsBeforeDotEnv() throws IOException {
		String source = readSource("scripts/release.ps1");
		int testGate = source.indexOf("GradleArgs @('test')");
		int dotenv = source.indexOf("Import-DotEnv (Join-Path $root '.env')");

		Assertions.assertThat(testGate).isGreaterThanOrEqualTo(0);
		Assertions.assertThat(dotenv).isGreaterThan(testGate);
	}

	@Test
	@DisplayName("android build applies shared source context snippet")
	void androidBuildAppliesSharedSourceContext() throws IOException {
		String source = readSource("android/build.gradle");

		Assertions.assertThat(source).contains("sentry-source-context.gradle");
		Assertions.assertThat(source).contains("autoUploadProguardMapping");
	}

	@Test
	@DisplayName("desktop build enables Sentry source context upload for java project")
	void desktopBuildEnablesSourceContext() throws IOException {
		String source = readSource("desktop/build.gradle");

		Assertions.assertThat(source).contains("apply plugin: 'io.sentry.jvm.gradle'");
		Assertions.assertThat(source).contains("sentry-source-context.gradle");
		Assertions.assertThat(source).contains("sentryProjectName = 'java'");
		Assertions.assertThat(source).contains("sentryUploadSourceBundleJava");
	}

	@Test
	@DisplayName("ios build enables Sentry source context upload for ios project")
	void iosBuildEnablesSourceContext() throws IOException {
		String source = readSource("ios/build.gradle");

		Assertions.assertThat(source).contains("apply plugin: 'io.sentry.jvm.gradle'");
		Assertions.assertThat(source).contains("sentry-source-context.gradle");
		Assertions.assertThat(source).contains("sentryProjectName = 'ios'");
		Assertions.assertThat(source).contains("sentryUploadSourceBundleJava");
	}

	@Test
	@DisplayName("ios CI requires SENTRY_AUTH_TOKEN for source context upload")
	void iosCiRequiresSentryAuthToken() throws IOException {
		String source = readSource(".github/workflows/ios-unsigned.yml");

		Assertions.assertThat(source).contains("SENTRY_AUTH_TOKEN");
		Assertions.assertThat(source).contains("Require Sentry auth token");
	}

	@Test
	@DisplayName("release.ps1 always uploads unsigned iOS IPA with GitHub Release assets")
	void releaseScriptAlwaysUploadsIosIpa() throws IOException {
		String source = readSource("scripts/release.ps1");

		Assertions.assertThat(source).contains("Missing unsigned iOS IPA");
		Assertions.assertThat(source).contains("$assets.Add($iosIpa.FullName)");
		Assertions.assertThat(source).doesNotContain("if ($iosIpa)");
	}

	@Test
	@DisplayName("release.ps1 resolves fetched IPA from dist path not command output")
	void releaseScriptResolvesFetchedIpaFromDistPath() throws IOException {
		String source = readSource("scripts/release.ps1");
		String common = readSource("scripts/release/_common.ps1");

		Assertions.assertThat(source).contains("Complete-UnsignedIosIpaViaActions");
		Assertions.assertThat(source).contains("Get-UnsignedIosIpaViaActions");
		Assertions.assertThat(source).doesNotContain("Get-Item -LiteralPath $ipaPath");
		Assertions.assertThat(common).contains("Out-Host");
	}

	@Test
	@DisplayName("ios CI is manual workflow_dispatch only")
	void iosCiIsManualWorkflowDispatchOnly() throws IOException {
		String source = readSource(".github/workflows/ios-unsigned.yml");

		Assertions.assertThat(source).contains("workflow_dispatch:");
		Assertions.assertThat(source).doesNotContain("tags:");
		Assertions.assertThat(source).doesNotContain("gh release upload");
	}

	private static String readSource(String relativePath) throws IOException {
		Path file = findRepoFile(relativePath);
		return Files.readString(file, StandardCharsets.UTF_8);
	}

	private static Path findRepoFile(String relativePath) {
		Path dir = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			// Prefer the Gradle root (settings.gradle) so "build.gradle" is not
			// core/build.gradle.
			Path candidate = dir.resolve(relativePath);
			if (Files.isRegularFile(dir.resolve("settings.gradle")) && Files.isRegularFile(candidate)) {
				return candidate;
			}
			dir = dir.getParent();
		}
		throw new AssertionError("Could not find " + relativePath + " from " + Paths.get("").toAbsolutePath());
	}
}
