package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Metabolism;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vampiric;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Fallthrough melee routes combat through the headless phantom kit. Procs that
 * touch {@code attacker/defender.sprite} or heal kit HP must not NPE and must
 * affect the EchoBoss body (Family A / ANDROID-1T class).
 */
@ExtendWith(GdxTestExtension.class)
class EchoBossHeadlessKitCombatTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("attackProc with Vampiric does not NPE when kit is headless")
	void attackProcWithVampiricDoesNotNpeWhenKitHeadless() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 1);

		Hero kit = boss.getEchoHero();
		Assertions.assertThat(kit.sprite).isNull();
		// Kit defaults to ALLY; Vampiric requires opposed alignment (boss is ENEMY).
		kit.alignment = Char.Alignment.ENEMY;

		WornShortsword sword = new WornShortsword();
		sword.enchant(new Vampiric());
		kit.belongings.weapon = sword;
		kit.HP = 1;
		kit.HT = 100;
		boss.HP = 1;
		boss.HT = 100;

		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 200; i++) {
				boss.attackProc(player, 20);
			}
		}).doesNotThrowAnyException();
		Assertions.assertThat(kit.sprite)
				.as("combat borrow must restore headless kit")
				.isNull();
	}

	@Test
	@DisplayName("attackProc with Vampiric heals EchoBoss body not only kit")
	void attackProcWithVampiricHealsBodyNotOnlyKit() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 1);

		Hero kit = boss.getEchoHero();
		kit.alignment = Char.Alignment.ENEMY;
		WornShortsword sword = new WornShortsword();
		sword.enchant(new Vampiric());
		kit.belongings.weapon = sword;
		kit.HP = 1;
		kit.HT = 100;
		boss.HP = 1;
		boss.HT = 100;

		boolean bodyHealed = false;
		for (int i = 0; i < 200; i++) {
			boss.HP = 1;
			kit.HP = 1;
			boss.attackProc(player, 20);
			if (boss.HP > 1) {
				bodyHealed = true;
				break;
			}
		}

		Assertions.assertThat(bodyHealed)
				.as("Vampiric heal during EchoBoss attackProc must raise body HP")
				.isTrue();
		Assertions.assertThat(kit.sprite).isNull();
	}

	@Test
	@DisplayName("defenseProc with Metabolism does not NPE when kit is headless")
	void defenseProcWithMetabolismDoesNotNpeWhenKitHeadless() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 1);

		Hero kit = boss.getEchoHero();
		Assertions.assertThat(kit.sprite).isNull();

		ClothArmor armor = new ClothArmor();
		armor.inscribe(new Metabolism());
		kit.belongings.armor = armor;
		kit.HP = 10;
		kit.HT = 100;
		boss.HP = 10;
		boss.HT = 100;
		Buff.affect(kit, Hunger.class).satisfy(Hunger.STARVING / 2f);

		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 200; i++) {
				boss.defenseProc(player, 8);
			}
		}).doesNotThrowAnyException();
		Assertions.assertThat(kit.sprite).isNull();
	}

	@Test
	@DisplayName("defenseProc with Metabolism heals EchoBoss body not only kit")
	void defenseProcWithMetabolismHealsBodyNotOnlyKit() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 1);

		Hero kit = boss.getEchoHero();
		ClothArmor armor = new ClothArmor();
		armor.inscribe(new Metabolism());
		kit.belongings.armor = armor;
		Buff.affect(kit, Hunger.class).satisfy(Hunger.STARVING / 2f);

		boolean bodyHealed = false;
		for (int i = 0; i < 200; i++) {
			boss.HP = 10;
			kit.HP = 10;
			boss.HT = 100;
			kit.HT = 100;
			boss.defenseProc(player, 8);
			if (boss.HP > 10) {
				bodyHealed = true;
				break;
			}
		}

		Assertions.assertThat(bodyHealed)
				.as("Metabolism heal during EchoBoss defenseProc must raise body HP")
				.isTrue();
		Assertions.assertThat(kit.sprite).isNull();
	}
}
