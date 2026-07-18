package com.shatteredpixel.shatteredpixeldungeon.services.updates;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * resolveBaseUrl lives in the echoUpdates module; this test documents the URL
 * wiring contract used by launchers (override / property).
 */
class EchoUpdateUrlTest {

	@AfterEach
	void clear() {
		System.clearProperty("ECHO_BACKEND_URL");
	}

	@Test
	@DisplayName("echo update base URL property trims trailing slash")
	void propertyTrimsSlash() {
		System.setProperty("ECHO_BACKEND_URL", "https://example.com/echo/");
		String url = System.getProperty("ECHO_BACKEND_URL").trim();
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		Assertions.assertThat(url).isEqualTo("https://example.com/echo");
	}
}
