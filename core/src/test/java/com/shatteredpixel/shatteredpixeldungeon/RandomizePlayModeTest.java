package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class RandomizePlayModeTest {

	@AfterEach
	void cleanup() {
		GamesInProgress.randomizedClass = false;
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
	}

	@Test
	@DisplayName("randomize is allowed in solo mode only")
	void randomizeAllowedInSoloOnly() {
		Assertions.assertThat(GamesInProgress.randomizeAllowedForPlayMode(EchoPlayMode.SOLO)).isTrue();
		Assertions.assertThat(GamesInProgress.randomizeAllowedForPlayMode(EchoPlayMode.RANKED)).isFalse();
		Assertions.assertThat(GamesInProgress.randomizeAllowedForPlayMode(EchoPlayMode.DEBUG)).isFalse();
	}

	@Test
	@DisplayName("game options include randomize and stay solo-only")
	void gameOptionsIncludeRandomizeAndStaySoloOnly() {
		Assertions.assertThat(HeroSelectScene.gameOptionsAllowed(EchoPlayMode.SOLO)).isTrue();
		Assertions.assertThat(HeroSelectScene.gameOptionsAllowed(EchoPlayMode.RANKED)).isFalse();
	}

	@Test
	@DisplayName("ranked mode clears randomized class flag")
	void rankedModeClearsRandomizedClassFlag() {
		GamesInProgress.randomizedClass = true;

		GamesInProgress.clearRandomizeIfDisallowed(EchoPlayMode.RANKED);

		Assertions.assertThat(GamesInProgress.randomizedClass).isFalse();
	}

	@Test
	@DisplayName("solo mode keeps randomized class flag")
	void soloModeKeepsRandomizedClassFlag() {
		GamesInProgress.randomizedClass = true;

		GamesInProgress.clearRandomizeIfDisallowed(EchoPlayMode.SOLO);

		Assertions.assertThat(GamesInProgress.randomizedClass).isTrue();
	}

	@Test
	@DisplayName("selecting ranked mode clears randomized class flag")
	void selectingRankedModeClearsRandomizedClassFlag() {
		GamesInProgress.randomizedClass = true;

		GamesInProgress.selectEchoPlayMode(EchoPlayMode.RANKED);

		Assertions.assertThat(GamesInProgress.randomizedClass).isFalse();
	}

	@Test
	@DisplayName("Statistics.reset ignores randomized class outside solo")
	void statisticsResetIgnoresRandomizedClassOutsideSolo() {
		GamesInProgress.randomizedClass = true;
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		Statistics.reset();

		Assertions.assertThat(Statistics.qualifiedForRandomVictoryBadge).isFalse();
	}

	@Test
	@DisplayName("Statistics.reset keeps randomized class in solo")
	void statisticsResetKeepsRandomizedClassInSolo() {
		GamesInProgress.randomizedClass = true;
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;

		Statistics.reset();

		Assertions.assertThat(Statistics.qualifiedForRandomVictoryBadge).isTrue();
	}
}
