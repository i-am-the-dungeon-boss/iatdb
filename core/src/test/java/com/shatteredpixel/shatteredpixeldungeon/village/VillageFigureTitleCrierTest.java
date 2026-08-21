package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class VillageFigureTitleCrierTest {

	@Test
	@DisplayName("The title is shouted the moment the body lands, not one period later")
	void criesImmediately() {
		VillageFigureTitleCrier crier = new VillageFigureTitleCrier(null, "Sewers Boss!");

		Assertions.assertThat(crier.advance(0f)).isTrue();
	}

	@Test
	@DisplayName("Nothing between shouts: the text has to clear before it comes back")
	void waitsOutThePeriod() {
		VillageFigureTitleCrier crier = new VillageFigureTitleCrier(null, "Sewers Boss!");
		crier.advance(0f);

		Assertions.assertThat(crier.advance(VillageFigureTitleCrier.PERIOD / 2f)).isFalse();
		Assertions.assertThat(crier.advance(VillageFigureTitleCrier.PERIOD / 2f)).isTrue();
	}

	@Test
	@DisplayName("A frame longer than the period is one shout, not a burst of them")
	void aLongFrameIsStillOneShout() {
		VillageFigureTitleCrier crier = new VillageFigureTitleCrier(null, "Sewers Boss!");
		crier.advance(0f);

		Assertions.assertThat(crier.advance(VillageFigureTitleCrier.PERIOD * 10f)).isTrue();
		Assertions.assertThat(crier.advance(0f)).isFalse();
	}
}
