package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoBossStopHuntingTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("stopHunting sets echo boss to passive and clears its target")
	void stopHuntingSetsPassiveAndClearsTarget() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		boss.state = boss.HUNTING;
		boss.enemy = hero;
		boss.enemySeen = true;

		boss.stopHunting();

		Assertions.assertThat(boss.state).isSameAs(boss.PASSIVE);
		Assertions.assertThat(boss.enemy).isNull();
		Assertions.assertThat(boss.enemySeen).isFalse();
	}

	@Test
	@DisplayName("stopAllHunting pacifies every live echo boss on the level")
	void stopAllHuntingPacifiesEveryLiveEchoBoss() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		boss.state = boss.HUNTING;
		boss.enemy = hero;

		int stopped = EchoBoss.stopAllHunting();

		Assertions.assertThat(stopped).isEqualTo(1);
		Assertions.assertThat(boss.state).isSameAs(boss.PASSIVE);
		Assertions.assertThat(boss.enemy).isNull();
	}
}
