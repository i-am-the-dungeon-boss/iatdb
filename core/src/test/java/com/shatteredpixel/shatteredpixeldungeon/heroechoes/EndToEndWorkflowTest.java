package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoBossLevel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * End-to-end workflow: boss victory snapshot -> depth-5 routing -> heroic boss
 * creation.
 */
@ExtendWith(GdxTestExtension.class)
class EndToEndWorkflowTest {

    @AfterEach
    void cleanup() {
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("Boss victory snapshot enables heroic boss on next depth-5 visit")
    void bossEchoEnablesEchoBossOnNextVisit() {
        EchoStorage storage = new EchoStorage();
        EchoCaptureTrigger.saveEcho(
                EchoTestSupport.warriorEchoWithData(5), 5, storage);

        CompositeEchoLookup.setEchoLookupForTests(storage);
        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(EchoBossLevel.class);

        Echo pending = Dungeon.getPendingEcho();
        Assertions.assertThat(pending).isNotNull();

        EchoBoss boss = new EchoBoss(pending, 5);
        Assertions.assertThat(boss.getEcho()).isEqualTo(pending);
        Assertions.assertThat(boss.HT).isGreaterThan(pending.ht);
    }

    @Test
    @DisplayName("Workflow falls back to default boss when no snapshots exist")
    void workflowFallsBackWithoutSnapshots() {
        CompositeEchoLookup.setEchoLookupForTests(new EchoStorage());

        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0))
                .isEqualTo(com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel.class);
        Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
    }
}
