package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.RogueArmor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * Echo {@link DeathMark} via
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityDeathMarkTest {

	private static Fight fight() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		fillFov(boss);
		return new Fight(player, boss);
	}

	private static void fillFov(EchoBoss boss) {
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		Dungeon.level.heroFOV = boss.fieldOfView;
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
	@DisplayName("Echo DeathMark activateAs applies DeathMarkTracker on the player")
	void marksPlayer() {
		Fight f = fight();
		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new DeathMark().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.player.buff(DeathMark.DeathMarkTracker.class)).isNotNull();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo DeathMark activateAs spends ClassArmor charge from the kit")
	void spendsCharge() {
		Fight f = fight();
		RogueArmor armor = new RogueArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new DeathMark().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo DeathMark activateAs clears busy so the boss turn can resume")
	void clearsBusy() {
		Fight f = fight();
		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new DeathMark().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo DeathMark refuse on empty cell clears busy for next turn")
	void refuseEmptyCellClearsBusy() {
		Fight f = fight();
		int empty = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(empty).isGreaterThanOrEqualTo(0);
		RogueArmor armor = new RogueArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new DeathMark().activateAs(f.echo(), armor, empty);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo DeathMark refuse on ally cell clears busy for next turn")
	void refuseAllyCellClearsBusy() {
		Fight f = fight();
		RogueArmor armor = new RogueArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new DeathMark().activateAs(f.echo(), armor, f.boss.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	private static int emptyAdjacent(int from) {
		for (int i : PathFinder.NEIGHBOURS8) {
			int cell = from + i;
			if (cell >= 0 && cell < Dungeon.level.length()
					&& Dungeon.level.map[cell] == Terrain.EMPTY
					&& Dungeon.hero.pos != cell
					&& com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell) == null) {
				return cell;
			}
		}
		return -1;
	}
}
