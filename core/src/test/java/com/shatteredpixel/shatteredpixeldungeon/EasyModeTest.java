package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EasyModeTest {

	@AfterEach
	void cleanup() {
		SPDSettings.easyMode(false);
		Dungeon.echoPlayMode = EchoPlayMode.NONE;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("easy mode setting defaults to off")
	void easyModeSettingDefaultsOff() {
		Assertions.assertThat(SPDSettings.easyMode()).isFalse();
	}

	@Test
	@DisplayName("easy mode is allowed in solo mode only")
	void easyModeAllowedInSoloOnly() {
		Assertions.assertThat(SPDSettings.easyModeAllowedForPlayMode(EchoPlayMode.SOLO)).isTrue();
		Assertions.assertThat(SPDSettings.easyModeAllowedForPlayMode(EchoPlayMode.RANKED)).isFalse();
		Assertions.assertThat(SPDSettings.easyModeAllowedForPlayMode(EchoPlayMode.NONE)).isFalse();
	}

	@Test
	@DisplayName("ranked mode clears applied easy mode settings")
	void rankedModeClearsAppliedEasyModeSettings() {
		SPDSettings.easyMode(true);
		Dungeon.easyMode = true;

		SPDSettings.clearEasyModeIfDisallowed(EchoPlayMode.RANKED);

		Assertions.assertThat(SPDSettings.easyMode()).isFalse();
		Assertions.assertThat(Dungeon.easyMode).isFalse();
	}

	@Test
	@DisplayName("solo mode keeps applied easy mode settings")
	void soloModeKeepsAppliedEasyModeSettings() {
		SPDSettings.easyMode(true);
		Dungeon.easyMode = true;

		SPDSettings.clearEasyModeIfDisallowed(EchoPlayMode.SOLO);

		Assertions.assertThat(SPDSettings.easyMode()).isTrue();
		Assertions.assertThat(Dungeon.easyMode).isTrue();
	}

	@Test
	@DisplayName("selecting ranked mode clears applied easy mode settings")
	void selectingRankedModeClearsAppliedEasyModeSettings() {
		SPDSettings.easyMode(true);
		Dungeon.easyMode = true;

		GamesInProgress.selectEchoPlayMode(EchoPlayMode.RANKED);

		Assertions.assertThat(SPDSettings.easyMode()).isFalse();
		Assertions.assertThat(Dungeon.easyMode).isFalse();
	}

	@Test
	@DisplayName("Dungeon.init copies easy mode from settings in solo")
	void initCopiesEasyModeFromSettingsInSolo() {
		SPDSettings.easyMode(true);
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.seed = 1L;
		Dungeon.daily = false;
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Dungeon.init();

		Assertions.assertThat(Dungeon.easyMode).isTrue();
	}

	@Test
	@DisplayName("Dungeon.init clears easy mode outside solo")
	void initClearsEasyModeOutsideSolo() {
		SPDSettings.easyMode(true);
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.seed = 1L;
		Dungeon.daily = false;
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;
		Dungeon.init();

		Assertions.assertThat(Dungeon.easyMode).isFalse();
	}

	@Test
	@DisplayName("easy mode floor rewards raise strength and queue an identified mapping scroll")
	void floorRewardsRaiseStrengthAndQueueIdentifiedMappingScroll() {
		Hero hero = new Hero();
		hero.STR = Hero.STARTING_STR;
		Dungeon.hero = hero;
		Dungeon.easyMode = true;
		Dungeon.depth = 2;
		Dungeon.branch = 0;

		SewerLevel level = new SewerLevel();
		Dungeon.applyEasyModeFloorRewards(level);

		Item scroll = level.findPrizeItem(ScrollOfMagicMapping.class);
		Assertions.assertThat(hero.STR).isEqualTo(Hero.STARTING_STR + 1);
		Assertions.assertThat(scroll).isInstanceOf(ScrollOfMagicMapping.class);
		Assertions.assertThat(scroll.isIdentified()).isTrue();
	}

	@Test
	@DisplayName("easy mode floor rewards are skipped when easy mode is off")
	void floorRewardsSkippedWhenOff() {
		Hero hero = new Hero();
		hero.STR = Hero.STARTING_STR;
		Dungeon.hero = hero;
		Dungeon.easyMode = false;
		Dungeon.depth = 2;
		Dungeon.branch = 0;

		SewerLevel level = new SewerLevel();
		Dungeon.applyEasyModeFloorRewards(level);

		Assertions.assertThat(hero.STR).isEqualTo(Hero.STARTING_STR);
		Assertions.assertThat(level.findPrizeItem(ScrollOfMagicMapping.class)).isNull();
	}

	@Test
	@DisplayName("easy mode floor rewards are skipped on boss levels")
	void floorRewardsSkippedOnBossLevels() {
		Hero hero = new Hero();
		hero.STR = Hero.STARTING_STR;
		Dungeon.hero = hero;
		Dungeon.easyMode = true;
		Dungeon.depth = 5;
		Dungeon.branch = 0;

		SewerLevel level = new SewerLevel();
		Dungeon.applyEasyModeFloorRewards(level);

		Assertions.assertThat(hero.STR).isEqualTo(Hero.STARTING_STR);
		Assertions.assertThat(level.findPrizeItem(ScrollOfMagicMapping.class)).isNull();
	}
}
