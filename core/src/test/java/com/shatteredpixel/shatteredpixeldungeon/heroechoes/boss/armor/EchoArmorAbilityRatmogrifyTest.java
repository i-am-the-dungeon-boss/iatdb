package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoArmorAbilityAdapter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.RogueArmor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Echo {@link Ratmogrify} via {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.EchoArmorAbilityAdapter}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityRatmogrifyTest {

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

	}

	@Test
	@DisplayName("Echo Ratmogrify adapter activate transforms a non-rat enemy into TransmogRat at the same cell")
	void transformsEnemyAtSameCell() {
		Fight f = fight();
		int cell = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(cell).isGreaterThanOrEqualTo(0);

		Snake snake = new Snake();
		snake.pos = cell;
		EchoTestSupport.linkStubSprite(snake);
		Dungeon.level.mobs.add(snake);
		Actor.add(snake);

		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new Ratmogrify(), cell);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(findMob(Snake.class)).isNull();
		Ratmogrify.TransmogRat rat = findMob(Ratmogrify.TransmogRat.class);
		Assertions.assertThat(rat).isNotNull();
		Assertions.assertThat(rat.pos).isEqualTo(cell);

		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Ratmogrify adapter activate spends ClassArmor charge from the kit")
	void spendsCharge() {
		Fight f = fight();
		int cell = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(cell).isGreaterThanOrEqualTo(0);

		Snake snake = new Snake();
		snake.pos = cell;
		EchoTestSupport.linkStubSprite(snake);
		Dungeon.level.mobs.add(snake);
		Actor.add(snake);

		RogueArmor armor = new RogueArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new Ratmogrify(), cell);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore - 50f);

		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Ratmogrify adapter activate clears busy so the boss turn can resume")
	void clearsBusy() {
		Fight f = fight();
		int cell = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(cell).isGreaterThanOrEqualTo(0);

		Snake snake = new Snake();
		snake.pos = cell;
		EchoTestSupport.linkStubSprite(snake);
		Dungeon.level.mobs.add(snake);
		Actor.add(snake);

		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new Ratmogrify(), cell);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo Ratmogrify adapter activate refuses when ClassArmor charge is too low")
	void refusesLowCharge() {
		Fight f = fight();
		int cell = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(cell).isGreaterThanOrEqualTo(0);

		Snake snake = new Snake();
		snake.pos = cell;
		EchoTestSupport.linkStubSprite(snake);
		Dungeon.level.mobs.add(snake);
		Actor.add(snake);

		RogueArmor armor = new RogueArmor();
		armor.charge = 0;

		boolean ok = EchoArmorAbilityAdapter.activate(f.boss, armor, new Ratmogrify(), cell);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(findMob(Snake.class)).isNotNull();
		Assertions.assertThat(findMob(Ratmogrify.TransmogRat.class)).isNull();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	private static <T extends Mob> T findMob(Class<T> type) {
		for (Mob m : Dungeon.level.mobs) {
			if (type.isInstance(m)) {
				return type.cast(m);
			}
		}
		return null;
	}

	private static int emptyAdjacent(int from) {
		for (int i : PathFinder.NEIGHBOURS8) {
			int cell = from + i;
			if (cell >= 0 && cell < Dungeon.level.length()
					&& Dungeon.level.map[cell] == Terrain.EMPTY
					&& Dungeon.hero.pos != cell
					&& Actor.findChar(cell) == null) {
				return cell;
			}
		}
		return -1;
	}
}
