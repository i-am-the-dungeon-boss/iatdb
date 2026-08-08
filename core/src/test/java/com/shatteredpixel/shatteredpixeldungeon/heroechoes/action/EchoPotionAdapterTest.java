package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoPotionAdapterTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
		com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion.initColors();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("Echo potion adapter applies haste on the boss body not the kit")
	void echoPotionAdapterBuffsBody() {
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
	@DisplayName("Echo potion adapter rejects Hero-only Strength without consuming")
	void echoPotionAdapterRejectsHeroOnlyStrength() {
		Hero player = EchoTestSupport.warriorHero();
		PotionOfStrength strength = new PotionOfStrength();
		strength.identify();
		strength.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Potion potion = boss.getEchoHero().belongings.getItem(PotionOfStrength.class);
		Assertions.assertThat(potion).isNotNull();

		boolean spent = EchoPotionAdapter.drink(boss, potion);

		Assertions.assertThat(spent).isFalse();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfStrength.class)).isNotNull();
	}
}
