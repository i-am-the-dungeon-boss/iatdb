package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.watabou.noosa.Camera;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

@ExtendWith(GdxTestExtension.class)
class PrisonBossEchoFightTest {

	/**
	 * Matches {@code PrisonBossLevel} tengu cell center (10, 27) on a 32-wide map.
	 */
	private static final int TENGU_CELL_POS = 10 + 27 * 32;

	@BeforeEach
	void setUpUiStubs() {
		new TargetHealthIndicator();
		Camera.reset();
	}

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
		Statistics.qualifiedForBossChallengeBadge = false;
		TargetHealthIndicator.instance = null;
		Camera.reset();
	}

	@Test
	@DisplayName("entering the cell seals the floor, spawns EchoBoss, and killing him unseals with stairs down")
	void enterCellSealsSpawnsEchoBossThenKillUnsealsWithStairs() {
		PrisonBossLevel level = createPrisonBossLevelWithPendingEcho();
		Hero hero = Dungeon.hero;
		EchoTestSupport.linkStubSprite(hero);
		// Avoid TengusMask heap sprite path when GameScene is absent.
		hero.subClass = HeroSubClass.BERSERKER;

		int exitCell = level.exit();
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[exitCell]).isNotEqualTo(Terrain.EXIT);
		Assertions.assertThat(findMob(level, EchoBoss.class)).isNull();
		Assertions.assertThat(level.tengu()).isNull();

		hero.pos = TENGU_CELL_POS;
		level.occupyCell(hero);

		Assertions.assertThat(level.locked).isTrue();
		Assertions.assertThat(level.state()).isEqualTo(PrisonBossLevel.State.ECHO_BOSS);
		EchoBoss boss = findMob(level, EchoBoss.class);
		Assertions.assertThat(boss).isNotNull();
		Assertions.assertThat(findMob(level, Tengu.class)).isNull();
		EchoTestSupport.linkStubSprite(boss);

		boss.die(hero);

		Assertions.assertThat(level.state()).isEqualTo(PrisonBossLevel.State.WON);
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.EXIT);
		Assertions.assertThat(level.getTransition(LevelTransition.Type.REGULAR_EXIT)).isNotNull();
	}

	private static PrisonBossLevel createPrisonBossLevelWithPendingEcho() {
		Echo echo = EchoTestSupport.warriorEchoWithData(10);
		Hero hero = Dungeon.hero;
		CompositeEchoLookup.setEchoLookupForTests(d -> EchoTestSupport.outcomeWithPolicy(echo));
		Dungeon.depth = 10;
		Dungeon.seed = 1L;
		Dungeon.prefetchEchoBossForDepth(10);

		PrisonBossLevel level = new PrisonBossLevel();
		level.create();
		Dungeon.level = level;
		hero.pos = level.entrance();
		level.heroFOV = new boolean[level.length()];
		Arrays.fill(level.heroFOV, true);
		return level;
	}

	private static <T extends Mob> T findMob(Level level, Class<T> type) {
		for (Mob mob : level.mobs) {
			if (type.isInstance(mob)) {
				return type.cast(mob);
			}
		}
		return null;
	}
}
