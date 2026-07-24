/*
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
 */

package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.watabou.noosa.Game;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class SentryCrashReportingTest {

	@AfterEach
	void resetReporter() {
		SentryCrashReporting.resetReporter();
	}

	@Test
	@DisplayName("report forwards throwable to the installed reporter")
	void reportForwardsThrowable() {
		List<Throwable> captured = new ArrayList<>();
		SentryCrashReporting.setReporter(captured::add);

		RuntimeException boom = new RuntimeException("boom");
		SentryCrashReporting.report(boom);

		Assertions.assertThat(captured).containsExactly(boom);
	}

	@Test
	@DisplayName("report ignores null throwable")
	void reportIgnoresNull() {
		List<Throwable> captured = new ArrayList<>();
		SentryCrashReporting.setReporter(captured::add);

		SentryCrashReporting.report(null);

		Assertions.assertThat(captured).isEmpty();
	}

	@Test
	@DisplayName("report does not send to Sentry on INDEV builds")
	void reportSkippedOnIndevBuilds() {
		String previous = Game.version;
		Game.version = "1.0.0-INDEV";
		try {
			List<Throwable> captured = new ArrayList<>();
			SentryCrashReporting.setReporter(captured::add);

			SentryCrashReporting.report(new RuntimeException("dev boom"));

			Assertions.assertThat(captured).isEmpty();
		} finally {
			Game.version = previous;
		}
	}

	@Test
	@DisplayName("report still sends on release builds")
	void reportSendsOnReleaseBuilds() {
		String previous = Game.version;
		Game.version = "1.0.0";
		try {
			List<Throwable> captured = new ArrayList<>();
			SentryCrashReporting.setReporter(captured::add);
			RuntimeException boom = new RuntimeException("prod boom");

			SentryCrashReporting.report(boom);

			Assertions.assertThat(captured).containsExactly(boom);
		} finally {
			Game.version = previous;
		}
	}

	@Test
	@DisplayName("init is skipped on INDEV builds")
	void initSkippedOnIndevBuilds() {
		Assertions.assertThat(SentryCrashReporting.shouldInit("0.0.3-INDEV", "https://a@b/1"))
				.isFalse();
	}

	@Test
	@DisplayName("init is skipped when DSN is blank")
	void initSkippedWhenDsnBlank() {
		Assertions.assertThat(SentryCrashReporting.shouldInit("0.0.3", "")).isFalse();
		Assertions.assertThat(SentryCrashReporting.shouldInit("0.0.3", "   ")).isFalse();
		Assertions.assertThat(SentryCrashReporting.shouldInit("0.0.3", null)).isFalse();
	}

	@Test
	@DisplayName("init is allowed for release builds with a DSN")
	void initAllowedForReleaseWithDsn() {
		Assertions.assertThat(SentryCrashReporting.shouldInit("0.0.3", "https://a@b/1")).isTrue();
	}

	@Test
	@DisplayName("reads DSN from classpath sentry.properties")
	void readsDsnFromClasspathSentryProperties() {
		Assertions.assertThat(SentryCrashReporting.readClasspathDsn())
				.isEqualTo("https://publickey@o000.ingest.sentry.io/000");
	}

	@Test
	@DisplayName("ios sentry.properties points at the ios Sentry project")
	void iosSentryPropertiesPointsAtIosProject() throws IOException {
		String ios = Files.readString(findRepoFile("ios/src/main/resources/sentry.properties"),
				StandardCharsets.UTF_8);
		String desktop = Files.readString(findRepoFile("desktop/src/main/resources/sentry.properties"),
				StandardCharsets.UTF_8);

		Assertions.assertThat(extractDsn(ios)).contains("/4511787603132496");
		Assertions.assertThat(extractDsn(ios)).isNotEqualTo(extractDsn(desktop));
	}

	@Test
	@DisplayName("desktop sentry.properties points at the java Sentry project")
	void desktopSentryPropertiesPointsAtJavaProject() throws IOException {
		String desktop = Files.readString(findRepoFile("desktop/src/main/resources/sentry.properties"),
				StandardCharsets.UTF_8);

		Assertions.assertThat(extractDsn(desktop)).contains("/4511778269691984");
	}

	private static String extractDsn(String properties) {
		for (String line : properties.split("\n")) {
			String trimmed = line.trim();
			if (trimmed.startsWith("dsn=")) {
				return trimmed.substring("dsn=".length()).trim();
			}
		}
		throw new AssertionError("missing dsn= in sentry.properties");
	}

	private static Path findRepoFile(String relativePath) {
		Path dir = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			Path candidate = dir.resolve(relativePath);
			if (Files.isRegularFile(candidate)) {
				return candidate;
			}
			dir = dir.getParent();
		}
		throw new AssertionError("Could not find " + relativePath + " from " + Paths.get("").toAbsolutePath());
	}
}
