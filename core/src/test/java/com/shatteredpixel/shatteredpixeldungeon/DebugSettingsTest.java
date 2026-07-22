package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoStorage;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

@ExtendWith(GdxTestExtension.class)
class DebugSettingsTest {

	@BeforeEach
	@AfterEach
	void cleanup() {
		DebugSettings.resetForTests();
		EchoTestSupport.resetWorkflowState();
		InterlevelScene.mode = InterlevelScene.Mode.NONE;
		InterlevelScene.returnDepth = 0;
		InterlevelScene.returnBranch = -1;
		InterlevelScene.returnPos = 0;
	}

	@Test
	@DisplayName("debug start depth label includes the chosen floor")
	void debugStartDepthLabelIncludesChosenFloor() {
		Assertions.assertThat(DebugSettings.startDepthTitle(14))
				.isEqualTo("Debug: Start Floor: 14");
	}

	@Test
	@DisplayName("debug start hero is level 100 with 100 strength")
	void debugStartHeroIsLevel100With100Strength() {
		Assertions.assertThat(DebugSettings.START_LEVEL).isEqualTo(100);
		Assertions.assertThat(DebugSettings.START_STR).isEqualTo(100);
	}

	@Test
	@DisplayName("debug start is off unless debug build and flag are enabled")
	void debugStartRequiresDebugBuildAndFlag() {
		DebugSettings.setDebugBuildOverride(false);
		DebugSettings.setDebugStart(true);
		Assertions.assertThat(DebugSettings.debugStart()).isFalse();

		DebugSettings.setDebugBuildOverride(true);
		DebugSettings.setDebugStart(false);
		Assertions.assertThat(DebugSettings.debugStart()).isFalse();

		DebugSettings.setDebugStart(true);
		Assertions.assertThat(DebugSettings.debugStart()).isTrue();
	}

	@Test
	@DisplayName("weak echo snapshots require debug build and flag")
	void weakEchoSnapshotsRequireDebugBuildAndFlag() {
		DebugSettings.setDebugBuildOverride(false);
		DebugSettings.setWeakEchoSnapshots(true);
		Assertions.assertThat(DebugSettings.weakEchoSnapshots()).isFalse();

		DebugSettings.setDebugBuildOverride(true);
		DebugSettings.setWeakEchoSnapshots(false);
		Assertions.assertThat(DebugSettings.weakEchoSnapshots()).isFalse();

		DebugSettings.setWeakEchoSnapshots(true);
		Assertions.assertThat(DebugSettings.weakEchoSnapshots()).isTrue();
	}

	@Test
	@DisplayName("debug start depth defaults to floor before the fourth boss")
	void debugStartDepthDefaultsBeforeFourthBoss() {
		DebugSettings.setDebugBuildOverride(true);
		Assertions.assertThat(DebugSettings.startDepth())
				.isEqualTo(DebugSettings.DEFAULT_START_DEPTH);
	}

	@Test
	@DisplayName("debug start depth can be chosen and is clamped to valid floors")
	void debugStartDepthCanBeChosenAndClamped() {
		DebugSettings.setDebugBuildOverride(true);
		DebugSettings.setStartDepth(14);
		Assertions.assertThat(DebugSettings.startDepth()).isEqualTo(14);

		DebugSettings.setStartDepth(0);
		Assertions.assertThat(DebugSettings.startDepth())
				.isEqualTo(DebugSettings.MIN_START_DEPTH);

		DebugSettings.setStartDepth(99);
		Assertions.assertThat(DebugSettings.startDepth())
				.isEqualTo(DebugSettings.MAX_START_DEPTH);
	}

	@Test
	@DisplayName("debug start hero pos is exit stairs when enabled, entrance otherwise")
	void debugStartHeroPosIsExitWhenEnabled() {
		DebugSettings.setDebugBuildOverride(true);
		DebugSettings.setDebugStart(true);
		Assertions.assertThat(DebugSettings.debugStartHeroPos())
				.isEqualTo(DebugSettings.START_AT_EXIT);

		DebugSettings.setDebugStart(false);
		Assertions.assertThat(DebugSettings.debugStartHeroPos()).isEqualTo(-1);
	}

	@Test
	@DisplayName("applyDebugStart sets chosen depth, level, and strength when enabled")
	void applyDebugStartSetsHeroAndDepth() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		Dungeon.depth = 1;

		DebugSettings.setDebugBuildOverride(true);
		DebugSettings.setDebugStart(true);
		DebugSettings.setStartDepth(14);
		DebugSettings.applyDebugStart();

