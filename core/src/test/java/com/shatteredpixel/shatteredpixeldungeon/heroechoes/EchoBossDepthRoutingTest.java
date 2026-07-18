package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoBossDepthRoutingTest {

    @AfterEach
    void reset() {
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("All boss depths resolve pending echo via prefetch when snapshot exists")
    void allBossDepthsResolvePendingEcho() {
        EchoStorage storage = new EchoStorage();

        for (int depth : EchoReplacementDecider.BOSS_DEPTHS) {
            EchoTestSupport.resetWorkflowState();
            storage.save(EchoTestSupport.warriorEchoWithData(depth));
            CompositeEchoLookup.setEchoLookupForTests(storage);

            Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(depth))
                    .as("depth %d", depth)
                    .isTrue();
            Assertions.assertThat(Dungeon.getPendingEcho())
                    .as("depth %d", depth)
                    .isNotNull();
            Assertions.assertThat(Dungeon.isEchoBossActive()).isTrue();
        }
    }
}
