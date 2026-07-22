package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoPlayerAuthTest {

	@AfterEach
	void cleanup() {
		EchoPlayerSession.resetForTests();
		EchoOnlineSettings.resetForTests();
	}

	@Test
	@DisplayName("missing session triggers auth bootstrap before upload path")
	void missingSessionTriggersAuthBootstrap() {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(201, "{"
				+ "\"token\":\"boot-jwt\","
				+ "\"exp\":1,"
				+ "\"username\":\"Booted\","
				+ "\"has_credentials\":false"
				+ "}");
		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		Assertions.assertThat(EchoPlayerSession.hasSession()).isFalse();
		EchoPlayerAuth.SessionResult result = EchoPlayerAuth.ensureSession(client, "Booted");

		Assertions.assertThat(result).isEqualTo(EchoPlayerAuth.SessionResult.OK);
		Assertions.assertThat(EchoPlayerSession.hasSession()).isTrue();
		Assertions.assertThat(transport.requests).hasSize(1);
		Assertions.assertThat(transport.requests.get(0).url).contains("/v1/auth/device");
	}

	@Test
	@DisplayName("register reports username taken instead of generic failure")
	void registerReportsUsernameTaken() {
		EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
		transport.enqueue(422,
				"{\"detail\":{\"username\":[\"That username is already taken. Please choose another.\"]}}");
		EchoClient client = new EchoClient("https://echo.test", "secret", transport);

		EchoPlayerAuth.SessionResult result = EchoPlayerAuth.ensureSession(client, "TakenName");

		Assertions.assertThat(result).isEqualTo(EchoPlayerAuth.SessionResult.USERNAME_TAKEN);
		Assertions.assertThat(EchoPlayerSession.hasSession()).isFalse();
	}
}
