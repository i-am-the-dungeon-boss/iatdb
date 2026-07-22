package com.shatteredpixel.shatteredpixeldungeon.items.stones;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class InventoryStoneUseAsTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Echo useAs StoneOfEnchantment enchants kit weapon and consumes the stone")
	void echoEnchantmentEnchantsKitWeapon() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		WornShortsword sword = new WornShortsword();
		sword.identify();
		kit.belongings.weapon = sword;

		StoneOfEnchantment stone = new StoneOfEnchantment();
		stone.collect(kit.belongings.backpack);

		boolean ok = stone.useAs(UseContext.echo(boss));

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(sword.enchantment).isNotNull();
		Assertions.assertThat(kit.belongings.getItem(StoneOfEnchantment.class)).isNull();
	}
}
