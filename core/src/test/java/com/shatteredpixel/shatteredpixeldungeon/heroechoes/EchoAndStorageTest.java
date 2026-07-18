package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoAndStorageTest {

    @AfterEach
    void cleanup() {
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("fromHero/create captures class, level, hp, and optional hero bundle")
    void fromHeroCapturesRequiredFields() {
        Bundle echoData = new Bundle();
        echoData.put("lvl", 6);

        Echo snap = Echo.create(
                5, EchoTestSupport.TEST_GAME_VERSION, 999L,
                "WARRIOR", 6, 28, 30, echoData);

        Assertions.assertThat(snap.depth).isEqualTo(5);
        Assertions.assertThat(snap.heroClass).isEqualTo("WARRIOR");
        Assertions.assertThat(snap.lvl).isEqualTo(6);
        Assertions.assertThat(snap.hp).isEqualTo(28);
        Assertions.assertThat(snap.ht).isEqualTo(30);
        Assertions.assertThat(snap.gameSeed).isEqualTo(999L);
        Assertions.assertThat(snap.echoData).isNotNull();
        Assertions.assertThat(snap.echoData.getInt("lvl")).isEqualTo(6);
    }

    @Test
    @DisplayName("Bundle round-trip preserves snapshot metadata")
    void bundleRoundTripPreservesFields() {
        Echo original = EchoTestSupport.warriorEcho(5);

        Bundle bundle = original.toBundle();
        Echo restored = Echo.fromBundle(bundle);

        Assertions.assertThat(restored.echoId).isEqualTo(original.echoId);
        Assertions.assertThat(restored.depth).isEqualTo(5);
        Assertions.assertThat(restored.heroClass).isEqualTo("WARRIOR");
        Assertions.assertThat(restored.lvl).isEqualTo(original.lvl);
        Assertions.assertThat(restored.gameVersion).isEqualTo(EchoTestSupport.TEST_GAME_VERSION);
    }

    @Test
    @DisplayName("isCompatibleWith accepts same major version only")
    void versionCompatibilityUsesMajorVersion() {
        Echo snap = EchoTestSupport.warriorEcho(5);
        snap.gameVersion = "0.0.1";

        Assertions.assertThat(snap.isCompatibleWith("0.0.5")).isTrue();
        Assertions.assertThat(snap.isCompatibleWith("1.0.0")).isFalse();
    }

    @Test
    @DisplayName("EchoStorage save and loadForDepth round-trip a snapshot")
    void saveAndLoadForDepth() {
        EchoStorage storage = new EchoStorage();
        Echo snap = EchoTestSupport.warriorEcho(5);

        storage.save(snap);

        Optional<Echo> loaded = storage.loadForDepth(
                5, EchoTestSupport.TEST_GAME_VERSION);
        Assertions.assertThat(loaded).isPresent();
        Assertions.assertThat(loaded.get().depth).isEqualTo(5);
        Assertions.assertThat(loaded.get().heroClass).isEqualTo("WARRIOR");
    }

    @Test
    @DisplayName("EchoStorage findEchoForDepth requires persisted echo policy")
    void findEchoForDepthRequiresPolicy() throws Exception {
        EchoStorage storage = new EchoStorage();
        Echo snap = EchoTestSupport.warriorEcho(5);
        storage.save(snap);

        Assertions.assertThat(storage.findEchoForDepth(5).isFound()).isTrue();
        Assertions.assertThat(storage.findEchoForDepth(5).result.policy).isNotNull();

        // overwrite with echo-only file (no policy)
        java.nio.file.Path path = java.nio.file.Path.of(
                EchoPlayModePaths.echoesDir(), "depth-5.dat");
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(path.toFile())) {
            com.watabou.utils.Bundle.write(snap.toFileBundle(), fos);
        }

        Assertions.assertThat(storage.findEchoForDepth(5).isNotFound()).isTrue();
    }

    @Test
    @DisplayName("EchoStorage skips incompatible versions on load")
    void skipsIncompatibleVersionOnLoad() {
        EchoStorage storage = new EchoStorage();
        Echo old = EchoTestSupport.echoWithVersion(5, "1.0.0");
        storage.save(old);

        Optional<Echo> loaded = storage.loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION);
        Assertions.assertThat(loaded).isEmpty();
    }

    @Test
    @DisplayName("EchoStorage getEchoesDir creates the echoes folder")
    void getEchoesDirCreatesFolder() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        EchoTestSupport.deleteRecursively(new File(EchoPlayModePaths.echoesDir()));

        File dir = EchoStorage.getEchoesDir();

        Assertions.assertThat(dir.exists()).isTrue();
        Assertions.assertThat(dir.isDirectory()).isTrue();
    }

    @Test
    @DisplayName("EchoStorage keeps only one echo per boss depth")
    void keepsOnlyOneEchoPerDepth() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        EchoStorage storage = new EchoStorage();
        for (int i = 0; i < EchoStorage.MAX_ECHOES_PER_DEPTH + 5; i++) {
            Echo snap = EchoTestSupport.warriorEcho(5);
            snap.echoId = "snap-" + i;
            snap.timestamp = 1_000L + i;
            storage.save(snap);
        }

        File dir = EchoStorage.getEchoesDir();
        File[] depthFiles = dir.listFiles((d, name) -> name.equals("depth-5.dat") || name.startsWith("depth-5-"));

        Assertions.assertThat(depthFiles).isNotNull();
        Assertions.assertThat(depthFiles.length).isEqualTo(1);
        Assertions.assertThat(depthFiles[0].getName()).isEqualTo("depth-5.dat");

        Optional<Echo> loaded = storage.loadForDepth(
                5, EchoTestSupport.TEST_GAME_VERSION);
        Assertions.assertThat(loaded).isPresent();
        Assertions.assertThat(loaded.get().echoId).isEqualTo("snap-5");
    }

    @Test
    @DisplayName("saving a new echo replaces the previous echo for that depth")
    void saveReplacesExistingEchoForSameDepth() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        EchoStorage storage = new EchoStorage();

        Echo first = EchoTestSupport.warriorEcho(5);
        first.echoId = "first";
        first.timestamp = 1_000L;
        storage.save(first);

        Echo second = EchoTestSupport.warriorEcho(5);
        second.echoId = "second";
        second.timestamp = 2_000L;
        storage.save(second);

        Optional<Echo> loaded = storage.loadForDepth(
                5, EchoTestSupport.TEST_GAME_VERSION);

        Assertions.assertThat(loaded).isPresent();
        Assertions.assertThat(loaded.get().echoId).isEqualTo("second");
    }
}
