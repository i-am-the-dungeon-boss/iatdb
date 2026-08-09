package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * Characterizes {@code plant_blocked} sensing: a harmful plant sitting on the
 * only reasonably short route to the hero is flagged and remembered as a
 * blocker cell; a bypassable one, or a beneficial plant, is not.
 */
@ExtendWith(GdxTestExtension.class)
class EchoPlantBlockedTest {

	@Test
	@DisplayName("a harmful plant on the sole corridor sets plant_blocked and the blocker cell")
	void harmfulPlantOnSoleCorridorBlocks() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = plantPolicyBoss(hero);
		int corridor = wallOffCorridorNorthOfHero(hero, boss);
		plant(new Firebloom(), corridor);
		giveEchoItem(boss, new PotionOfLiquidFlame());
		fillFov(boss);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, plantPolicy());

		Assertions.assertThat(status.selfStatuses).contains("plant_blocked");
		Assertions.assertThat(boss.plantBlockerCell()).isEqualTo(corridor);
		Assertions.assertThat(status.isRoleReady("CLEAR_PLANT")).isTrue();
	}

	@Test
	@DisplayName("CLEAR_PLANT is not ready without the fire tool even while plant_blocked")
	void clearPlantNotReadyWithoutItem() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = plantPolicyBoss(hero);
		int corridor = wallOffCorridorNorthOfHero(hero, boss);
		plant(new Firebloom(), corridor);
		fillFov(boss);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, plantPolicy());

		Assertions.assertThat(status.selfStatuses).contains("plant_blocked");
		Assertions.assertThat(status.isRoleReady("CLEAR_PLANT")).isFalse();
	}

	@Test
	@DisplayName("an open room with a cheap detour around the plant does not set plant_blocked")
	void openRoomWithCheapDetourNotBlocked() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = plantPolicyBoss(hero);
		// Boss 2 cells north of hero on an otherwise open 7x7 level; a single
		// plant directly on the straight line costs at most a one-cell detour.
		// Level.insideMap excludes the outer ring, and pathfinding assumes
		// endpoints stay off it, so this stays one row short of the edge.
		boss.pos = hero.pos - 2 * Dungeon.level.width();
		plant(new Firebloom(), hero.pos - Dungeon.level.width());
		giveEchoItem(boss, new PotionOfLiquidFlame());
		fillFov(boss);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, plantPolicy());

		Assertions.assertThat(status.selfStatuses).doesNotContain("plant_blocked");
		Assertions.assertThat(status.isRoleReady("CLEAR_PLANT")).isFalse();
	}

	@Test
	@DisplayName("a beneficial plant on the sole corridor never sets plant_blocked")
	void beneficialPlantNeverBlocks() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = plantPolicyBoss(hero);
		int corridor = wallOffCorridorNorthOfHero(hero, boss);
		plant(new Sungrass(), corridor);
		giveEchoItem(boss, new PotionOfLiquidFlame());
		fillFov(boss);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, plantPolicy());

		Assertions.assertThat(status.selfStatuses).doesNotContain("plant_blocked");
		Assertions.assertThat(boss.plantBlockerCell()).isEqualTo(-1);
	}

	/**
	 * Walls the entire row between {@code boss} (placed two rows north of
	 * {@code hero}) and the hero, except one corridor cell directly above the
	 * hero, and returns that cell.
	 */
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

	private static void giveEchoItem(
			EchoBoss boss, com.shatteredpixel.shatteredpixeldungeon.items.Item item) {
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
}
