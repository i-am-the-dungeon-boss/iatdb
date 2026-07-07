package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

@ExtendWith(GdxTestExtension.class)
class EchoOnlineServiceTest {

	@AfterEach
	void cleanup() {
		EchoOnlineService.resetForTests();
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("echoLookup returns composite lookup wired to local storage")
	void echoLookupReturnsCompositeLookup() {
		EchoReplacementDecider.EchoLookup lookup = EchoOnlineService.echoLookup();

		Assertions.assertThat(lookup).isInstanceOf(CompositeEchoLookup.class);
	}

	@Test
	@DisplayName("resetForTests clears cached lookup singleton")
	void resetClearsCachedLookup() {
		EchoReplacementDecider.EchoLookup first = EchoOnlineService.echoLookup();
		EchoOnlineService.resetForTests();
		EchoReplacementDecider.EchoLookup second = EchoOnlineService.echoLookup();

		Assertions.assertThat(second).isNotSameAs(first);
	}
}
