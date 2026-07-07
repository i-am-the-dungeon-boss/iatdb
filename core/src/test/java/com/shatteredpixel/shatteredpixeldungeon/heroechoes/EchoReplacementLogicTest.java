package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class BossReplacementLogicTest {

    @Test
    @DisplayName("Uses hero boss when snapshot available on boss depth")
    void usesHeroBossWhenSnapshotAvailable() {
        EchoReplacementDecider.EchoLookup lookup =
                depth -> Optional.of(EchoTestSupport.warriorEchoWithData(depth));
        boolean should = EchoReplacementDecider.shouldUseEchoBoss(5, lookup);
        Assertions.assertThat(should).isTrue();
    }

    @Test
    @DisplayName("Metadata-only echo does not replace boss")
    void metadataOnlyEchoDoesNotReplaceBoss() {
        EchoReplacementDecider.EchoLookup lookup =
                depth -> Optional.of(EchoTestSupport.warriorEcho(depth));
        boolean should = EchoReplacementDecider.shouldUseEchoBoss(5, lookup);
        Assertions.assertThat(should).isFalse();
    }

    @Test
    @DisplayName("Falls back when no snapshot available")
    void fallsBackWhenNoSnapshotAvailable() {
        EchoReplacementDecider.EchoLookup lookup = depth -> Optional.empty();
        boolean should = EchoReplacementDecider.shouldUseEchoBoss(5, lookup);
        Assertions.assertThat(should).isFalse();
    }

    @Test
    @DisplayName("Non-boss depths never use hero boss even if snapshots exist")
    void nonBossDepthsNeverUseHeroBoss() {
        EchoReplacementDecider.EchoLookup lookup =
                depth -> Optional.of(EchoTestSupport.warriorEcho(depth));
        boolean should = EchoReplacementDecider.shouldUseEchoBoss(4, lookup);
        Assertions.assertThat(should).isFalse();
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
