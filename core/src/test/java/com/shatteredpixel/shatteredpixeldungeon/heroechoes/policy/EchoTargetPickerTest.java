package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(GdxTestExtension.class)
class EchoTargetPickerTest {

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

	@Test
	@DisplayName("non-AOE pick returns none when out of LOS but enemy is not invisible")
	void nonAoeReturnsNoneWhenOccludedNotInvisible() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		int lastSeen = hero.pos;
		boss.noteEnemySeenAt(lastSeen);
		boss.setBlindDefenseShotsLeftForTests(2);
		hero.pos = lastSeen + 1;

		EchoPolicyStatus status = new EchoPolicyStatus.Builder().enemyInLos(false).build();
		int cell = EchoTargetPicker.pick(boss, status, "WandOfFireblast", false);

		Assertions.assertThat(cell).isEqualTo(-1);
	}

	@Test
	@DisplayName("non-AOE pick aims at last seen while invisible with blind shots remaining")
	void nonAoeAimsLastSeenWhileInvisibleWithShots() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		int lastSeen = hero.pos;
		boss.noteEnemySeenAt(lastSeen);
		boss.setBlindDefenseShotsLeftForTests(2);
		hero.pos = lastSeen + 1;
		hero.invisible = 1;

		EchoPolicyStatus status = invisibleOutOfLosStatus();
		int cell = EchoTargetPicker.pick(boss, status, "WandOfFireblast", false);

		Assertions.assertThat(cell).isEqualTo(lastSeen);
	}

	@Test
	@DisplayName("non-AOE pick returns none when invisible but blind shots are exhausted")
	void nonAoeReturnsNoneWhenBlindShotsExhausted() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		int lastSeen = hero.pos;
		boss.noteEnemySeenAt(lastSeen);
		boss.setBlindDefenseShotsLeftForTests(0);
		hero.pos = lastSeen + 1;
		hero.invisible = 1;

		EchoPolicyStatus status = invisibleOutOfLosStatus();
		int cell = EchoTargetPicker.pick(boss, status, "SpiritBow", false);

		Assertions.assertThat(cell).isEqualTo(-1);
	}

	@Test
	@DisplayName("non-AOE pick returns none without LOS when last seen is unknown")
	void nonAoeReturnsNoneWithoutLosOrLastSeen() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		boss.noteEnemySeenAt(-1);
		boss.setBlindDefenseShotsLeftForTests(2);
		hero.invisible = 1;

		EchoPolicyStatus status = invisibleOutOfLosStatus();
		int cell = EchoTargetPicker.pick(boss, status, "SpiritBow", false);

		Assertions.assertThat(cell).isEqualTo(-1);
	}

	@Test
	@DisplayName("blink pick lands farther from the hero than the echo currently is")
	void blinkPickLandsFartherFromHero() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		int distBefore = Dungeon.level.distance(boss.pos, hero.pos);

		int cell = EchoTargetPicker.pickBlinkAway(boss);

		Assertions.assertThat(cell).isGreaterThanOrEqualTo(0);
		Assertions.assertThat(cell).isNotEqualTo(hero.pos);
		Assertions.assertThat(Dungeon.level.distance(cell, hero.pos)).isGreaterThan(distBefore);
	}

	@Test
	@DisplayName("StoneOfBlink item pick uses blink-away aiming not enemy cell")
	void blinkItemPickDoesNotAimAtHero() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);

		int cell = EchoTargetPicker.pick(
				boss, new EchoPolicyStatus.Builder().enemyInLos(true).build(), "StoneOfBlink", false);

		Assertions.assertThat(cell).isNotEqualTo(hero.pos);
		Assertions.assertThat(Dungeon.level.distance(cell, hero.pos))
				.isGreaterThan(Dungeon.level.distance(boss.pos, hero.pos));
	}

	@Test
	@DisplayName("AOE pick aims near last seen while invisible with blind shots remaining")
	void aoeAimsNearLastSeenWhileInvisibleWithShots() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		int lastSeen = hero.pos;
		boss.noteEnemySeenAt(lastSeen);
		boss.setBlindDefenseShotsLeftForTests(2);
		hero.pos = lastSeen + Dungeon.level.width() * 2;
		hero.invisible = 1;

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.enemyInLos(false)
				.enemyStatuses(invisibleStatuses())
				.safeHazards(Collections.singleton(EchoPolicyHazards.FIRE_AOE))
				.build();
		int cell = EchoTargetPicker.pick(boss, status, "PotionOfLiquidFlame", true);

		Assertions.assertThat(cell).isGreaterThanOrEqualTo(0);
		Assertions.assertThat(Dungeon.level.distance(cell, lastSeen)).isLessThanOrEqualTo(1);
		Assertions.assertThat(Dungeon.level.distance(cell, hero.pos)).isGreaterThan(1);
	}

	@Test
	@DisplayName("consuming two blind defense shots exhausts further last-seen aim")
	void consumingTwoBlindShotsExhaustsAim() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		int lastSeen = hero.pos;
		boss.noteEnemySeenAt(lastSeen);
		boss.rearmBlindDefense();
		hero.pos = lastSeen + 1;
		hero.invisible = 1;
		EchoPolicyStatus status = invisibleOutOfLosStatus();

		Assertions.assertThat(EchoTargetPicker.pick(boss, status, "WandOfFireblast", false))
				.isEqualTo(lastSeen);
		boss.consumeBlindDefenseShot();
		Assertions.assertThat(EchoTargetPicker.pick(boss, status, "WandOfFireblast", false))
				.isEqualTo(lastSeen);
		boss.consumeBlindDefenseShot();
		Assertions.assertThat(EchoTargetPicker.pick(boss, status, "WandOfFireblast", false))
				.isEqualTo(-1);
	}

	@Test
	@DisplayName("rearming blind defense restores two last-seen shots after visibility")
	void rearmingBlindDefenseRestoresShots() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		boss.setBlindDefenseShotsLeftForTests(0);

		boss.rearmBlindDefense();

		Assertions.assertThat(boss.blindDefenseShotsLeft()).isEqualTo(2);
	}

	private static EchoPolicyStatus invisibleOutOfLosStatus() {
		return new EchoPolicyStatus.Builder()
				.enemyInLos(false)
				.enemyStatuses(invisibleStatuses())
				.build();
	}

	private static Set<String> invisibleStatuses() {
		Set<String> statuses = new HashSet<>();
		statuses.add("invisible");
		return statuses;
	}
}
