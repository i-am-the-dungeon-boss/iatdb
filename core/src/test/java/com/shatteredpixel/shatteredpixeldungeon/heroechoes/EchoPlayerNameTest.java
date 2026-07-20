package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoWireCodec;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoPlayerNameTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("fromHero captures player name from settings")
	void fromHeroCapturesPlayerName() {
		SPDSettings.playerName("  Marwan  ");
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		hero.lvl = 5;
		hero.HP = hero.HT = 30;

		Echo echo = Echo.fromHero(hero, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);

		Assertions.assertThat(echo.userName).isEqualTo("Marwan");
	}

	@Test
	@DisplayName("fromHero uses Anonymous plus class when settings are blank")
	void fromHeroDefaultsBlankPlayerName() {
		SPDSettings.playerName("");
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		hero.lvl = 5;
		hero.HP = hero.HT = 30;

		Echo echo = Echo.fromHero(hero, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);

		Assertions.assertThat(echo.userName).isEqualTo("Anonymous Warrior");
	}

	@Test
	@DisplayName("default user name includes hero class")
	void defaultUserNameIncludesHeroClass() {
		Assertions.assertThat(Echo.defaultUserName("MAGE")).isEqualTo("Anonymous Mage");
		Assertions.assertThat(Echo.defaultUserName("HUNTRESS")).isEqualTo("Anonymous Huntress");
	}

	@Test
	@DisplayName("encodeEchoUpload always includes user_name")
	void encodeEchoUploadAlwaysIncludesUserName() throws Exception {
		Echo named = EchoTestSupport.warriorEchoWithData(5);
		named.userName = "Marwan";

		Assertions.assertThat(EchoWireCodec.encodeEchoUpload(named, "test-client"))
				.contains("\"user_name\":\"Marwan\"");
	}
}
