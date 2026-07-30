package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.HuntressArmor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilitySpiritHawkTest {

	private static Fight fight() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 1);
		return new Fight(player, boss);
	}

	private static final class Fight {
		final EchoBoss boss;

		Fight(Hero player, EchoBoss boss) {
			this.boss = boss;
		}

		UseContext echo() {
			return UseContext.echo(boss);
		}
	}

	@Test
	@DisplayName("Echo SpiritHawk activateAs spawns a HawkAlly beside the boss body")
	void spawnsHawkAllyBesideBoss() {
		Fight f = fight();
		HuntressArmor armor = new HuntressArmor();
		armor.charge = 100;

		boolean ok = new SpiritHawk().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		SpiritHawk.HawkAlly hawk = findMob(SpiritHawk.HawkAlly.class);
		Assertions.assertThat(hawk).isNotNull();
		Assertions.assertThat(Dungeon.level.distance(f.boss.pos, hawk.pos)).isEqualTo(1);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo SpiritHawk activateAs spends armor charge")
	void spendsCharge() {
		Fight f = fight();
		HuntressArmor armor = new HuntressArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new SpiritHawk().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo SpiritHawk activateAs clears busy so the boss turn can resume")
	void clearsBusy() {
		Fight f = fight();
		HuntressArmor armor = new HuntressArmor();
		armor.charge = 100;

		boolean ok = new SpiritHawk().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo SpiritHawk activateAs registers HawkAlly in Actor when headless")
	void registersHawkInActorWhenHeadless() {
		Fight f = fight();
		HuntressArmor armor = new HuntressArmor();
		armor.charge = 100;

		boolean ok = new SpiritHawk().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		SpiritHawk.HawkAlly hawk = findMob(SpiritHawk.HawkAlly.class);
		Assertions.assertThat(hawk).isNotNull();
		Assertions.assertThat(hawk.sprite).isNull();
		Assertions.assertThat(Actor.findChar(hawk.pos)).isSameAs(hawk);
		Assertions.assertThat(Actor.chars()).contains(hawk);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	private static <T extends Mob> T findMob(Class<T> type) {
		for (Mob m : Dungeon.level.mobs) {
			if (type.isInstance(m)) {
				return type.cast(m);
			}
		}
		for (Char ch : Actor.chars()) {
			if (type.isInstance(ch)) {
				return type.cast(ch);
			}
		}
		return null;
	}
}
