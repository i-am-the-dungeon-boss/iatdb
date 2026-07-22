package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndChallenges;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class ChallengesPlayModeTest {

	@AfterEach
	void cleanup() {
		SPDSettings.challenges(0);
		Dungeon.echoPlayMode = EchoPlayMode.NONE;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("challenges are allowed in solo mode only")
	void challengesAllowedInSoloOnly() {
		Assertions.assertThat(Challenges.allowedForPlayMode(EchoPlayMode.SOLO)).isTrue();
		Assertions.assertThat(Challenges.allowedForPlayMode(EchoPlayMode.RANKED)).isFalse();
		Assertions.assertThat(Challenges.allowedForPlayMode(EchoPlayMode.NONE)).isFalse();
	}

	@Test
	@DisplayName("hero select game options are shown in solo mode only")
	void gameOptionsShownInSoloOnly() {
		Assertions.assertThat(HeroSelectScene.gameOptionsAllowed(EchoPlayMode.SOLO)).isTrue();
		Assertions.assertThat(HeroSelectScene.gameOptionsAllowed(EchoPlayMode.RANKED)).isFalse();
		Assertions.assertThat(HeroSelectScene.gameOptionsAllowed(EchoPlayMode.DEBUG)).isFalse();
		Assertions.assertThat(HeroSelectScene.gameOptionsAllowed(EchoPlayMode.NONE)).isFalse();
	}

	@Test
	@DisplayName("ranked mode clears applied challenge settings")
	void rankedModeClearsAppliedChallengeSettings() {
		SPDSettings.challenges(Challenges.NO_FOOD | Challenges.DARKNESS);
		Dungeon.challenges = Challenges.NO_FOOD;

		Challenges.clearIfDisallowed(EchoPlayMode.RANKED);

		Assertions.assertThat(SPDSettings.challenges()).isEqualTo(0);
		Assertions.assertThat(Dungeon.challenges).isEqualTo(0);
	}

	@Test
	@DisplayName("solo mode keeps applied challenge settings")
	void soloModeKeepsAppliedChallengeSettings() {
		SPDSettings.challenges(Challenges.NO_FOOD);
		Dungeon.challenges = Challenges.NO_FOOD;

		Challenges.clearIfDisallowed(EchoPlayMode.SOLO);

		Assertions.assertThat(SPDSettings.challenges()).isEqualTo(Challenges.NO_FOOD);
		Assertions.assertThat(Dungeon.challenges).isEqualTo(Challenges.NO_FOOD);
	}

	@Test
	@DisplayName("selecting ranked mode clears applied challenge settings")
	void selectingRankedModeClearsAppliedChallengeSettings() {
		SPDSettings.challenges(Challenges.NO_FOOD);
		Dungeon.challenges = Challenges.NO_FOOD;

		GamesInProgress.selectEchoPlayMode(EchoPlayMode.RANKED);

		Assertions.assertThat(SPDSettings.challenges()).isEqualTo(0);
		Assertions.assertThat(Dungeon.challenges).isEqualTo(0);
	}

	@Test
	@DisplayName("challenges window has an untested disclaimer message")
	void challengesWindowHasUntestedDisclaimer() {
		Assertions.assertThat(Messages.get(WndChallenges.class, "disclaimer")).isNotBlank();
	}

	@Test
	@DisplayName("Dungeon.init clears challenges outside solo mode")
	void initClearsChallengesOutsideSolo() {
		SPDSettings.challenges(Challenges.NO_FOOD);
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.seed = 1L;
		Dungeon.daily = false;
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;
		Dungeon.init();

		Assertions.assertThat(Dungeon.challenges).isEqualTo(0);
	}

	@Test
	@DisplayName("Dungeon.init keeps challenges in solo mode")
	void initKeepsChallengesInSolo() {
		SPDSettings.challenges(Challenges.NO_FOOD);
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.seed = 1L;
		Dungeon.daily = false;
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Dungeon.init();

		Assertions.assertThat(Dungeon.challenges).isEqualTo(Challenges.NO_FOOD);
	}
}
