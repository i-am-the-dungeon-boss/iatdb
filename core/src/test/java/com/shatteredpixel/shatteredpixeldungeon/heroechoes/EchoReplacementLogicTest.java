package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class BossReplacementLogicTest {

    @AfterEach
    void cleanup() {
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("Uses hero boss when snapshot available on boss depth")
    void usesHeroBossWhenSnapshotAvailable() {
        CompositeEchoLookup.setEchoLookupForTests(depth -> EchoTestSupport
                .outcomeWithPolicy(EchoTestSupport.warriorEchoWithData(depth)));

        boolean should = Dungeon.prefetchEchoBossForDepth(5);

        Assertions.assertThat(should).isTrue();
        Assertions.assertThat(Dungeon.getPendingEcho()).isNotNull();
    }

    @Test
    @DisplayName("Metadata-only echo does not replace boss")
    void metadataOnlyEchoDoesNotReplaceBoss() {
        CompositeEchoLookup.setEchoLookupForTests(depth -> EchoTestSupport
                .outcomeWithPolicy(EchoTestSupport.warriorEcho(depth)));

        boolean should = Dungeon.prefetchEchoBossForDepth(5);

        Assertions.assertThat(should).isFalse();
        Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
    }

    @Test
    @DisplayName("Falls back when no snapshot available")
    void fallsBackWhenNoSnapshotAvailable() {
        CompositeEchoLookup.setEchoLookupForTests(depth -> EchoLookupOutcome.notFound());

        boolean should = Dungeon.prefetchEchoBossForDepth(5);

        Assertions.assertThat(should).isFalse();
    }

    @Test
    @DisplayName("Non-boss depths never use hero boss even if snapshots exist")
    void nonBossDepthsNeverUseHeroBoss() {
        CompositeEchoLookup.setEchoLookupForTests(depth -> EchoTestSupport
                .outcomeWithPolicy(EchoTestSupport.warriorEchoWithData(depth)));

        boolean should = Dungeon.prefetchEchoBossForDepth(4);

        Assertions.assertThat(should).isFalse();
        Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
    }

    @Test
    @DisplayName("All boss depths 5/10/15/20/25 are recognized")
    void allBossDepthsRecognized() {
        for (int depth : EchoReplacementDecider.BOSS_DEPTHS) {
            Assertions.assertThat(EchoReplacementDecider.isBossDepth(depth)).isTrue();
        }
        Assertions.assertThat(EchoReplacementDecider.isBossDepth(6)).isFalse();
    }
}
