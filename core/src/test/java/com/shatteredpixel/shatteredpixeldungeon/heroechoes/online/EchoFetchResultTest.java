package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoFetchResultTest {

	@Test
	@DisplayName("holds echo and policy pair")
	void holdsEchoAndPolicy() {
		EchoFetchResult result = new EchoFetchResult(
				EchoTestSupport.warriorEchoWithData(5),
				EchoPolicy.fallback());

		Assertions.assertThat(result.echo.hasCombatData()).isTrue();
		Assertions.assertThat(result.policy.isSupported()).isTrue();
	}
}
