package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(GdxTestExtension.class)
class EchoDoorBreakTest {

	@Test
	@DisplayName("visibility flips near a door accumulate door-stall count")
	void visibilityFlipsNearDoorAccumulateStall() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, fireblastDoorBreakPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		int door = hero.pos + 1;
		Dungeon.level.map[door] = Terrain.DOOR;
		Dungeon.level.buildFlagMaps();
		boss.noteEnemySeenAt(hero.pos);

		boss.noteDoorStallVisibility(true);
		boss.noteDoorStallVisibility(false);
		boss.noteDoorStallVisibility(true);
		boss.noteDoorStallVisibility(false);

		Assertions.assertThat(boss.doorStallCell()).isEqualTo(door);
		Assertions.assertThat(boss.doorStallCount()).isGreaterThanOrEqualTo(2);
	}

	@Test
	@DisplayName("status marks door_stalling only at stall threshold")
	void statusDoorStallingAtThreshold() {
		Hero hero = mageWithFireblast();
		EchoPolicy policy = fireblastDoorBreakPolicy();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		int door = hero.pos + 1;
		Dungeon.level.map[door] = Terrain.DOOR;
		Dungeon.level.buildFlagMaps();
		boss.noteEnemySeenAt(hero.pos);
		boss.noteDoorStallVisibility(true);
		boss.noteDoorStallVisibility(false); // count 1

		EchoPolicyStatus early = EchoPolicyStatusBuilder.build(boss, policy);
		Assertions.assertThat(early.doorStalling).isFalse();

		boss.noteDoorStallVisibility(true);
		boss.noteDoorStallVisibility(false); // count 2+
		EchoPolicyStatus ready = EchoPolicyStatusBuilder.build(boss, policy);
		Assertions.assertThat(ready.doorStalling).isTrue();
		Assertions.assertThat(ready.isRoleReady("DOOR_BREAK")).isTrue();
	}

	@Test
	@DisplayName("door_break reaction picks DOOR_BREAK when door-stalling")
	void doorBreakReactionPicksDoorBreakWhenStalling() {
		EchoPolicy policy = fireblastDoorBreakPolicy();
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.doorStalling(true)
				.enemyInLos(false)
				.rolesReady(set("DOOR_BREAK", "CLOSE_IN", "RANGED"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("DOOR_BREAK");
		Assertions.assertThat(choice.layer).isEqualTo("reactions");
	}

	@Test
	@DisplayName("door_break reaction is skipped before stall threshold")
	void doorBreakReactionSkippedBeforeThreshold() {
		EchoPolicy policy = fireblastDoorBreakPolicy();
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.doorStalling(false)
				.enemyInLos(false)
				.rolesReady(set("DOOR_BREAK", "CLOSE_IN"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice == null || !"DOOR_BREAK".equals(choice.useRole)).isTrue();
	}

	@Test
	@DisplayName("door_break reaction is skipped when DOOR_BREAK is not ready")
	void doorBreakSkippedWithoutDoorBreakRole() {
		EchoPolicy policy = fireblastDoorBreakPolicy();
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.doorStalling(true)
				.enemyInLos(false)
				.rolesReady(set("RANGED", "CLOSE_IN"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice == null || !"DOOR_BREAK".equals(choice.useRole)).isTrue();
	}

	@Test
	@DisplayName("DOOR_BREAK execute throws blast stone at the stalling door")
	void doorBreakExecuteAimsAtDoor() {
		Hero hero = EchoTestSupport.warriorHero();
		StoneOfBlast stone = new StoneOfBlast();
		stone.quantity(1);
		stone.collect(hero.belongings.backpack);
		EchoPolicy policy = doorBreakPolicy("StoneOfBlast");
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		int door = hero.pos + 1;
		Dungeon.level.map[door] = Terrain.DOOR;
		Dungeon.level.buildFlagMaps();
		boss.noteEnemySeenAt(hero.pos);
		boss.noteDoorStallVisibility(true);
		boss.noteDoorStallVisibility(false);
		boss.noteDoorStallVisibility(true);
		boss.noteDoorStallVisibility(false);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.doorStalling(true)
				.rolesReady(set("DOOR_BREAK"))
				.build();
		EchoPolicyChoice choice = new EchoPolicyChoice("DOOR_BREAK", "reactions", null);

		boolean spent = EchoRoleExecutor.execute(boss, policy, status, choice);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(Dungeon.level.map[door]).isEqualTo(Terrain.EMBERS);
		Assertions.assertThat(boss.isDoorStalling()).isFalse();
	}

	@Test
	@DisplayName("huntress bow alone does not arm DOOR_BREAK readiness")
	void bowAloneDoesNotReadyDoorBreak() {
		Hero hero = huntressWithBow();
		EchoPolicy policy = EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "MAX_DAMAGE")
						.put("items", new JSONArray().put("SpiritBow")))
				.put("DOOR_BREAK", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("WandOfFireblast")))
				.put("CLOSE_IN", EchoTestSupport.capability("*move_closer")));
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(status.isRoleReady("DOOR_BREAK")).isFalse();
		Assertions.assertThat(status.isRoleReady("RANGED")).isTrue();
	}

	private static Hero mageWithFireblast() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 8;
		hero.HP = hero.HT = 40;
		WandOfFireblast wand = new WandOfFireblast();
		wand.identify();
		wand.curCharges = 3;
		wand.collect(hero.belongings.backpack);
		return hero;
	}

	private static Hero huntressWithBow() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.HUNTRESS.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		Assertions.assertThat(hero.belongings.getItem(SpiritBow.class)).isNotNull();
		return hero;
	}

	private static EchoPolicy fireblastDoorBreakPolicy() {
		return doorBreakPolicy("WandOfFireblast");
	}

	private static EchoPolicy doorBreakPolicy(String itemId) {
		JSONObject when = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("door_stalling", true))
				.put(new JSONObject().put("role_ready", "DOOR_BREAK")));
		return EchoPolicy.fromJson(new JSONObject()
				.put("policy_schema_version", "0.0.1")
				.put("capabilities", new JSONObject()
						.put("DOOR_BREAK", new JSONObject()
								.put("pick", "FIRST_LEGAL")
								.put("items", new JSONArray().put(itemId)))
						.put("RANGED", new JSONObject()
								.put("pick", "FIRST_LEGAL")
								.put("items", new JSONArray()
										.put("WandOfMagicMissile")
										.put("WandOfFireblast")))
						.put("CLOSE_IN", EchoTestSupport.capability("*move_closer"))
						.put("MELEE", EchoTestSupport.capability("*melee")))
				.put("reactions", new JSONArray().put(new JSONObject()
						.put("id", "door_break")
						.put("priority", 101)
						.put("when", when)
						.put("do", new JSONObject()
								.put("use_role", "DOOR_BREAK")
								.put("target", "door_cell"))))
				.put("recipes", new JSONArray())
				.put("positioning", new JSONObject())
				.put("matchups", new JSONObject())
				.put("selection", new JSONObject()
						.put("order", new JSONArray()
								.put("reactions").put("default"))
						.put("default_roles", new JSONArray().put("CLOSE_IN").put("MELEE")))
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
