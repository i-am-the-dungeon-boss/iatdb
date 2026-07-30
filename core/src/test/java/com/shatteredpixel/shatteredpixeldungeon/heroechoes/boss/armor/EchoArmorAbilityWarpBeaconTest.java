package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MageArmor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Echo {@link WarpBeacon} via
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityWarpBeaconTest {

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

		Hero kit() {
			return boss.getEchoHero();
		}
	}

	@Test
	@DisplayName("Echo WarpBeacon activateAs places WarpBeaconTracker on the kit")
	void placeBeaconOnKit() {
		Fight f = fight();
		int beacon = f.boss.pos;
		Dungeon.level.visited[beacon] = true;
		Dungeon.level.mapped[beacon] = true;

		MageArmor armor = new MageArmor();
		armor.charge = 100;

		boolean ok = new WarpBeacon().activateAs(f.echo(), armor, beacon);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.kit().buff(WarpBeacon.WarpBeaconTracker.class)).isNotNull();
		Assertions.assertThat(f.boss.buff(WarpBeacon.WarpBeaconTracker.class)).isNull();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo WarpBeacon recall teleports boss body to beacon like Hero tele option")
	void recallTeleportsBody() {
		Fight f = fight();
		int beacon = f.boss.pos;
		Dungeon.level.visited[beacon] = true;
		Dungeon.level.mapped[beacon] = true;

		MageArmor armor = new MageArmor();
		armor.charge = 100;

		Assertions.assertThat(new WarpBeacon().activateAs(f.echo(), armor, beacon)).isTrue();

		int away = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(away).isGreaterThanOrEqualTo(0);
		f.boss.pos = away;
		Dungeon.level.occupyCell(f.boss);

		boolean recalled = new WarpBeacon().activateAs(f.echo(), armor, f.boss.pos);

		Assertions.assertThat(recalled).isTrue();
		Assertions.assertThat(f.boss.pos).isEqualTo(beacon);
		Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(f.boss)).isEqualTo(beacon);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo WarpBeacon recall clears busy so the boss turn can resume")
	void recallClearsBusy() {
		Fight f = fight();
		int beacon = f.boss.pos;
		Dungeon.level.visited[beacon] = true;
		Dungeon.level.mapped[beacon] = true;

		MageArmor armor = new MageArmor();
		armor.charge = 100;

		Assertions.assertThat(new WarpBeacon().activateAs(f.echo(), armor, beacon)).isTrue();

		int away = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(away).isGreaterThanOrEqualTo(0);
		f.boss.pos = away;
		Dungeon.level.occupyCell(f.boss);

		boolean recalled = new WarpBeacon().activateAs(f.echo(), armor, f.boss.pos);

		Assertions.assertThat(recalled).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo WarpBeacon place clears busy so the boss turn can resume")
	void placeClearsBusy() {
		Fight f = fight();
		int beacon = f.boss.pos;
		Dungeon.level.visited[beacon] = true;
		Dungeon.level.mapped[beacon] = true;

		MageArmor armor = new MageArmor();
		armor.charge = 100;

		boolean ok = new WarpBeacon().activateAs(f.echo(), armor, beacon);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo WarpBeacon recall spends ClassArmor charge from the kit")
	void recallSpendsCharge() {
		Fight f = fight();
		int beacon = f.boss.pos;
		Dungeon.level.visited[beacon] = true;
		Dungeon.level.mapped[beacon] = true;

		MageArmor armor = new MageArmor();
		armor.charge = 100;

		Assertions.assertThat(new WarpBeacon().activateAs(f.echo(), armor, beacon)).isTrue();
		float chargeAfterPlace = armor.charge;

		int away = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(away).isGreaterThanOrEqualTo(0);
		f.boss.pos = away;
		Dungeon.level.occupyCell(f.boss);

		Assertions.assertThat(new WarpBeacon().activateAs(f.echo(), armor, f.boss.pos)).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeAfterPlace);
	
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