		Assertions.assertThat(Dungeon.depth).isEqualTo(14);
		Assertions.assertThat(hero.lvl).isEqualTo(DebugSettings.START_LEVEL);
		Assertions.assertThat(hero.STR).isEqualTo(DebugSettings.START_STR);
	}

	@Test
	@DisplayName("applyDebugStart is a no-op when disabled")
	void applyDebugStartSkippedWhenDisabled() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		Dungeon.depth = 1;
		int startingLevel = hero.lvl;

		DebugSettings.setDebugBuildOverride(true);
		DebugSettings.setDebugStart(false);
		DebugSettings.applyDebugStart();

		Assertions.assertThat(Dungeon.depth).isEqualTo(1);
		Assertions.assertThat(hero.lvl).isEqualTo(startingLevel);
	}

	@Test
	@DisplayName("floor jump is available only in a live debug run")
	void floorJumpRequiresLiveDebugRun() {
		DebugSettings.setDebugBuildOverride(true);
		Assertions.assertThat(DebugSettings.canJumpToFloor()).isFalse();

		Hero hero = EchoTestSupport.warriorHero();
		Dungeon.level = stubLevel(false);
		Assertions.assertThat(DebugSettings.canJumpToFloor()).isTrue();

		DebugSettings.setDebugBuildOverride(false);
		Assertions.assertThat(DebugSettings.canJumpToFloor()).isFalse();
		Assertions.assertThat(hero).isNotNull();
	}

	@Test
	@DisplayName("depth slider title switches to jump when in a live run")
	void depthSliderTitleSwitchesToJumpInLiveRun() {
		DebugSettings.setDebugBuildOverride(true);
		Assertions.assertThat(DebugSettings.depthSliderTitle(14))
				.isEqualTo("Debug: Start Floor: 14");

		EchoTestSupport.warriorHero();
		Dungeon.level = stubLevel(false);
		Assertions.assertThat(DebugSettings.depthSliderTitle(14))
				.isEqualTo("Debug: Jump Floor: 14");
	}

	@Test
	@DisplayName("depth slider stays enabled in-run even when quick start is off")
	void depthSliderEnabledInRunWithoutQuickStart() {
		DebugSettings.setDebugBuildOverride(true);
		DebugSettings.setDebugStart(false);
		Assertions.assertThat(DebugSettings.depthSliderEnabled()).isFalse();

		EchoTestSupport.warriorHero();
		Dungeon.level = stubLevel(false);
		Assertions.assertThat(DebugSettings.depthSliderEnabled()).isTrue();
	}

	@Test
	@DisplayName("depth slider value follows current floor in-run and start depth otherwise")
	void depthSliderValueFollowsCurrentFloorInRun() {
		DebugSettings.setDebugBuildOverride(true);
		DebugSettings.setStartDepth(19);
		Assertions.assertThat(DebugSettings.depthSliderValue()).isEqualTo(19);

		EchoTestSupport.warriorHero();
		Dungeon.level = stubLevel(false);
		Dungeon.depth = 9;
		Assertions.assertThat(DebugSettings.depthSliderValue()).isEqualTo(9);
	}

	@Test
	@DisplayName("applyDepthSlider stores start depth when not in a run")
	void applyDepthSliderStoresStartDepthOutOfRun() {
		DebugSettings.setDebugBuildOverride(true);
		Assertions.assertThat(DebugSettings.applyDepthSlider(14)).isFalse();
		Assertions.assertThat(DebugSettings.startDepth()).isEqualTo(14);
		Assertions.assertThat(InterlevelScene.mode).isEqualTo(InterlevelScene.Mode.NONE);
	}

	@Test
	@DisplayName("applyDepthSlider arms RETURN jump to exit stairs in a live run")
	void applyDepthSliderArmsReturnJumpInLiveRun() {
		DebugSettings.setDebugBuildOverride(true);
		EchoTestSupport.warriorHero();
		Dungeon.level = stubLevel(false);
		Dungeon.depth = 4;
		Dungeon.branch = 0;

		Assertions.assertThat(DebugSettings.applyDepthSlider(5)).isTrue();
		Assertions.assertThat(InterlevelScene.mode).isEqualTo(InterlevelScene.Mode.RETURN);
		Assertions.assertThat(InterlevelScene.returnDepth).isEqualTo(5);
		Assertions.assertThat(InterlevelScene.returnBranch).isEqualTo(0);
		Assertions.assertThat(InterlevelScene.returnPos).isEqualTo(DebugSettings.START_AT_EXIT);
		Assertions.assertThat(DebugSettings.startDepth()).isEqualTo(5);
	}

	@Test
	@DisplayName("applyDepthSlider is a no-op when already on the chosen main-path floor")
	void applyDepthSliderNoOpOnSameFloor() {
		DebugSettings.setDebugBuildOverride(true);
		EchoTestSupport.warriorHero();
		Dungeon.level = stubLevel(false);
		Dungeon.depth = 10;
		Dungeon.branch = 0;

		Assertions.assertThat(DebugSettings.applyDepthSlider(10)).isFalse();
		Assertions.assertThat(InterlevelScene.mode).isEqualTo(InterlevelScene.Mode.NONE);
	}

	@Test
	@DisplayName("applyDepthSlider abandons a sealed echo boss floor before jumping away")
	void applyDepthSliderAbandonsSealedEchoBossBeforeJump() {
		DebugSettings.setDebugBuildOverride(true);
		EchoStorage storage = new EchoStorage();
		storage.save(EchoTestSupport.warriorEchoWithData(5));
		CompositeEchoLookup.setEchoLookupForTests(storage);
		Dungeon.depth = 5;
		Dungeon.branch = 0;
		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isTrue();

		EchoTestSupport.warriorHero();
		Dungeon.level = stubLevel(true);
		Dungeon.generatedLevels.add(5);

		Assertions.assertThat(DebugSettings.applyDepthSlider(4)).isTrue();
		Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();
		Assertions.assertThat(Dungeon.getPendingEcho()).isNull();
		Assertions.assertThat(Dungeon.levelHasBeenGenerated(5, 0)).isFalse();
		Assertions.assertThat(InterlevelScene.returnDepth).isEqualTo(4);
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
