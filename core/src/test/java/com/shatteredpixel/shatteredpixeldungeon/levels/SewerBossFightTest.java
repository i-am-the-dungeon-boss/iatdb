package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class SewerBossFightTest {

	@BeforeEach
	void setUp() {
		BossFightTestSupport.setUpUiStubs();
	}

	@AfterEach
	void cleanup() {
		BossFightTestSupport.tearDownUiStubs();
		Statistics.qualifiedForBossChallengeBadge = false;
	}

	@Test
	@DisplayName("noticing Goo seals the floor, and killing him unseals with a key for the locked exit")
	void noticeGooSealsThenKillUnsealsWithKeyForExit() {
		SewerBossLevel level = BossFightTestSupport.createWithoutEcho(5, SewerBossLevel::new);
		Hero hero = Dungeon.hero;

		int exitCell = level.exit();
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.LOCKED_EXIT);
		Goo goo = BossFightTestSupport.findMob(level, Goo.class);
		Assertions.assertThat(goo).isNotNull();
		EchoTestSupport.linkStubSprite(goo);

		goo.notice();

		Assertions.assertThat(level.locked).isTrue();
		Assertions.assertThat(level.map[level.entrance()]).isEqualTo(Terrain.WATER);

		goo.die(hero);

		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[level.entrance()]).isEqualTo(Terrain.ENTRANCE);
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.LOCKED_EXIT);
		Assertions.assertThat(findWornKey(level)).isNotNull();
		Assertions.assertThat(level.getTransition(LevelTransition.Type.REGULAR_EXIT)).isNotNull();
	}

	@Test
	@DisplayName("noticing EchoBoss seals the floor, and killing him unseals with a key for the locked exit")
	void noticeEchoBossSealsThenKillUnsealsWithKeyForExit() {
		SewerBossLevel level = BossFightTestSupport.createWithPendingEcho(5, SewerBossLevel::new);
		Hero hero = Dungeon.hero;

		int exitCell = level.exit();
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.LOCKED_EXIT);
		Assertions.assertThat(BossFightTestSupport.findMob(level, Goo.class)).isNull();
		EchoBoss boss = BossFightTestSupport.findMob(level, EchoBoss.class);
		Assertions.assertThat(boss).isNotNull();
		EchoTestSupport.linkStubSprite(boss);

		boss.notice();

		Assertions.assertThat(level.locked).isTrue();
		Assertions.assertThat(level.map[level.entrance()]).isEqualTo(Terrain.WATER);

		boss.die(hero);

		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[level.entrance()]).isEqualTo(Terrain.ENTRANCE);
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.LOCKED_EXIT);
		Assertions.assertThat(findWornKey(level)).isNotNull();
		Assertions.assertThat(level.getTransition(LevelTransition.Type.REGULAR_EXIT)).isNotNull();
	}

	private static WornKey findWornKey(Level level) {
		for (Heap heap : level.heaps.valueList()) {
			for (Item item : heap.items) {
				if (item instanceof WornKey) {
					return (WornKey) item;
				}
			}
		}
		return null;
	}
}
