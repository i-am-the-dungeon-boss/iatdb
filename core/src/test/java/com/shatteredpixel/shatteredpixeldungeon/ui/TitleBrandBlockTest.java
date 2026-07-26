package com.shatteredpixel.shatteredpixeldungeon.ui;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TitleBrandBlockTest {

	@Test
	@DisplayName("brand title centers between hero character and menu")
	void brandTitleCentersBetweenHeroCharacterAndMenu() {
		float characterBottom = 40f;
		float menuTop = 100f;
		float titleHeight = 20f;

		Assertions.assertThat(TitleBrandBlock.brandTitleYBetween(characterBottom, menuTop, titleHeight))
				.isEqualTo(60f);
	}

	@Test
	@DisplayName("brand title hugs character when the band is too short")
	void brandTitleHugsCharacterWhenBandIsTooShort() {
		float characterBottom = 40f;
		float menuTop = 50f;
		float titleHeight = 20f;

		Assertions.assertThat(TitleBrandBlock.brandTitleYBetween(characterBottom, menuTop, titleHeight))
				.isEqualTo(characterBottom);
	}
}
