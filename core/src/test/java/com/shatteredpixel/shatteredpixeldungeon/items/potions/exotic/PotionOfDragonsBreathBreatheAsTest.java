package com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
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
class PotionOfDragonsBreathBreatheAsTest {

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
	@DisplayName("Echo drinkAs refuses without a target cell (no foot shatter)")
	void echoDrinkAsRefusesWithoutTarget() {
		Hero player = EchoTestSupport.warriorHero();
		PotionOfDragonsBreath breath = new PotionOfDragonsBreath();
		breath.identify();
		breath.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		PotionOfDragonsBreath potion =
				(PotionOfDragonsBreath) kit.belongings.getItem(PotionOfDragonsBreath.class);
		Assertions.assertThat(potion).isNotNull();

		boolean spent = potion.drinkAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isFalse();
		Assertions.assertThat(kit.belongings.getItem(PotionOfDragonsBreath.class)).isNotNull();
		Assertions.assertThat(player.buff(Burning.class)).isNull();
	}

	@Test
	@DisplayName("Echo breatheAs applies Burning and Cripple on the aimed foe from boss body")
	void echoBreatheAsBurnsAimedFoe() {
		Hero player = EchoTestSupport.warriorHero();
		PotionOfDragonsBreath breath = new PotionOfDragonsBreath();
		breath.identify();
		breath.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		PotionOfDragonsBreath potion =
				(PotionOfDragonsBreath) kit.belongings.getItem(PotionOfDragonsBreath.class);
		Assertions.assertThat(potion).isNotNull();
		Assertions.assertThat(player.sprite.ch).isSameAs(player);
		Assertions.assertThat(boss.sprite.ch).isSameAs(boss);
		Assertions.assertThat(kit.sprite).isNull();
		float kitBefore = kit.cooldown();

		boolean spent = potion.breatheAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
		Assertions.assertThat(kit.belongings.getItem(PotionOfDragonsBreath.class)).isNull();
		Assertions.assertThat(player.buff(Burning.class)).isNotNull();
		Assertions.assertThat(player.buff(Cripple.class)).isNotNull();
	}

	@Test
	@DisplayName("Hero breatheAs applies Burning on the foe and spends the hero turn")
	void heroBreatheAsBurnsAndSpends() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		PotionOfDragonsBreath breath = new PotionOfDragonsBreath();
		breath.identify();
		breath.collect(hero.belongings.backpack);

		float before = hero.cooldown();
		boolean spent = breath.breatheAs(UseContext.hero(hero), target.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
		Assertions.assertThat(hero.belongings.getItem(PotionOfDragonsBreath.class)).isNull();
		Assertions.assertThat(target.buff(Burning.class)).isNotNull();
		Assertions.assertThat(target.buff(Cripple.class)).isNotNull();
	}
}
