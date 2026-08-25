package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.HashMap;

/**
 * A burning echo goes to water — but only to water it can stand in. Water under
 * an AoE DoT is not a destination: another water tile is chosen when one exists,
 * otherwise the burn is put out some other way (CLEANSE_BURN).
 */
@ExtendWith(GdxTestExtension.class)
class EchoBurningWaterTest {

	@Test
	@DisplayName("water covered by fire is skipped; a clear water tile is sensed instead")
	void hazardousWaterSkippedForClearWater() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, waterPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		int width = Dungeon.level.width();
		int burningWater = boss.pos - width;
		int clearWater = boss.pos + 2 * width;
		makeWater(burningWater, clearWater);
		Blob.seed(burningWater, 10, Fire.class);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, waterPolicy());

		Assertions.assertThat(status.isTerrainNear("water")).isTrue();
		Assertions.assertThat(status.terrainNearCell.get("water")).isEqualTo(clearWater);
		Assertions.assertThat(status.terrainNearDistance.get("water")).isEqualTo(2);
		Assertions.assertThat(status.isRoleReady("MOVE_TO_WATER")).isTrue();
	}

	@Test
	@DisplayName("gas over the only water tile leaves no water near and MOVE_TO_WATER unready")
	void gasCoveredWaterIsNotNear() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, waterPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		int water = boss.pos - Dungeon.level.width();
		makeWater(water);
		Blob.seed(water, 40, ToxicGas.class);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, waterPolicy());

		Assertions.assertThat(status.isTerrainNear("water")).isFalse();
		Assertions.assertThat(status.isRoleReady("MOVE_TO_WATER")).isFalse();
	}

	@Test
	@DisplayName("burning echo takes CLEANSE_BURN when the only water is on fire")
	void burningFallsBackToCleanseWhenWaterIsHazardous() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfFrost frost = new PotionOfFrost();
		frost.identify();
		frost.collect(hero.belongings.backpack);
		EchoPolicy policy = burnPolicy();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		int water = boss.pos - Dungeon.level.width();
		makeWater(water);
		Blob.seed(water, 10, Fire.class);
		Buff.affect(boss, Burning.class);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		EchoPlan plan = EchoPolicyMatcher.choose(policy, status, new HashMap<String, Integer>());

		Assertions.assertThat(plan).isNotNull();
		Assertions.assertThat(plan.useRole).isEqualTo("CLEANSE_BURN");
	}

	@Test
	@DisplayName("burning echo still steps into water when the water is clear")
	void burningStepsIntoClearWater() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfFrost frost = new PotionOfFrost();
		frost.identify();
		frost.collect(hero.belongings.backpack);
		EchoPolicy policy = burnPolicy();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		makeWater(boss.pos - Dungeon.level.width());
		Buff.affect(boss, Burning.class);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		EchoPlan plan = EchoPolicyMatcher.choose(policy, status, new HashMap<String, Integer>());

		Assertions.assertThat(plan).isNotNull();
		Assertions.assertThat(plan.useRole).isEqualTo("MOVE_TO_WATER");
	}

	private static void makeWater(int... cells) {
		for (int cell : cells) {
			Dungeon.level.map[cell] = Terrain.WATER;
		}
		Dungeon.level.buildFlagMaps();
	}

	private static void fillFov(EchoBoss boss) {
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		Arrays.fill(Dungeon.level.heroFOV, true);
	}

	private static EchoPolicy waterPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("MOVE_TO_WATER", EchoTestSupport.capability("*move_to_terrain:water"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	/** Mirrors the backend's burn_step_into_water / burn_use_frost reactions. */
	private static EchoPolicy burnPolicy() {
		JSONObject root = new JSONObject(EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("MOVE_TO_WATER", EchoTestSupport.capability("*move_to_terrain:water"))
				.put("CLEANSE_BURN", EchoTestSupport.capability("PotionOfFrost"))
				.put("MELEE", EchoTestSupport.capability("*melee"))).root().toString());
		root.put("reactions", new JSONArray()
				.put(new JSONObject()
						.put("id", "burn_step_into_water")
						.put("priority", 108)
						.put("when", new JSONObject().put("all", new JSONArray()
								.put(new JSONObject().put("self_status", "burning"))
								.put(new JSONObject().put("terrain_near", "water"))
								.put(new JSONObject().put("role_ready", "MOVE_TO_WATER"))))
						.put("do", new JSONObject().put("use_role", "MOVE_TO_WATER")))
				.put(new JSONObject()
						.put("id", "burn_use_frost")
						.put("priority", 107)
						.put("when", new JSONObject().put("all", new JSONArray()
								.put(new JSONObject().put("self_status", "burning"))
								.put(new JSONObject().put("terrain_near_none", "water"))
								.put(new JSONObject().put("role_ready", "CLEANSE_BURN"))))
						.put("do", new JSONObject().put("use_role", "CLEANSE_BURN"))));
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("reactions").put("default"))
				.put("default_roles", new JSONArray().put("MELEE")));
		return EchoPolicy.fromJson(root);
	}
}
