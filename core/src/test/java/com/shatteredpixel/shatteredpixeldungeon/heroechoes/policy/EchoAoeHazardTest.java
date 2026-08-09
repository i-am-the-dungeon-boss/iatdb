package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.plants.Blindweed;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sorrowmoss;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * Characterizes the path-hazard mask: harmful plants and the predicted
 * {@code Blob.evolve} gas-growth ring.
 */
@ExtendWith(GdxTestExtension.class)
class EchoAoeHazardTest {

	@Test
	@DisplayName("harmful plants are hazards for pathing")
	void harmfulPlantsAreHazards() {
		EchoBoss boss = plainBoss();
		int cell = boss.pos + 1;

		plant(new Firebloom(), cell);
		Assertions.assertThat(EchoAoeDots.isHarmfulPlantAt(cell)).isTrue();
		Assertions.assertThat(EchoAoeDots.isAoeHazardForPath(boss, cell)).isTrue();
	}

	@Test
	@DisplayName("every plant in the harmful set is refused")
	void everyHarmfulPlantIsRefused() {
		EchoBoss boss = plainBoss();
		int cell = boss.pos + 1;

		plant(new Sorrowmoss(), cell);
		Assertions.assertThat(EchoAoeDots.isHarmfulPlantAt(cell)).isTrue();
		plant(new Blindweed(), cell);
		Assertions.assertThat(EchoAoeDots.isHarmfulPlantAt(cell)).isTrue();
	}

	@Test
	@DisplayName("beneficial plants stay pathable")
	void beneficialPlantsStayPathable() {
		EchoBoss boss = plainBoss();
		int cell = boss.pos + 1;

		plant(new Sungrass(), cell);

		Assertions.assertThat(EchoAoeDots.isHarmfulPlantAt(cell)).isFalse();
		Assertions.assertThat(EchoAoeDots.isAoeHazardForPath(boss, cell)).isFalse();
	}

	@Test
	@DisplayName("a harmful plant is never waived, even when the growth ring is")
	void harmfulPlantSurvivesRelaxation() {
		EchoBoss boss = plainBoss();
		int cell = boss.pos + 1;

		plant(new Firebloom(), cell);

		Assertions.assertThat(EchoAoeDots.isAoeHazardForPath(boss, cell, false)).isTrue();
	}

	@Test
	@DisplayName("the orthogonal ring around a dense gas cloud is predicted to fill next tick")
	void gasGrowthRingIsPredicted() {
		EchoBoss boss = plainBoss();
		int gas = boss.pos + 3;
		Blob.seed(gas, 40, ToxicGas.class);
		int ring = gas + 1;

		Assertions.assertThat(EchoAoeDots.isPredictedGasAt(boss, ring)).isTrue();
		Assertions.assertThat(EchoAoeDots.isAoeHazardForPath(boss, ring)).isTrue();
	}

	@Test
	@DisplayName("the predicted growth ring is waived when the strict mask is relaxed")
	void gasGrowthRingIsWaivable() {
		EchoBoss boss = plainBoss();
		int gas = boss.pos + 3;
		Blob.seed(gas, 40, ToxicGas.class);
		int ring = gas + 1;

		Assertions.assertThat(EchoAoeDots.isAoeHazardForPath(boss, ring, false)).isFalse();
		Assertions.assertThat(EchoAoeDots.isPredictedGasOnly(boss, ring)).isTrue();
	}

	@Test
	@DisplayName("a cell holding gas now is a hazard under both strictness levels")
	void currentGasIsNeverWaived() {
		EchoBoss boss = plainBoss();
		int gas = boss.pos + 3;
		Blob.seed(gas, 40, ToxicGas.class);

		Assertions.assertThat(EchoAoeDots.isAoeHazardForPath(boss, gas, true)).isTrue();
		Assertions.assertThat(EchoAoeDots.isAoeHazardForPath(boss, gas, false)).isTrue();
	}

	@Test
	@DisplayName("far cells outside the growth ring stay pathable")
	void cellsOutsideGrowthRingStayPathable() {
		EchoBoss boss = plainBoss();
		int gas = boss.pos + 3;
		Blob.seed(gas, 40, ToxicGas.class);
		int farCell = gas + 3;

		Assertions.assertThat(EchoAoeDots.isPredictedGasAt(boss, farCell)).isFalse();
		Assertions.assertThat(EchoAoeDots.isAoeHazardForPath(boss, farCell)).isFalse();
	}

	@Test
	@DisplayName("leave-AoE refuses to step onto a harmful plant")
	void leaveExitAvoidsHarmfulPlants() {
		EchoBoss boss = plainBoss();
		Blob.seed(boss.pos, 10, Fire.class);
		// Fence the boss in with harmful plants except one clear cell.
		int clear = boss.pos + 1;
		for (int i = 0; i < com.watabou.utils.PathFinder.NEIGHBOURS8.length; i++) {
			int n = boss.pos + com.watabou.utils.PathFinder.NEIGHBOURS8[i];
			if (n != clear && Dungeon.level.insideMap(n)) {
				plant(new Firebloom(), n);
			}
		}

		int exit = EchoAoeDots.bestExit(boss, -1, false);

		Assertions.assertThat(exit).isEqualTo(clear);
	}

	@Test
	@DisplayName("LEAVE_AOE is not ready when every exit holds a harmful plant")
	void noExitWhenRingedByHarmfulPlants() {
		EchoBoss boss = plainBoss();
		Blob.seed(boss.pos, 10, Fire.class);
		for (int i = 0; i < com.watabou.utils.PathFinder.NEIGHBOURS8.length; i++) {
			int n = boss.pos + com.watabou.utils.PathFinder.NEIGHBOURS8[i];
			if (Dungeon.level.insideMap(n)) {
				plant(new Firebloom(), n);
			}
		}

		Assertions.assertThat(EchoAoeDots.canLeave(boss)).isFalse();
	}

	private static EchoBoss plainBoss() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("LEAVE_AOE", EchoTestSupport.capability("*leave_aoe"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 4);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		return boss;
	}

	private static void plant(Plant plant, int cell) {
		plant.pos = cell;
		Dungeon.level.plants.put(cell, plant);
	}
}
