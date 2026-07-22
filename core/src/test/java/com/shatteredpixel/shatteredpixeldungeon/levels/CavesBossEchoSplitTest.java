package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
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
class CavesBossEchoSplitTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
		Statistics.qualifiedForBossChallengeBadge = false;
	}

	@Test
	@DisplayName("pending echo starts an echo fight without DM300")
	void pendingEchoStartsEchoFightWithoutDM300() {
		CavesBossLevel level = createCavesBossLevelWithPendingEcho();

		level.seal();

		Assertions.assertThat(level.state()).isEqualTo(CavesBossLevel.State.ECHO_BOSS);
		Assertions.assertThat(findMob(level, EchoBoss.class)).isNotNull();
		Assertions.assertThat(findMob(level, DM300.class)).isNull();
		Assertions.assertThat(level.locked).isTrue();
		Assertions.assertThat(level.mobs.stream().filter(EchoBoss.class::isInstance).count()).isEqualTo(1);
	}

	@Test
	@DisplayName("without echo, seal begins the DM300 fight")
	void withoutEchoStartsDM300Fight() {
		CavesBossLevel level = createCavesBossLevelWithoutEcho();

		level.seal();

		Assertions.assertThat(level.state()).isEqualTo(CavesBossLevel.State.FIGHT);
		Assertions.assertThat(findMob(level, DM300.class)).isNotNull();
		Assertions.assertThat(findMob(level, EchoBoss.class)).isNull();
	}

	@Test
	@DisplayName("echo fight victory completes without DM300 pylon unseal path")
	void echoVictoryCompletesWithoutDM300Phases() {
		CavesBossLevel level = createCavesBossLevelWithPendingEcho();
		level.seal();
		Assertions.assertThat(level.state()).isEqualTo(CavesBossLevel.State.ECHO_BOSS);

		level.completeEchoBossVictory();

		Assertions.assertThat(level.state()).isEqualTo(CavesBossLevel.State.WON);
		Assertions.assertThat(level.locked).isFalse();
		Assertions.assertThat(level.map[level.entrance()]).isEqualTo(Terrain.ENTRANCE);
		Assertions.assertThat(level.blobs.get(CavesBossLevel.PylonEnergy.class)).isNull();
	}

	@Test
	@DisplayName("echo seal is idempotent so a second seal does not spawn another boss")
	void echoSealIsIdempotent() {
		CavesBossLevel level = createCavesBossLevelWithPendingEcho();
		level.seal();
		Assertions.assertThat(level.mobs.stream().filter(EchoBoss.class::isInstance).count()).isEqualTo(1);

		level.seal();

		Assertions.assertThat(level.state()).isEqualTo(CavesBossLevel.State.ECHO_BOSS);
		Assertions.assertThat(level.mobs.stream().filter(EchoBoss.class::isInstance).count()).isEqualTo(1);
	}

	@Test
	@DisplayName("echo fight state restores and completes without DM300")
	void echoFightRestoresWithoutDM300() {
		int priorVersionCode = Game.versionCode;
		Game.versionCode = ShatteredPixelDungeon.v2_5_4;
		try {
			CavesBossLevel level = createCavesBossLevelWithPendingEcho();
			level.seal();

			Bundle bundle = new Bundle();
			level.storeInBundle(bundle);

			CavesBossLevel restored = new CavesBossLevel();
			restored.restoreFromBundle(bundle);
			Dungeon.level = restored;

			Assertions.assertThat(restored.state()).isEqualTo(CavesBossLevel.State.ECHO_BOSS);
			Assertions.assertThat(findMob(restored, EchoBoss.class)).isNotNull();
			Assertions.assertThat(findMob(restored, DM300.class)).isNull();

			restored.completeEchoBossVictory();
			Assertions.assertThat(restored.state()).isEqualTo(CavesBossLevel.State.WON);
		} finally {
			Game.versionCode = priorVersionCode;
		}
	}

	private static CavesBossLevel createCavesBossLevelWithPendingEcho() {
		Echo echo = EchoTestSupport.warriorEchoWithData(15);
		Hero hero = Dungeon.hero;
		CompositeEchoLookup.setEchoLookupForTests(d -> EchoTestSupport.outcomeWithPolicy(echo));
		Dungeon.depth = 15;
		Dungeon.seed = 1L;
		Dungeon.prefetchEchoBossForDepth(15);

		CavesBossLevel level = new CavesBossLevel();
		level.create();
		Dungeon.level = level;
		hero.pos = level.pointToCell(CavesBossLevel.mainArena.center());
		return level;
	}

	private static CavesBossLevel createCavesBossLevelWithoutEcho() {
		Hero hero = EchoTestSupport.warriorHero();
		CompositeEchoLookup.setEchoLookupForTests(
				d -> com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome.notFound());
		Dungeon.depth = 15;
		Dungeon.seed = 1L;
		Dungeon.prefetchEchoBossForDepth(15);

		CavesBossLevel level = new CavesBossLevel();
		level.create();
		Dungeon.level = level;
		hero.pos = level.pointToCell(CavesBossLevel.mainArena.center());
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
