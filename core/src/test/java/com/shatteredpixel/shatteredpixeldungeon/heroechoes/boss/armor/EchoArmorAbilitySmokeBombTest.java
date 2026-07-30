package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb;
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

/**
 * Echo {@link SmokeBomb} via
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilitySmokeBombTest {

	private static Fight fight() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		return new Fight(player, boss);
	}

	private static void grantTalent(Hero kit, Talent talent, int points) {
		while (kit.talents.size() < 4) {
			kit.talents.add(new java.util.LinkedHashMap<>());
		}
		kit.talents.get(3).put(talent, points);
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
	@DisplayName("Echo SmokeBomb teleports the boss body to an empty cell")
	void smokeBombTeleportsBossBody() {
		Fight f = fight();
		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		int start = f.boss.pos;
		int kitPosBefore = f.boss.getEchoHero().pos;

		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new SmokeBomb().activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.boss.pos).isEqualTo(dest);
		Assertions.assertThat(f.boss.pos).isNotEqualTo(start);
		Assertions.assertThat(f.boss.getEchoHero().pos).isEqualTo(kitPosBefore);
		Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(f.boss)).isEqualTo(dest);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo SmokeBomb activateAs spends ClassArmor charge from the kit")
	void smokeBombSpendsCharge() {
		Fight f = fight();
		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);

		RogueArmor armor = new RogueArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;
		SmokeBomb ability = new SmokeBomb();
		float expectedUse = ability.chargeUse(f.boss.getEchoHero());

		boolean ok = ability.activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore - expectedUse);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo SmokeBomb activateAs clears busy so the boss turn can resume")
	void smokeBombClearsBusy() {
		Fight f = fight();
		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);

		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new SmokeBomb().activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo SmokeBomb shadow-step clears busy so the boss turn can resume")
	void smokeBombShadowStepClearsBusy() {
		Fight f = fight();
		Hero kit = f.boss.getEchoHero();
		grantTalent(kit, Talent.SHADOW_STEP, 1);
		kit.invisible = 1;

		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);

		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new SmokeBomb().activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.boss.pos).isEqualTo(dest);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo SmokeBomb shadow-step spends reduced charge from the kit")
	void smokeBombShadowStepReducedCharge() {
		Fight f = fight();
		Hero kit = f.boss.getEchoHero();
		grantTalent(kit, Talent.SHADOW_STEP, 4);
		kit.invisible = 1;

		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);

		RogueArmor armor = new RogueArmor();
		armor.charge = 100;
		SmokeBomb ability = new SmokeBomb();
		float expectedUse = ability.chargeUse(kit);

		boolean ok = ability.activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(expectedUse).isLessThan(35f);
		Assertions.assertThat(armor.charge).isEqualTo(100f - expectedUse);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo SmokeBomb activateAs refuses when ClassArmor charge is too low")
	void smokeBombRefusesLowCharge() {
		Fight f = fight();
		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		int start = f.boss.pos;

		RogueArmor armor = new RogueArmor();
		armor.charge = 0;

		boolean ok = new SmokeBomb().activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(f.boss.pos).isEqualTo(start);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}
}
