package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Launchers live outside {@code :core}; this guards the dotenv wiring contract
 * by asserting the call sites remain in source.
 */
class EchoOnlineSettingsLauncherWiringTest {

	@Test
	@DisplayName("desktop launcher loads EchoOnlineSettings dotenv on startup")
	void desktopLauncherLoadsDotEnv() throws IOException {
		String source = readSource(
				"desktop/src/main/java/com/shatteredpixel/shatteredpixeldungeon/desktop/DesktopLauncher.java");

		Assertions.assertThat(source).contains("EchoOnlineSettings.loadDefaultDotEnv()");
	}

	@Test
	@DisplayName("android launcher loads EchoOnlineSettings dotenv on startup")
	void androidLauncherLoadsDotEnv() throws IOException {
		String source = readSource(
				"android/src/main/java/com/shatteredpixel/shatteredpixeldungeon/android/AndroidLauncher.java");

		Assertions.assertThat(source).contains("EchoOnlineSettings.loadDefaultDotEnv()");
	}

	private static String readSource(String relativePath) throws IOException {
		Path file = findRepoFile(relativePath);
		return Files.readString(file, StandardCharsets.UTF_8);
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
