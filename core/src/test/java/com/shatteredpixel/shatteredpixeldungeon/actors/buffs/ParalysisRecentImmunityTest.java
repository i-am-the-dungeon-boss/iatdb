package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas;
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
class ParalysisRecentImmunityTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Hero gains 3-turn paralysis immunity after paralysis ends")
	void heroGainsImmunityAfterParalysis() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Buff.affect(hero, Paralysis.class, 1f).detach();

		Paralysis.Immunity immunity = hero.buff(Paralysis.Immunity.class);
		Assertions.assertThat(immunity).isNotNull();
		Assertions.assertThat(immunity.cooldown()).isEqualTo(3f);
		Assertions.assertThat(hero.isImmune(Paralysis.class)).isTrue();
		Assertions.assertThat(hero.isImmune(ParalyticGas.class)).isTrue();
	}

	@Test
	@DisplayName("EchoBoss gains 3-turn paralysis immunity after paralysis ends")
	void echoBossGainsImmunityAfterParalysis() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Buff.affect(boss, Paralysis.class, 1f).detach();

		Paralysis.Immunity immunity = boss.buff(Paralysis.Immunity.class);
		Assertions.assertThat(immunity).isNotNull();
		Assertions.assertThat(immunity.cooldown()).isEqualTo(3f);
		Assertions.assertThat(boss.isImmune(Paralysis.class)).isTrue();
		Assertions.assertThat(boss.isImmune(ParalyticGas.class)).isTrue();
	}

	@Test
	@DisplayName("other mobs do not gain paralysis immunity after paralysis ends")
	void otherMobsDoNotGainImmunity() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Rat rat = new Rat();
		rat.pos = hero.pos + Dungeon.level.width();
		Actor.add(rat);
		Dungeon.level.mobs.add(rat);

		Buff.affect(rat, Paralysis.class, 1f).detach();

		Assertions.assertThat(rat.buff(Paralysis.Immunity.class)).isNull();
		Assertions.assertThat(rat.isImmune(Paralysis.class)).isFalse();
	}

	@Test
	@DisplayName("paralytic gas does not re-paralyze Hero during recent immunity")
	void gasSkipsHeroDuringImmunity() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		Buff.affect(hero, Paralysis.class, 1f).detach();

		ParalyticGas gas = Blob.seed(hero.pos, 1000, ParalyticGas.class);
		Actor.add(gas);
		gas.act();

		Assertions.assertThat(hero.buff(Paralysis.class)).isNull();
	}
}
