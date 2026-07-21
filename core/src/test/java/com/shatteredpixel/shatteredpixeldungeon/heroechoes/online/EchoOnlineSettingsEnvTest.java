package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class EchoOnlineSettingsEnvTest {

	@AfterEach
	void cleanup() {
		EchoOnlineSettings.resetForTests();
	}

	@Test
	@DisplayName("reads backend URL from ECHO_BACKEND_URL")
	void readsBackendUrlFromEnv() {
		Map<String, String> env = new HashMap<>();
		env.put(EchoOnlineSettings.BACKEND_URL, " http://localhost:3000 ");
		EchoOnlineSettings.setEnvForTests(env::get);

		Assertions.assertThat(EchoOnlineSettings.backendUrl()).isEqualTo("http://localhost:3000");
	}

	@Test
	@DisplayName("reads API key from ECHO_API_KEY")
	void readsApiKeyFromEnv() {
		Map<String, String> env = new HashMap<>();
		env.put(EchoOnlineSettings.API_KEY, "secret-key");
		EchoOnlineSettings.setEnvForTests(env::get);

		Assertions.assertThat(EchoOnlineSettings.apiKey()).isEqualTo("secret-key");
	}

	@Test
	@DisplayName("loads values from a dotenv file")
	void loadsValuesFromDotEnvFile() throws Exception {
		EchoOnlineSettings.setEnvForTests(key -> null);
		java.nio.file.Path envFile = java.nio.file.Files.createTempFile("echo-online", ".env");
		java.nio.file.Files.writeString(
				envFile,
				"ECHO_BACKEND_URL=http://localhost:3000\nECHO_API_KEY=secret\n");

		EchoOnlineSettings.loadDotEnv(envFile.toFile());

		Assertions.assertThat(EchoOnlineSettings.backendUrl()).isEqualTo("http://localhost:3000");
		Assertions.assertThat(EchoOnlineSettings.apiKey()).isEqualTo("secret");
	}

	@Test
	@DisplayName("returns empty values when env vars are unset")
	void returnsEmptyWhenUnset() {
		EchoOnlineSettings.setEnvForTests(key -> null);

		Assertions.assertThat(EchoOnlineSettings.backendUrl()).isEmpty();
		Assertions.assertThat(EchoOnlineSettings.apiKey()).isEmpty();
	}

	@Test
	@DisplayName("apiKey ignores ECHO_API_KEY_RELEASE (Android assemble only)")
	void apiKeyIgnoresReleaseEnvKey() {
		Map<String, String> env = new HashMap<>();
		env.put("ECHO_API_KEY_RELEASE", "release-only-key");
		EchoOnlineSettings.setEnvForTests(env::get);

		Assertions.assertThat(EchoOnlineSettings.apiKey()).isEmpty();
	}

	@Test
	@DisplayName("uses build defaults when env and dotenv are unset")
	void usesBuildDefaultsWhenUnset() {
		EchoOnlineSettings.setEnvForTests(key -> null);
		EchoOnlineSettings.setBuildDefaults("https://echo.example.com", "build-key");

		Assertions.assertThat(EchoOnlineSettings.backendUrl()).isEqualTo("https://echo.example.com");
		Assertions.assertThat(EchoOnlineSettings.apiKey()).isEqualTo("build-key");
	}

	@Test
	@DisplayName("env values take precedence over build defaults")
	void envTakesPrecedenceOverBuildDefaults() {
		Map<String, String> env = new HashMap<>();
		env.put(EchoOnlineSettings.BACKEND_URL, "http://localhost:3000");
		env.put(EchoOnlineSettings.API_KEY, "env-key");
		EchoOnlineSettings.setEnvForTests(env::get);
		EchoOnlineSettings.setBuildDefaults("https://echo.example.com", "build-key");

		Assertions.assertThat(EchoOnlineSettings.backendUrl()).isEqualTo("http://localhost:3000");
		Assertions.assertThat(EchoOnlineSettings.apiKey()).isEqualTo("env-key");
	}

	@Test
	@DisplayName("dotenv values take precedence over build defaults")
	void dotenvTakesPrecedenceOverBuildDefaults() throws Exception {
		EchoOnlineSettings.setEnvForTests(key -> null);
		java.nio.file.Path envFile = java.nio.file.Files.createTempFile("echo-online", ".env");
		java.nio.file.Files.writeString(
				envFile,
				"ECHO_BACKEND_URL=http://dotenv.local:3000\nECHO_API_KEY=dotenv-key\n");
		EchoOnlineSettings.loadDotEnv(envFile.toFile());
		EchoOnlineSettings.setBuildDefaults("https://echo.example.com", "build-key");

		Assertions.assertThat(EchoOnlineSettings.backendUrl()).isEqualTo("http://dotenv.local:3000");
		Assertions.assertThat(EchoOnlineSettings.apiKey()).isEqualTo("dotenv-key");
	}

	@Test
	@DisplayName("rewrites localhost to Android emulator loopback host")
	void rewritesLocalhostToAndroidEmulatorLoopback() {
		Assertions.assertThat(EchoOnlineSettings.forAndroidEmulatorLoopback("http://localhost:3000"))
				.isEqualTo("http://10.0.2.2:3000");
		Assertions.assertThat(EchoOnlineSettings.forAndroidEmulatorLoopback("http://127.0.0.1:3000"))
				.isEqualTo("http://10.0.2.2:3000");
		Assertions.assertThat(EchoOnlineSettings.forAndroidEmulatorLoopback("https://echo.example.com"))
				.isEqualTo("https://echo.example.com");
	}
}
