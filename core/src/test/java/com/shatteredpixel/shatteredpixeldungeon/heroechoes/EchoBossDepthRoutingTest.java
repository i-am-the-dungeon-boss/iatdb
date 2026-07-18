package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel;
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
    @DisplayName("All boss depths resolve pending echo when snapshot exists")
    void allBossDepthsResolvePendingEcho() {
        EchoStorage storage = new EchoStorage();

        for (int depth : EchoReplacementDecider.BOSS_DEPTHS) {
            EchoTestSupport.resetWorkflowState();
            storage.save(EchoTestSupport.warriorEchoWithData(depth));
            CompositeEchoLookup.setEchoLookupForTests(storage);

            Dungeon.levelClassForDepth(depth, 0);

            Assertions.assertThat(Dungeon.getPendingEcho())
                    .as("depth %d", depth)
                    .isNotNull();
            Assertions.assertThat(Dungeon.isEchoBossActive()).isTrue();
        }
    }

    @Test
    @DisplayName("Depth 5 routes to EchoBossLevel; other boss depths keep regional boss levels")
    void depthFiveUsesHeroicLevelOthersKeepRegional() {
        EchoStorage storage = new EchoStorage();
        storage.save(EchoTestSupport.warriorEchoWithData(5));
        CompositeEchoLookup.setEchoLookupForTests(storage);

        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(EchoBossLevel.class);

        EchoTestSupport.resetWorkflowState();
        storage = new EchoStorage();
        storage.save(EchoTestSupport.warriorEchoWithData(10));
        CompositeEchoLookup.setEchoLookupForTests(storage);

        Assertions.assertThat(Dungeon.levelClassForDepth(10, 0)).isEqualTo(PrisonBossLevel.class);
        Assertions.assertThat(Dungeon.isEchoBossActive()).isTrue();
    }
}
