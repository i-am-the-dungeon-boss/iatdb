package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.IntegrityReport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.IntegrityReportQueue;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.utils.SaveIntegrity;

import com.watabou.utils.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class WndGameDebugToolsTest {

	@AfterEach
	void cleanup() {
		DebugSettings.resetForTests();
	}

	@Test
	@DisplayName("pause-menu echo debug tools are hidden in release builds")
	void echoDebugToolsHiddenInReleaseBuilds() {
		DebugSettings.setDebugBuildOverride(false);
		Assertions.assertThat(WndGame.showsEchoDebugTools()).isFalse();
	}

	@Test
	@DisplayName("pause-menu echo debug tools are available in debug builds")
	void echoDebugToolsAvailableInDebugBuilds() {
		DebugSettings.setDebugBuildOverride(true);
		Assertions.assertThat(WndGame.showsEchoDebugTools()).isTrue();
	}

	@Test
	@DisplayName("pause menu includes stop-echo-hunting action in debug builds")
	void pauseMenuIncludesStopEchoHuntingInDebugBuilds() throws Exception {
		String source = java.nio.file.Files.readString(
				findSource("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndGame.java"));
		Assertions.assertThat(source).contains("stop_echo_hunting");
		Assertions.assertThat(source).contains("EchoBoss.stopAllHunting");
		Assertions.assertThat(source).contains("showsEchoDebugTools()");
	}

	@Test
	@DisplayName("pause menu includes restock-ground-items action in debug builds")
	void pauseMenuIncludesRestockGroundItemsInDebugBuilds() throws Exception {
		String source = java.nio.file.Files.readString(
				findSource("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndGame.java"));
		Assertions.assertThat(source).contains("restock_ground_items");
		Assertions.assertThat(source).contains("DebugArenaItems.restockGround");
	}

	@Test
	@DisplayName("pause menu includes give-echo-arsenal action in debug builds")
	void pauseMenuIncludesGiveEchoArsenalInDebugBuilds() throws Exception {
		String source = java.nio.file.Files.readString(
				findSource("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndGame.java"));
		Assertions.assertThat(source).contains("give_echo_arsenal");
		Assertions.assertThat(source).contains("DebugEchoArsenal.grantAndCycleAll");
	}

	@Test
	@DisplayName("pause menu includes give-echo-armor-ability action in debug builds")
	void pauseMenuIncludesGiveEchoArmorAbilityInDebugBuilds() throws Exception {
		String source = java.nio.file.Files.readString(
				findSource("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndGame.java"));
		Assertions.assertThat(source).contains("give_echo_armor_ability");
		Assertions.assertThat(source).contains("DebugEchoArsenal.grantArmorAbilityAll");
	}

	@Test
	@DisplayName("pause menu includes the debug save-marking action in debug builds")
	void pauseMenuIncludesMarkSaveInDebugBuilds() throws Exception {
		String source = java.nio.file.Files.readString(
				findSource("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndGame.java"));
		Assertions.assertThat(source).contains("mark_save_state");
		Assertions.assertThat(source).contains("markSaveState()");
	}

	@Test
	@DisplayName("marking the save reproduces a real detection, once")
	void markSaveStateMarksAndReportsOnce() throws Exception {
		EchoTestSupport.resetWorkflowState();
		IntegrityReportQueue.clear();
		GamesInProgress.curSlot = 1;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.seed = 7L;
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Dungeon.init();
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.level = new SewerLevel();
		Dungeon.level.create();
		try {
			Assertions.assertThat(WndGame.markSaveState()).isTrue();
			Assertions.assertThat(SaveIntegrity.isModified()).isTrue();
			Assertions.assertThat(IntegrityReportQueue.pending()).hasSize(1);
			Assertions.assertThat(IntegrityReportQueue.pending().get(0).reason)
					.isEqualTo(IntegrityReport.REASON_DEBUG);

			// pressing it twice is still one detection, exactly as a reload would be
			Assertions.assertThat(WndGame.markSaveState()).isTrue();
			Assertions.assertThat(IntegrityReportQueue.pending()).hasSize(1);

			// and the mark is on disk, so it survives the next load
			Dungeon.loadGame(1);
			Assertions.assertThat(SaveIntegrity.isModified()).isTrue();
			Assertions.assertThat(IntegrityReportQueue.pending()).hasSize(1);
		} finally {
			IntegrityReportQueue.clear();
			EchoTestSupport.deleteRecursively(
					new java.io.File(FileUtils.getFileHandle(GamesInProgress.gameFolder(1)).path()));
			SPDSettings.slotLayout(0);
			SaveIntegrity.reset();
			Dungeon.hero = null;
			Dungeon.level = null;
			GamesInProgress.curSlot = 0;
		}
	}

	@Test
	@DisplayName("the debug button reports the run's current state")
	void markSaveStateLabelReflectsState() {
		SaveIntegrity.reset();
		String clean = WndGame.markSaveStateLabel();

		SaveIntegrity.mark();
		String marked = WndGame.markSaveStateLabel();

		Assertions.assertThat(clean).isNotEqualTo(marked);
		Assertions.assertThat(marked).containsIgnoringCase("marked");
		SaveIntegrity.reset();
	}

	@Test
	@DisplayName("marking the save does nothing without a run")
	void markSaveStateNeedsARun() {
		Dungeon.hero = null;
		Assertions.assertThat(WndGame.markSaveState()).isFalse();
		Assertions.assertThat(SaveIntegrity.isModified()).isFalse();
	}

	@Test
	@DisplayName("settings debug checkboxes follow debug build flag")
	void settingsDebugCheckboxesFollowDebugBuildFlag() {
		DebugSettings.setDebugBuildOverride(false);
		Assertions.assertThat(DebugSettings.isDebugBuild()).isFalse();

		DebugSettings.setDebugBuildOverride(true);
		Assertions.assertThat(DebugSettings.isDebugBuild()).isTrue();
	}

	private static java.nio.file.Path findSource(String relativePath) throws java.io.IOException {
		java.nio.file.Path dir = java.nio.file.Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			java.nio.file.Path candidate = dir.resolve(relativePath);
			if (java.nio.file.Files.isRegularFile(candidate)) {
				return candidate;
			}
			dir = dir.getParent();
		}
		throw new AssertionError("Could not find " + relativePath);
	}
}
