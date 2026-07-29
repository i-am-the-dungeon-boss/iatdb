package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class ParalyticGasTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("paralytic gas applies Hero paralysis for 3 turns")
	void heroGetsShortParalysis() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		ParalyticGas gas = Blob.seed(hero.pos, 1000, ParalyticGas.class);
		Actor.add(gas);
		gas.act();

		Paralysis paralysis = hero.buff(Paralysis.class);
		Assertions.assertThat(paralysis).isNotNull();
		Assertions.assertThat(paralysis.cooldown()).isEqualTo(3f);
	}

	@Test
	@DisplayName("paralytic gas applies EchoBoss paralysis for 3 turns")
	void echoBossGetsShortParalysis() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		ParalyticGas gas = Blob.seed(boss.pos, 1000, ParalyticGas.class);
		Actor.add(gas);
		gas.act();

		Paralysis paralysis = boss.buff(Paralysis.class);
		Assertions.assertThat(paralysis).isNotNull();
		Assertions.assertThat(paralysis.cooldown()).isEqualTo(3f);
	}

	@Test
	@DisplayName("paralytic gas does not refresh Hero paralysis while already affected")
	void heroParalysisIsNotRefreshed() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		Buff.affect(hero, Paralysis.class, 1.5f);

		ParalyticGas gas = Blob.seed(hero.pos, 1000, ParalyticGas.class);
		Actor.add(gas);
		gas.act();

		Assertions.assertThat(hero.buff(Paralysis.class).cooldown()).isEqualTo(1.5f);
	}

	@Test
	@DisplayName("paralytic gas does not refresh EchoBoss paralysis while already affected")
	void echoBossParalysisIsNotRefreshed() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		Buff.affect(boss, Paralysis.class, 1.5f);

		ParalyticGas gas = Blob.seed(boss.pos, 1000, ParalyticGas.class);
		Actor.add(gas);
		gas.act();

		Assertions.assertThat(boss.buff(Paralysis.class).cooldown()).isEqualTo(1.5f);
	}

	@Test
	@DisplayName("paralytic gas keeps full paralysis duration on other mobs")
	void otherMobsKeepFullDuration() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Rat rat = new Rat();
		rat.pos = hero.pos + Dungeon.level.width();
		Actor.add(rat);
		Dungeon.level.mobs.add(rat);

		ParalyticGas gas = Blob.seed(rat.pos, 1000, ParalyticGas.class);
		Actor.add(gas);
		gas.act();

		Paralysis paralysis = rat.buff(Paralysis.class);
		Assertions.assertThat(paralysis).isNotNull();
		Assertions.assertThat(paralysis.cooldown()).isEqualTo(Paralysis.DURATION);
	}
}
