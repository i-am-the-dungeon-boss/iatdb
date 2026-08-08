package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Phase 1 characterization: Hero {@link Item#cast(Hero, int)} entry point
 * before Echo throw adapters replace shared {@code throw}.
 */
@ExtendWith(GdxTestExtension.class)
class ItemHeroCastTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
		QuickSlotButton.lastTarget = null;
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
		QuickSlotButton.lastTarget = null;
	}

	@Test
	@DisplayName("Hero cast spends the throw delay and detaches one item")
	void heroCastSpendsThrowDelayAndDetachesOneItem() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		PotionOfParalyticGas gas = new PotionOfParalyticGas();
		gas.identify();
		gas.quantity(2);
		gas.collect(hero.belongings.backpack);
		float before = hero.cooldown();

		gas.cast(hero, target.pos);

		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
		PotionOfParalyticGas remaining = hero.belongings.getItem(PotionOfParalyticGas.class);
		Assertions.assertThat(remaining).isNotNull();
		Assertions.assertThat(remaining.quantity()).isEqualTo(1);
	}

	@Test
	@DisplayName("Hero cast targets the collision character in QuickSlot")
	void heroCastTargetsCollisionCharacterInQuickSlot() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		PotionOfParalyticGas gas = new PotionOfParalyticGas();
		gas.identify();
		gas.collect(hero.belongings.backpack);

		gas.cast(hero, target.pos);

		Assertions.assertThat(QuickSlotButton.lastTarget).isSameAs(target);
	}

	@Test
	@DisplayName("Hero throw applies Improvised Projectiles only to hostile targets")
	void heroThrowAppliesImprovisedProjectilesOnlyToHostileTargets() {
		Hero hero = EchoTestSupport.warriorHero();
		while (hero.pointsInTalent(Talent.IMPROVISED_PROJECTILES) < 1) {
			hero.upgradeTalent(Talent.IMPROVISED_PROJECTILES);
		}
		EchoBoss hostile = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, hostile, 2);

		PotionOfParalyticGas gas = new PotionOfParalyticGas();
		gas.identify();
		gas.collect(hero.belongings.backpack);

		gas.cast(hero, hostile.pos);

		Assertions.assertThat(hostile.buff(Blindness.class)).isNotNull();
		Assertions.assertThat(hero.buff(Talent.ImprovisedProjectileCooldown.class)).isNotNull();
	}

	@Test
	@DisplayName("Hero throw does not apply Improvised Projectiles to allied targets")
	void heroThrowDoesNotApplyImprovisedProjectilesToAlliedTargets() {
		Hero hero = EchoTestSupport.warriorHero();
		while (hero.pointsInTalent(Talent.IMPROVISED_PROJECTILES) < 1) {
			hero.upgradeTalent(Talent.IMPROVISED_PROJECTILES);
		}
		EchoBoss unused = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, unused, 2);

		Rat ally = new Rat();
		ally.alignment = Char.Alignment.ALLY;
		ally.pos = hero.pos + 1;
		Dungeon.level.mobs.add(ally);

		PotionOfParalyticGas gas = new PotionOfParalyticGas();
		gas.identify();
		gas.collect(hero.belongings.backpack);

		gas.cast(hero, ally.pos);

		Assertions.assertThat(ally.buff(Blindness.class)).isNull();
		Assertions.assertThat(hero.buff(Talent.ImprovisedProjectileCooldown.class)).isNull();
	}
}
