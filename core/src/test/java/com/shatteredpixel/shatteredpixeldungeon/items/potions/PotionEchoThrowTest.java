package com.shatteredpixel.shatteredpixeldungeon.items.potions;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoPotionAdapter;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoThrowAdapter;

@ExtendWith(GdxTestExtension.class)
class PotionEchoThrowTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
		Potion.initColors();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("echo potion throw does not identify type for the hero")
	void echoPotionThrowDoesNotIdentifyForHero() {
		Hero player = EchoTestSupport.warriorHero();
		PotionOfLiquidFlame seed = new PotionOfLiquidFlame();
		Assertions.assertThat(seed.isKnown()).isFalse();
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Potion potion = boss.getEchoHero().belongings.getItem(PotionOfLiquidFlame.class);
		Assertions.assertThat(potion).isNotNull();
		Assertions.assertThat(potion.isKnown()).isFalse();

		Assertions.assertThat(EchoThrowAdapter.throwItem(boss, potion, player.pos)).isTrue();

		Assertions.assertThat(new PotionOfLiquidFlame().isKnown())
				.as("echo shatter must not teach the living hero the potion type")
				.isFalse();
	}

	@Test
	@DisplayName("hero potion throw still identifies type when seen")
	void heroPotionThrowStillIdentifiesWhenSeen() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		PotionOfLiquidFlame flame = new PotionOfLiquidFlame();
		Assertions.assertThat(flame.isKnown()).isFalse();
		flame.collect(hero.belongings.backpack);

		flame.cast(hero, target.pos);

		Assertions.assertThat(new PotionOfLiquidFlame().isKnown()).isTrue();
	}

	@Test
	@DisplayName("Hero potion cast spends the hero turn")
	void heroCastSpendsTurn() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		PotionOfParalyticGas gas = new PotionOfParalyticGas();
		gas.identify();
		gas.collect(hero.belongings.backpack);

		float before = hero.cooldown();
		gas.cast(hero, target.pos);

		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
		Assertions.assertThat(hero.belongings.getItem(PotionOfParalyticGas.class)).isNull();
	}

	@Test
	@DisplayName("Echo potion throw detaches and shatters without phantom spend")
	void echoThrowAsDetachesWithoutPhantomSpend() {
		Hero player = EchoTestSupport.warriorHero();
		PotionOfParalyticGas gas = new PotionOfParalyticGas();
		gas.identify();
		gas.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Potion potion = kit.belongings.getItem(PotionOfParalyticGas.class);
		Assertions.assertThat(potion).isNotNull();
		float kitBefore = kit.cooldown();

		boolean spent = EchoThrowAdapter.throwItem(boss, potion, player.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
		Assertions.assertThat(kit.belongings.getItem(PotionOfParalyticGas.class)).isNull();
	}

	@Test
	@DisplayName("Echo drink applies haste on the boss body not the kit")
	void echoDrinkAsBuffsBody() {
		Hero player = EchoTestSupport.warriorHero();
		PotionOfHaste haste = new PotionOfHaste();
		haste.identify();
		haste.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Potion potion = kit.belongings.getItem(PotionOfHaste.class);
		Assertions.assertThat(potion).isNotNull();

		boolean spent = EchoPotionAdapter.drink(boss, potion);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Haste.class)).isNotNull();
		Assertions.assertThat(kit.buff(Haste.class)).isNull();
		Assertions.assertThat(kit.belongings.getItem(PotionOfHaste.class)).isNull();
	}

	@Test
	@DisplayName("Hero drink applies heal on the hero and spends time")
	void heroDrinkAsHealsAndSpends() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);
		hero.HP = Math.max(1, hero.HT / 4);

		PotionOfHealing healing = new PotionOfHealing();
		healing.identify();
		healing.collect(hero.belongings.backpack);

		float before = hero.cooldown();
		healing.drink(hero);

		Assertions.assertThat(hero.buff(
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing.class)).isNotNull();
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
		Assertions.assertThat(hero.belongings.getItem(PotionOfHealing.class)).isNull();
	}
}
