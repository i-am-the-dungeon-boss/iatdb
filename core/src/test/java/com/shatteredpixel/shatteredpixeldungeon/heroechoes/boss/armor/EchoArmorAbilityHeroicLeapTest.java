package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.WarriorArmor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Echo {@link HeroicLeap} via
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityHeroicLeapTest {

	private static Fight fight() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
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
	@DisplayName("Echo HeroicLeap activateAs moves the boss body to the landing cell")
	void movesBossBody() {
		Fight f = fight();
		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		int start = f.boss.pos;
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new HeroicLeap().activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.boss.pos).isEqualTo(dest);
		Assertions.assertThat(f.boss.pos).isNotEqualTo(start);
		Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(f.boss)).isEqualTo(dest);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo HeroicLeap activateAs spends ClassArmor charge from the kit")
	void spendsCharge() {
		Fight f = fight();
		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new HeroicLeap().activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo HeroicLeap activateAs clears busy so the boss turn can resume")
	void clearsBusy() {
		Fight f = fight();
		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new HeroicLeap().activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo HeroicLeap plays jump VFX when the body sprite has a parent")
	void playsJumpWhenSpriteHasParent() {
		Fight f = fight();
		EchoTestSupport.attachInstantProjectileParent(f.boss);
		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		int jumpsBefore = EchoTestSupport.stubSpriteJumpCalls(f.boss);
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new HeroicLeap().activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(EchoTestSupport.stubSpriteJumpCalls(f.boss))
				.isGreaterThan(jumpsBefore);
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
