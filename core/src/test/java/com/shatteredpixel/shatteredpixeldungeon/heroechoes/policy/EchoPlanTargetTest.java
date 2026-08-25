package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The sense phase is the only phase that knows the geometry, so the plan it
 * hands to execute has to carry the target cell with it — see
 * {@code docs/hero-echoes/echo-boss-code-patterns.md} § 5.1. Without that,
 * CLEAR_PLANT fired at the hero instead of at the blocking plant.
 */
@ExtendWith(GdxTestExtension.class)
class EchoPlanTargetTest {

	@Test
	@DisplayName("sense hands CLEAR_PLANT the blocking plant's cell as its target")
	void senseTargetsClearPlantAtTheBlocker() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = plantPolicyBoss(hero);
		int corridor = wallOffCorridorNorthOfHero(hero, boss);
		plant(new Firebloom(), corridor);
		giveEchoItem(boss, new PotionOfLiquidFlame());
		fillFov(boss);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, plantPolicy());

		Assertions.assertThat(status.isRoleReady("CLEAR_PLANT")).isTrue();
		Assertions.assertThat(status.targetCellFor("CLEAR_PLANT")).isEqualTo(corridor);
	}

	@Test
	@DisplayName("roles with no sensed geometry carry no target")
	void untargetedRolesCarryNoTarget() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = plantPolicyBoss(hero);
		fillFov(boss);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, plantPolicy());

		Assertions.assertThat(status.targetCellFor("MELEE")).isEqualTo(-1);
		Assertions.assertThat(status.targetCellFor("CLEAR_PLANT")).isEqualTo(-1);
	}

	@Test
	@DisplayName("the matcher copies the sensed target onto the chosen plan")
	void matcherCopiesTargetOntoChoice() {
		EchoPolicy policy = clearLosReactionPolicy();
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.enemyInLos(false)
				.rolesReady(set("CLEAR_LOS", "MELEE"))
				.roleTargetCell("CLEAR_LOS", 77)
				.build();

		EchoPlan choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("CLEAR_LOS");
		Assertions.assertThat(choice.targetCell).isEqualTo(77);
	}

	@Test
	@DisplayName("execute burns the plan's target cell, not the hero's")
	void executeAimsAtThePlanTarget() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = plantPolicy();
		EchoBoss boss = plantPolicyBoss(hero);
		giveEchoItem(boss, new PotionOfLiquidFlame());
		fillFov(boss);
		// An ordinary empty cell beside the echo: nothing about it would ever be
		// picked as an aim, so fire landing there can only have come from the plan.
		int target = boss.pos + 1;

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.rolesReady(set("CLEAR_PLANT"))
				.roleTargetCell("CLEAR_PLANT", target)
				.build();
		EchoPlan choice = EchoPlan.resolve("CLEAR_PLANT", "reactions", null, status);

		boolean spent = EchoRoleExecutor.execute(boss, policy, status, choice);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(Blob.volumeAt(target, Fire.class)).isGreaterThan(0);
		Assertions.assertThat(Blob.volumeAt(hero.pos, Fire.class)).isEqualTo(0);
	}

	@Test
	@DisplayName("CLEAR_LOS is not ready while nothing has sensed a blocker to clear")
	void clearLosNotReadyWithoutBlocker() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("CLEAR_LOS", EchoTestSupport.capability("PotionOfLiquidFlame"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 4);
		giveEchoItem(boss, new PotionOfLiquidFlame());
		fillFov(boss);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.isRoleReady("CLEAR_LOS")).isFalse();
	}

	private static int wallOffCorridorNorthOfHero(Hero hero, EchoBoss boss) {
		Level level = Dungeon.level;
		int width = level.width();
		boss.pos = hero.pos - 2 * width;
		int corridor = hero.pos - width;
		int rowStart = corridor - (corridor % width);
		for (int x = 0; x < width; x++) {
			int cell = rowStart + x;
			if (cell != corridor) {
				level.map[cell] = Terrain.WALL;
			}
		}
		level.buildFlagMaps();
		return corridor;
	}

	private static void plant(Plant plant, int cell) {
		plant.pos = cell;
		Dungeon.level.plants.put(cell, plant);
	}

	private static void fillFov(EchoBoss boss) {
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
	}

	private static void giveEchoItem(EchoBoss boss, Item item) {
		item.identify();
		item.collect(boss.getEchoHero().belongings.backpack);
	}

	private static EchoBoss plantPolicyBoss(Hero hero) {
		EchoPolicy policy = plantPolicy();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 4);
		return boss;
	}

	private static EchoPolicy plantPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("CLEAR_PLANT", EchoTestSupport.capability("PotionOfLiquidFlame"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy clearLosReactionPolicy() {
		JSONObject when = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("enemy_in_los", false))
				.put(new JSONObject().put("role_ready", "CLEAR_LOS")));
		return EchoPolicy.fromJson(new JSONObject()
				.put("policy_schema_version", "0.0.1")
				.put("capabilities", new JSONObject()
						.put("CLEAR_LOS", EchoTestSupport.capability("PotionOfLiquidFlame"))
						.put("MELEE", EchoTestSupport.capability("*melee")))
				.put("reactions", new JSONArray().put(new JSONObject()
						.put("id", "clear_los")
						.put("priority", 101)
						.put("when", when)
						.put("do", new JSONObject().put("use_role", "CLEAR_LOS"))))
				.put("recipes", new JSONArray())
				.put("positioning", new JSONObject())
				.put("matchups", new JSONObject())
				.put("selection", new JSONObject()
						.put("order", new JSONArray().put("reactions").put("default"))
						.put("default_roles", new JSONArray().put("MELEE")))
				.put("tuning", new JSONObject()));
	}

	private static Set<String> set(String... roles) {
		Set<String> out = new HashSet<>();
		for (String role : roles) {
			out.add(role);
		}
		return out;
	}
}
