package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

@ExtendWith(GdxTestExtension.class)
class EchoPlayModePathsTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("ranked and solo use separate echo directories")
	void rankedAndSoloUseSeparateEchoDirs() {
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;
		Assertions.assertThat(EchoPlayModePaths.echoesDir()).isEqualTo("echoes-ranked");

		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Assertions.assertThat(EchoPlayModePaths.echoesDir()).isEqualTo("echoes-solo");
	}

	@Test
	@DisplayName("ranked and solo use separate game save folders")
	void rankedAndSoloUseSeparateGameFolders() {
		GamesInProgress.selectedEchoPlayMode = EchoPlayMode.RANKED;
		Assertions.assertThat(GamesInProgress.gameFolder(1)).isEqualTo("game1-ranked");

		GamesInProgress.selectedEchoPlayMode = EchoPlayMode.SOLO;
		Assertions.assertThat(GamesInProgress.gameFolder(1)).isEqualTo("game1-solo");
	}

	@Test
	@DisplayName("ranked echoes are not visible from solo storage")
	void rankedEchoesAreNotVisibleFromSoloStorage() {
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;
		new EchoStorage().save(EchoTestSupport.warriorEcho(5));

		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Optional<Echo> soloEcho = new EchoStorage().loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION);

		Assertions.assertThat(soloEcho).isEmpty();
	}
}
