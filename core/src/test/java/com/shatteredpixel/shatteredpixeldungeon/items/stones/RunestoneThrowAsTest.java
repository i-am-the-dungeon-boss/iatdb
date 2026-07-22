package com.shatteredpixel.shatteredpixeldungeon.items.stones;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
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
class RunestoneThrowAsTest {

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
	@DisplayName("Hero StoneOfBlast throwAs damages the foe and spends the hero turn")
	void heroStoneOfBlastThrowAsDamagesAndSpends() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);
		java.util.Arrays.fill(Dungeon.level.heroFOV, false);

		StoneOfBlast stone = new StoneOfBlast();
		stone.collect(hero.belongings.backpack);
		float before = hero.cooldown();
		int hpBefore = target.HP;

		boolean spent = stone.throwAs(UseContext.hero(hero), target.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
		Assertions.assertThat(hero.belongings.getItem(StoneOfBlast.class)).isNull();
		Assertions.assertThat(target.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo StoneOfBlast throwAs activates blast and damages hero without phantom spend")
	void echoStoneOfBlastThrowAsDamagesHeroWithoutPhantomSpend() {
		Hero player = EchoTestSupport.warriorHero();
		StoneOfBlast stone = new StoneOfBlast();
		stone.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		// Headless fixtures have no GameScene emitters — skip blast VFX.
		java.util.Arrays.fill(Dungeon.level.heroFOV, false);

		Hero kit = boss.getEchoHero();
		StoneOfBlast kitStone = kit.belongings.getItem(StoneOfBlast.class);
		Assertions.assertThat(kitStone).isNotNull();
		float kitBefore = kit.cooldown();
		int hpBefore = player.HP;

		boolean spent = kitStone.throwAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
		Assertions.assertThat(kit.belongings.getItem(StoneOfBlast.class)).isNull();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}
}
