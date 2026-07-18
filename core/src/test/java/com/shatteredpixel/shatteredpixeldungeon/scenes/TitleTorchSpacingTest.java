package com.shatteredpixel.shatteredpixeldungeon.scenes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TitleTorchSpacingTest {

	@Test
	@DisplayName("title torches sit outside the brand block for wider spacing")
	void titleTorchesSitOutsideBrandBlock() {
		Assertions.assertThat(TitleScene.TORCH_OUTSET_LANDSCAPE).isGreaterThan(0f);
		Assertions.assertThat(TitleScene.TORCH_OUTSET_PORTRAIT).isGreaterThan(0f);
		Assertions.assertThat(TitleScene.torchLeftX(100f, 12f)).isEqualTo(88f);
		Assertions.assertThat(TitleScene.torchRightX(200f, 12f)).isEqualTo(212f);
	}

	@Test
	@DisplayName("brand title y keeps torch tops on screen in landscape")
	void brandTitleYKeepsTorchTopsOnScreenInLandscape() {
		float insetsTop = 0f;
		float topRegion = 80f;
		float titleHeight = 100f;
		// Centered would be negative / above safe top — clearance must win.
		float y = TitleScene.brandTitleY(insetsTop, topRegion, titleHeight, true);
		Assertions.assertThat(y).isGreaterThanOrEqualTo(TitleScene.BRAND_TOP_CLEARANCE_LANDSCAPE);
		Assertions.assertThat(y).isGreaterThan(insetsTop + 2f + (topRegion - titleHeight) / 2f);
	}

	@Test
	@DisplayName("menu region starts below brand when title is pushed down")
	void menuRegionStartsBelowBrandWhenTitlePushedDown() {
		float insetsTop = 0f;
		float topRegion = 80f;
		float titleBottom = 120f;
		Assertions.assertThat(TitleScene.menuRegionTop(insetsTop, topRegion, titleBottom))
				.isEqualTo(120f);
		Assertions.assertThat(TitleScene.menuRegionTop(insetsTop, topRegion, 50f))
				.isEqualTo(80f);
	}
}
