package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
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
	@DisplayName("createRegionalBoss returns default when pending echo is for a different depth")
	void createRegionalBossDefaultsWhenPendingEchoDepthMismatches() {
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoTestSupport
				.outcomeWithPolicy(EchoTestSupport.warriorEchoWithData(5)));
		Dungeon.prefetchEchoBossForDepth(5);

		Dungeon.depth = 15;
		DM300 defaultBoss = new DM300();
		Mob result = EchoBossSpawner.createRegionalBoss(defaultBoss);

		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
		Assertions.assertThat(result).isSameAs(defaultBoss);
	}

	@Test
	@DisplayName("unloadable solo echo file falls back to default boss")
	void unloadableSoloEchoFallsBackToDefaultBoss() throws Exception {
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Echo snap = EchoTestSupport.warriorEchoWithData(5);
		// Write echo-only bundle (no policy) — same shape as a stale save.
		FileUtils.bundleToFile("echoes-solo/depth-5.dat", snap.toFileBundle());
		CompositeEchoLookup.setEchoLookupForTests(new EchoStorage());

		Dungeon.depth = 5;
		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isFalse();
		Goo defaultBoss = new Goo();
		Assertions.assertThat(EchoBossSpawner.createRegionalBoss(defaultBoss)).isSameAs(defaultBoss);
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
	@DisplayName("createRegionalBoss returns each regional default when no echo is pending")
	void createRegionalBossReturnsRegionalDefaultsWithoutEcho() {
		Assertions.assertThat(EchoBossSpawner.createRegionalBoss(new Goo())).isInstanceOf(Goo.class);
		Assertions.assertThat(EchoBossSpawner.createRegionalBoss(new DM300())).isInstanceOf(DM300.class);
		Assertions.assertThat(EchoBossSpawner.createRegionalBoss(new DwarfKing())).isInstanceOf(DwarfKing.class);
		Assertions.assertThat(EchoBossSpawner.createRegionalBoss(new YogDzewa())).isInstanceOf(YogDzewa.class);
	}

	@Test
	@DisplayName("matching pending echo still replaces the default boss for that depth")
	void matchingPendingEchoStillReplacesDefaultBoss() {
		Echo echo = EchoTestSupport.warriorEchoWithData(5);
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoTestSupport.outcomeWithPolicy(echo));
		Dungeon.depth = 5;
		Dungeon.prefetchEchoBossForDepth(5);

		Mob result = EchoBossSpawner.createRegionalBoss(new Goo());

		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isTrue();
		Assertions.assertThat(result).isInstanceOf(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss.class);
	}
}
