package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JavaEchoHttpTransportTest {

	@Test
	@DisplayName("HTTP connect and read timeouts keep prior defaults")
	void timeoutsKeepPriorDefaults() {
		Assertions.assertThat(JavaEchoHttpTransport.CONNECT_TIMEOUT_MS).isEqualTo(5000);
		Assertions.assertThat(JavaEchoHttpTransport.READ_TIMEOUT_MS).isEqualTo(8000);
	}
}
