package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

@ExtendWith(GdxTestExtension.class)
class EchoBossContinueRetreatTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
		GamesInProgress.curSlot = 0;
	}

	@Test
	@DisplayName("continue retreats when echo boss floor is sealed")
	void retreatsWhenSealedEchoBossFloor() {
		prepareActiveEchoBossAtDepth(5);
		Level level = stubLevel(true);

		Assertions.assertThat(Dungeon.shouldRetreatEchoBossOnContinue(level)).isTrue();
	}

	@Test
	@DisplayName("continue does not retreat before the echo boss seals the floor")
	void noRetreatBeforeSeal() {
		prepareActiveEchoBossAtDepth(5);
		Level level = stubLevel(false);

		Assertions.assertThat(Dungeon.shouldRetreatEchoBossOnContinue(level)).isFalse();
	}

	@Test
	@DisplayName("continue does not retreat on a sealed non-echo boss floor")
	void noRetreatWithoutEchoBoss() {
		Dungeon.depth = 5;
		Dungeon.branch = 0;
		Level level = stubLevel(true);

		Assertions.assertThat(Dungeon.shouldRetreatEchoBossOnContinue(level)).isFalse();
	}

	@Test
	@DisplayName("abandon clears echo state, LockedFloor, and forgets the boss level")
	void abandonClearsFightAndForgetsBossLevel() throws Exception {
		prepareActiveEchoBossAtDepth(5);
		Hero hero = EchoTestSupport.warriorHero();
		Buff.affect(hero, LockedFloor.class);
		Dungeon.hero = hero;

		GamesInProgress.curSlot = 1;
		Dungeon.generatedLevels.add(5);
		String depthPath = GamesInProgress.depthFile(1, 5, 0);
		FileUtils.bundleToFile(depthPath, new Bundle());
		Assertions.assertThat(FileUtils.fileExists(depthPath)).isTrue();

		Dungeon.abandonSealedEchoBossFloor();

		Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
		Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
		Assertions.assertThat(Dungeon.levelHasBeenGenerated(5, 0)).isFalse();
		Assertions.assertThat(FileUtils.fileExists(depthPath)).isFalse();
		Assertions.assertThat(hero.buff(LockedFloor.class)).isNull();
	}

	private static void prepareActiveEchoBossAtDepth(int depth) {
		EchoStorage storage = new EchoStorage();
		storage.save(EchoTestSupport.warriorEchoWithData(depth));
		CompositeEchoLookup.setEchoLookupForTests(storage);
		Dungeon.depth = depth;
		Dungeon.branch = 0;
		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(depth)).isTrue();
	}

	private static Level stubLevel(boolean locked) {
		Level level = new Level() {
			@Override
			public String tilesTex() {
				return null;
			}

			@Override
			public String waterTex() {
				return null;
			}

			@Override
			protected boolean build() {
				return true;
			}

			@Override
			protected void createMobs() {
			}

			@Override
			protected void createItems() {
			}
		};
		level.setSize(7, 7);
		Arrays.fill(level.map, Terrain.EMPTY);
		level.mobs = new HashSet<>();
		level.heaps = new com.watabou.utils.SparseArray<>();
		level.blobs = new HashMap<>();
		level.plants = new com.watabou.utils.SparseArray<>();
		level.traps = new com.watabou.utils.SparseArray<>();
		level.locked = locked;
		return level;
	}
}
