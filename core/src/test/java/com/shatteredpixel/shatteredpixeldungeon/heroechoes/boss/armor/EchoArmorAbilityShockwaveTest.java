package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.WarriorArmor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Echo {@link Shockwave} via
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityShockwaveTest {

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
	}

	@Test
	@DisplayName("Echo Shockwave activateAs damages the player in the cone")
	void shockwaveDamagesPlayerInCone() {
		Fight f = fight();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;
		int hpBefore = f.player.HP;
		f.player.invisible = 1;

		boolean ok = new Shockwave().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Shockwave activateAs applies Paralysis or Cripple on cone targets")
	void shockwaveAppliesDebuffOnConeTargets() {
		Fight f = fight();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;
		f.player.invisible = 1;

		boolean ok = new Shockwave().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(
				f.player.buff(Paralysis.class) != null || f.player.buff(Cripple.class) != null)
				.isTrue();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Shockwave activateAs spends ClassArmor charge from the kit")
	void shockwaveSpendsCharge() {
		Fight f = fight();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new Shockwave().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore - 35f);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Shockwave activateAs clears busy so the boss turn can resume")
	void shockwaveClearsBusy() {
		Fight f = fight();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new Shockwave().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Shockwave activateAs dispels Invisibility on the boss body")
	void shockwaveDispelsInvisibility() {
		Fight f = fight();
		Buff.affect(f.boss, Invisibility.class, Invisibility.DURATION);
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new Shockwave().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.boss.buff(Invisibility.class)).isNull();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Shockwave activateAs fires cone MagicMissile when the body sprite has a parent")
	void shockwaveFiresMagicMissileWhenParentLive() {
		Fight f = fight();
		EchoTestSupport.InstantProjectileGroup fx =
				EchoTestSupport.attachInstantProjectileParent(f.boss);
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;
		f.player.invisible = 1;

		boolean ok = new Shockwave().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(fx.magicMissileRecycles).isGreaterThan(0);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Shockwave activateAs applies cone damage synchronously without a sprite parent")
	void shockwaveHeadlessStillDamagesPlayer() {
		Fight f = fight();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;
		int hpBefore = f.player.HP;
		f.player.invisible = 1;
		Assertions.assertThat(UseContext.canWorldFx(f.boss)).isFalse();

		boolean ok = new Shockwave().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Shockwave activateAs refuses when ClassArmor charge is too low")
	void shockwaveRefusesLowCharge() {
		Fight f = fight();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 0;
		int hpBefore = f.player.HP;

		boolean ok = new Shockwave().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(f.player.HP).isEqualTo(hpBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Shockwave activateAs skips effect when aimed at the boss body")
	void shockwaveSkipsSelfTarget() {
		Fight f = fight();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;
		int hpBefore = f.player.HP;

		boolean ok = new Shockwave().activateAs(f.echo(), armor, f.boss.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isEqualTo(100f);
		Assertions.assertThat(f.player.HP).isEqualTo(hpBefore);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}
}
