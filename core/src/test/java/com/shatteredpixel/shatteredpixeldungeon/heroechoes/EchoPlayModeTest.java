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

	@Test
	@DisplayName("debug mode keeps online sync disabled")
	void debugModeUsesLocalEchoesOnly() {
		EchoOnlineSettings.setBackendUrl("https://echo.test");
		Dungeon.echoPlayMode = EchoPlayMode.DEBUG;

		Assertions.assertThat(EchoOnlineSettings.isOnlineEnabled()).isFalse();
	}

	@Test
	@DisplayName("debug mode does not persist leaderboard locally")
	void debugModeSkipsLocalLeaderboard() {
		Dungeon.echoPlayMode = EchoPlayMode.DEBUG;

		Assertions.assertThat(EchoPlayModePaths.persistsLeaderboardLocally()).isFalse();
	}

	@Test
	@DisplayName("debug play mode is allowed only in debug builds")
	void debugPlayModeAllowedOnlyInDebugBuilds() {
		com.shatteredpixel.shatteredpixeldungeon.DebugSettings.setDebugBuildOverride(true);
		Assertions.assertThat(EchoPlayMode.isAllowed(EchoPlayMode.DEBUG)).isTrue();

		com.shatteredpixel.shatteredpixeldungeon.DebugSettings.setDebugBuildOverride(false);
		Assertions.assertThat(EchoPlayMode.isAllowed(EchoPlayMode.DEBUG)).isFalse();
		Assertions.assertThat(EchoPlayMode.isAllowed(EchoPlayMode.SOLO)).isTrue();
		Assertions.assertThat(EchoPlayMode.isAllowed(EchoPlayMode.RANKED)).isTrue();
	}

	@Test
	@DisplayName("selecting debug mode is ignored outside debug builds")
	void selectingDebugModeIgnoredOutsideDebugBuilds() {
		com.shatteredpixel.shatteredpixeldungeon.DebugSettings.setDebugBuildOverride(false);

		GamesInProgress.selectEchoPlayMode(EchoPlayMode.DEBUG);

		Assertions.assertThat(GamesInProgress.selectedEchoPlayMode).isNotEqualTo(EchoPlayMode.DEBUG);
	}

	@Test
	@DisplayName("release builds never route to DebugArenaLevel even if mode is DEBUG")
	void releaseBuildsNeverRouteToDebugArena() {
		com.shatteredpixel.shatteredpixeldungeon.DebugSettings.setDebugBuildOverride(false);
		Dungeon.echoPlayMode = EchoPlayMode.DEBUG;

		Assertions.assertThat(Dungeon.levelClassForDepth(1, 0))
				.isNotEqualTo(com.shatteredpixel.shatteredpixeldungeon.levels.DebugArenaLevel.class);
	}
}
