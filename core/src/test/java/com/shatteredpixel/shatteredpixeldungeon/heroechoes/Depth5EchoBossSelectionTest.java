package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class Depth5EchoBossSelectionTest {

    @AfterEach
    void reset() {
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("Depth 5 uses default boss level when no echo is available")
    void depth5UsesDefaultBossWithoutEcho() {
        CompositeEchoLookup.setEchoLookupForTests(depth -> Optional.empty());

        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(SewerBossLevel.class);
        Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
        Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
    }

    @Test
    @DisplayName("Depth 5 uses EchoBossLevel when snapshot available")
    void depth5UsesEchoBossLevelWhenAvailable() {
        EchoStorage storage = new EchoStorage();
        storage.save(EchoTestSupport.warriorEchoWithData(5));
        CompositeEchoLookup.setEchoLookupForTests(storage);

        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(EchoBossLevel.class);
        Assertions.assertThat(Dungeon.getPendingEcho()).isNotNull();
    }

    @Test
    @DisplayName("resolveEcho stores pending snapshot for the boss room")
    void resolveEchoStoresPendingSnapshot() {
        EchoStorage storage = new EchoStorage();
        storage.save(EchoTestSupport.warriorEchoWithData(5));
        CompositeEchoLookup.setEchoLookupForTests(storage);

        Echo resolved = Dungeon.resolveEcho(5);

        Assertions.assertThat(resolved).isNotNull();
        Assertions.assertThat(Dungeon.getPendingEcho()).isEqualTo(resolved);
        Assertions.assertThat(Dungeon.getPendingEchoPolicy()).isNotNull();
    }

    @Test
    @DisplayName("Corrupted snapshot lookup falls back to default boss level")
    void corruptedSnapshotFallsBackToDefaultBoss() {
        CompositeEchoLookup.setEchoLookupForTests(depth -> {
            throw new RuntimeException("corrupt snapshot");
        });

        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(SewerBossLevel.class);
        Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
    }

    @Test
    @DisplayName("Save and restore preserves hero boss choice on boss floor")
    void saveRestorePreservesBossChoice() {
        EchoStorage storage = new EchoStorage();
        Echo snap = EchoTestSupport.warriorEchoWithData(5);
        storage.save(snap);
        CompositeEchoLookup.setEchoLookupForTests(storage);
        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(EchoBossLevel.class);

        Bundle bundle = new Bundle();
        Dungeon.storeEchoChoiceInBundle(bundle);

        EchoTestSupport.resetWorkflowState();
        CompositeEchoLookup.setEchoLookupForTests(depth -> Optional.empty());

        Dungeon.restoreEchoChoiceFromBundle(bundle);

        Assertions.assertThat(Dungeon.isEchoBossActive()).isTrue();
        Assertions.assertThat(Dungeon.getPendingEcho()).isNotNull();
        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(EchoBossLevel.class);
    }
}
