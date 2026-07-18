package com.shatteredpixel.shatteredpixeldungeon.scenes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModeDescLayoutTest {

	@Test
	@DisplayName("mode desc max width leaves margin inside available area")
	void modeDescMaxWidthLeavesMargin() {
		Assertions.assertThat(HeroSelectScene.modeDescMaxWidth(100)).isEqualTo(92);
		Assertions.assertThat(HeroSelectScene.modeDescMaxWidth(240)).isEqualTo(232);
	}

	@Test
	@DisplayName("mode desc uses smaller font in narrow layouts")
	void modeDescUsesSmallerFontWhenNarrow() {
		Assertions.assertThat(HeroSelectScene.modeDescFontSize(100)).isEqualTo(5);
		Assertions.assertThat(HeroSelectScene.modeDescFontSize(140)).isEqualTo(5);
		Assertions.assertThat(HeroSelectScene.modeDescFontSize(141)).isEqualTo(6);
		Assertions.assertThat(HeroSelectScene.modeDescFontSize(300)).isEqualTo(6);
	}

	@Test
	@DisplayName("mode desc gap shrinks to keep following UI in reserved space")
	void modeDescGapShrinksToKeepFollowingUiInReservedSpace() {
		Assertions.assertThat(HeroSelectScene.gapAfterModeDesc(20f, 8f)).isEqualTo(12f);
		Assertions.assertThat(HeroSelectScene.gapAfterModeDesc(20f, 20f)).isEqualTo(2f);
		Assertions.assertThat(HeroSelectScene.gapAfterModeDesc(20f, 30f)).isEqualTo(2f);
	}

	@Test
	@DisplayName("start scene mode desc max width matches available content width")
	void startSceneModeDescMaxWidthMatchesContentWidth() {
		Assertions.assertThat(StartScene.modeDescMaxWidth(120)).isEqualTo(104);
		Assertions.assertThat(StartScene.modeDescMaxWidth(320)).isEqualTo(304);
	}
}
