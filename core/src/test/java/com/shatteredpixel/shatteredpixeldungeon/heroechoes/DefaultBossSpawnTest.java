package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;
import com.shatteredpixel.shatteredpixeldungeon.levels.HallsBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel;
import com.watabou.utils.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class DefaultBossSpawnTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
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
		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
	}

	@Test
	@DisplayName("shouldSpawn is false when pending echo is for a different depth")
	void shouldSpawnFalseWhenPendingEchoDepthMismatches() {
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoTestSupport
				.outcomeWithPolicy(EchoTestSupport.warriorEchoWithData(5)));
		Dungeon.prefetchEchoBossForDepth(5);

		Dungeon.depth = 15;

		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
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
		Assertions.assertThat(EchoBossSpawner.create(5).getEcho().echoId).isEqualTo(echo.echoId);
	}
}
