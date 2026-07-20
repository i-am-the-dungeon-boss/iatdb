package com.shatteredpixel.shatteredpixeldungeon.scenes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PixelSceneRenderTextZoomTest {

	@Test
	@DisplayName("clamps render zoom to at least 1 when scale rounds to 0")
	void clampsWhenScaleRoundsToZero() {
		Assertions.assertThat(PixelScene.renderTextZoom(4, 0f)).isEqualTo(1);
		Assertions.assertThat(PixelScene.renderTextZoom(2, 0.2f)).isEqualTo(1);
	}

	@Test
	@DisplayName("preserves normal render zoom")
	void preservesNormalZoom() {
		Assertions.assertThat(PixelScene.renderTextZoom(4, 1f)).isEqualTo(4);
		Assertions.assertThat(PixelScene.renderTextZoom(3, 2f)).isEqualTo(6);
	}
}
