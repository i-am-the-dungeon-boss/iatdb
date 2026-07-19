package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoClient;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoClientTest;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineSettings;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineSync;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.watabou.utils.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

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
    @DisplayName("ranked capture uploads to backend and writes no local echo file")
    void rankedCaptureUploadsWithoutLocalFile() throws Exception {
        Dungeon.echoPlayMode = EchoPlayMode.RANKED;
        EchoClientTest.FakeEchoHttpTransport transport = new EchoClientTest.FakeEchoHttpTransport();
        transport.enqueue(201, "{}");
        EchoOnlineSettings.setOnlineEnabled(true);
        EchoOnlineSettings.setBackendUrl("https://echo.test");
        EchoOnlineSettings.setApiKey("secret");
        EchoOnlineSync sync = new EchoOnlineSync(
                new EchoClient("https://echo.test", "secret", transport));
        EchoOnlineSync.setDefaultForTests(sync);

        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);

        EchoCaptureTrigger.captureBossVictory(hero, 5, new EchoStorage());
        sync.awaitBackgroundTasksForTests();

        Assertions.assertThat(transport.requests).hasSize(1);
        Assertions.assertThat(transport.requests.get(0).url).endsWith("/v1/echoes");
        Assertions.assertThat(transport.requests.get(0).body).contains("\"depth\":5");
        Assertions.assertThat(FileUtils.fileExists("echoes-ranked/depth-5.dat")).isFalse();
        Assertions.assertThat(EchoTestSupport.countEchoFiles()).isZero();
    }

    @Test
    @DisplayName("solo onBossDefeated snapshots the living hero into echoes-solo")
    void soloOnBossDefeatedSnapshotsLivingHero() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        Dungeon.depth = 5;
        Dungeon.hero = livingWarriorWithPlate();

        EchoCaptureTrigger.onBossDefeated();

        Echo loaded = new EchoStorage().loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION).orElseThrow();
        Assertions.assertThat(new File("echoes-solo/depth-5.dat")).exists();
        Assertions.assertThat(loaded.heroClass).isEqualTo("WARRIOR");
        Assertions.assertThat(EchoHeroSnapshot.restoreHero(loaded).belongings.armor())
                .isInstanceOf(PlateArmor.class);
    }

    @Test
    @DisplayName("solo capture writes policy so findEchoForDepth can load the boss echo")
    void soloCaptureWritesPolicyForLookup() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        Dungeon.depth = 5;
        Dungeon.hero = livingWarriorWithPlate();

        EchoCaptureTrigger.onBossDefeated();

        var outcome = new EchoStorage().findEchoForDepth(5);
        Assertions.assertThat(outcome.isFound()).isTrue();
        Assertions.assertThat(outcome.result.policy).isNotNull();
        Assertions.assertThat(outcome.result.echo.hasCombatData()).isTrue();
    }

    private static Hero livingWarriorWithPlate() {
        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        hero.lvl = 12;
        hero.HP = hero.HT = 80;
        PlateArmor armor = new PlateArmor();
        armor.identify();
        hero.belongings.armor = armor;
        return hero;
    }
}
