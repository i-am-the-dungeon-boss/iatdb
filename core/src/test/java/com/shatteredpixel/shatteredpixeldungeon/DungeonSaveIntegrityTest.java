package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoCaptureTrigger;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.IntegrityReportQueue;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.utils.SaveIntegrity;

import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;

/**
 * Loading a save whose bytes were changed outside the game must mark the run and
 * then behave completely normally — the run still loads, and nothing is surfaced.
 */
@ExtendWith(GdxTestExtension.class)
class DungeonSaveIntegrityTest {

	private static final int SLOT = 1;

	private int priorVersionCode;

	@BeforeEach
	void setUp() {
		// floor files record the running version and refuse to restore below v2.5.4
		priorVersionCode = Game.versionCode;
		Game.versionCode = ShatteredPixelDungeon.v2_5_4;
		EchoTestSupport.resetWorkflowState();
		IntegrityReportQueue.clear();
		// a fresh install: this device has never written a checksummed save
		SPDSettings.slotLayout(0);
		GamesInProgress.curSlot = SLOT;
	}

	@AfterEach
	void tearDown() {
		IntegrityReportQueue.clear();
		for (EchoPlayMode mode : new EchoPlayMode[] { EchoPlayMode.RANKED, EchoPlayMode.SOLO }) {
			Dungeon.echoPlayMode = mode;
			for (int slot : new int[] { 0, SLOT }) {
				deleteIfPresent(GamesInProgress.gameFolder(slot));
			}
		}
		deleteIfPresent("bones.dat");
		SPDSettings.slotLayout(0);
		GamesInProgress.curSlot = 0;
		Dungeon.hero = null;
		Dungeon.level = null;
		SaveIntegrity.reset();
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Game.versionCode = priorVersionCode;
	}

	@Test
	@DisplayName("a save the game wrote itself loads clean")
	void untouchedSaveLoadsClean() throws IOException {
		startRankedRun();
		Dungeon.saveAll();

		Dungeon.loadGame(SLOT);

		Assertions.assertThat(SaveIntegrity.isModified()).isFalse();
	}

	@Test
	@DisplayName("editing the game file marks the run")
	void editedGameFileMarksRun() throws IOException {
		startRankedRun();
		Dungeon.saveAll();

		editGameFileGold(999999);
		Dungeon.loadGame(SLOT);

		Assertions.assertThat(SaveIntegrity.isModified()).isTrue();
		// the point of loading anyway: the player keeps playing, they just get watched
		Assertions.assertThat(Dungeon.hero).isNotNull();
		Assertions.assertThat(Dungeon.gold).isEqualTo(999999);
	}

	@Test
	@DisplayName("editing the game file queues exactly one report")
	void editedGameFileQueuesOneReport() throws IOException {
		startRankedRun();
		Dungeon.saveAll();
		editGameFileGold(4242);

		Dungeon.loadGame(SLOT);

		Assertions.assertThat(IntegrityReportQueue.pending()).hasSize(1);
		Assertions.assertThat(IntegrityReportQueue.pending().get(0).heroClass)
				.isEqualTo(HeroClass.WARRIOR.name());
		Assertions.assertThat(IntegrityReportQueue.pending().get(0).saveSlot).isEqualTo(SLOT);
	}

	@Test
	@DisplayName("the mark survives the game saving over the file, and reports only once")
	void markSurvivesResaveWithoutReportingAgain() throws IOException {
		startRankedRun();
		Dungeon.saveAll();
		editGameFileGold(4242);
		Dungeon.loadGame(SLOT);

		// the game now owns the file again and rewrites it normally
		Dungeon.level = new SewerLevel();
		Dungeon.level.create();
		Dungeon.saveAll();
		Dungeon.loadGame(SLOT);

		Assertions.assertThat(SaveIntegrity.isModified()).isTrue();
		Assertions.assertThat(IntegrityReportQueue.pending()).hasSize(1);
	}

	@Test
	@DisplayName("restoring the flag by hand does not clear the mark")
	void handEditedFlagDoesNotClearMark() throws IOException {
		startRankedRun();
		Dungeon.saveAll();
		editGameFileGold(4242);
		Dungeon.loadGame(SLOT);
		Dungeon.level = new SewerLevel();
		Dungeon.level.create();
		Dungeon.saveAll();

		// strip the trailer entirely, the most obvious "reset" a save editor would try
		String path = GamesInProgress.gameFile(SLOT);
		byte[] bytes = FileUtils.getFileHandle(path).readBytes();
		byte[] stripped = new byte[bytes.length - 8];
		System.arraycopy(bytes, 0, stripped, 0, stripped.length);
		FileUtils.getFileHandle(path).writeBytes(stripped, false);

		Dungeon.loadGame(SLOT);

		Assertions.assertThat(SaveIntegrity.isModified()).isTrue();
	}

	@Test
	@DisplayName("a save written before checksums existed is not treated as modified")
	void legacySaveIsNotMarked() throws IOException {
		startRankedRun();
		Dungeon.saveAll();

		// rewrite it the way the old code did: bundle straight to file, no trailer, and
		// forget that this install ever signed the slot — that is what an upgrade looks like
		String path = GamesInProgress.gameFile(SLOT);
		Bundle legacy = readWholeFile(path);
		FileUtils.bundleToFile(path, legacy);
		SPDSettings.slotLayout(0);

		Dungeon.loadGame(SLOT);

		Assertions.assertThat(SaveIntegrity.isModified()).isFalse();
		Assertions.assertThat(IntegrityReportQueue.pending()).isEmpty();
	}

