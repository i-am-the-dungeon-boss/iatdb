package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The socket endpoint override, which only matters for local development but
 * has to reach every platform the game is developed on.
 *
 * <p>Desktop resolves it from the project {@code .env} at runtime. Android
 * cannot: there is no project directory on the device, so the value must be
 * baked into {@code BuildConfig} at build time, exactly as the backend URL and
 * API key already are.
 */
@DisplayName("World socket URL configuration")
class EchoWorldSocketUrlTest {

	@AfterEach
	void reset() {
		EchoOnlineSettings.resetForTests();
	}

	@Test
	@DisplayName("is empty when nothing configures it, meaning the backend origin is used")
	void defaultsToEmpty() {
		EchoOnlineSettings.setEnvForTests(java.util.Collections.emptyMap());

		assertThat(EchoOnlineSettings.worldSocketUrl()).isEmpty();
	}

	@Test
	@DisplayName("falls back to the build-baked value, which is all Android has")
	void fallsBackToTheBuildDefault() {
		EchoOnlineSettings.setEnvForTests(java.util.Collections.emptyMap());
		EchoOnlineSettings.setBuildDefaults("http://10.0.2.2:3000", "key", "ws://10.0.2.2:3001");

		assertThat(EchoOnlineSettings.worldSocketUrl()).isEqualTo("ws://10.0.2.2:3001");
	}

	@Test
	@DisplayName("prefers an env or dotenv value over the build-baked one")
	void envWinsOverTheBuildDefault() {
		EchoOnlineSettings.setBuildDefaults("http://10.0.2.2:3000", "key", "ws://10.0.2.2:3001");
		EchoOnlineSettings.setEnvForTests(
				java.util.Collections.singletonMap("ECHO_WORLD_SOCKET_URL", "ws://elsewhere:9999"));

		assertThat(EchoOnlineSettings.worldSocketUrl()).isEqualTo("ws://elsewhere:9999");
	}

	@Test
	@DisplayName("rewrites a ws loopback host for the Android emulator")
	void rewritesWsLoopbackForTheEmulator() {
		// The emulator's own localhost is the device, not the development
		// machine; without this the socket dials into the sandbox and hangs.
		assertThat(EchoOnlineSettings.forAndroidEmulatorLoopback("ws://localhost:3001"))
				.isEqualTo("ws://10.0.2.2:3001");
		assertThat(EchoOnlineSettings.forAndroidEmulatorLoopback("ws://127.0.0.1:3001"))
				.isEqualTo("ws://10.0.2.2:3001");
	}

	@Test
	@DisplayName("android build.gradle bakes the socket URL into BuildConfig")
	void androidBuildGradleBakesTheSocketUrl() throws IOException {
		String source = readSource("android/build.gradle");

		Assertions.assertThat(source).contains("ECHO_WORLD_SOCKET_URL");
		Assertions.assertThat(source).contains("echoWorldSocketUrl");
	}

	@Test
	@DisplayName("echo-env.gradle resolves the socket URL from .env")
	void echoEnvResolvesTheSocketUrl() throws IOException {
		String source = readSource("gradle/echo-env.gradle");

		Assertions.assertThat(source).contains("ECHO_WORLD_SOCKET_URL");
	}

	@Test
	@DisplayName("android launcher passes the socket URL through emulator loopback rewriting")
	void androidLauncherRewritesTheSocketUrl() throws IOException {
		String source = readSource(
				"android/src/main/java/com/shatteredpixel/shatteredpixeldungeon/android/AndroidLauncher.java");

		Assertions.assertThat(source).contains("BuildConfig.ECHO_WORLD_SOCKET_URL");
		// Must go through the same rewrite as the backend URL, or a debug build
		// on the emulator gets a socket URL pointing at the device itself.
		Assertions.assertThat(source).contains("forAndroidEmulatorLoopback(echoWorldSocketUrl)");
	}

	@Test
	@DisplayName("ios launcher passes a socket override through, even though it is empty")
	void iosLauncherPassesTheSocketOverride() throws IOException {
		// iOS ships no build-baked override and, being a packaged app, reaches no
		// project .env on a device — so the empty string is deliberate: it makes
		// worldSocketUrl() fall through to deriving wss:// from the backend URL.
		// What matters is that the launcher calls the three-argument form at all;
		// the two-argument one would leave the socket wired to nothing.
		String source = readSource(
				"ios/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ios/IOSLauncher.java");
		String call = source.substring(
				source.indexOf("EchoOnlineSettings.setBuildDefaults("),
				source.indexOf(");", source.indexOf("EchoOnlineSettings.setBuildDefaults(")));

		Assertions.assertThat(call).contains("EchoOnlineSettings.PRODUCTION_BACKEND_URL");
		Assertions.assertThat(call).contains("EchoBuildConfig.ECHO_API_KEY");

		// count arguments on the code alone: the comment explaining the empty
		// override contains commas of its own
		StringBuilder code = new StringBuilder();
		for (String line : call.split("\n")) {
			String trimmed = line.trim();
			if (!trimmed.startsWith("//")) {
				code.append(trimmed);
			}
		}
		Assertions.assertThat(code.toString().split(",", -1)).hasSize(3);
	}

	private static String readSource(String relativePath) throws IOException {
		// The test working directory is the module, not the repo root, so walk up
		// until the file appears — the same approach the launcher wiring test uses.
		Path dir = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			Path candidate = dir.resolve(relativePath);
			if (Files.isRegularFile(candidate)) {
				return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
			}
			dir = dir.getParent();
		}
		throw new IOException("Not found from " + Paths.get("").toAbsolutePath() + ": " + relativePath);
	}
}
