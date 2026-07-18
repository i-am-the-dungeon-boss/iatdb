package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end workflow: boss victory snapshot -> depth-5 routing -> echo boss
 * spawn inside SewerBossLevel.
 */
@ExtendWith(GdxTestExtension.class)
class EndToEndWorkflowTest {

    @AfterEach
    void cleanup() {
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("Boss victory snapshot enables echo boss on next depth-5 visit")
    void bossEchoEnablesEchoBossOnNextVisit() {
        EchoStorage storage = new EchoStorage();
        EchoCaptureTrigger.saveEcho(
                EchoTestSupport.warriorEchoWithData(5), 5, storage);

        CompositeEchoLookup.setEchoLookupForTests(storage);
        Dungeon.depth = 5;
        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(SewerBossLevel.class);
        Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isTrue();
        Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isTrue();

        Echo pending = Dungeon.getPendingEcho();
        Assertions.assertThat(pending).isNotNull();

        Mob boss = EchoBossSpawner.createRegionalBoss(new Goo());
        Assertions.assertThat(boss).isInstanceOf(EchoBoss.class);
        Assertions.assertThat(((EchoBoss) boss).getEcho()).isEqualTo(pending);
        Assertions.assertThat(boss.HT).isGreaterThan(pending.ht);
    }

    @Test
    @DisplayName("Workflow falls back to default boss when no snapshots exist")
    void workflowFallsBackWithoutSnapshots() {
        CompositeEchoLookup.setEchoLookupForTests(new EchoStorage());

        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(SewerBossLevel.class);
        Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isFalse();
        Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
    }
}
