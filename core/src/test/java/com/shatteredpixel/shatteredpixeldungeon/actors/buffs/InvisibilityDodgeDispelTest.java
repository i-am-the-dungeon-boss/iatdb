package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class InvisibilityDodgeDispelTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Echo missile miss does not dispel hero invisibility")
	void echoMissileMissKeepsHeroInvisibility() {
		Hero hero = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Buff.affect(hero, Invisibility.class, Invisibility.DURATION);
		Assertions.assertThat(hero.invisible).isGreaterThan(0);

		// Infinite evasion → guaranteed dodge (miss)
		Buff.affect(hero, MonkEnergy.MonkAbility.Focus.FocusBuff.class);

		ThrowingKnife knives = new ThrowingKnife();
		knives.identify();
		knives.quantity(3);
		Assertions.assertThat(knives.collect(boss.getEchoHero().belongings.backpack)).isTrue();

		boolean spent = knives.throwAs(UseContext.echo(boss), hero.pos);
		Assertions.assertThat(spent).isTrue();

		Assertions.assertThat(hero.buff(Invisibility.class))
				.as("dodged Echo shot must not break hero invisibility")
				.isNotNull();
		Assertions.assertThat(hero.invisible).isGreaterThan(0);
	}

	@Test
	@DisplayName("Hero missile miss does not dispel hero invisibility")
	void heroMissileMissKeepsInvisibility() {
		Hero hero = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Buff.affect(hero, Invisibility.class, Invisibility.DURATION);
		Buff.affect(boss, MonkEnergy.MonkAbility.Focus.FocusBuff.class);

		ThrowingKnife knife = new ThrowingKnife();
		knife.identify();
		knife.quantity(1);
		boolean hit = hero.shoot(boss, knife);

		Assertions.assertThat(hit).isFalse();
		Assertions.assertThat(hero.buff(Invisibility.class))
				.as("hero dodge-miss must keep invisibility")
				.isNotNull();
		Assertions.assertThat(hero.invisible).isGreaterThan(0);
	}

	@Test
	@DisplayName("EchoBoss melee miss does not dispel boss invisibility")
	void echoBossMeleeMissKeepsInvisibility() {
		Hero hero = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Buff.affect(boss, Invisibility.class, Invisibility.DURATION);
		Assertions.assertThat(boss.invisible).isGreaterThan(0);

		Buff.affect(hero, MonkEnergy.MonkAbility.Focus.FocusBuff.class);
		boss.aggro(hero);
		boss.onAttackComplete();

		Assertions.assertThat(boss.buff(Invisibility.class))
				.as("EchoBoss dodge-miss must keep invisibility")
				.isNotNull();
		Assertions.assertThat(boss.invisible).isGreaterThan(0);
	}

	@Test
	@DisplayName("any damage hit dispels EchoBoss invisibility")
	void anyDamageHitDispelsEchoBossInvisibility() {
		Hero hero = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Buff.affect(boss, Invisibility.class, Invisibility.DURATION);
		Assertions.assertThat(boss.invisible).isGreaterThan(0);

		// Wand zaps damage with the wand as src (not Dungeon.hero)
		boss.damage(5, new WandOfMagicMissile());

		Assertions.assertThat(boss.buff(Invisibility.class))
				.as("damaging an invisible EchoBoss must reveal them")
				.isNull();
		Assertions.assertThat(boss.invisible).isEqualTo(0);
	}

	@Test
	@DisplayName("any damage hit dispels hero invisibility")
	void anyDamageHitDispelsHeroInvisibility() {
		Hero hero = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Buff.affect(hero, Invisibility.class, Invisibility.DURATION);
		Assertions.assertThat(hero.invisible).isGreaterThan(0);

		hero.damage(5, boss);

		Assertions.assertThat(hero.buff(Invisibility.class))
				.as("damaging an invisible hero must reveal them")
				.isNull();
		Assertions.assertThat(hero.invisible).isEqualTo(0);
	}

	@Test
	@DisplayName("zero damage does not dispel hero invisibility")
	void zeroDamageKeepsHeroInvisibility() {
		Hero hero = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Buff.affect(hero, Invisibility.class, Invisibility.DURATION);

		hero.damage(0, boss);

		Assertions.assertThat(hero.buff(Invisibility.class)).isNotNull();
		Assertions.assertThat(hero.invisible).isGreaterThan(0);
	}

	@Test
	@DisplayName("Hero missile hit still dispels hero invisibility")
	void heroMissileHitDispelsInvisibility() {
		Hero hero = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Buff.affect(hero, Invisibility.class, Invisibility.DURATION);

		ThrowingKnife knife = new ThrowingKnife();
		knife.identify();
		knife.quantity(1);
		// Invisible surprise attack always hits
		boolean hit = hero.shoot(boss, knife);

		Assertions.assertThat(hit).isTrue();
		Assertions.assertThat(hero.buff(Invisibility.class)).isNull();
		Assertions.assertThat(hero.invisible).isEqualTo(0);
	}

	private static Hero rogueHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.ROGUE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}
}
