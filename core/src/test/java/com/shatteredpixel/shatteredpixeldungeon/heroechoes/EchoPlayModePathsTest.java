package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoPlayModePathsTest {

	@Test
	@DisplayName("ranked and solo use separate echo directories")
	void rankedAndSoloUseSeparateEchoDirs() {
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;
		Assertions.assertThat(EchoPlayModePaths.echoesDir()).isEqualTo("echoes-ranked");

		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Assertions.assertThat(EchoPlayModePaths.echoesDir()).isEqualTo("echoes-solo");
	}

	@Test
	@DisplayName("debug mode uses a separate echo directory and save folder")
	void debugModeUsesSeparateEchoDirAndSaveFolder() {
		Dungeon.echoPlayMode = EchoPlayMode.DEBUG;
		Assertions.assertThat(EchoPlayModePaths.echoesDir()).isEqualTo("echoes-debug");
		Assertions.assertThat(EchoPlayModePaths.gameFolderSuffix()).isEqualTo("-debug");

		GamesInProgress.selectedEchoPlayMode = EchoPlayMode.DEBUG;
		Dungeon.echoPlayMode = EchoPlayMode.DEBUG;
		Assertions.assertThat(GamesInProgress.gameFolder(1)).isEqualTo("game1-debug");
	}

	@Test
	@DisplayName("null play mode argument uses solo echo directory, not legacy echoes/")
	void nullPlayModeArgumentUsesSoloEchoDir() {
		Assertions.assertThat(EchoPlayModePaths.echoesDir(null)).isEqualTo("echoes-solo");
	}

	@Test
	@DisplayName("ranked and solo use separate game save folders")
	void rankedAndSoloUseSeparateGameFolders() {
		GamesInProgress.selectEchoPlayMode(EchoPlayMode.RANKED);
		Assertions.assertThat(GamesInProgress.gameFolder(1)).isEqualTo("game1-ranked");

		GamesInProgress.selectEchoPlayMode(EchoPlayMode.SOLO);
		Assertions.assertThat(GamesInProgress.gameFolder(1)).isEqualTo("game1-solo");
	}

	@Test
	@DisplayName("selecting ranked mode updates dungeon mode for save folders")
	void selectingRankedModeUpdatesDungeonModeForSaveFolders() {
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;

		GamesInProgress.selectEchoPlayMode(EchoPlayMode.RANKED);

		Assertions.assertThat(Dungeon.echoPlayMode).isEqualTo(EchoPlayMode.RANKED);
		Assertions.assertThat(GamesInProgress.gameFolder(1)).isEqualTo("game1-ranked");
	}

	@Test
	@DisplayName("active solo dungeon mode keeps solo save folder when selected mode is still ranked")
	void activeSoloDungeonModeKeepsSoloSaveFolder() {
		GamesInProgress.selectedEchoPlayMode = EchoPlayMode.RANKED;
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;

		Assertions.assertThat(GamesInProgress.gameFolder(1)).isEqualTo("game1-solo");
	}

	@Test
	@DisplayName("easy mode uses separate echo directories and leaderboard files")
	void easyModeUsesSeparateEchoDirsAndLeaderboards() {
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Dungeon.easyMode = false;
		Assertions.assertThat(EchoPlayModePaths.echoesDir()).isEqualTo("echoes-solo");
		Assertions.assertThat(EchoPlayModePaths.leaderboardFile()).isEqualTo("leaderboard-solo.json");

		Dungeon.easyMode = true;
		Assertions.assertThat(EchoPlayModePaths.echoesDir()).isEqualTo("echoes-solo-easy");
		Assertions.assertThat(EchoPlayModePaths.leaderboardFile()).isEqualTo("leaderboard-solo-easy.json");
		Assertions.assertThat(GamesInProgress.gameFolder(1)).isEqualTo("game1-solo");
	}

	@Test
	@DisplayName("easy ranked mode keeps ranked folder with easy suffix")
	void easyRankedModeKeepsRankedFolderWithEasySuffix() {
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;
		Dungeon.easyMode = true;
		Assertions.assertThat(EchoPlayModePaths.echoesDir()).isEqualTo("echoes-ranked-easy");
		Assertions.assertThat(EchoPlayModePaths.leaderboardFile()).isEqualTo("leaderboard-ranked-easy.json");
	}

	@Test
	@DisplayName("ranked echoes are not visible from solo storage")
	void rankedEchoesAreNotVisibleFromSoloStorage() {
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;
		new EchoStorage().save(EchoTestSupport.warriorEcho(5));

		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Echo soloEcho = new EchoStorage().loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION);

		Assertions.assertThat(soloEcho).isNull();
	}
}
