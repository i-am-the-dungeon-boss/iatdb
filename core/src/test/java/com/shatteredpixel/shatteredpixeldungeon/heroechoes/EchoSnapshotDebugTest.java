package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoSnapshotDebugTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("weak snapshot flag lowers echo metadata when enabled")
	void weakSnapshotLowersMetadata() {
		DebugSettings.setDebugBuildOverride(true);
		DebugSettings.setWeakEchoSnapshots(true);
		Echo echo = EchoTestSupport.warriorEcho(5);

		EchoSnapshotDebug.applyIfEnabled(echo);

		Assertions.assertThat(echo.lvl).isEqualTo(EchoSnapshotDebug.WEAK_LEVEL);
		Assertions.assertThat(echo.hp).isEqualTo(EchoSnapshotDebug.WEAK_HP);
		Assertions.assertThat(echo.ht).isEqualTo(EchoSnapshotDebug.WEAK_HT);
	}

	@Test
	@DisplayName("weak snapshot flag leaves echo unchanged when disabled")
	void weakSnapshotSkippedWhenDisabled() {
		DebugSettings.setDebugBuildOverride(true);
		DebugSettings.setWeakEchoSnapshots(false);
		Echo echo = EchoTestSupport.warriorEcho(5);

		EchoSnapshotDebug.applyIfEnabled(echo);

		Assertions.assertThat(echo.lvl).isEqualTo(6);
		Assertions.assertThat(echo.hp).isEqualTo(28);
		Assertions.assertThat(echo.ht).isEqualTo(30);
	}

	@Test
	@DisplayName("weak snapshot lowers bundled hero level, hp, and strength")
	void weakSnapshotLowersBundledHeroStats() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		hero.STR = 18;

		Echo echo = Echo.create(
				5,
				EchoTestSupport.TEST_GAME_VERSION,
				12345L,
				"WARRIOR",
				hero.lvl,
				hero.HP,
				hero.HT,
				EchoTestSupport.bundleHero(hero)
		);

		EchoSnapshotDebug.weaken(echo);

		Hero restored = new Hero();
		restored.live();
		restored.restoreFromBundle(echo.echoData);

		Assertions.assertThat(restored.lvl).isEqualTo(EchoSnapshotDebug.WEAK_LEVEL);
		Assertions.assertThat(restored.HP).isEqualTo(EchoSnapshotDebug.WEAK_HP);
		Assertions.assertThat(restored.HT).isEqualTo(EchoSnapshotDebug.WEAK_HT);
		Assertions.assertThat(restored.STR).isEqualTo(EchoSnapshotDebug.WEAK_STR);
	}

	@Test
	@DisplayName("capture applies weak snapshot when debug flag is enabled")
	void captureAppliesWeakSnapshotWhenEnabled() {
		DebugSettings.setDebugBuildOverride(true);
		DebugSettings.setWeakEchoSnapshots(true);
		EchoStorage storage = new EchoStorage();
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		hero.lvl = 8;
		hero.HP = hero.HT = 40;
		hero.STR = 16;

		EchoCaptureTrigger.captureBossVictory(hero, 5, storage);

		Echo loaded = storage.loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION).orElseThrow();
		Assertions.assertThat(loaded.lvl).isEqualTo(EchoSnapshotDebug.WEAK_LEVEL);
		Assertions.assertThat(loaded.hp).isEqualTo(EchoSnapshotDebug.WEAK_HP);
		Assertions.assertThat(loaded.ht).isEqualTo(EchoSnapshotDebug.WEAK_HT);
	}
}
