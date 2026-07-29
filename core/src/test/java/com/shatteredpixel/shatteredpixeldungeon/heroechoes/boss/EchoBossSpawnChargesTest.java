package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoBossSpawnChargesTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("EchoBoss spawn refills CloakOfShadows to chargeCap")
	void spawnRefillsCloakCharges() {
		Hero player = rogueHero();
		CloakOfShadows seed = player.belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(seed).isNotNull();
		String fullStatus = seed.status();
		seed.directCharge(-100);

		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);

		CloakOfShadows cloak = boss.getEchoHero().belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		Assertions.assertThat(cloak.status())
				.as("spawned echo cloak must match a full charge display")
				.isEqualTo(fullStatus);
		Assertions.assertThat(cloak.canActivateStealth()).isTrue();
	}

	@Test
	@DisplayName("EchoBoss spawn refills backpack wands to maxCharges")
	void spawnRefillsWandCharges() {
		Hero player = mageHero();
		WandOfMagicMissile seed = new WandOfMagicMissile();
		seed.identify();
		seed.curCharges = 0;
		seed.partialCharge = 0f;
		seed.collect(player.belongings.backpack);

		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);

		WandOfMagicMissile wand = boss.getEchoHero().belongings.getItem(WandOfMagicMissile.class);
		Assertions.assertThat(wand).isNotNull();
		Assertions.assertThat(wand.curCharges).isEqualTo(wand.maxCharges);
		Assertions.assertThat(wand.partialCharge).isZero();
	}

	@Test
	@DisplayName("EchoBoss spawn refills MagesStaff imbued wand charges")
	void spawnRefillsMagesStaffCharges() {
		Hero player = mageHero();
		MagesStaff staff = player.belongings.getItem(MagesStaff.class);
		Assertions.assertThat(staff).isNotNull();
		Assertions.assertThat(staff.wand()).isNotNull();
		staff.setWandCharges(0);

		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);

		MagesStaff kitStaff = boss.getEchoHero().belongings.getItem(MagesStaff.class);
		Assertions.assertThat(kitStaff).isNotNull();
		Assertions.assertThat(kitStaff.wand()).isNotNull();
		Assertions.assertThat(kitStaff.wand().curCharges)
				.isEqualTo(kitStaff.wand().maxCharges);
	}

	private static Hero rogueHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.ROGUE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static Hero mageHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}
}
