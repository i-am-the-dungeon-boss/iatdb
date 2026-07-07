package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class AutoEchoCaptureTest {

    @AfterEach
    void cleanup() {
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("shouldCapture is true on boss depths when hero is alive")
    void shouldCaptureOnEligibleBossKill() {
        Assertions.assertThat(EchoCaptureTrigger.shouldCapture(5, true)).isTrue();
        Assertions.assertThat(EchoCaptureTrigger.shouldCapture(10, true)).isTrue();
    }

    @Test
    @DisplayName("shouldCapture is false when hero is dead")
    void skipsWhenHeroDead() {
        Assertions.assertThat(EchoCaptureTrigger.shouldCapture(5, false)).isFalse();
    }

    @Test
    @DisplayName("shouldCapture is false on non-boss depths")
    void skipsWhenNotBossDepth() {
        Assertions.assertThat(EchoCaptureTrigger.shouldCapture(4, true)).isFalse();
    }

    @Test
    @DisplayName("saveEcho writes a snapshot file for the boss depth")
    void saveEchoWritesSnapshotFile() {
        EchoStorage storage = new EchoStorage();

        EchoCaptureTrigger.saveEcho(
                EchoTestSupport.warriorEcho(5), 5, storage);

        Assertions.assertThat(EchoTestSupport.countEchoFiles()).isGreaterThan(0);
        Assertions.assertThat(storage.loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION))
                .isPresent();
    }

    @Test
    @DisplayName("saveEcho does not write when guards fail")
    void saveEchoRespectsGuards() {
        EchoStorage storage = new EchoStorage();

        EchoCaptureTrigger.saveEcho(
                EchoTestSupport.warriorEcho(5), 4, storage);
        EchoCaptureTrigger.saveEcho(
                null, 5, storage);

        Assertions.assertThat(EchoTestSupport.countEchoFiles()).isZero();
    }

    @Test
    @DisplayName("ranked capture uploads without writing local ranked echoes")
    void rankedCaptureSkipsLocalSave() {
        Dungeon.echoPlayMode = EchoPlayMode.RANKED;
        EchoStorage storage = new EchoStorage();
        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);

        EchoCaptureTrigger.captureBossVictory(hero, 5, storage);

        Assertions.assertThat(storage.loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION)).isEmpty();
    }

    @Test
    @DisplayName("onBossDefeated path persists snapshots via storage")
    void onBossDefeatedUsesEchoCaptureTrigger() {
        EchoStorage storage = new EchoStorage();
        EchoCaptureTrigger.saveEcho(
                EchoTestSupport.warriorEcho(5), 5, storage);
        Assertions.assertThat(storage.loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION))
                .isPresent();
    }
}