	@Test
	@DisplayName("opening an old save twice without playing is not a detection")
	void legacySaveOpenedTwiceIsNotMarked() throws IOException {
		startRankedRun();
		Dungeon.saveAll();
		String path = GamesInProgress.gameFile(SLOT);
		FileUtils.bundleToFile(path, readWholeFile(path));
		SPDSettings.slotLayout(0);

		// open it, back out to the title, open it again — the game never saved in between
		Dungeon.loadGame(SLOT);
		Dungeon.loadGame(SLOT);

		Assertions.assertThat(SaveIntegrity.isModified()).isFalse();
		Assertions.assertThat(IntegrityReportQueue.pending()).isEmpty();
	}

	@Test
	@DisplayName("a legacy save is signed once the game saves it again")
	void legacySaveIsAdoptedOnNextWrite() throws IOException {
		startRankedRun();
		Dungeon.saveAll();
		String path = GamesInProgress.gameFile(SLOT);
		FileUtils.bundleToFile(path, readWholeFile(path));
		SPDSettings.slotLayout(0);
		Dungeon.loadGame(SLOT);

		Dungeon.level = new SewerLevel();
		Dungeon.level.create();
		Dungeon.saveAll();
		Dungeon.loadGame(SLOT);

		Assertions.assertThat(SaveIntegrity.isModified()).isFalse();
		// and it is now protected: the same edit is caught
		editGameFileGold(7777);
		Dungeon.loadGame(SLOT);
		Assertions.assertThat(SaveIntegrity.isModified()).isTrue();
	}

	@Test
	@DisplayName("editing a floor file marks the run mid-play, and the floor still loads")
	void editedDepthFileMarksRunAndStillLoads() throws IOException {
		startRankedRun();
		Dungeon.saveAll();
		Dungeon.loadGame(SLOT);
		Assertions.assertThat(SaveIntegrity.isModified()).isFalse();

		String depthPath = GamesInProgress.depthFile(SLOT, Dungeon.depth, Dungeon.branch);
		FileUtils.bundleToFile(depthPath, readWholeFile(depthPath));

		Assertions.assertThat(Dungeon.loadLevel(SLOT)).isNotNull();
		Assertions.assertThat(SaveIntegrity.isModified()).isTrue();
	}

	@Test
	@DisplayName("an old save's floor files are adopted too, so revisiting one is not a detection")
	void legacyFloorFilesAreAdoptedWithTheSlot() throws IOException {
		startRankedRun();
		Dungeon.saveAll();

		// an install upgrading from before checksums: nothing in the slot is signed
		String gamePath = GamesInProgress.gameFile(SLOT);
		String depthPath = GamesInProgress.depthFile(SLOT, Dungeon.depth, Dungeon.branch);
		FileUtils.bundleToFile(gamePath, readWholeFile(gamePath));
		FileUtils.bundleToFile(depthPath, readWholeFile(depthPath));
		SPDSettings.slotLayout(0);

		Dungeon.loadGame(SLOT);

		Assertions.assertThat(Dungeon.loadLevel(SLOT)).isNotNull();
		Assertions.assertThat(SaveIntegrity.isModified()).isFalse();
		Assertions.assertThat(IntegrityReportQueue.pending()).isEmpty();
	}

	@Test
	@DisplayName("the mark only silences the run it belongs to")
	void markOnlyAppliesToTheRunItBelongsTo() {
		startRankedRun();
		SaveIntegrity.mark();
		Assertions.assertThat(Dungeon.currentRunModified()).isTrue();

		// the run ends — a mark left behind must not silence anything after it
		Dungeon.hero = null;
		Assertions.assertThat(Dungeon.currentRunModified()).isFalse();

		// and a fresh run starts clean
		startRankedRun();
		Assertions.assertThat(SaveIntegrity.isModified()).isFalse();
		Assertions.assertThat(Dungeon.currentRunModified()).isFalse();
	}

	@Test
	@DisplayName("a marked run captures no echo and no leaderboard row")
	void markedRunCapturesNothing() {
		startRankedRun();
		SaveIntegrity.mark();

		Assertions.assertThat(EchoCaptureTrigger.shouldCapture(5, true)).isFalse();

		SaveIntegrity.reset();
		Assertions.assertThat(EchoCaptureTrigger.shouldCapture(5, true)).isTrue();
	}

	private static Bundle readWholeFile(String path) throws IOException {
		return FileUtils.bundleFromFile(path);
	}

	private static void editGameFileGold(int gold) throws IOException {
		// what a save editor does: decompress, change a value, write it back. The
		// trailing checksum bytes are invisible to anything that just reads the gzip.
		String path = GamesInProgress.gameFile(SLOT);
		Bundle bundle = readWholeFile(path);
		bundle.put("gold", gold);
		FileUtils.bundleToFile(path, bundle);
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
		Dungeon.level = new SewerLevel();
		Dungeon.level.create();
		GamesInProgress.curSlot = SLOT;
	}
}
