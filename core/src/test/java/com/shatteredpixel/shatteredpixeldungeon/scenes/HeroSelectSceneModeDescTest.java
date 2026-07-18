package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class HeroSelectSceneModeDescTest {

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
	@DisplayName("solo and ranked description messages are defined")
	void soloAndRankedDescriptionMessagesAreDefined() {
		Assertions.assertThat(Messages.get(HeroSelectScene.class, "solo_desc")).isNotBlank();
		Assertions.assertThat(Messages.get(HeroSelectScene.class, "ranked_desc")).isNotBlank();
		Assertions.assertThat(Messages.get(HeroSelectScene.class, "solo_desc"))
				.isNotEqualTo(Messages.get(HeroSelectScene.class, "ranked_desc"));
	}
}
