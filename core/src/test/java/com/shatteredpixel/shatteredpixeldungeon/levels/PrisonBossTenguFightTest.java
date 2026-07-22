package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
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
class PrisonBossTenguFightTest {

	/**
	 * Matches {@code PrisonBossLevel} tengu cell center (10, 27) on a 32-wide map.
	 */
	private static final int TENGU_CELL_POS = 10 + 27 * 32;
	/**
	 * Hallway cell with y <= startHallway.top + 1 — resumes fight from FIGHT_PAUSE.
	 */
	private static final int HALLWAY_RESUME_POS = 10 + 8 * 32;

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
	@DisplayName("entering Tengu's cell seals the floor, spawns him, and clearing the fight unseals with stairs down")
	void enterCellSealsSpawnsBossThenKillUnsealsWithStairs() {
		PrisonBossLevel level = createPrisonBossLevelWithoutEcho();
		Hero hero = Dungeon.hero;
		EchoTestSupport.linkStubSprite(hero);
		// Avoid TengusMask heap sprite path when GameScene is absent.
		hero.subClass = HeroSubClass.BERSERKER;

		int exitCell = level.exit();
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[exitCell]).isNotEqualTo(Terrain.EXIT);
		Assertions.assertThat(findMob(level, Tengu.class)).isNull();

		hero.pos = TENGU_CELL_POS;
		level.occupyCell(hero);

		Assertions.assertThat(level.locked).isTrue();
		Assertions.assertThat(level.state()).isEqualTo(PrisonBossLevel.State.FIGHT_START);
		Tengu tengu = findMob(level, Tengu.class);
		Assertions.assertThat(tengu).isNotNull().isSameAs(level.tengu());
		Assertions.assertThat(findMob(level, EchoBoss.class)).isNull();
		EchoTestSupport.linkStubSprite(tengu);

		// Phase 1 ends when Tengu drops to half HP.
		tengu.HP = tengu.HT / 2 + 1;
		tengu.damage(tengu.HT, hero);
		Assertions.assertThat(level.state()).isEqualTo(PrisonBossLevel.State.FIGHT_PAUSE);

		// Walking back up the hallway resumes the arena phase.
		EchoTestSupport.linkStubSprite(tengu);
		hero.pos = HALLWAY_RESUME_POS;
		level.occupyCell(hero);
		Assertions.assertThat(level.state()).isEqualTo(PrisonBossLevel.State.FIGHT_ARENA);
		EchoTestSupport.linkStubSprite(tengu);

		// Lethal hit schedules the same progress() path as a live fight.
		tengu.HP = 1;
		tengu.damage(tengu.HT, hero);
		Assertions.assertThat(tengu.HP).isZero();
		level.progress();

		Assertions.assertThat(level.state()).isEqualTo(PrisonBossLevel.State.WON);
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[exitCell]).isEqualTo(Terrain.EXIT);
		Assertions.assertThat(level.getTransition(LevelTransition.Type.REGULAR_EXIT)).isNotNull();
	}

	private static PrisonBossLevel createPrisonBossLevelWithoutEcho() {
		Hero hero = EchoTestSupport.warriorHero();
		CompositeEchoLookup.setEchoLookupForTests(d -> EchoLookupOutcome.notFound());
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
