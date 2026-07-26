package com.shatteredpixel.shatteredpixeldungeon.ui;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TitleHeroLogoCharacterBoundsTest {

	@Test
	@DisplayName("resting character bottom ignores viewport padding below the sprite")
	void restingCharacterBottomIgnoresViewportPadding() {
		float logoY = 10f;
		float viewport = TitleHeroLogoTiming.VIEWPORT;
		float heroHeight = 30f;
		float componentBottom = logoY + viewport + 8f;

		float characterBottom = TitleHeroLogo.restingCharacterBottom(logoY, viewport, heroHeight);

		Assertions.assertThat(characterBottom).isEqualTo(logoY + (viewport + heroHeight) / 2f);
		Assertions.assertThat(characterBottom).isLessThan(componentBottom);
	}
}
