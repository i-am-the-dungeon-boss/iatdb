package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class LevelRoutingTest {

    @AfterEach
    void reset() {
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("Depths 1-4 are SewerLevel; depth 5 is SewerBossLevel without a saved echo")
    void routingForFirstFiveDepths() {
        Assertions.assertThat(Dungeon.levelClassForDepth(1, 0)).isEqualTo(SewerLevel.class);
        Assertions.assertThat(Dungeon.levelClassForDepth(2, 0)).isEqualTo(SewerLevel.class);
        Assertions.assertThat(Dungeon.levelClassForDepth(3, 0)).isEqualTo(SewerLevel.class);
        Assertions.assertThat(Dungeon.levelClassForDepth(4, 0)).isEqualTo(SewerLevel.class);
        Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(SewerBossLevel.class);
    }
}
