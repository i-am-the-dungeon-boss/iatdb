package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.EchoBossSpawner;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;
import com.shatteredpixel.shatteredpixeldungeon.levels.HallsBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(GdxTestExtension.class)
class DefaultBossSpawnTest {

	@AfterEach
	void cleanup() {
		SentryCrashReporting.resetReporter();
	}

	@Test
	@DisplayName("prefetch for a new boss depth ignores a pending echo from a different depth")
	void prefetchIgnoresPendingEchoFromDifferentDepth() {
		CompositeEchoLookup.setEchoLookupForTests(depth -> {
			if (depth == 5) {
				return EchoTestSupport.outcomeWithPolicy(EchoTestSupport.warriorEchoWithData(5));
			}
			return EchoLookupOutcome.notFound();
		});

		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isTrue();
		Assertions.assertThat(Dungeon.getPendingEcho().depth).isEqualTo(5);

		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(10)).isFalse();
		Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
		Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
		Dungeon.depth = 10;
		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
		Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault()).isTrue();
	}

	@Test
	@DisplayName("shouldSpawn is false when pending echo is for a different depth")
	void shouldSpawnFalseWhenPendingEchoDepthMismatches() {
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoTestSupport
				.outcomeWithPolicy(EchoTestSupport.warriorEchoWithData(5)));
		Dungeon.prefetchEchoBossForDepth(5);

		Dungeon.depth = 15;

		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
		Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault()).isFalse();
	}

	@Test
	@DisplayName("unloadable solo echo file falls back without activating echo")
	void unloadableSoloEchoFallsBackWithoutActivatingEcho() throws Exception {
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Echo snap = EchoTestSupport.warriorEchoWithData(5);
		// Write echo-only bundle (no policy) — same shape as a stale save.
		FileUtils.bundleToFile("echoes-solo/depth-5.dat", snap.toFileBundle());
		CompositeEchoLookup.setEchoLookupForTests(new EchoStorage());

		Dungeon.depth = 5;
		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isFalse();
		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
		Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault()).isTrue();
	}

	@Test
	@DisplayName("each boss depth keeps its regional level class with no echo")
	void bossDepthsKeepRegionalLevelClassWithoutEcho() {
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoLookupOutcome.notFound());

		Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(SewerBossLevel.class);
		Assertions.assertThat(Dungeon.levelClassForDepth(10, 0)).isEqualTo(PrisonBossLevel.class);
		Assertions.assertThat(Dungeon.levelClassForDepth(15, 0)).isEqualTo(CavesBossLevel.class);
		Assertions.assertThat(Dungeon.levelClassForDepth(20, 0)).isEqualTo(CityBossLevel.class);
		Assertions.assertThat(Dungeon.levelClassForDepth(25, 0)).isEqualTo(HallsBossLevel.class);

		for (int depth : EchoReplacementDecider.BOSS_DEPTHS) {
			Dungeon.depth = depth;
			Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(depth))
					.as("depth %d", depth)
					.isFalse();
			Assertions.assertThat(EchoBossSpawner.shouldSpawn())
					.as("depth %d", depth)
					.isFalse();
			Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault())
					.as("depth %d", depth)
					.isTrue();
		}
	}

	@Test
	@DisplayName("matching pending echo activates shouldSpawn for that depth")
	void matchingPendingEchoActivatesShouldSpawn() {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoTestSupport.outcomeWithPolicy(echo));
		Dungeon.depth = 5;
		Dungeon.prefetchEchoBossForDepth(5);

		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isTrue();
		Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault()).isFalse();
		Assertions.assertThat(EchoBossSpawner.create(5).getEcho().echoId).isEqualTo(echo.echoId);
	}

	@Test
	@DisplayName("NOT_FOUND prefetch enables shouldSpawnDefault only")
	void notFoundEnablesShouldSpawnDefaultOnly() {
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoLookupOutcome.notFound());
		Dungeon.depth = 5;
		Dungeon.prefetchEchoBossForDepth(5);

		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
		Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault()).isTrue();
		Assertions.assertThat(Dungeon.wasEchoLookupNotFound()).isTrue();
	}

	@Test
	@DisplayName("FOUND prefetch enables shouldSpawn only")
	void foundEnablesShouldSpawnOnly() {
		CompositeEchoLookup.setEchoLookupForTests(
				depth -> EchoTestSupport.outcomeWithPolicy(EchoTestSupport.warriorEchoWithData(5)));
		Dungeon.depth = 5;
		Dungeon.prefetchEchoBossForDepth(5);

		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isTrue();
		Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault()).isFalse();
		Assertions.assertThat(Dungeon.wasEchoLookupFound()).isTrue();
	}

	@Test
	@DisplayName("clearPendingEcho keeps NOT_FOUND latch for default spawn")
	void clearPendingEchoKeepsNotFoundLatch() {
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoLookupOutcome.notFound());
		Dungeon.depth = 10;
		Dungeon.prefetchEchoBossForDepth(10);

		Dungeon.clearPendingEcho();

		Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault()).isTrue();
	}

	@Test
	@DisplayName("unset resolve reports Sentry then Retry FOUND returns ECHO")
	void unsetResolveRetryFoundReturnsEcho() {
		String previous = Game.version;
		Game.version = "1.0.0";
		try {
			List<Throwable> captured = new ArrayList<>();
			SentryCrashReporting.setReporter(captured::add);
			AtomicInteger lookups = new AtomicInteger();
			CompositeEchoLookup.setEchoLookupForTests(depth -> {
				if (lookups.incrementAndGet() == 1) {
					return EchoTestSupport.outcomeWithPolicy(EchoTestSupport.warriorEchoWithData(5));
				}
				return EchoLookupOutcome.notFound();
			});
			Dungeon.depth = 5;
			Dungeon.echoPlayMode = EchoPlayMode.SOLO;

			EchoBossSpawner.BossSpawnChoice choice = EchoBossSpawner.resolveBossSpawn(
					failed -> EchoPrefetchUserChoice.ABORT);

			Assertions.assertThat(captured).isNotEmpty();
			Assertions.assertThat(captured.get(0).getMessage()).contains("boss spawn");
			Assertions.assertThat(choice).isEqualTo(EchoBossSpawner.BossSpawnChoice.ECHO);
			Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isTrue();
		} finally {
			Game.version = previous;
		}
	}

	@Test
	@DisplayName("ERROR resolve reports Sentry then Retry NOT_FOUND returns DEFAULT")
	void errorResolveRetryNotFoundReturnsDefault() {
		String previous = Game.version;
		Game.version = "1.0.0";
		try {
			List<Throwable> captured = new ArrayList<>();
			SentryCrashReporting.setReporter(captured::add);
			AtomicInteger lookups = new AtomicInteger();
			CompositeEchoLookup.setEchoLookupForTests(depth -> {
				// Prefetch + first recovery cycle ERROR; after user Retry → NOT_FOUND.
				if (lookups.getAndIncrement() < 2) {
					return EchoLookupOutcome.error(EchoLookupOutcome.FailureKind.NETWORK);
				}
				return EchoLookupOutcome.notFound();
			});
			Dungeon.depth = 5;
			Dungeon.echoPlayMode = EchoPlayMode.SOLO;
			Dungeon.prefetchEchoBossOutcome(5);
			Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault()).isFalse();

			AtomicInteger prompts = new AtomicInteger();
			EchoBossSpawner.BossSpawnChoice choice = EchoBossSpawner.resolveBossSpawn(failed -> {
				prompts.incrementAndGet();
				return EchoPrefetchUserChoice.RETRY;
			});

			Assertions.assertThat(captured).isNotEmpty();
			Assertions.assertThat(prompts.get()).isGreaterThanOrEqualTo(1);
			Assertions.assertThat(choice).isEqualTo(EchoBossSpawner.BossSpawnChoice.DEFAULT);
			Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault()).isTrue();
		} finally {
			Game.version = previous;
		}
	}

	@Test
	@DisplayName("ERROR resolve Abort returns ABORT without default spawn")
	void errorResolveAbortReturnsAbort() {
		String previous = Game.version;
		Game.version = "1.0.0";
		try {
			List<Throwable> captured = new ArrayList<>();
			SentryCrashReporting.setReporter(captured::add);
			CompositeEchoLookup.setEchoLookupForTests(
					depth -> EchoLookupOutcome.error(EchoLookupOutcome.FailureKind.SERVER));
			Dungeon.depth = 5;
			Dungeon.echoPlayMode = EchoPlayMode.RANKED;
			Dungeon.prefetchEchoBossOutcome(5);

			EchoBossSpawner.BossSpawnChoice choice = EchoBossSpawner.resolveBossSpawn(
					failed -> EchoPrefetchUserChoice.ABORT);

			Assertions.assertThat(captured).isNotEmpty();
			Assertions.assertThat(choice).isEqualTo(EchoBossSpawner.BossSpawnChoice.ABORT);
			Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
			Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault()).isFalse();
		} finally {
			Game.version = previous;
		}
	}

	@Test
	@DisplayName("bundle round-trip preserves NOT_FOUND for default spawn")
	void bundleRoundTripPreservesNotFound() {
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoLookupOutcome.notFound());
		Dungeon.depth = 5;
		Dungeon.prefetchEchoBossForDepth(5);
		Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault()).isTrue();

		Bundle bundle = new Bundle();
		Dungeon.storeEchoChoiceInBundle(bundle);

		EchoTestSupport.resetWorkflowState();
		Dungeon.depth = 5;
		Dungeon.restoreEchoChoiceFromBundle(bundle);

		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
		Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault()).isTrue();
	}

	@Test
	@DisplayName("bundle round-trip preserves FOUND for echo spawn")
	void bundleRoundTripPreservesFound() {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoTestSupport.outcomeWithPolicy(echo));
		Dungeon.depth = 5;
		Dungeon.prefetchEchoBossForDepth(5);

		Bundle bundle = new Bundle();
		Dungeon.storeEchoChoiceInBundle(bundle);

		EchoTestSupport.resetWorkflowState();
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoLookupOutcome.notFound());
		Dungeon.depth = 5;
		Dungeon.restoreEchoChoiceFromBundle(bundle);

		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isTrue();
		Assertions.assertThat(EchoBossSpawner.shouldSpawnDefault()).isFalse();
	}
}
