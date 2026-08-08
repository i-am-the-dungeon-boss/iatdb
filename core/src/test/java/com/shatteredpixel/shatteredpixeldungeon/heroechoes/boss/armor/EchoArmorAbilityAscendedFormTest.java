package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoArmorAbilityAdapter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClericArmor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Echo {@link AscendedForm} via
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.EchoArmorAbilityAdapter}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityAscendedFormTest {

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

	}

	@Test
	@DisplayName("Echo AscendedForm adapter activate applies AscendBuff on the boss body")
	void ascendBuffOnBossBody() {
		Fight f = fight();
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new AscendedForm(), null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.boss.buff(AscendedForm.AscendBuff.class)).isNotNull();
		Assertions.assertThat(f.boss.getEchoHero().buff(AscendedForm.AscendBuff.class)).isNull();

		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo AscendedForm adapter activate spends ClassArmor charge from the kit")
	void ascendedFormSpendsCharge() {
		Fight f = fight();
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new AscendedForm(), null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore - 50f);

		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo AscendedForm adapter activate clears busy so the boss turn can resume")
	void ascendedFormClearsBusy() {
		Fight f = fight();
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new AscendedForm(), null);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo AscendedForm adapter activate dispels Invisibility on the boss body")
	void ascendedFormDispelsInvisibility() {
		Fight f = fight();
		Buff.affect(f.boss, Invisibility.class, Invisibility.DURATION);
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new AscendedForm(), null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.boss.buff(Invisibility.class)).isNull();

		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo AscendedForm adapter activate refuses when ClassArmor charge is too low")
	void ascendedFormRefusesLowCharge() {
		Fight f = fight();
		ClericArmor armor = new ClericArmor();
		armor.charge = 0;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new AscendedForm(), null);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(f.boss.buff(AscendedForm.AscendBuff.class)).isNull();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo AscendedForm adapter activate plays operate VFX on the boss body when parent is live")
	void ascendedFormOperateVfxOnBossBody() {
		Fight f = fight();
		EchoTestSupport.attachInstantProjectileParent(f.boss);
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new AscendedForm(), null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(EchoTestSupport.stubSpriteOperateCalls(f.boss)).isGreaterThan(0);

		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}
}
