package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class Depth5EchoBossSelectionTest {

    @AfterEach
    void reset() {
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("Depth 5 keeps SewerBossLevel; prefetch activates echo when snapshot available")
    void depth5KeepsSewerBossLevelWhenEchoAvailable() {
        EchoStorage storage = new EchoStorage();
        storage.save(EchoTestSupport.warriorEchoWithData(5));
        CompositeEchoLookup.setEchoLookupForTests(storage);

        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(SewerBossLevel.class);
        Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isTrue();
        Assertions.assertThat(Dungeon.getPendingEcho()).isNotNull();
        Assertions.assertThat(Dungeon.isEchoBossActive()).isTrue();
        Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isTrue();
    }

    @Test
    @DisplayName("Depth 5 createRegionalBoss returns echo instead of Goo when pending")
    void depth5CreateRegionalBossReturnsEchoInsteadOfGoo() {
        Echo echo = EchoTestSupport.warriorEchoWithData(5);
        EchoStorage storage = new EchoStorage();
        storage.save(echo);
        CompositeEchoLookup.setEchoLookupForTests(storage);
        Dungeon.depth = 5;
        Dungeon.prefetchEchoBossForDepth(5);

        Mob result = EchoBossSpawner.createRegionalBoss(new Goo());

        Assertions.assertThat(result).isInstanceOf(EchoBoss.class);
        Assertions.assertThat(((EchoBoss) result).getEcho().echoId).isEqualTo(echo.echoId);
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
    @DisplayName("Corrupted snapshot prefetch falls back without activating echo")
    void corruptedSnapshotFallsBackToDefaultBoss() {
        CompositeEchoLookup.setEchoLookupForTests(depth -> EchoLookupOutcome.error());

        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(SewerBossLevel.class);
        Assertions.assertThat(Dungeon.prefetchEchoBossOutcome(5).isError()).isTrue();
        Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
        Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
    }

    @Test
    @DisplayName("Save and restore preserves echo boss choice on sewer boss floor")
    void saveRestorePreservesBossChoice() {
        EchoStorage storage = new EchoStorage();
        Echo snap = EchoTestSupport.warriorEchoWithData(5);
        storage.save(snap);
        CompositeEchoLookup.setEchoLookupForTests(storage);
        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(SewerBossLevel.class);
        Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isTrue();

        Bundle bundle = new Bundle();
        Dungeon.storeEchoChoiceInBundle(bundle);

        EchoTestSupport.resetWorkflowState();
        CompositeEchoLookup.setEchoLookupForTests(depth -> EchoLookupOutcome.notFound());

        Dungeon.restoreEchoChoiceFromBundle(bundle);

        Assertions.assertThat(Dungeon.isEchoBossActive()).isTrue();
        Assertions.assertThat(Dungeon.getPendingEcho()).isNotNull();
        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(SewerBossLevel.class);
        Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isTrue();
    }
}
