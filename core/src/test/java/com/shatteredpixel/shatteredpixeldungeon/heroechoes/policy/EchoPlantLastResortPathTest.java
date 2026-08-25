package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * Harmful plants are impassable to the echo, which is right while it has an
 * answer to them. With no way to burn the plant and no way to fight at range,
 * that rule left a melee echo standing in a corridor forever. A fight it cannot
 * reach is worse than a plant it has to walk through, so the plant stops being
 * a wall — but only in exactly that case.
 */
@ExtendWith(GdxTestExtension.class)
class EchoPlantLastResortPathTest {

	@Test
	@DisplayName("a melee-only echo walled off by a plant accepts the plant path")
	void meleeOnlyEchoAcceptsPlantPath() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = meleeOnlyPolicy();
		EchoBoss boss = boss(hero, policy);
		int corridor = wallOffCorridorNorthOfHero(hero, boss);
		plant(new Firebloom(), corridor);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.selfStatuses).contains("plant_blocked");
		Assertions.assertThat(status.isRoleReady("CLEAR_PLANT")).isFalse();
		Assertions.assertThat(boss.policyCellPathable(corridor)).isTrue();
		Assertions.assertThat(boss.modifyPassable(passableCopy())[corridor]).isTrue();
	}

	@Test
	@DisplayName("an echo that can burn the plant still refuses to walk into it")
	void echoWithClearPlantKitStillAvoidsPlant() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = clearPlantPolicy();
		EchoBoss boss = boss(hero, policy);
		int corridor = wallOffCorridorNorthOfHero(hero, boss);
		plant(new Firebloom(), corridor);
		give(boss, new PotionOfLiquidFlame());

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.isRoleReady("CLEAR_PLANT")).isTrue();
		Assertions.assertThat(boss.policyCellPathable(corridor)).isFalse();
	}

	@Test
	@DisplayName("an echo that can still shoot refuses to walk into the plant")
	void echoWithRangedKitStillAvoidsPlant() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = rangedPolicy();
		EchoBoss boss = boss(hero, policy);
		int corridor = wallOffCorridorNorthOfHero(hero, boss);
		plant(new Firebloom(), corridor);
		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.curCharges = 5;
		give(boss, wand);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.isRoleReady("RANGED")).isTrue();
		Assertions.assertThat(boss.policyCellPathable(corridor)).isFalse();
	}

	@Test
	@DisplayName("a plant that is merely a detour is still not walked through")
	void bypassablePlantIsStillAvoided() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = meleeOnlyPolicy();
		EchoBoss boss = boss(hero, policy);
		// Open room: one cell of detour, so nothing is blocked and the plant
		// keeps its usual "do not step here" standing.
		boss.pos = hero.pos - 2 * Dungeon.level.width();
		int onTheLine = hero.pos - Dungeon.level.width();
		plant(new Firebloom(), onTheLine);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.selfStatuses).doesNotContain("plant_blocked");
		Assertions.assertThat(boss.policyCellPathable(onTheLine)).isFalse();
	}

	private static boolean[] passableCopy() {
		return Dungeon.level.passable.clone();
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

	private static void give(EchoBoss boss, Item item) {
		item.identify();
		item.collect(boss.getEchoHero().belongings.backpack);
	}

	private static EchoBoss boss(Hero hero, EchoPolicy policy) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 4);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		return boss;
	}

	private static EchoPolicy meleeOnlyPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("CLOSE_IN", EchoTestSupport.capability("*move_closer"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy clearPlantPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("CLEAR_PLANT", EchoTestSupport.capability("PotionOfLiquidFlame"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy rangedPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", EchoTestSupport.capability("WandOfMagicMissile"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}
}
