package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class DebugSettingsTest {

	@AfterEach
	void cleanup() {
		DebugSettings.resetForTests();
		EchoTestSupport.resetWorkflowState();
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
	@DisplayName("applyDebugStart sets depth, level, and strength when enabled")
	void applyDebugStartSetsHeroAndDepth() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		Dungeon.depth = 1;

		DebugSettings.setDebugBuildOverride(true);
		DebugSettings.setDebugStart(true);
		DebugSettings.applyDebugStart();

		Assertions.assertThat(Dungeon.depth).isEqualTo(DebugSettings.START_DEPTH);
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
}
