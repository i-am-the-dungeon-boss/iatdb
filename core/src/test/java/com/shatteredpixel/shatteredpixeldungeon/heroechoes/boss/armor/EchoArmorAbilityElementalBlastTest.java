package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoArmorAbilityAdapter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MageArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Echo {@link ElementalBlast} via
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.EchoArmorAbilityAdapter}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityElementalBlastTest {

	private static Fight fight() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 1);
		return new Fight(player, boss);
	}

	private static final class Fight {
		final Hero player;
		final EchoBoss boss;

		Fight(Hero player, EchoBoss boss) {
			this.player = player;
			this.boss = boss;
		}

		void imbueMagicMissileStaff() {
			boss.getEchoHero().belongings.weapon = new MagesStaff(new WandOfMagicMissile());
		}
	}

	@Test
	@DisplayName("Echo ElementalBlast adapter activate damages the Hero with imbued staff")
	void damagesHeroWithImbuedStaff() {
		Fight f = fight();
		f.imbueMagicMissileStaff();
		MageArmor armor = new MageArmor();
		armor.charge = 100;
		EchoTestSupport.attachInstantProjectileParent(f.boss);
		int hpBefore = f.player.HP;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new ElementalBlast(), f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);

		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ElementalBlast adapter activate spends MageArmor charge from the kit")
	void spendsMageArmorCharge() {
		Fight f = fight();
		f.imbueMagicMissileStaff();
		MageArmor armor = new MageArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new ElementalBlast(), f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore - 35f);

		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ElementalBlast adapter activate does not NPE when the body is off stage")
	void doesNotNpeWhenKitHeadless() {
		Fight f = fight();
		f.imbueMagicMissileStaff();
		Assertions.assertThat(f.boss.getEchoHero().sprite).isSameAs(f.boss.sprite);

		MageArmor armor = new MageArmor();
		armor.charge = 100;
		EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(f.boss);
		int hpBefore = f.player.HP;

		Assertions
				.assertThatCode(
						() -> EchoArmorAbilityAdapter.activate(f.boss, armor, new ElementalBlast(), f.player.pos))
				.doesNotThrowAnyException();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
		Assertions.assertThat(fx.magicMissileRecycles).isGreaterThan(0);

		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ElementalBlast adapter activate applies gameplay when boss body has no scene parent")
	void appliesGameplayWithoutSceneParent() {
		Fight f = fight();
		f.imbueMagicMissileStaff();
		Assertions.assertThat(f.boss.getEchoHero().sprite).isSameAs(f.boss.sprite);
		Assertions.assertThat(f.boss.sprite.parent).isNull();

		MageArmor armor = new MageArmor();
		armor.charge = 100;
		int hpBefore = f.player.HP;

		Assertions
				.assertThatCode(
						() -> EchoArmorAbilityAdapter.activate(f.boss, armor, new ElementalBlast(), f.player.pos))
				.doesNotThrowAnyException();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);

		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ElementalBlast adapter activate clears busy so the boss turn can resume")
	void clearsBusy() {
		Fight f = fight();
		f.imbueMagicMissileStaff();
		MageArmor armor = new MageArmor();
		armor.charge = 100;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new ElementalBlast(), f.player.pos);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ElementalBlast adapter activate refuses when kit has no imbued staff")
	void refusesWithoutStaff() {
		Fight f = fight();
		f.boss.getEchoHero().belongings.weapon = new WornShortsword();
		MageArmor armor = new MageArmor();
		armor.charge = 100;
		int hpBefore = f.player.HP;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new ElementalBlast(), f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isEqualTo(100);
		Assertions.assertThat(f.player.HP).isEqualTo(hpBefore);

		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}
}
