package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class StartSceneModeDescTest {

	@Test
	@DisplayName("continue screen reuses hero-select solo description")
	void continueScreenReusesSoloDescription() {
		Assertions.assertThat(StartScene.modeDescriptionText(EchoPlayMode.SOLO))
				.isEqualTo(Messages.get(HeroSelectScene.class, "solo_desc"));
	}

	@Test
	@DisplayName("continue screen reuses hero-select ranked description")
	void continueScreenReusesRankedDescription() {
		Assertions.assertThat(StartScene.modeDescriptionText(EchoPlayMode.RANKED))
				.isEqualTo(Messages.get(HeroSelectScene.class, "ranked_desc"));
	}

	@Test
	@DisplayName("continue screen has no description when mode is none")
	void continueScreenHasNoDescriptionWhenNone() {
		Assertions.assertThat(StartScene.modeDescriptionText(EchoPlayMode.NONE)).isNull();
	}
}
