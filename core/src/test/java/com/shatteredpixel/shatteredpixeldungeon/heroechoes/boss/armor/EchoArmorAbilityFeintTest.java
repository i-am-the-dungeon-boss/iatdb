package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.DuelistArmor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityFeintTest {

	@Test
	@DisplayName("Echo Feint moves boss body and leaves AfterImage at departure cell")
	void echoFeintMovesBodyAndLeavesImage() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		int dest = emptyAdjacent(boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		int start = boss.pos;

		DuelistArmor armor = new DuelistArmor();
		armor.charge = 100;

		boolean ok = new Feint().activateAs(UseContext.echo(boss), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.pos).isEqualTo(dest);
		Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(boss)).isEqualTo(dest);
		Feint.AfterImage image = null;
		for (Mob m : Dungeon.level.mobs) {
			if (m instanceof Feint.AfterImage) {
				image = (Feint.AfterImage) m;
				break;
			}
		}
		Assertions.assertThat(image).isNotNull();
		Assertions.assertThat(image.pos).isEqualTo(start);
		Assertions.assertThat(image.alignment).isEqualTo(boss.alignment);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
	}

	@Test
	@DisplayName("Echo Feint activateAs clears busy so the boss turn can resume")
	void echoFeintClearsBusy() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		int dest = emptyAdjacent(boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);

		DuelistArmor armor = new DuelistArmor();
		armor.charge = 100;

		boolean ok = new Feint().activateAs(UseContext.echo(boss), armor, dest);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
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
