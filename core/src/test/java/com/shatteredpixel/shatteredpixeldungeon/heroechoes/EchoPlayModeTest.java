package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineSettings;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoPlayModeTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
		Dungeon.echoPlayMode = EchoPlayMode.NONE;
		GamesInProgress.selectedEchoPlayMode = EchoPlayMode.NONE;
	}

	@Test
	@DisplayName("ranked mode enables online sync when backend is configured")
	void rankedModeUsesOnlineWhenConfigured() {
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		Assertions.assertThat(EchoOnlineSettings.isOnlineEnabled()).isTrue();
	}

	@Test
	@DisplayName("applies selected ranked mode over a continued solo save")
	void appliesSelectedRankedModeOverContinuedSave() {
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		GamesInProgress.selectedEchoPlayMode = EchoPlayMode.RANKED;
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;

		GamesInProgress.applySelectedEchoPlayMode();

		Assertions.assertThat(Dungeon.echoPlayMode).isEqualTo(EchoPlayMode.RANKED);
		Assertions.assertThat(EchoOnlineSettings.isOnlineEnabled()).isTrue();
	}

	@Test
	@DisplayName("ranked mode does not persist leaderboard locally")
	void rankedModeSkipsLocalLeaderboard() {
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		Assertions.assertThat(EchoPlayModePaths.persistsLeaderboardLocally()).isFalse();
	}

	@Test
	@DisplayName("solo mode keeps online sync disabled")
	void soloModeUsesLocalEchoesOnly() {
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;

		Assertions.assertThat(EchoOnlineSettings.isOnlineEnabled()).isFalse();
	}
}
