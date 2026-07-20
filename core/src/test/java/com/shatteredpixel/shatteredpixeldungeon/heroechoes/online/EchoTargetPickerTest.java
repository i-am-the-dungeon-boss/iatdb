package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;

@ExtendWith(GdxTestExtension.class)
class EchoTargetPickerTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("non-AOE pick returns enemy cell")
	void nonAoeReturnsEnemyCell() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder().build();
		int cell = EchoTargetPicker.pick(boss, status, "WandOfFireblast", false);

		Assertions.assertThat(cell).isEqualTo(hero.pos);
	}

	@Test
	@DisplayName("AOE pick never chooses a cell that harms an unsafe echo")
	void aoeNeverHarmsUnsafeEcho() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.unsafeHazards(Collections.singleton(EchoPolicyHazards.FIRE_AOE))
				.build();
		int cell = EchoTargetPicker.pick(boss, status, "PotionOfLiquidFlame", true);

		if (cell >= 0) {
			Assertions.assertThat(Dungeon.level.distance(cell, boss.pos)).isGreaterThan(1);
		}
	}

	@Test
	@DisplayName("AOE pick finds a legal cell when echo is marked safe for fire_aoe")
	void aoeFindsCellWhenSafe() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.safeHazards(Collections.singleton(EchoPolicyHazards.FIRE_AOE))
				.build();
		int cell = EchoTargetPicker.pick(boss, status, "PotionOfLiquidFlame", true);

		Assertions.assertThat(cell).isGreaterThanOrEqualTo(0);
	}

	@Test
	@DisplayName("pick returns none when level is missing")
	void returnsNoneWithoutLevel() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		Dungeon.level = null;

		int cell = EchoTargetPicker.pick(
				boss, new EchoPolicyStatus.Builder().build(), "WandOfFireblast", false);

		Assertions.assertThat(cell).isEqualTo(-1);
	}
}
