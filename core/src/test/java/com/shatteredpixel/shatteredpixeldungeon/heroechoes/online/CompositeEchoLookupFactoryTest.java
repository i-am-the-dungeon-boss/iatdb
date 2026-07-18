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
class CompositeEchoLookupFactoryTest {

	@AfterEach
	void cleanup() {
		CompositeEchoLookup.resetForTests();
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("echoLookup returns composite lookup wired to local storage")
	void echoLookupReturnsCompositeLookup() {
		EchoReplacementDecider.EchoLookup lookup = CompositeEchoLookup.echoLookup();

		Assertions.assertThat(lookup).isInstanceOf(CompositeEchoLookup.class);
	}

	@Test
	@DisplayName("resetForTests clears cached lookup singleton")
	void resetClearsCachedLookup() {
		EchoReplacementDecider.EchoLookup first = CompositeEchoLookup.echoLookup();
		CompositeEchoLookup.resetForTests();
		EchoReplacementDecider.EchoLookup second = CompositeEchoLookup.echoLookup();

		Assertions.assertThat(second).isNotSameAs(first);
	}

	@Test
	@DisplayName("setEchoLookupForTests overrides the process-wide lookup")
	void setEchoLookupForTestsOverridesLookup() {
		EchoReplacementDecider.EchoLookup override = depth -> java.util.Optional.empty();
		CompositeEchoLookup.setEchoLookupForTests(override);

		Assertions.assertThat(CompositeEchoLookup.echoLookup()).isSameAs(override);
	}
}
