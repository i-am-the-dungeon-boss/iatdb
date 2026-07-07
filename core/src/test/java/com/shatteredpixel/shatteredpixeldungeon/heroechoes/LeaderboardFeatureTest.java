package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class LeaderboardFeatureTest {

    @AfterEach
    void cleanup() {
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("EchoLeaderboardEntry maps fight result fields and win rate proxy")
    void leaderboardEntryFromFightResult() {
        EchoFightResult result = new EchoFightResult(
                "snap-1", true, 5, 123L, 846, "WARRIOR", 50, 20, 30);

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

        storage.append(new EchoFightResult("a", true, 5, 1000L, 846, "WARRIOR", 50, 30, 100));
        storage.append(new EchoFightResult("b", false, 5, 1001L, 846, "MAGE", 70, 20, 90));
        storage.append(new EchoFightResult("c", true, 5, 1002L, 846, "ROGUE", 40, 40, 80));

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
                "snap-1", true, 5, 123L, 846, "WARRIOR", 10, 5, 20);

        Assertions.assertThat(record.echoId).isEqualTo("snap-1");
        Assertions.assertThat(record.playerClass).isEqualTo("WARRIOR");
        Assertions.assertThat(record.gameVersion).isEqualTo(846);
    }

    @Test
    @DisplayName("EchoLeaderboardStorage prunes entries beyond cap")
    void capRotationPrunesOldest() throws Exception {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        EchoLeaderboardStorage storage = new EchoLeaderboardStorage();
        for (int i = 0; i < 205; i++) {
            storage.append(new EchoFightResult(
                    "id-" + i, true, 5, i, 846, "WARRIOR", i, 0, 10));
        }

        Assertions.assertThat(storage.loadTop(300).size()).isLessThanOrEqualTo(200);
        Assertions.assertThat(storage.loadTop(1).get(0).timestamp).isGreaterThan(150);
    }

    @Test
    @DisplayName("EchoLeaderboardStorage ignores corrupted leaderboard files")
    void ignoresCorruptedFile() throws Exception {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        try (FileWriter writer = new FileWriter(EchoPlayModePaths.leaderboardFile())) {
            writer.write("this is not,a,valid,leaderboard\n");
        }

        EchoLeaderboardStorage storage = new EchoLeaderboardStorage();
        Assertions.assertThat(storage.loadTop(10)).isEmpty();
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
        Assertions.assertThat(new File("leaderboard-ranked.json").exists()).isFalse();
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
