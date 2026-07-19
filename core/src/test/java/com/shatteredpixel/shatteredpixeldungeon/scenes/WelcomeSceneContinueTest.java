package com.shatteredpixel.shatteredpixeldungeon.scenes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WelcomeSceneContinueTest {

	@Test
	@DisplayName("continue always goes to title scene, not hero select")
	void continueAlwaysGoesToTitleScene() {
		Assertions.assertThat(WelcomeScene.continueTarget()).isEqualTo(TitleScene.class);
	}

	@Test
	@DisplayName("first run skips welcome screen")
	void firstRunSkipsWelcomeScreen() {
		Assertions.assertThat(WelcomeScene.shouldSkipWelcome(0, 883, false)).isTrue();
	}

	@Test
	@DisplayName("same version skips welcome screen")
	void sameVersionSkipsWelcomeScreen() {
		Assertions.assertThat(WelcomeScene.shouldSkipWelcome(883, 883, false)).isTrue();
	}

	@Test
	@DisplayName("version update still shows welcome screen")
	void versionUpdateStillShowsWelcomeScreen() {
		Assertions.assertThat(WelcomeScene.shouldSkipWelcome(800, 883, false)).isFalse();
	}
}
