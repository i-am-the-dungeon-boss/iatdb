package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class OptionSliderTest {

	@Test
	@DisplayName("setTitleText updates the visible slider title")
	void setTitleTextUpdatesVisibleTitle() {
		OptionSlider slider = new OptionSlider("Start Floor", "1", "26", 1, 26) {
			@Override
			protected void onChange() {
			}
		};

		slider.setTitleText("Start Floor: 14");

		Assertions.assertThat(slider.titleText()).isEqualTo("Start Floor: 14");
	}
}
