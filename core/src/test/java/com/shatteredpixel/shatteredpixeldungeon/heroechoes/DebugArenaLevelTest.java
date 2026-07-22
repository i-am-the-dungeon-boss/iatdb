package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.DebugArenaLevel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class DebugArenaLevelTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("debug play mode routes every depth to DebugArenaLevel")
	void debugModeRoutesToDebugArenaLevel() {
		com.shatteredpixel.shatteredpixeldungeon.DebugSettings.setDebugBuildOverride(true);
		Dungeon.echoPlayMode = EchoPlayMode.DEBUG;

		Assertions.assertThat(Dungeon.levelClassForDepth(1, 0)).isEqualTo(DebugArenaLevel.class);
		Assertions.assertThat(Dungeon.levelClassForDepth(5, 0)).isEqualTo(DebugArenaLevel.class);
		Assertions.assertThat(Dungeon.levelClassForDepth(25, 0)).isEqualTo(DebugArenaLevel.class);
	}

	@Test
	@DisplayName("arena drops all catalog items and spawns a mirror echo of the hero")
	void arenaDropsAllItemsAndSpawnsMirrorEcho() {
		com.shatteredpixel.shatteredpixeldungeon.DebugSettings.setDebugBuildOverride(true);
		Dungeon.echoPlayMode = EchoPlayMode.DEBUG;
		Dungeon.depth = 1;
		Dungeon.branch = 0;
		Dungeon.seed = 1L;
		Dungeon.hero = new Hero();
		HeroClass.WARRIOR.initHero(Dungeon.hero);
		Dungeon.hero.live();

		DebugArenaLevel level = new DebugArenaLevel();
		level.create();

		int heapItems = 0;
		for (Heap heap : level.heaps.valueList()) {
			if (heap != null) {
				heapItems += heap.items.size();
			}
		}
		Assertions.assertThat(heapItems).isGreaterThanOrEqualTo(DebugArenaItems.createAll().size());

		EchoBoss mirror = null;
		for (Mob mob : level.mobs) {
			if (mob instanceof EchoBoss) {
				mirror = (EchoBoss) mob;
				break;
			}
		}
		Assertions.assertThat(mirror).isNotNull();
		Assertions.assertThat(mirror.getEcho().heroClass).isEqualTo(HeroClass.WARRIOR.name());
		Assertions.assertThat(Dungeon.isEchoBossActive()).isTrue();
		Assertions.assertThat(Dungeon.getPendingEcho()).isSameAs(mirror.getEcho());
	}
}
