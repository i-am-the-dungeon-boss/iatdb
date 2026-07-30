package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
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
 * Echo {@link Endure} via {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityEndureTest {

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
	@DisplayName("Echo Endure activateAs applies EndureTracker on the boss body")
	void endureTrackerOnBossBody() {
		Fight f = fight();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new Endure().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.boss.buff(Endure.EndureTracker.class)).isNotNull();
		Assertions.assertThat(f.boss.getEchoHero().buff(Endure.EndureTracker.class)).isNull();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Endure activateAs spends ClassArmor charge from the kit")
	void endureSpendsCharge() {
		Fight f = fight();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new Endure().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore - 50f);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Endure activateAs clears busy so the boss turn can resume")
	void endureClearsBusy() {
		Fight f = fight();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new Endure().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Endure activateAs dispels Invisibility on the boss body")
	void endureDispelsInvisibility() {
		Fight f = fight();
		Buff.affect(f.boss, Invisibility.class, Invisibility.DURATION);
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new Endure().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.boss.buff(Invisibility.class)).isNull();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Endure activateAs refuses when ClassArmor charge is too low")
	void endureRefusesLowCharge() {
		Fight f = fight();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 0;

		boolean ok = new Endure().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(f.boss.buff(Endure.EndureTracker.class)).isNull();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Endure activateAs plays operate VFX on the boss body when parent is live")
	void endureOperateVfxOnBossBody() {
		Fight f = fight();
		EchoTestSupport.attachInstantProjectileParent(f.boss);
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new Endure().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(EchoTestSupport.stubSpriteOperateCalls(f.boss)).isGreaterThan(0);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}
}
