package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicyChoice;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicyStatus;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoRoleExecutor;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClericArmor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Echo {@link PowerOfMany} via
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs}.
 *
 * <p>Policy executor gap: {@link com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoTargetPicker}
 * aims at the Hero (enemy) cell. Summon requires an empty passable tile, so
 * {@link EchoRoleExecutor} ARMOR_ABILITY with PowerOfMany does not spawn a
 * LightAlly until targeting picks an empty FOV cell.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityPowerOfManyTest {

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
	@DisplayName("Echo PowerOfMany activateAs summons LightAlly with PowerBuff and Barrier on empty FOV cell")
	void summonsLightAllyOnEmptyFovCell() {
		Fight f = fight();
		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		Assertions.assertThat(Dungeon.level.heroFOV[dest]).isTrue();

		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		boolean ok = new PowerOfMany().activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		PowerOfMany.LightAlly ally = findMob(PowerOfMany.LightAlly.class);
		Assertions.assertThat(ally).isNotNull();
		Assertions.assertThat(ally.pos).isEqualTo(dest);
		Assertions.assertThat(ally.buff(PowerOfMany.PowerBuff.class)).isNotNull();
		Assertions.assertThat(ally.buff(Barrier.class)).isNotNull();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo PowerOfMany activateAs spends ClassArmor charge from the kit")
	void spendsCharge() {
		Fight f = fight();
		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);

		ClericArmor armor = new ClericArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new PowerOfMany().activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore - 35f);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo PowerOfMany activateAs clears busy so the boss turn can resume")
	void clearsBusy() {
		Fight f = fight();
		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);

		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		boolean ok = new PowerOfMany().activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo PowerOfMany activateAs refuses when ClassArmor charge is too low")
	void refusesLowCharge() {
		Fight f = fight();
		int dest = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);

		ClericArmor armor = new ClericArmor();
		armor.charge = 0;

		boolean ok = new PowerOfMany().activateAs(f.echo(), armor, dest);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(findMob(PowerOfMany.LightAlly.class)).isNull();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo PowerOfMany activateAs refuses occupied non-ally cell without spending charge")
	void refusesEnemyOccupiedCell() {
		Fight f = fight();

		ClericArmor armor = new ClericArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new PowerOfMany().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore);
		Assertions.assertThat(findMob(PowerOfMany.LightAlly.class)).isNull();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("EchoRoleExecutor ARMOR_ABILITY with PowerOfMany does not summon when aim is the Hero cell")
	void executorAimGapDoesNotSummonAtHeroCell() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, armorClericPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		Hero kit = boss.getEchoHero();
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;
		kit.belongings.armor = armor;
		armor.activate(kit);
		kit.armorAbility = new PowerOfMany();

		boolean spent = EchoRoleExecutor.execute(
				boss,
				armorClericPolicy(),
				new EchoPolicyStatus.Builder()
						.enemyInLos(true)
						.rolesReady(java.util.Set.of("ARMOR_ABILITY"))
						.build(),
				new EchoPolicyChoice("ARMOR_ABILITY", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(findMob(PowerOfMany.LightAlly.class)).isNull();
		Assertions.assertThat(armor.charge).isEqualTo(100f);
	}

	private static EchoPolicy armorClericPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("ARMOR_ABILITY", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new org.json.JSONArray().put("ClericArmor"))));
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
