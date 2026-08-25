package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * A ranged echo used to dance in and out of a doorway forever: KEEP_DISTANCE
 * backed it out of line of sight, RANGED then reported ready but could not aim,
 * the turn fell through to the hunting AI, and the AI marched it back into
 * melee range. The doorway itself is handled in {@code EchoBossDoorForceTest}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoDoorDanceTest {

	@Test
	@DisplayName("RANGED is not ready while the hero is out of line of sight")
	void rangedNotReadyWithoutLineOfSight() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = rangedPolicy();
		EchoBoss boss = rangedBoss(hero, policy);
		blindEcho(boss);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.enemyInLos).isFalse();
		Assertions.assertThat(status.isRoleReady("RANGED")).isFalse();
	}

	@Test
	@DisplayName("RANGED stays ready against a cloaked hero while blind-defence shots remain")
	void rangedReadyAgainstCloakedHero() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = rangedPolicy();
		EchoBoss boss = rangedBoss(hero, policy);
		boss.noteEnemySeenAt(hero.pos);
		hero.invisible = 5;

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.enemyInLos).isFalse();
		Assertions.assertThat(boss.blindDefenseShotsLeft()).isGreaterThan(0);
		Assertions.assertThat(status.isRoleReady("RANGED")).isTrue();
	}

	@Test
	@DisplayName("a kiting retreat prefers the cell it can still shoot from")
	void kitingRetreatPrefersLineOfFire() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = rangedPolicy();
		EchoBoss boss = rangedBoss(hero, policy);
		Level level = Dungeon.level;
		// Hero east of the echo. Both candidate retreats to the west are equally
		// far, but a wall stub blinds the lower-indexed one — which is exactly
		// the one the old index tiebreak picked.
		hero.pos = cell(5, 3);
		boss.pos = cell(3, 3);
		level.map[cell(3, 2)] = Terrain.WALL;
		level.buildFlagMaps();
		int blind = cell(2, 2);
		int shootable = cell(2, 3);
		Assertions.assertThat(hasTerrainLine(blind, hero.pos)).isFalse();
		Assertions.assertThat(hasTerrainLine(shootable, hero.pos)).isTrue();

		boolean stepped = boss.policyStepFurther(hero.pos, true);

		Assertions.assertThat(stepped).isTrue();
		Assertions.assertThat(boss.pos).isEqualTo(shootable);
	}

	private static int cell(int x, int y) {
		return y * Dungeon.level.width() + x;
	}

	private static boolean hasTerrainLine(int from, int to) {
		int terrainOnly = Ballistica.STOP_TARGET | Ballistica.STOP_SOLID;
		return new Ballistica(from, to, terrainOnly).collisionPos == to;
	}

	private static void blindEcho(EchoBoss boss) {
		Arrays.fill(boss.fieldOfView, false);
	}

	private static EchoBoss rangedBoss(Hero hero, EchoPolicy policy) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.identify();
		wand.curCharges = 5;
		wand.collect(boss.getEchoHero().belongings.backpack);
		return boss;
	}

	private static EchoPolicy rangedPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", EchoTestSupport.capability("WandOfMagicMissile"))
				.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}
}
