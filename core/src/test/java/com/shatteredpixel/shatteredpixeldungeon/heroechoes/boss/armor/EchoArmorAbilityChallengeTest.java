package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.DuelistArmor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * Echo {@link Challenge} via {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityChallengeTest {

	private static Fight fight() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 1);
		fillFov(boss);
		return new Fight(player, boss);
	}

	private static void fillFov(EchoBoss boss) {
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
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
	@DisplayName("Echo Challenge activateAs applies DuelParticipant on the player")
	void challengeAppliesDuelParticipantOnPlayer() {
		Fight f = fight();
		DuelistArmor armor = new DuelistArmor();
		armor.charge = 100;

		boolean ok = new Challenge().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.player.buff(Challenge.DuelParticipant.class)).isNotNull();
		Assertions.assertThat(f.boss.buff(Challenge.DuelParticipant.class)).isNotNull();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Challenge activateAs spends ClassArmor charge from the kit")
	void challengeSpendsCharge() {
		Fight f = fight();
		DuelistArmor armor = new DuelistArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new Challenge().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Challenge activateAs clears busy so the boss turn can resume")
	void challengeClearsBusy() {
		Fight f = fight();
		DuelistArmor armor = new DuelistArmor();
		armor.charge = 100;

		boolean ok = new Challenge().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Challenge activateAs uses boss fieldOfView when heroFOV is empty")
	void challengeUsesBossFovWhenHeroFovEmpty() {
		Fight f = fight();
		Arrays.fill(Dungeon.level.heroFOV, false);
		DuelistArmor armor = new DuelistArmor();
		armor.charge = 100;

		boolean ok = new Challenge().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.player.buff(Challenge.DuelParticipant.class)).isNotNull();
		Assertions.assertThat(armor.charge).isLessThan(100);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}
}
