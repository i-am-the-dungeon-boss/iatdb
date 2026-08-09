package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.AntiEntropy;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoBossCombatBuffTransferTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("AntiEntropy Burning from defenseProc lands on EchoBoss body not kit")
	void antiEntropyBurningLandsOnBodyNotKit() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 1);
		Arrays.fill(Dungeon.level.heroFOV, false);

		Hero kit = boss.getEchoHero();
		ClothArmor armor = new ClothArmor();
		armor.inscribe(new AntiEntropy());
		kit.belongings.armor = armor;

		boolean burned = false;
		for (int i = 0; i < 200; i++) {
			boss.defenseProc(player, 8);
			if (boss.buff(Burning.class) != null) {
				burned = true;
				break;
			}
		}

		Assertions.assertThat(burned).isTrue();
		Assertions.assertThat(kit.buff(Burning.class))
				.as("combat-applied Burning must leave the phantom kit")
				.isNull();
		Assertions.assertThat(boss.buff(Burning.class)).isNotNull();
	}
}
