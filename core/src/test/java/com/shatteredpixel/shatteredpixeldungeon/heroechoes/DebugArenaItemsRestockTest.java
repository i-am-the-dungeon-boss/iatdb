package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.DebugArenaLevel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class DebugArenaItemsRestockTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

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
