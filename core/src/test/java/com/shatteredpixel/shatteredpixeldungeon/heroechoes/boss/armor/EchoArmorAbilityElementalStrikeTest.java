package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.DuelistArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Echo {@link ElementalStrike} via
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityElementalStrikeTest {

	private static Fight fight(int bossOffset) {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, bossOffset);
		Assertions.assertThat(player.sprite.ch).isSameAs(player);
		Assertions.assertThat(boss.sprite.ch).isSameAs(boss);
		Assertions.assertThat(boss.getEchoHero().sprite).isNull();
		return new Fight(player, boss);
	}

	private static Fight fight() {
		return fight(2);
	}

	private static final class Fight {
		final Hero player;
		final EchoBoss boss;

		Fight(Hero player, EchoBoss boss) {
			this.player = player;
			this.boss = boss;
		}

		UseContext echo() {
			return UseContext.echo(boss);
		}

		DuelistArmor armorWithMelee() {
			DuelistArmor armor = new DuelistArmor();
			armor.charge = 100;
			boss.getEchoHero().belongings.weapon = new WornShortsword();
			return armor;
		}
	}

	@Test
	@DisplayName("Echo ElementalStrike activateAs damages the player from boss body aim")
	void elementalStrikeDamagesPlayerFromBossAim() {
		Fight f = fight();
		DuelistArmor armor = f.armorWithMelee();
		EchoTestSupport.attachInstantProjectileParent(f.boss);
		int hpBefore = f.player.HP;
		f.player.invisible = 1;

		boolean ok = new ElementalStrike().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ElementalStrike activateAs spends DuelistArmor charge from the kit")
	void elementalStrikeSpendsCharge() {
		Fight f = fight();
		DuelistArmor armor = f.armorWithMelee();
		float chargeBefore = armor.charge;

		boolean ok = new ElementalStrike().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore - 25f);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ElementalStrike activateAs clears busy so the boss turn can resume")
	void elementalStrikeClearsBusy() {
		Fight f = fight();
		DuelistArmor armor = f.armorWithMelee();

		boolean ok = new ElementalStrike().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ElementalStrike activateAs dispels Invisibility on the boss body")
	void elementalStrikeDispelsInvisibility() {
		Fight f = fight();
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(
				f.boss, Invisibility.class, Invisibility.DURATION);
		DuelistArmor armor = f.armorWithMelee();

		boolean ok = new ElementalStrike().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.boss.buff(Invisibility.class)).isNull();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ElementalStrike activateAs fires cone MagicMissile when the body sprite has a parent")
	void elementalStrikeFiresMagicMissileWhenParentLive() {
		Fight f = fight();
		EchoTestSupport.InstantProjectileGroup fx =
				EchoTestSupport.attachInstantProjectileParent(f.boss);
		DuelistArmor armor = f.armorWithMelee();
		f.player.invisible = 1;

		boolean ok = new ElementalStrike().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(fx.magicMissileRecycles).isGreaterThan(0);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ElementalStrike activateAs applies cone damage synchronously without a sprite parent")
	void elementalStrikeHeadlessStillDamagesPlayer() {
		Fight f = fight();
		DuelistArmor armor = f.armorWithMelee();
		int hpBefore = f.player.HP;
		f.player.invisible = 1;
		Assertions.assertThat(UseContext.canWorldFx(f.boss)).isFalse();

		boolean ok = new ElementalStrike().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ElementalStrike activateAs refuses when ClassArmor charge is too low")
	void elementalStrikeRefusesLowCharge() {
		Fight f = fight();
		DuelistArmor armor = f.armorWithMelee();
		armor.charge = 0;
		int hpBefore = f.player.HP;

		boolean ok = new ElementalStrike().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(f.player.HP).isEqualTo(hpBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ElementalStrike activateAs keeps kit headless after the strike")
	void elementalStrikeKeepsKitHeadless() {
		Fight f = fight();
		DuelistArmor armor = f.armorWithMelee();
		EchoTestSupport.attachInstantProjectileParent(f.boss);

		boolean ok = new ElementalStrike().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.boss.getEchoHero().sprite).isNull();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}
}
