package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
			return EchoLookupOutcome.notFound();
		});

		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isFalse();
		Assertions.assertThat(lookups.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("levelClassForDepth does not resolve or activate echo")
	void levelClassForDepthDoesNotResolveEcho() {
		EchoStorage storage = new EchoStorage();
		storage.save(EchoTestSupport.warriorEchoWithData(5));
		AtomicInteger lookups = new AtomicInteger();
		CompositeEchoLookup.setEchoLookupForTests(depth -> {
			lookups.incrementAndGet();
			return storage.findEchoForDepth(depth);
		});

		Dungeon.levelClassForDepth(5, 0);

		Assertions.assertThat(lookups.get()).isZero();
		Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
		Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
	}

	@Test
	@DisplayName("storeEchoChoice does not persist or keep pending echo off boss depth")
	void storeDoesNotPersistPendingOffBossDepth() {
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoTestSupport
				.outcomeWithPolicy(EchoTestSupport.warriorEchoWithData(5)));
		Dungeon.depth = 5;
		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isTrue();

		Dungeon.depth = 6;
		Bundle bundle = new Bundle();
		Dungeon.storeEchoChoiceInBundle(bundle);

		Assertions.assertThat(bundle.contains("pending_echo")).isFalse();
		Assertions.assertThat(bundle.getBoolean("echo_boss_active")).isFalse();
		Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
		Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
	}

	@Test
	@DisplayName("restoreEchoChoice ignores pending echo when current depth is not a boss floor")
	void restoreIgnoresPendingOffBossDepth() {
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoTestSupport
				.outcomeWithPolicy(EchoTestSupport.warriorEchoWithData(5)));
		Dungeon.depth = 5;
		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isTrue();

		Bundle bundle = new Bundle();
		Dungeon.storeEchoChoiceInBundle(bundle);

		EchoTestSupport.resetWorkflowState();
		Dungeon.depth = 4;
		Dungeon.restoreEchoChoiceFromBundle(bundle);

		Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
		Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
	}

	@Test
	@DisplayName("store and restore keep pending echo only while on that boss depth")
	void storeRestoreKeepsPendingOnMatchingBossDepth() {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoTestSupport.outcomeWithPolicy(echo));
		Dungeon.depth = 5;
		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isTrue();

		Bundle bundle = new Bundle();
		Dungeon.storeEchoChoiceInBundle(bundle);

		EchoTestSupport.resetWorkflowState();
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoLookupOutcome.notFound());
		Dungeon.depth = 5;
		Dungeon.restoreEchoChoiceFromBundle(bundle);

		Assertions.assertThat(Dungeon.isEchoBossActive()).isTrue();
		Assertions.assertThat(Dungeon.getPendingEcho()).isNotNull();
		Assertions.assertThat(Dungeon.getPendingEcho().echoId).isEqualTo(echo.echoId);
	}
}
