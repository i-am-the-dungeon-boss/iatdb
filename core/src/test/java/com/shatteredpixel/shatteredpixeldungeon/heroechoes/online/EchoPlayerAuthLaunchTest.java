package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoPlayerAuthLaunchTest {

	@AfterEach
	void cleanup() {
		EchoPlayerSession.resetForTests();
		EchoOnlineSettings.resetForTests();
	}

	@Test
	@DisplayName("launch validation keeps session when /me succeeds")
	void launchValidationKeepsValidSession() {
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		EchoPlayerSession.applyAuthResponse("old-jwt", "Hero", false, null);

		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(200, "{\"username\":\"Hero\",\"has_credentials\":false}");
		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		Assertions.assertThat(EchoPlayerAuth.validateOrRefreshOnLaunch(client)).isTrue();
		Assertions.assertThat(EchoPlayerSession.jwt()).isEqualTo("old-jwt");
		Assertions.assertThat(transport.requests.get(0).url).endsWith("/v1/auth/me");
	}

	@Test
	@DisplayName("launch validation refreshes JWT via device when token expired but player remains")
	void launchValidationRefreshesWhenTokenExpired() {
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		EchoPlayerSession.applyAuthResponse("expired-jwt", "Hero", false, null);

		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(401, "{\"detail\":\"Unauthorized\"}");
		transport.enqueue(200, "{"
				+ "\"token\":\"fresh-jwt\","
				+ "\"exp\":1,"
				+ "\"username\":\"Hero\","
				+ "\"has_credentials\":false"
				+ "}");
		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		Assertions.assertThat(EchoPlayerAuth.validateOrRefreshOnLaunch(client)).isTrue();
		Assertions.assertThat(EchoPlayerSession.jwt()).isEqualTo("fresh-jwt");
		Assertions.assertThat(transport.requests.get(1).url).endsWith("/v1/auth/device");
		Assertions.assertThat(transport.requests.get(1).body).doesNotContain("username");
	}

	@Test
	@DisplayName("launch validation clears session when player is gone without recreating")
	void launchValidationClearsWhenPlayerGone() {
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		EchoOnlineSettings.setApiKey("secret");
		EchoPlayerSession.applyAuthResponse("stale-jwt", "Hero", false, null);

		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(401, "{\"detail\":\"Unauthorized\"}");
		transport.enqueue(404, "{\"detail\":\"player_gone\"}");
		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		Assertions.assertThat(EchoPlayerAuth.validateOrRefreshOnLaunch(client)).isFalse();
		Assertions.assertThat(EchoPlayerSession.hasSession()).isFalse();
		Assertions.assertThat(transport.requests).hasSize(2);
		Assertions.assertThat(transport.requests.get(1).body).doesNotContain("username");
	}
}
