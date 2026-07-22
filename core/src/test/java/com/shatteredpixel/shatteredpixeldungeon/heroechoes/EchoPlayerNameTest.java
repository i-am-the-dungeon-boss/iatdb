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
		SPDSettings.playerName("  Alex  ");
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		hero.lvl = 5;
		hero.HP = hero.HT = 30;

		Echo echo = Echo.fromHero(hero, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);

		Assertions.assertThat(echo.userName).isEqualTo("Alex");
	}

	@Test
	@DisplayName("fromHero uses Anonymous when settings are blank")
	void fromHeroDefaultsBlankPlayerName() {
		SPDSettings.playerName("");
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		hero.lvl = 5;
		hero.HP = hero.HT = 30;

		Echo echo = Echo.fromHero(hero, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);

		Assertions.assertThat(echo.userName).isEqualTo("Anonymous");
	}

	@Test
	@DisplayName("default user name is Anonymous")
	void defaultUserNameIsAnonymous() {
		Assertions.assertThat(Echo.defaultUserName("MAGE")).isEqualTo("Anonymous");
		Assertions.assertThat(Echo.defaultUserName("HUNTRESS")).isEqualTo("Anonymous");
		Assertions.assertThat(Echo.defaultUserName(null)).isEqualTo("Anonymous");
	}

	@Test
	@DisplayName("encodeEchoUpload omits user_name; server sets it from player JWT")
	void encodeEchoUploadOmitsUserName() throws Exception {
		Echo named = EchoTestSupport.warriorEchoWithData(5);
		named.userName = "Alex";

		Assertions.assertThat(EchoWireCodec.encodeEchoUpload(named, "test-client"))
				.doesNotContain("user_name");
	}
}
