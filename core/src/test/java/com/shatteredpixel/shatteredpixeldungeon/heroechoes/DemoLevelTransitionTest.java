package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DemoLevelTransitionTest {

	@AfterEach
	void clearDemoFlag() {
		Dungeon.demoPackage = false;
		Dungeon.depth = 0;
		Dungeon.branch = 0;
	}

	@Test
	@DisplayName("demo depth-5 exit routes to floor 26; depth-26 entrance routes to floor 5")
	void demoRoutesSewersBossToAmuletRoom() {
		Dungeon.demoPackage = true;
		Assertions.assertThat(
				LevelTransition.demoDestDepth(LevelTransition.Type.REGULAR_EXIT, 5, 0))
				.isEqualTo(26);
		Assertions.assertThat(
				LevelTransition.demoDestDepth(LevelTransition.Type.REGULAR_ENTRANCE, 26, 0))
				.isEqualTo(5);
	}

	@Test
	@DisplayName("demo leaves every other stair to the original depth math")
	void demoLeavesOtherStairsAlone() {
		Dungeon.demoPackage = true;
		Assertions.assertThat(
				LevelTransition.demoDestDepth(LevelTransition.Type.REGULAR_EXIT, 4, 0))
				.isNull();
		Assertions.assertThat(
				LevelTransition.demoDestDepth(LevelTransition.Type.REGULAR_ENTRANCE, 25, 0))
				.isNull();
		Dungeon.demoPackage = false;
		Assertions.assertThat(
				LevelTransition.demoDestDepth(LevelTransition.Type.REGULAR_EXIT, 5, 0))
				.isNull();
		Assertions.assertThat(
				LevelTransition.demoDestDepth(LevelTransition.Type.REGULAR_ENTRANCE, 26, 0))
				.isNull();
	}

	@Test
	@DisplayName("stairs back from floor 26 go to 5 on a demo package and 25 otherwise")
	void stairsBackFromFloor26() {
		Level level = blankLevel();
		Dungeon.depth = 26;
		Dungeon.branch = 0;

		Dungeon.demoPackage = true;
		LevelTransition demoBack = new LevelTransition(level, 0, LevelTransition.Type.REGULAR_ENTRANCE);
		Assertions.assertThat(demoBack.destDepth).isEqualTo(5);

		Dungeon.demoPackage = false;
		LevelTransition fullBack = new LevelTransition(level, 0, LevelTransition.Type.REGULAR_ENTRANCE);
		Assertions.assertThat(fullBack.destDepth).isEqualTo(25);
	}

	@Test
	@DisplayName("a floor-26 stair built on a demo package returns to 25 when the live package is not a demo")
	void savedFloor26StairFollowsLivePackage() {
		Level level = blankLevel();
		Dungeon.depth = 26;
		Dungeon.branch = 0;
		Dungeon.demoPackage = true;
		LevelTransition back = new LevelTransition(level, 0, LevelTransition.Type.REGULAR_ENTRANCE);
		Assertions.assertThat(back.destDepth).isEqualTo(5);

		Dungeon.demoPackage = false;
		LevelTransition.applyPackageRoute(back, 26, 0);
		Assertions.assertThat(back.destDepth).isEqualTo(25);
	}

	@Test
	@DisplayName("a floor-5 exit built on a demo package returns to 6 when the live package is not a demo")
	void savedFloor5ExitFollowsLivePackage() {
		Level level = blankLevel();
		Dungeon.depth = 5;
		Dungeon.branch = 0;
		Dungeon.demoPackage = true;
		LevelTransition down = new LevelTransition(level, 0, LevelTransition.Type.REGULAR_EXIT);
		Assertions.assertThat(down.destDepth).isEqualTo(26);

		Dungeon.demoPackage = false;
		LevelTransition.applyPackageRoute(down, 5, 0);
		Assertions.assertThat(down.destDepth).isEqualTo(6);
	}

	private static Level blankLevel() {
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
		level.setSize(1, 1);
		return level;
	}
}
