package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
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
class HallsBossFightTest {

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
	@DisplayName("moving toward Yog seals and spawns him; killing him reveals the exit stairs")
	void approachYogSealsSpawnsThenKillRevealsExitStairs() {
		HallsBossLevel level = BossFightTestSupport.createWithoutEcho(25, HallsBossLevel::new);
		Hero hero = Dungeon.hero;

		int exitCell = level.exit();
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[exitCell]).isNotEqualTo(Terrain.EXIT);
		Assertions.assertThat(BossFightTestSupport.findMob(level, YogDzewa.class)).isNull();

		hero.pos = level.entrance() + 2 * level.width();
		level.occupyCell(hero);

		Assertions.assertThat(level.locked).isTrue();
		YogDzewa yog = BossFightTestSupport.findMob(level, YogDzewa.class);
		Assertions.assertThat(yog).isNotNull();
		Assertions.assertThat(BossFightTestSupport.findMob(level, EchoBoss.class)).isNull();
		EchoTestSupport.linkStubSprite(yog);

		yog.die(hero);

		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.EXIT);
		Assertions.assertThat(level.map[level.entrance()]).isEqualTo(Terrain.ENTRANCE);
		Assertions.assertThat(level.getTransition(LevelTransition.Type.REGULAR_EXIT)).isNotNull();
	}

	@Test
	@DisplayName("moving toward the arena seals and spawns EchoBoss; killing him reveals the exit stairs")
	void approachArenaSealsSpawnsEchoBossThenKillRevealsExitStairs() {
		HallsBossLevel level = BossFightTestSupport.createWithPendingEcho(25, HallsBossLevel::new);
		Hero hero = Dungeon.hero;

		int exitCell = level.exit();
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[exitCell]).isNotEqualTo(Terrain.EXIT);
		Assertions.assertThat(BossFightTestSupport.findMob(level, EchoBoss.class)).isNull();

		hero.pos = level.entrance() + 2 * level.width();
		level.occupyCell(hero);

		Assertions.assertThat(level.locked).isTrue();
		EchoBoss boss = BossFightTestSupport.findMob(level, EchoBoss.class);
		Assertions.assertThat(boss).isNotNull();
		Assertions.assertThat(BossFightTestSupport.findMob(level, YogDzewa.class)).isNull();
		EchoTestSupport.linkStubSprite(boss);

		boss.die(hero);

		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.EXIT);
		Assertions.assertThat(level.map[level.entrance()]).isEqualTo(Terrain.ENTRANCE);
		Assertions.assertThat(level.getTransition(LevelTransition.Type.REGULAR_EXIT)).isNotNull();
	}
}
