package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.DuelistArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MageArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.RogueArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Canvas-aligned Echo armor-ability kinds not already covered by
 * ArmorAbilityActivateAsTest.
 */
@ExtendWith(GdxTestExtension.class)
class EchoBossArsenalArmorAbilityKindsTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

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
	@DisplayName("Echo DeathMark activateAs marks the player from boss body aim")
	void deathMarkMarksPlayer() {
		Fight f = fight();
		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new DeathMark().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.player.buff(DeathMark.DeathMarkTracker.class)).isNotNull();
		Assertions.assertThat(armor.charge).isLessThan(100);
	}

	@Test
	@DisplayName("Echo ElementalBlast activateAs damages the Hero with imbued staff")
	void elementalBlastDamagesHero() {
		Fight f = fight();
		MageArmor armor = new MageArmor();
		armor.charge = 100;
		f.boss.getEchoHero().belongings.weapon = new MagesStaff(new WandOfMagicMissile());
		EchoTestSupport.attachInstantProjectileParent(f.boss);
		int hpBefore = f.player.HP;

		boolean ok = new ElementalBlast().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo ElementalStrike activateAs damages the Hero from boss body aim")
	void elementalStrikeDamagesHero() {
		Fight f = fight();
		DuelistArmor armor = new DuelistArmor();
		armor.charge = 100;
		f.boss.getEchoHero().belongings.weapon = new WornShortsword();
		EchoTestSupport.attachInstantProjectileParent(f.boss);
		int hpBefore = f.player.HP;

		boolean ok = new ElementalStrike().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
	}
}
