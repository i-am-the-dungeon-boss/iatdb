package com.shatteredpixel.shatteredpixeldungeon.heroechoes.debug;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.levels.DebugArenaLevel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class DebugArenaItemsRestockTest {

	@Test
	@DisplayName("restockGround clears existing heaps and drops every catalog item")
	void restockGroundClearsHeapsAndDropsEveryCatalogItem() {
		DebugSettings.setDebugBuildOverride(true);
		Dungeon.echoPlayMode = EchoPlayMode.DEBUG;
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.seed = 1L;
		Dungeon.hero = new Hero();
		HeroClass.WARRIOR.initHero(Dungeon.hero);
		Dungeon.hero.live();

		DebugArenaLevel level = new DebugArenaLevel();
		level.create();
		Dungeon.level = level;

		Assertions.assertThat(countHeapItems(level)).isGreaterThan(0);
		Heap first = level.heaps.valueList().get(0);
		first.destroy();
		Assertions.assertThat(countHeapItems(level)).isLessThan(DebugArenaItems.createAll().size());

		int dropped = DebugArenaItems.restockGround();

		Assertions.assertThat(dropped).isEqualTo(DebugArenaItems.createAll().size());
		Assertions.assertThat(countHeapItems(level)).isEqualTo(dropped);
	}

	@Test
	@DisplayName("restockGround preserves potions the hero already identified")
	void restockGroundPreservesHeroPotionKnowledge() {
		DebugSettings.setDebugBuildOverride(true);
		Dungeon.echoPlayMode = EchoPlayMode.DEBUG;
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.seed = 1L;
		Dungeon.hero = new Hero();
		HeroClass.WARRIOR.initHero(Dungeon.hero);
		Dungeon.hero.live();
		Item.clearCurrent();
		PotionOfLiquidFlame known = new PotionOfLiquidFlame();
		known.identify();
		Assertions.assertThat(known.isKnown()).isTrue();
		int imageBefore = known.image;
		known.collect(Dungeon.hero.belongings.backpack);

		DebugArenaLevel level = new DebugArenaLevel();
		level.create();
		Dungeon.level = level;

		DebugArenaItems.restockGround();

		Assertions.assertThat(new PotionOfLiquidFlame().isKnown())
				.as("ground restock must not wipe potions the hero already knew")
				.isTrue();
		Assertions.assertThat(new PotionOfLiquidFlame().image)
				.as("ground restock must not re-roll potion color mapping")
				.isEqualTo(imageBefore);
	}

	@Test
	@DisplayName("restockGround is a no-op outside debug builds")
	void restockGroundNoOpOutsideDebugBuilds() {
		DebugSettings.setDebugBuildOverride(false);
		Dungeon.level = new DebugArenaLevel();

		Assertions.assertThat(DebugArenaItems.restockGround()).isZero();
	}

	private static int countHeapItems(com.shatteredpixel.shatteredpixeldungeon.levels.Level level) {
		int total = 0;
		for (Heap heap : level.heaps.valueList()) {
			if (heap != null) {
				total += heap.items.size();
			}
		}
		return total;
	}
}
