package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.badlogic.gdx.Files;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.watabou.utils.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class LeaderboardFeatureTest {

    @AfterEach
    void cleanup() {
        FileUtils.setDefaultFileProperties(Files.FileType.Local, "");
        EchoTestSupport.deleteRecursively(new File("app-files"));
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("EchoLeaderboardEntry maps fight result fields and win rate proxy")
    void leaderboardEntryFromFightResult() {
        EchoFightResult result = new EchoFightResult(
                "snap-1", true, 5, 123L, "0.0.1", "WARRIOR", 50, 20, 30);

        EchoLeaderboardEntry entry = EchoLeaderboardEntry.fromFightResult(result, 2);

        Assertions.assertThat(entry.rank).isEqualTo(2);
        Assertions.assertThat(entry.echoId).isEqualTo("snap-1");
        Assertions.assertThat(entry.bossWin).isTrue();
        Assertions.assertThat(entry.depth).isEqualTo(5);
        Assertions.assertThat(entry.playerClass).isEqualTo("WARRIOR");
        Assertions.assertThat(entry.damageDealt).isEqualTo(50);
        Assertions.assertThat(entry.damageTaken).isEqualTo(20);
        Assertions.assertThat(entry.turns).isEqualTo(30);
        Assertions.assertThat(entry.winRateProxy).isEqualTo(1f);
    }

    @Test
    @DisplayName("EchoLeaderboardStorage appends and loads top entries by win first, then damage dealt desc")
    void appendsAndLoadsTop() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        EchoLeaderboardStorage storage = new EchoLeaderboardStorage();

        storage.append(new EchoFightResult("a", true, 5, 1000L, "0.0.1", "WARRIOR", 50, 30, 100));
        storage.append(new EchoFightResult("b", false, 5, 1001L, "0.0.1", "MAGE", 70, 20, 90));
        storage.append(new EchoFightResult("c", true, 5, 1002L, "0.0.1", "ROGUE", 40, 40, 80));

        List<EchoFightResult> top = storage.loadTop(2);
        Assertions.assertThat(top).hasSize(2);
        Assertions.assertThat(top.get(0).bossWin).isTrue();
        Assertions.assertThat(top.get(1).bossWin).isTrue();
        Assertions.assertThat(top.get(0).damageDealt).isGreaterThanOrEqualTo(top.get(1).damageDealt);
    }

    @Test
    @DisplayName("EchoFightResult stores snapshot id, player class, and game version")
    void recordStoresExtendedMetadata() {
        EchoFightResult record = new EchoFightResult(
                "snap-1", true, 5, 123L, "0.0.1", "WARRIOR", 10, 5, 20);

        Assertions.assertThat(record.echoId).isEqualTo("snap-1");
        Assertions.assertThat(record.playerClass).isEqualTo("WARRIOR");
        Assertions.assertThat(record.gameVersion).isEqualTo("0.0.1");
    }

    @Test
    @DisplayName("EchoLeaderboardStorage prunes entries beyond cap")
    void capRotationPrunesOldest() throws Exception {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        EchoLeaderboardStorage storage = new EchoLeaderboardStorage();
        for (int i = 0; i < 205; i++) {
            storage.append(new EchoFightResult(
                    "id-" + i, true, 5, i, "0.0.1", "WARRIOR", i, 0, 10));
        }

        Assertions.assertThat(storage.loadTop(300).size()).isLessThanOrEqualTo(200);
        Assertions.assertThat(storage.loadTop(1).get(0).timestamp).isGreaterThan(150);
    }

    @Test
    @DisplayName("EchoLeaderboardStorage ignores corrupted leaderboard files")
    void ignoresCorruptedFile() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        FileUtils.getFileHandle(EchoPlayModePaths.leaderboardFile())
                .writeBytes("this is not,a,valid,leaderboard\n".getBytes(), false);

        EchoLeaderboardStorage storage = new EchoLeaderboardStorage();
        Assertions.assertThat(storage.loadTop(10)).isEmpty();
    }

    @Test
    @DisplayName("EchoLeaderboardStorage skips legacy rows that omit player class")
    void skipsLegacyRowsWithoutPlayerClass() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        FileUtils.getFileHandle(EchoPlayModePaths.leaderboardFile())
                .writeBytes("true,5,1000,10,5,20\nid-1,true,5,1001,0.0.1,WARRIOR,40,12,8\n"
                        .getBytes(), false);

        EchoLeaderboardStorage storage = new EchoLeaderboardStorage();
        List<EchoFightResult> top = storage.loadTop(10);

        Assertions.assertThat(top).hasSize(1);
        Assertions.assertThat(top.get(0).playerClass).isEqualTo("WARRIOR");
    }

    @Test
    @DisplayName("EchoFightRecorder rejects null player class")
    void fightRecorderRejectsNullPlayerClass() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        EchoFightRecorder recorder = new EchoFightRecorder(new EchoLeaderboardStorage());

        Assertions.assertThatThrownBy(() -> recorder.recordBossVictory(
                EchoTestSupport.warriorEcho(5), 5, null, "0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("player_class");
    }

    @Test
    @DisplayName("solo default boss kill records killer on local leaderboard")
    void soloDefaultBossKillRecordsKillerOnLocalLeaderboard() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        Dungeon.depth = 5;
        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);

        EchoCaptureTrigger.onBossDefeated();

        List<EchoFightResult> top = new EchoLeaderboardStorage().loadTop(10);
        Assertions.assertThat(top).hasSize(1);
        Assertions.assertThat(top.get(0).bossWin).isFalse();
        Assertions.assertThat(top.get(0).echoId).isNull();
        Assertions.assertThat(top.get(0).playerClass).isEqualTo("WARRIOR");
        Assertions.assertThat(top.get(0).depth).isEqualTo(5);
    }

    @Test
    @DisplayName("echo boss floor does not double-record leaderboard via onBossDefeated")
    void echoBossFloorDoesNotDoubleRecordViaOnBossDefeated() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        Dungeon.depth = 5;
        Echo local = EchoTestSupport.warriorEchoWithData(5);
        CompositeEchoLookup.setEchoLookupForTests(d -> EchoTestSupport.outcomeWithPolicy(local));
        Assertions.assertThat(Dungeon.prefetchEchoBossOutcome(5).isFound()).isTrue();

        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.MAGE.initHero(hero);

        EchoCaptureTrigger.onBossDefeated();

        Assertions.assertThat(new EchoLeaderboardStorage().loadTop(10)).isEmpty();
    }

    @Test
    @DisplayName("solo leaderboard save uses FileUtils local storage not process cwd")
    void soloLeaderboardSaveUsesFileUtilsLocalStorageNotProcessCwd() {
        FileUtils.setDefaultFileProperties(Files.FileType.Local, "app-files/");
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;

        EchoLeaderboardStorage storage = new EchoLeaderboardStorage();
        storage.append(new EchoFightResult(
                "solo-1", true, 5, 1000L, "0.0.1", "WARRIOR", 50, 10, 20));

        Assertions.assertThat(FileUtils.fileExists("leaderboard-solo.json")).isTrue();
        Assertions.assertThat(new File("leaderboard-solo.json")).doesNotExist();
        Assertions.assertThat(storage.loadTop(1)).hasSize(1);
        Assertions.assertThat(storage.loadTop(1).get(0).echoId).isEqualTo("solo-1");
    }

    @Test
    @DisplayName("EchoFightRecorder skips local storage in ranked mode")
    void fightRecorderSkipsLocalStorageInRankedMode() {
        Dungeon.echoPlayMode = EchoPlayMode.RANKED;
        EchoLeaderboardStorage storage = new EchoLeaderboardStorage();
        EchoFightRecorder recorder = new EchoFightRecorder(storage);
        Echo snap = EchoTestSupport.warriorEcho(5);

        recorder.recordBossVictory(snap, 5, HeroClass.MAGE, EchoTestSupport.TEST_GAME_VERSION);

        Assertions.assertThat(storage.loadTop(10)).isEmpty();
        Assertions.assertThat(FileUtils.fileExists("leaderboard-ranked.json")).isFalse();
    }

    @Test
    @DisplayName("EchoFightRecorder records boss win and hero defeat outcomes")
    void fightRecorderRecordsOutcomes() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        EchoLeaderboardStorage storage = new EchoLeaderboardStorage();
        EchoFightRecorder recorder = new EchoFightRecorder(storage);
        Echo snap = EchoTestSupport.warriorEcho(5);

        recorder.trackDamageDealt(40);
        recorder.trackDamageTaken(15);
        recorder.trackTurn();
        recorder.recordBossVictory(snap, 5, HeroClass.MAGE, EchoTestSupport.TEST_GAME_VERSION);

        recorder.trackDamageDealt(60);
        recorder.recordBossDefeat(snap, 5, HeroClass.MAGE, EchoTestSupport.TEST_GAME_VERSION);

        List<EchoFightResult> entries = storage.loadTop(10);
        Assertions.assertThat(entries).hasSize(2);
        Assertions.assertThat(entries.stream().filter(r -> r.bossWin).count()).isEqualTo(1);
        Assertions.assertThat(entries.stream().filter(r -> !r.bossWin).count()).isEqualTo(1);
        Assertions.assertThat(entries.get(0).echoId).isEqualTo(snap.echoId);
    }
}
