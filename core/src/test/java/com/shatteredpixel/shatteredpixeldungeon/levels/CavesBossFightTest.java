package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
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
class CavesBossFightTest {

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
	@DisplayName("approaching a pylon seals and spawns DM300; killing him unseals the gate to the exit")
	void approachPylonSealsSpawnsDM300ThenKillUnsealsGateToExit() {
		CavesBossLevel level = BossFightTestSupport.createWithoutEcho(15, CavesBossLevel::new);
		Hero hero = Dungeon.hero;

		int exitCell = level.exit();
		int gatePos = level.pointToCell(new com.watabou.utils.Point(
				CavesBossLevel.gate.left, CavesBossLevel.gate.top));
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.EXIT);
		Assertions.assertThat(level.solid[gatePos]).isTrue();
		Assertions.assertThat(level.invalidHeroPos(exitCell)).isTrue();
		Assertions.assertThat(BossFightTestSupport.findMob(level, DM300.class)).isNull();

		hero.pos = CavesBossLevel.pylonPositions[0] + 1;
		level.occupyCell(hero);

		Assertions.assertThat(level.locked).isTrue();
		Assertions.assertThat(level.state()).isEqualTo(CavesBossLevel.State.FIGHT);
		DM300 dm300 = BossFightTestSupport.findMob(level, DM300.class);
		Assertions.assertThat(dm300).isNotNull();
		Assertions.assertThat(BossFightTestSupport.findMob(level, EchoBoss.class)).isNull();
		EchoTestSupport.linkStubSprite(dm300);

		dm300.die(hero);

		Assertions.assertThat(level.state()).isEqualTo(CavesBossLevel.State.WON);
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.solid[gatePos]).isFalse();
		Assertions.assertThat(level.invalidHeroPos(exitCell)).isFalse();
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.EXIT);
		Assertions.assertThat(level.getTransition(LevelTransition.Type.REGULAR_EXIT)).isNotNull();
	}

	@Test
	@DisplayName("approaching a pylon seals and spawns EchoBoss; killing him unseals the gate to the exit")
	void approachPylonSealsSpawnsEchoBossThenKillUnsealsGateToExit() {
		CavesBossLevel level = BossFightTestSupport.createWithPendingEcho(15, CavesBossLevel::new);
		Hero hero = Dungeon.hero;

		int exitCell = level.exit();
		int gatePos = level.pointToCell(new com.watabou.utils.Point(
				CavesBossLevel.gate.left, CavesBossLevel.gate.top));
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.invalidHeroPos(exitCell)).isTrue();
		Assertions.assertThat(BossFightTestSupport.findMob(level, EchoBoss.class)).isNull();

		hero.pos = CavesBossLevel.pylonPositions[0] + 1;
		level.occupyCell(hero);

		Assertions.assertThat(level.locked).isTrue();
		Assertions.assertThat(level.state()).isEqualTo(CavesBossLevel.State.ECHO_BOSS);
		EchoBoss boss = BossFightTestSupport.findMob(level, EchoBoss.class);
		Assertions.assertThat(boss).isNotNull();
		Assertions.assertThat(BossFightTestSupport.findMob(level, DM300.class)).isNull();
		EchoTestSupport.linkStubSprite(boss);

		boss.die(hero);

		Assertions.assertThat(level.state()).isEqualTo(CavesBossLevel.State.WON);
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.solid[gatePos]).isFalse();
		Assertions.assertThat(level.invalidHeroPos(exitCell)).isFalse();
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.EXIT);
		Assertions.assertThat(level.getTransition(LevelTransition.Type.REGULAR_EXIT)).isNotNull();
	}
}
