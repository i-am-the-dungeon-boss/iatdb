package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class HallsBossEchoSplitTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
		Statistics.qualifiedForBossChallengeBadge = false;
	}

	@Test
	@DisplayName("pending echo starts a halls echo fight without YogDzewa")
	void pendingEchoStartsEchoFightWithoutYog() {
		HallsBossLevel level = createHallsBossLevelWithPendingEcho();

		level.seal();

		Assertions.assertThat(findMob(level, EchoBoss.class)).isNotNull();
		Assertions.assertThat(findMob(level, YogDzewa.class)).isNull();
		Assertions.assertThat(level.locked).isTrue();
		Assertions.assertThat(level.mobs.stream().filter(EchoBoss.class::isInstance).count()).isEqualTo(1);
	}

	@Test
	@DisplayName("without echo, seal begins the YogDzewa fight")
	void withoutEchoStartsYogFight() {
		HallsBossLevel level = createHallsBossLevelWithoutEcho();

		level.seal();

		Assertions.assertThat(findMob(level, YogDzewa.class)).isNotNull();
		Assertions.assertThat(findMob(level, EchoBoss.class)).isNull();
		Assertions.assertThat(level.locked).isTrue();
	}

	@Test
	@DisplayName("echo seal is idempotent so a second seal does not spawn another boss")
	void echoSealIsIdempotent() {
		HallsBossLevel level = createHallsBossLevelWithPendingEcho();
		level.seal();
		Assertions.assertThat(level.mobs.stream().filter(EchoBoss.class::isInstance).count()).isEqualTo(1);

		level.seal();

		Assertions.assertThat(level.mobs.stream().filter(EchoBoss.class::isInstance).count()).isEqualTo(1);
	}

	private static HallsBossLevel createHallsBossLevelWithPendingEcho() {
		Echo echo = EchoTestSupport.warriorEchoWithData(25);
		Hero hero = Dungeon.hero;
		CompositeEchoLookup.setEchoLookupForTests(d -> EchoTestSupport.outcomeWithPolicy(echo));
		Dungeon.depth = 25;
		Dungeon.seed = 1L;
		Dungeon.prefetchEchoBossForDepth(25);

		HallsBossLevel level = new HallsBossLevel();
		level.create();
		Dungeon.level = level;
		hero.pos = level.entrance();
		level.heroFOV = new boolean[level.length()];
		return level;
	}

	private static HallsBossLevel createHallsBossLevelWithoutEcho() {
		Hero hero = EchoTestSupport.warriorHero();
		CompositeEchoLookup.setEchoLookupForTests(d -> EchoLookupOutcome.notFound());
		Dungeon.depth = 25;
		Dungeon.seed = 1L;
		Dungeon.prefetchEchoBossForDepth(25);

		HallsBossLevel level = new HallsBossLevel();
		level.create();
		Dungeon.level = level;
		hero.pos = level.entrance();
		level.heroFOV = new boolean[level.length()];
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
