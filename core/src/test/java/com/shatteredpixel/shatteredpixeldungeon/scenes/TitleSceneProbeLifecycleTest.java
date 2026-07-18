package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class TitleSceneProbeLifecycleTest {

	@Test
	@DisplayName("backend probe UI is skipped after title scene is destroyed")
	void backendProbeUiSkippedAfterDestroy() {
		TitleScene scene = new TitleScene();
		Assertions.assertThat(TitleScene.canApplyBackendProbeUi(scene)).isTrue();

		scene.destroy();

		Assertions.assertThat(TitleScene.canApplyBackendProbeUi(scene)).isFalse();
	}
}
