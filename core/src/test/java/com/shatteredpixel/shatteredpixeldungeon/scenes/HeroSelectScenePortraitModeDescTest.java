package com.shatteredpixel.shatteredpixeldungeon.scenes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HeroSelectScenePortraitModeDescTest {

	@Test
	@DisplayName("mode description sits above its anchor control")
	void modeDescriptionSitsAboveAnchor() {
		Assertions.assertThat(HeroSelectScene.modeDescAbove(100f, 20f)).isEqualTo(78f);
		Assertions.assertThat(HeroSelectScene.modeDescAbove(50f, 12f)).isEqualTo(36f);
	}

	@Test
	@DisplayName("mode description fades with the rest of the UI")
	void modeDescriptionFadesWithUi() {
		Assertions.assertThat(HeroSelectScene.modeDescAlpha(0f)).isEqualTo(0f);
		Assertions.assertThat(HeroSelectScene.modeDescAlpha(0.25f)).isEqualTo(0.25f);
		Assertions.assertThat(HeroSelectScene.modeDescAlpha(1f)).isEqualTo(1f);
	}

	@Test
	@DisplayName("portrait keeps mode description visible with start button until UI fades")
	void portraitKeepsModeDescVisibleWithStartButton() {
		Assertions.assertThat(HeroSelectScene.modeDescVisibleAfterHeroSelected(false)).isTrue();
	}
}
