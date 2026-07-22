package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class HeroSelectSceneModeDescTest {

	@AfterEach
	void cleanup() {
		DebugSettings.resetForTests();
	}
	@Test
	@DisplayName("solo mode uses solo_desc message key")
	void soloModeUsesSoloDescKey() {
		Assertions.assertThat(HeroSelectScene.modeDescriptionKey(EchoPlayMode.SOLO))
				.isEqualTo("solo_desc");
	}

	@Test
	@DisplayName("ranked mode uses ranked_desc message key")
	void rankedModeUsesRankedDescKey() {
		Assertions.assertThat(HeroSelectScene.modeDescriptionKey(EchoPlayMode.RANKED))
				.isEqualTo("ranked_desc");
	}

	@Test
	@DisplayName("none mode has no description key")
	void noneModeHasNoDescriptionKey() {
		Assertions.assertThat(HeroSelectScene.modeDescriptionKey(EchoPlayMode.NONE)).isNull();
	}

	@Test
	@DisplayName("debug mode uses debug_desc message key")
	void debugModeUsesDebugDescKey() {
		com.shatteredpixel.shatteredpixeldungeon.DebugSettings.setDebugBuildOverride(true);
		Assertions.assertThat(HeroSelectScene.modeDescriptionKey(EchoPlayMode.DEBUG))
				.isEqualTo("debug_desc");
	}

	@Test
	@DisplayName("debug mode description is hidden outside debug builds")
	void debugModeDescriptionHiddenOutsideDebugBuilds() {
		DebugSettings.setDebugBuildOverride(false);
		Assertions.assertThat(HeroSelectScene.modeDescriptionKey(EchoPlayMode.DEBUG)).isNull();
	}

	@Test
	@DisplayName("solo and ranked description messages are defined")
	void soloAndRankedDescriptionMessagesAreDefined() {
		Assertions.assertThat(Messages.get(HeroSelectScene.class, "solo_desc")).isNotBlank();
		Assertions.assertThat(Messages.get(HeroSelectScene.class, "ranked_desc")).isNotBlank();
		Assertions.assertThat(Messages.get(HeroSelectScene.class, "solo_desc"))
				.isNotEqualTo(Messages.get(HeroSelectScene.class, "ranked_desc"));
	}

	@Test
	@DisplayName("debug description message is defined")
	void debugDescriptionMessageIsDefined() {
		Assertions.assertThat(Messages.get(HeroSelectScene.class, "debug_desc")).isNotBlank();
	}
}
