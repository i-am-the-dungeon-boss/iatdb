package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoBossPrefetchTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("shouldPrefetch is true on boss depth")
	void shouldPrefetchOnBossDepth() {
		Assertions.assertThat(EchoBossPrefetch.shouldPrefetch(5, 0)).isTrue();
		Assertions.assertThat(EchoBossPrefetch.shouldPrefetch(25, 0)).isTrue();
	}

	@Test
	@DisplayName("shouldPrefetch is false off boss depth")
	void shouldNotPrefetchOffBossDepth() {
		Assertions.assertThat(EchoBossPrefetch.shouldPrefetch(4, 0)).isFalse();
		Assertions.assertThat(EchoBossPrefetch.shouldPrefetch(5, 1)).isFalse();
	}

	@Test
	@DisplayName("prefetch resolves pending echo before level generation")
	void prefetchResolvesPendingEcho() {
		EchoStorage storage = new EchoStorage();
		storage.save(EchoTestSupport.warriorEchoWithData(5));
		Dungeon.setEchoLookup(storage);

		Assertions.assertThat(Dungeon.getPendingEcho()).isNull();

		boolean echoBoss = EchoBossPrefetch.prefetch(5);

		Assertions.assertThat(echoBoss).isTrue();
		Assertions.assertThat(Dungeon.getPendingEcho()).isNotNull();
		Assertions.assertThat(Dungeon.getPendingEcho().hasCombatData()).isTrue();
		Assertions.assertThat(Dungeon.isEchoBossActive()).isTrue();
	}

	@Test
	@DisplayName("prefetch skips metadata-only echoes")
	void prefetchSkipsMetadataOnlyEcho() {
		EchoStorage storage = new EchoStorage();
		storage.save(EchoTestSupport.warriorEcho(5));
		Dungeon.setEchoLookup(storage);

		boolean echoBoss = EchoBossPrefetch.prefetch(5);

		Assertions.assertThat(echoBoss).isFalse();
		Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
		Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
	}

	@Test
	@DisplayName("all boss depths are eligible for prefetch")
	void allBossDepthsEligible() {
		for (int depth : EchoReplacementDecider.BOSS_DEPTHS) {
			Assertions.assertThat(EchoBossPrefetch.shouldPrefetch(depth, 0))
					.as("depth %d", depth)
					.isTrue();
		}
	}
}
