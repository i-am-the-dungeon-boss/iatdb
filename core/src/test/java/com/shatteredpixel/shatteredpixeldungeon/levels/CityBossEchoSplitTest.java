package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
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

import java.util.Arrays;

@ExtendWith(GdxTestExtension.class)
class CityBossEchoSplitTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
		Statistics.qualifiedForBossChallengeBadge = false;
	}

	@Test
	@DisplayName("pending echo starts a city echo fight without DwarfKing")
	void pendingEchoStartsEchoFightWithoutDwarfKing() {
		CityBossLevel level = createCityBossLevelWithPendingEcho();

		level.seal();

		Assertions.assertThat(findMob(level, EchoBoss.class)).isNotNull();
		Assertions.assertThat(findMob(level, DwarfKing.class)).isNull();
		Assertions.assertThat(level.locked).isTrue();
		Assertions.assertThat(level.mobs.stream().filter(EchoBoss.class::isInstance).count()).isEqualTo(1);
	}

	@Test
	@DisplayName("without echo, seal begins the DwarfKing fight")
	void withoutEchoStartsDwarfKingFight() {
		CityBossLevel level = createCityBossLevelWithoutEcho();

		level.seal();

		Assertions.assertThat(findMob(level, DwarfKing.class)).isNotNull();
		Assertions.assertThat(findMob(level, EchoBoss.class)).isNull();
		Assertions.assertThat(level.locked).isTrue();
	}

	@Test
	@DisplayName("echo seal is idempotent so a second seal does not spawn another boss")
	void echoSealIsIdempotent() {
		CityBossLevel level = createCityBossLevelWithPendingEcho();
		level.seal();
		Assertions.assertThat(level.mobs.stream().filter(EchoBoss.class::isInstance).count()).isEqualTo(1);

		level.seal();

		Assertions.assertThat(level.mobs.stream().filter(EchoBoss.class::isInstance).count()).isEqualTo(1);
	}

	@Test
	@DisplayName("visible echo notice during seal does not spawn a second boss")
	void visibleNoticeDuringSealDoesNotDoubleSpawn() {
		CityBossLevel level = createCityBossLevelWithPendingEcho();
		Arrays.fill(level.heroFOV, true);

		level.seal();

		Assertions.assertThat(level.mobs.stream().filter(EchoBoss.class::isInstance).count()).isEqualTo(1);
	}

	private static CityBossLevel createCityBossLevelWithPendingEcho() {
		Echo echo = EchoTestSupport.warriorEchoWithData(20);
		Hero hero = Dungeon.hero;
		CompositeEchoLookup.setEchoLookupForTests(d -> EchoTestSupport.outcomeWithPolicy(echo));
		Dungeon.depth = 20;
		Dungeon.seed = 1L;
		Dungeon.prefetchEchoBossForDepth(20);

		CityBossLevel level = new CityBossLevel();
		level.create();
		Dungeon.level = level;
		hero.pos = CityBossLevel.throne;
		level.heroFOV = new boolean[level.length()];
		return level;
	}

	private static CityBossLevel createCityBossLevelWithoutEcho() {
		Hero hero = EchoTestSupport.warriorHero();
		CompositeEchoLookup.setEchoLookupForTests(d -> EchoLookupOutcome.notFound());
		Dungeon.depth = 20;
		Dungeon.seed = 1L;
		Dungeon.prefetchEchoBossForDepth(20);

		CityBossLevel level = new CityBossLevel();
		level.create();
		Dungeon.level = level;
		hero.pos = CityBossLevel.throne;
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
