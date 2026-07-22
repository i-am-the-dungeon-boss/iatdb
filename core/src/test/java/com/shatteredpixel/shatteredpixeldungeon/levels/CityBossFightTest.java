package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class CityBossFightTest {

	/** Same formula as CityBossLevel.topDoor (package-private layout). */
	private static final int TOP_DOOR = 7 + 25 * 15;

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
	@DisplayName("entering the arena seals and spawns DwarfKing; killing him opens the path to the exit")
	void enterArenaSealsSpawnsDwarfKingThenKillOpensPathToExit() {
		CityBossLevel level = BossFightTestSupport.createWithoutEcho(20, CityBossLevel::new);
		Hero hero = Dungeon.hero;

		int exitCell = level.exit();
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.EXIT);
		Assertions.assertThat(level.map[TOP_DOOR]).isEqualTo(Terrain.LOCKED_DOOR);
		Assertions.assertThat(level.invalidHeroPos(exitCell)).isTrue();
		Assertions.assertThat(BossFightTestSupport.findMob(level, DwarfKing.class)).isNull();

		hero.pos = CityBossLevel.throne;
		level.occupyCell(hero);

		Assertions.assertThat(level.locked).isTrue();
		DwarfKing king = BossFightTestSupport.findMob(level, DwarfKing.class);
		Assertions.assertThat(king).isNotNull();
		Assertions.assertThat(BossFightTestSupport.findMob(level, EchoBoss.class)).isNull();
		EchoTestSupport.linkStubSprite(king);

		king.die(hero);

		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[TOP_DOOR]).isEqualTo(Terrain.DOOR);
		Assertions.assertThat(level.invalidHeroPos(exitCell)).isFalse();
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.EXIT);
		Assertions.assertThat(level.getTransition(LevelTransition.Type.REGULAR_EXIT)).isNotNull();
	}

	@Test
	@DisplayName("entering the arena seals and spawns EchoBoss; killing him opens the path to the exit")
	void enterArenaSealsSpawnsEchoBossThenKillOpensPathToExit() {
		CityBossLevel level = BossFightTestSupport.createWithPendingEcho(20, CityBossLevel::new);
		Hero hero = Dungeon.hero;

		int exitCell = level.exit();
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[TOP_DOOR]).isEqualTo(Terrain.LOCKED_DOOR);
		Assertions.assertThat(level.invalidHeroPos(exitCell)).isTrue();
		Assertions.assertThat(BossFightTestSupport.findMob(level, EchoBoss.class)).isNull();

		hero.pos = CityBossLevel.throne;
		level.occupyCell(hero);

		Assertions.assertThat(level.locked).isTrue();
		EchoBoss boss = BossFightTestSupport.findMob(level, EchoBoss.class);
		Assertions.assertThat(boss).isNotNull();
		Assertions.assertThat(BossFightTestSupport.findMob(level, DwarfKing.class)).isNull();
		EchoTestSupport.linkStubSprite(boss);

		boss.die(hero);

		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[TOP_DOOR]).isEqualTo(Terrain.DOOR);
		Assertions.assertThat(level.invalidHeroPos(exitCell)).isFalse();
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.EXIT);
		Assertions.assertThat(level.getTransition(LevelTransition.Type.REGULAR_EXIT)).isNotNull();
	}
}
