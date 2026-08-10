package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.watabou.utils.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

/**
 * {@link Dungeon#saveAllOnCrash()} runs from a launcher's uncaught-exception handler,
 * so its contract is: save when it can, and never throw on top of the crash being
 * reported.
 */
@ExtendWith(GdxTestExtension.class)
class DungeonSaveOnCrashTest {

	private static final int SLOT = 1;

	@BeforeEach
	void setUp() {
		EchoTestSupport.resetWorkflowState();
		Dungeon.hero = null;
		Dungeon.level = null;
		Dungeon.depth = 5;
		Dungeon.branch = 0;
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;
		GamesInProgress.curSlot = SLOT;
	}

	@AfterEach
	void tearDown() {
		// Dungeon.init() and saveAll() write into the test file sandbox; leave none behind.
		for (EchoPlayMode mode : new EchoPlayMode[] { EchoPlayMode.RANKED, EchoPlayMode.SOLO }) {
			Dungeon.echoPlayMode = mode;
			for (int slot : new int[] { 0, SLOT }) {
				deleteIfPresent(GamesInProgress.gameFolder(slot));
			}
		}
		deleteIfPresent("bones.dat");
		GamesInProgress.curSlot = 0;
		Dungeon.hero = null;
		Dungeon.level = null;
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
	}

	@Test
	@DisplayName("writes the ranked save when a live run is in memory")
	void writesRankedSaveForLiveRun() {
		startRankedRun();

		Assertions.assertThat(Dungeon.saveAllOnCrash()).isTrue();
		Assertions.assertThat(GamesInProgress.gameFolder(SLOT)).endsWith("-ranked");
		Assertions.assertThat(FileUtils.fileLength(GamesInProgress.gameFile(SLOT)))
				.isGreaterThan(1);
	}

	@Test
	@DisplayName("does nothing without a run in memory")
	void doesNothingWithoutRun() {
		Assertions.assertThat(Dungeon.saveAllOnCrash()).isFalse();
		Assertions.assertThat(FileUtils.fileLength(GamesInProgress.gameFile(SLOT))).isZero();
	}

	@Test
	@DisplayName("does not throw when the crash left no level loaded")
	void doesNotThrowWithoutLevel() {
		EchoTestSupport.warriorHero();
		Dungeon.level = null;

		Assertions.assertThatCode(Dungeon::saveAllOnCrash).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("does not throw when no save slot is active")
	void doesNotThrowWithoutSlot() {
		startRankedRun();
		GamesInProgress.curSlot = 0;

		Assertions.assertThat(Dungeon.saveAllOnCrash()).isFalse();
	}

	private static void deleteIfPresent(String path) {
		if (FileUtils.dirExists(path) || FileUtils.fileExists(path)) {
			EchoTestSupport.deleteRecursively(new File(FileUtils.getFileHandle(path).path()));
		}
	}

	/** A ranked run with the statics {@link Dungeon#saveGame} reads already populated. */
	private static void startRankedRun() {
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.seed = 1L;
		Dungeon.daily = false;
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;
		Dungeon.init();
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		// A generated level, not a stub: saveLevel() bundles collections that only
		// Level.create() populates.
		Dungeon.level = new SewerLevel();
		Dungeon.level.create();
		GamesInProgress.curSlot = SLOT;
	}
}
