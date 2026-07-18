package com.shatteredpixel.shatteredpixeldungeon.ui;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IatdbIconFrameTest {

	@Test
	@DisplayName("IATDB mark occupies a 16x16 slot after Journal on the title icon row")
	void iatdbMarkIsSixteenBySixteenAfterJournal() {
		Assertions.assertThat(IatdbIconFrame.X).isEqualTo(153);
		Assertions.assertThat(IatdbIconFrame.Y).isEqualTo(0);
		Assertions.assertThat(IatdbIconFrame.WIDTH).isEqualTo(16);
		Assertions.assertThat(IatdbIconFrame.HEIGHT).isEqualTo(16);
	}
}
