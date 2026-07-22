package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class WndGameDebugToolsTest {

	@AfterEach
	void cleanup() {
		DebugSettings.resetForTests();
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("pause-menu echo debug tools are hidden in release builds")
	void echoDebugToolsHiddenInReleaseBuilds() {
		DebugSettings.setDebugBuildOverride(false);
		Assertions.assertThat(WndGame.showsEchoDebugTools()).isFalse();
	}

	@Test
	@DisplayName("pause-menu echo debug tools are available in debug builds")
	void echoDebugToolsAvailableInDebugBuilds() {
		DebugSettings.setDebugBuildOverride(true);
		Assertions.assertThat(WndGame.showsEchoDebugTools()).isTrue();
	}

	@Test
	@DisplayName("settings debug checkboxes follow debug build flag")
	void settingsDebugCheckboxesFollowDebugBuildFlag() {
		DebugSettings.setDebugBuildOverride(false);
		Assertions.assertThat(DebugSettings.isDebugBuild()).isFalse();

		DebugSettings.setDebugBuildOverride(true);
		Assertions.assertThat(DebugSettings.isDebugBuild()).isTrue();
	}
}
