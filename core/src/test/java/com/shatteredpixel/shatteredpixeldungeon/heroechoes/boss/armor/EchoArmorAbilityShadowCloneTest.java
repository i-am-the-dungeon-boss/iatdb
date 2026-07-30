package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.RogueArmor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Echo {@link ShadowClone} via
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityShadowCloneTest {

	private static Fight fight(int bossOffset) {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, bossOffset);
		Assertions.assertThat(boss.getEchoHero().sprite).isNull();
		return new Fight(player, boss);
	}

	private static Fight fight() {
		return fight(1);
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
	@DisplayName("Echo ShadowClone activateAs spawns a ShadowAlly beside the boss body")
	void shadowCloneSpawnsAllyBesideBoss() {
		Fight f = fight();
		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new ShadowClone().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		ShadowClone.ShadowAlly ally = findShadowAlly();
		Assertions.assertThat(ally).isNotNull();
		Assertions.assertThat(Dungeon.level.distance(f.boss.pos, ally.pos)).isEqualTo(1);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ShadowClone activateAs spends ClassArmor charge from the kit")
	void shadowCloneSpendsCharge() {
		Fight f = fight();
		RogueArmor armor = new RogueArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new ShadowClone().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore - 35f);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ShadowClone activateAs clears busy so the boss turn can resume")
	void shadowCloneClearsBusy() {
		Fight f = fight();
		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new ShadowClone().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ShadowClone activateAs appear does not NPE when kit is headless")
	void shadowCloneAppearDoesNotNpeWhenHeadless() {
		Fight f = fight(2);

		Assertions.assertThat(f.boss.getEchoHero().sprite).isNull();

		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		Assertions.assertThatCode(() -> new ShadowClone().activateAs(f.echo(), armor, null))
				.doesNotThrowAnyException();

		ShadowClone.ShadowAlly ally = findShadowAlly();
		Assertions.assertThat(ally).isNotNull();
		Assertions.assertThat(ally.sprite).isNull();
		Assertions.assertThat(Dungeon.level.distance(f.boss.pos, ally.pos)).isEqualTo(1);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ShadowClone activateAs registers ShadowAlly in Actor when headless")
	void shadowCloneRegistersAllyInActorWhenHeadless() {
		Fight f = fight();
		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new ShadowClone().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		ShadowClone.ShadowAlly ally = findShadowAlly();
		Assertions.assertThat(ally).isNotNull();
		Assertions.assertThat(ally.sprite).isNull();

		boolean inActor = false;
		for (com.shatteredpixel.shatteredpixeldungeon.actors.Char ch : Actor.chars()) {
			if (ch == ally) {
				inActor = true;
				break;
			}
		}
		Assertions.assertThat(inActor).isTrue();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo ShadowClone activateAs refuses when ClassArmor charge is too low")
	void shadowCloneRefusesLowCharge() {
		Fight f = fight();
		RogueArmor armor = new RogueArmor();
		armor.charge = 0;

		boolean ok = new ShadowClone().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(findShadowAlly()).isNull();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	private static ShadowClone.ShadowAlly findShadowAlly() {
		for (Mob m : Dungeon.level.mobs) {
			if (m instanceof ShadowClone.ShadowAlly) {
				return (ShadowClone.ShadowAlly) m;
			}
		}
		return null;
	}
}
