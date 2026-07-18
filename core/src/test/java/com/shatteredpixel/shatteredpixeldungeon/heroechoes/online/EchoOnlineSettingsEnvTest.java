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
}
