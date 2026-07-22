package com.shatteredpixel.shatteredpixeldungeon.items.potions;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class PotionThrowAsTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
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
	@DisplayName("Echo potion throwAs detaches and shatters without phantom spend")
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

		boolean spent = potion.throwAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
		Assertions.assertThat(kit.belongings.getItem(PotionOfParalyticGas.class)).isNull();
	}

	@Test
	@DisplayName("Echo drinkAs applies haste on the boss body not the kit")
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

		boolean spent = potion.drinkAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Haste.class)).isNotNull();
		Assertions.assertThat(kit.buff(Haste.class)).isNull();
		Assertions.assertThat(kit.belongings.getItem(PotionOfHaste.class)).isNull();
	}

	@Test
	@DisplayName("Hero drinkAs applies heal on the hero and spends time")
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
		boolean spent = healing.drinkAs(UseContext.hero(hero));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hero.buff(
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing.class)).isNotNull();
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
		Assertions.assertThat(hero.belongings.getItem(PotionOfHealing.class)).isNull();
	}
}
