package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoFetchResult;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(GdxTestExtension.class)
class DungeonEchoBossPrefetchTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("shouldPrefetch is true on boss depth")
	void shouldPrefetchOnBossDepth() {
		Assertions.assertThat(Dungeon.shouldPrefetchEchoBoss(5, 0)).isTrue();
		Assertions.assertThat(Dungeon.shouldPrefetchEchoBoss(25, 0)).isTrue();
	}

	@Test
	@DisplayName("shouldPrefetch is false off boss depth")
	void shouldNotPrefetchOffBossDepth() {
		Assertions.assertThat(Dungeon.shouldPrefetchEchoBoss(4, 0)).isFalse();
		Assertions.assertThat(Dungeon.shouldPrefetchEchoBoss(5, 1)).isFalse();
	}

	@Test
	@DisplayName("prefetch resolves pending echo before level generation")
	void prefetchResolvesPendingEcho() {
		EchoStorage storage = new EchoStorage();
		storage.save(EchoTestSupport.warriorEchoWithData(5));
		CompositeEchoLookup.setEchoLookupForTests(storage);

		Assertions.assertThat(Dungeon.getPendingEcho()).isNull();

		boolean echoBoss = Dungeon.prefetchEchoBossForDepth(5);

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
		CompositeEchoLookup.setEchoLookupForTests(storage);

		boolean echoBoss = Dungeon.prefetchEchoBossForDepth(5);

		Assertions.assertThat(echoBoss).isFalse();
		Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
		Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
	}

	@Test
	@DisplayName("all boss depths are eligible for prefetch")
	void allBossDepthsEligible() {
		for (int depth : EchoReplacementDecider.BOSS_DEPTHS) {
			Assertions.assertThat(Dungeon.shouldPrefetchEchoBoss(depth, 0))
					.as("depth %d", depth)
					.isTrue();
		}
	}

	@Test
	@DisplayName("prefetch looks up local storage once")
	void prefetchLooksUpLocalStorageOnce() {
		EchoStorage storage = new EchoStorage();
		storage.save(EchoTestSupport.warriorEchoWithData(5));
		AtomicInteger lookups = new AtomicInteger();
		CompositeEchoLookup.setEchoLookupForTests(depth -> {
			lookups.incrementAndGet();
			return storage.findEchoForDepth(depth);
		});

		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isTrue();
		Assertions.assertThat(lookups.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("prefetch looks up once when local storage has no echo")
	void prefetchLooksUpOnceWhenMissing() {
		AtomicInteger lookups = new AtomicInteger();
		CompositeEchoLookup.setEchoLookupForTests(depth -> {
			lookups.incrementAndGet();
			return Optional.<EchoFetchResult>empty();
		});

		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isFalse();
		Assertions.assertThat(lookups.get()).isEqualTo(1);
	}
}
