package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class EchoOnlineSettingsEnvTest {

	@AfterEach
	void cleanup() {
		EchoOnlineSettings.resetForTests();
	}

	@Test
	@DisplayName("does not use java.util.function (RoboVM-safe env loading)")
	void doesNotUseJavaUtilFunction() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoOnlineSettings.java");

		Assertions.assertThat(source).doesNotContain("import java.util.function");
		Assertions.assertThat(source).doesNotContain("Function<");
		Assertions.assertThat(source).doesNotContain("System::getenv");
		Assertions.assertThat(source).contains("setEnvForTests(Map");
		Assertions.assertThat(source).contains("new FileInputStream(file)");
	}

	private static String readSource(String relativePath) throws IOException {
		Path dir = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			Path candidate = dir.resolve(relativePath);
			if (Files.isRegularFile(candidate)) {
				return Files.readString(candidate, StandardCharsets.UTF_8);
			}
			dir = dir.getParent();
		}
		throw new AssertionError("Could not find " + relativePath + " from " + Paths.get("").toAbsolutePath());
	}

	@Test
	@DisplayName("reads backend URL from ECHO_BACKEND_URL")
	void readsBackendUrlFromEnv() {
		Map<String, String> env = new HashMap<>();
		env.put(EchoOnlineSettings.BACKEND_URL, " http://localhost:3000 ");
		EchoOnlineSettings.setEnvForTests(env);

		Assertions.assertThat(EchoOnlineSettings.backendUrl()).isEqualTo("http://localhost:3000");
	}

	@Test
	@DisplayName("reads API key from ECHO_API_KEY")
	void readsApiKeyFromEnv() {
		Map<String, String> env = new HashMap<>();
		env.put(EchoOnlineSettings.API_KEY, "secret-key");
		EchoOnlineSettings.setEnvForTests(env);

		Assertions.assertThat(EchoOnlineSettings.apiKey()).isEqualTo("secret-key");
	}

	@Test
	@DisplayName("loads values from a dotenv file")
	void loadsValuesFromDotEnvFile() throws Exception {
		EchoOnlineSettings.setEnvForTests(Collections.emptyMap());
		Path envFile = Files.createTempFile("echo-online", ".env");
		Files.writeString(
				envFile,
				"ECHO_BACKEND_URL=http://localhost:3000\nECHO_API_KEY=secret\n",
				StandardCharsets.UTF_8);

		EchoOnlineSettings.loadDotEnv(envFile.toFile());

		Assertions.assertThat(EchoOnlineSettings.backendUrl()).isEqualTo("http://localhost:3000");
		Assertions.assertThat(EchoOnlineSettings.apiKey()).isEqualTo("secret");
	}

	@Test
	@DisplayName("returns empty values when env vars are unset")
	void returnsEmptyWhenUnset() {
		EchoOnlineSettings.setEnvForTests(Collections.emptyMap());

		Assertions.assertThat(EchoOnlineSettings.backendUrl()).isEmpty();
		Assertions.assertThat(EchoOnlineSettings.apiKey()).isEmpty();
	}

	@Test
	@DisplayName("apiKey only reads ECHO_API_KEY at runtime")
	void apiKeyOnlyReadsEchoApiKey() {
		Map<String, String> env = new HashMap<>();
		env.put("SOME_OTHER_KEY", "not-the-api-key");
		EchoOnlineSettings.setEnvForTests(env);

		Assertions.assertThat(EchoOnlineSettings.apiKey()).isEmpty();
	}

	@Test
	@DisplayName("uses build defaults when env and dotenv are unset")
	void usesBuildDefaultsWhenUnset() {
		EchoOnlineSettings.setEnvForTests(Collections.emptyMap());
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
		EchoOnlineSettings.setEnvForTests(env);
		EchoOnlineSettings.setBuildDefaults("https://echo.example.com", "build-key");

		Assertions.assertThat(EchoOnlineSettings.backendUrl()).isEqualTo("http://localhost:3000");
		Assertions.assertThat(EchoOnlineSettings.apiKey()).isEqualTo("env-key");
	}

	@Test
	@DisplayName("dotenv values take precedence over build defaults")
	void dotenvTakesPrecedenceOverBuildDefaults() throws Exception {
		EchoOnlineSettings.setEnvForTests(Collections.emptyMap());
		Path envFile = Files.createTempFile("echo-online", ".env");
		Files.writeString(
				envFile,
				"ECHO_BACKEND_URL=http://dotenv.local:3000\nECHO_API_KEY=dotenv-key\n",
				StandardCharsets.UTF_8);
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
