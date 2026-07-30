package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClericArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Grim;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Echo {@link Trinity} via {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityTrinityTest {

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

		UseContext echo() {
			return UseContext.echo(boss);
		}
	}

	@Test
	@DisplayName("Echo Trinity activateAs applies BodyFormBuff on boss body from imbued enchantment")
	void trinityBodyFormBuffOnBossFromEnchantment() {
		Fight f = fight();
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		Trinity trinity = new Trinity();
		trinity.imbueBodyForm(new Blazing());

		boolean ok = trinity.activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		BodyForm.BodyFormBuff buff = f.boss.buff(BodyForm.BodyFormBuff.class);
		Assertions.assertThat(buff).isNotNull();
		Assertions.assertThat(buff.enchant()).isInstanceOf(Blazing.class);
		Assertions.assertThat(f.boss.getEchoHero().buff(BodyForm.BodyFormBuff.class)).isNull();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Trinity activateAs applies BodyFormBuff on boss body from imbued glyph")
	void trinityBodyFormBuffOnBossFromGlyph() {
		Fight f = fight();
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		Trinity trinity = new Trinity();
		trinity.imbueBodyForm(new AntiMagic());

		boolean ok = trinity.activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		BodyForm.BodyFormBuff buff = f.boss.buff(BodyForm.BodyFormBuff.class);
		Assertions.assertThat(buff).isNotNull();
		Assertions.assertThat(buff.glyph()).isInstanceOf(AntiMagic.class);
		Assertions.assertThat(f.boss.getEchoHero().buff(BodyForm.BodyFormBuff.class)).isNull();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Trinity activateAs spends standard ClericArmor charge for common bodyForm")
	void trinitySpendsStandardCharge() {
		Fight f = fight();
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		Trinity trinity = new Trinity();
		trinity.imbueBodyForm(new Blazing());

		boolean ok = trinity.activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isEqualTo(75f);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Trinity activateAs spends double ClericArmor charge for rare bodyForm")
	void trinitySpendsDoubleChargeForRareEnchantment() {
		Fight f = fight();
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		Trinity trinity = new Trinity();
		trinity.imbueBodyForm(new Grim());

		boolean ok = trinity.activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isEqualTo(50f);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Trinity activateAs clears busy so the boss turn can resume")
	void trinityClearsBusy() {
		Fight f = fight();
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		Trinity trinity = new Trinity();
		trinity.imbueBodyForm(new Blazing());

		boolean ok = trinity.activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Trinity activateAs dispels Invisibility on the boss body")
	void trinityDispelsInvisibility() {
		Fight f = fight();
		Buff.affect(f.boss, Invisibility.class, Invisibility.DURATION);
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		Trinity trinity = new Trinity();
		trinity.imbueBodyForm(new Blazing());

		boolean ok = trinity.activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.boss.buff(Invisibility.class)).isNull();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Trinity activateAs refuses when ClassArmor charge is too low")
	void trinityRefusesLowCharge() {
		Fight f = fight();
		ClericArmor armor = new ClericArmor();
		armor.charge = 0;

		Trinity trinity = new Trinity();
		trinity.imbueBodyForm(new Blazing());

		boolean ok = trinity.activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(f.boss.buff(BodyForm.BodyFormBuff.class)).isNull();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Trinity activateAs refuses when bodyForm is not imbued")
	void trinityRefusesWithoutBodyForm() {
		Fight f = fight();
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		boolean ok = new Trinity().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(f.boss.buff(BodyForm.BodyFormBuff.class)).isNull();
		Assertions.assertThat(armor.charge).isEqualTo(100f);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}
}
