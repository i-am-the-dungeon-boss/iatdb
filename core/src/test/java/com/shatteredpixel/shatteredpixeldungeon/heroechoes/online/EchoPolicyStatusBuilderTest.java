package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoPolicyStatusBuilderTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("nearestTerrainCell returns closest matching terrain within max distance")
	void nearestTerrainCellFindsClosest() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(5), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 0);
		Level level = Dungeon.level;
		int from = hero.pos;
		int waterNear = from + 1;
		int waterFar = from + level.width() * 2;
		level.map[waterNear] = Terrain.WATER;
		level.map[waterFar] = Terrain.WATER;
		level.buildFlagMaps();

		int[] found = EchoPolicyStatusBuilder.nearestTerrainCell(level, from, Terrain.WATER, 5);

		Assertions.assertThat(found).isNotNull();
		Assertions.assertThat(found[0]).isEqualTo(waterNear);
		Assertions.assertThat(found[1]).isEqualTo(1);
	}

	@Test
	@DisplayName("build marks HEAL ready when healing potion is in echo inventory")
	void buildMarksHealReadyWithPotion() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		potion.collect(hero.belongings.backpack);
		EchoPolicy policy = EchoTestSupport.healCapabilityPolicy();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.isRoleReady("HEAL")).isTrue();
		Assertions.assertThat(status.isRoleReady("MELEE")).isTrue();
	}

	@Test
	@DisplayName("build respects potion_reserve so HEAL is not ready at reserve count")
	void buildRespectsPotionReserve() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		potion.collect(hero.belongings.backpack);
		JSONObject root = new JSONObject(EchoTestSupport.healCapabilityPolicy().root().toString());
		root.put("tuning", new JSONObject()
				.put("potion_reserve", new JSONObject().put("HEAL", 1)));
		EchoPolicy policy = EchoPolicy.fromJson(root);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.isRoleReady("HEAL")).isFalse();
	}
}
