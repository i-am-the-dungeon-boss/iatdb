package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class PrisonBossEchoSplitTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
		Statistics.qualifiedForBossChallengeBadge = false;
	}

	@Test
	@DisplayName("pending echo creates no Tengu and starts an echo fight from START")
	void pendingEchoStartsEchoFightWithoutTengu() {
		PrisonBossLevel level = createPrisonBossLevelWithPendingEcho();

		Assertions.assertThat(level.tengu()).isNull();

		level.progress();

		Assertions.assertThat(level.state()).isEqualTo(PrisonBossLevel.State.ECHO_BOSS);
		Assertions.assertThat(level.tengu()).isNull();
		Assertions.assertThat(findMob(level, EchoBoss.class)).isNotNull();
		Assertions.assertThat(findMob(level, Tengu.class)).isNull();
		Assertions.assertThat(level.locked).isTrue();
	}

	@Test
	@DisplayName("without echo, START progress begins the Tengu fight")
	void withoutEchoStartsTenguFight() {
		PrisonBossLevel level = createPrisonBossLevelWithoutEcho();

		Assertions.assertThat(level.tengu()).isNotNull();

		level.progress();

		Assertions.assertThat(level.state()).isEqualTo(PrisonBossLevel.State.FIGHT_START);
		Assertions.assertThat(findMob(level, Tengu.class)).isSameAs(level.tengu());
		Assertions.assertThat(findMob(level, EchoBoss.class)).isNull();
	}

	@Test
	@DisplayName("echo fight victory completes without touching Tengu phases")
	void echoVictoryCompletesWithoutTenguPhases() {
		PrisonBossLevel level = createPrisonBossLevelWithPendingEcho();
		level.progress();
		Assertions.assertThat(level.state()).isEqualTo(PrisonBossLevel.State.ECHO_BOSS);

		level.completeEchoBossVictory();

		Assertions.assertThat(level.state()).isEqualTo(PrisonBossLevel.State.WON);
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.tengu()).isNull();
		Assertions.assertThat(level.state()).isNotIn(
				PrisonBossLevel.State.FIGHT_START,
				PrisonBossLevel.State.FIGHT_PAUSE,
				PrisonBossLevel.State.FIGHT_ARENA);
	}

	@Test
	@DisplayName("ECHO_BOSS progress is a no-op so Tengu phases stay isolated")
	void echoBossProgressDoesNotAdvanceTenguPhases() {
		PrisonBossLevel level = createPrisonBossLevelWithPendingEcho();
		level.progress();
		PrisonBossLevel.State before = level.state();

		level.progress();

		Assertions.assertThat(level.state()).isEqualTo(before);
		Assertions.assertThat(level.state()).isEqualTo(PrisonBossLevel.State.ECHO_BOSS);
		Assertions.assertThat(findMob(level, Tengu.class)).isNull();
	}

	@Test
	@DisplayName("echo fight state restores without a Tengu instance")
	void echoFightRestoresWithoutTengu() {
		int priorVersionCode = Game.versionCode;
		Game.versionCode = ShatteredPixelDungeon.v2_5_4;
		try {
			PrisonBossLevel level = createPrisonBossLevelWithPendingEcho();
			level.progress();

			Bundle bundle = new Bundle();
			level.storeInBundle(bundle);

			PrisonBossLevel restored = new PrisonBossLevel();
			restored.restoreFromBundle(bundle);
			Dungeon.level = restored;

			Assertions.assertThat(restored.state()).isEqualTo(PrisonBossLevel.State.ECHO_BOSS);
			Assertions.assertThat(restored.tengu()).isNull();
			Assertions.assertThat(findMob(restored, EchoBoss.class)).isNotNull();

			restored.completeEchoBossVictory();
			Assertions.assertThat(restored.state()).isEqualTo(PrisonBossLevel.State.WON);
		} finally {
			Game.versionCode = priorVersionCode;
		}
	}

	private static PrisonBossLevel createPrisonBossLevelWithPendingEcho() {
		Hero hero = EchoTestSupport.warriorHero();
		Echo echo = EchoTestSupport.warriorEchoWithData(10);
		CompositeEchoLookup.setEchoLookupForTests(d -> EchoTestSupport.outcomeWithPolicy(echo));
		Dungeon.depth = 10;
		Dungeon.seed = 1L;
		Dungeon.prefetchEchoBossForDepth(10);

		PrisonBossLevel level = new PrisonBossLevel();
		level.create();
		Dungeon.level = level;
		hero.pos = level.entrance();
		return level;
	}

	private static PrisonBossLevel createPrisonBossLevelWithoutEcho() {
		Hero hero = EchoTestSupport.warriorHero();
		CompositeEchoLookup.setEchoLookupForTests(
				d -> com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome.notFound());
		Dungeon.depth = 10;
		Dungeon.seed = 1L;
		Dungeon.prefetchEchoBossForDepth(10);

		PrisonBossLevel level = new PrisonBossLevel();
		level.create();
		Dungeon.level = level;
		hero.pos = level.entrance();
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
