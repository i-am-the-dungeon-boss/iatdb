package com.shatteredpixel.shatteredpixeldungeon.items.potions;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FireImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Phase 1 characterization: Hero {@link Potion#drink(Hero)} via execute
 * AC_DRINK.
 */
@ExtendWith(GdxTestExtension.class)
class PotionHeroDrinkTest {

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
	@DisplayName("Hero drink detaches the potion and applies it to the hero")
	void heroDrinkDetachesPotionAndAppliesToHero() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);
		hero.HP = Math.max(1, hero.HT / 4);

		PotionOfHealing healing = new PotionOfHealing();
		healing.identify();
		healing.collect(hero.belongings.backpack);

		healing.drink(hero);

		Assertions.assertThat(hero.buff(Healing.class)).isNotNull();
		Assertions.assertThat(hero.belongings.getItem(PotionOfHealing.class)).isNull();
	}

	@Test
	@DisplayName("Hero drink records Catalog and potion talent use")
	void heroDrinkRecordsCatalogAndPotionTalentUse() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		PotionOfHealing healing = new PotionOfHealing();
		healing.identify();
		healing.collect(hero.belongings.backpack);
		int usesBefore = Catalog.useCount(PotionOfHealing.class);

		healing.drink(hero);

		Assertions.assertThat(Catalog.useCount(PotionOfHealing.class)).isGreaterThan(usesBefore);
	}

	@Test
	@DisplayName("Hero Dragon's Blood drink applies FireImbue on the hero not the echo")
	void heroDragonsBloodElixirBuffsHeroNotEcho() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		ElixirOfDragonsBlood elixir = new ElixirOfDragonsBlood();
		elixir.collect(hero.belongings.backpack);

		elixir.drink(hero);
		Assertions.assertThat(hero.buff(FireImbue.class)).isNotNull();
		Assertions.assertThat(boss.buff(FireImbue.class)).isNull();
		Assertions.assertThat(boss.getEchoHero().buff(FireImbue.class)).isNull();
	}

	@Test
	@DisplayName("Hero Arcane Armor drink applies ArcaneArmor on the hero not the echo")
	void heroArcaneArmorElixirBuffsHeroNotEcho() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		ElixirOfArcaneArmor elixir = new ElixirOfArcaneArmor();
		elixir.collect(hero.belongings.backpack);

		elixir.drink(hero);
		Assertions.assertThat(hero.buff(ArcaneArmor.class)).isNotNull();
		Assertions.assertThat(boss.buff(ArcaneArmor.class)).isNull();
		Assertions.assertThat(boss.getEchoHero().buff(ArcaneArmor.class)).isNull();
	}
}
