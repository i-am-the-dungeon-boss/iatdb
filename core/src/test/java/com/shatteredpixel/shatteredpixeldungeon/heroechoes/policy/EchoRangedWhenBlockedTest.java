package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Inferno;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.HashMap;

/**
 * An echo that cannot walk to the hero has to shoot it. The hero standing in a
 * blob far away is exactly the payoff the echo seeded, so a turn spent not
 * shooting is the blob burning down without pressure behind it.
 */
@ExtendWith(GdxTestExtension.class)
class EchoRangedWhenBlockedTest {

	@Test
	@DisplayName("route walled off by a blob: the echo shoots the distant hero instead of closing")
	void shootsWhenItCannotClose() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = closeInThenRangedPolicy();
		EchoBoss boss = rangedBoss(hero, policy);
		burnColumn(3);
		Blob.seed(hero.pos, 120, Inferno.class);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		EchoPlan plan = EchoPolicyMatcher.choose(policy, status, new HashMap<String, Integer>());

		Assertions.assertThat(status.distance).isEqualTo(4);
		Assertions.assertThat(status.enemyInLos).isTrue();
		Assertions.assertThat(status.isRoleReady(EchoRole.CLOSE_IN.id())).isFalse();
		Assertions.assertThat(status.isRoleReady(EchoRole.RANGED.id())).isTrue();
		Assertions.assertThat(plan).isNotNull();
		Assertions.assertThat(plan.useRole).isEqualTo(EchoRole.RANGED.id());
		Assertions.assertThat(EchoTargetPicker.pick(boss, status, "WandOfMagicMissile", false))
				.isEqualTo(hero.pos);
	}

	@Test
	@DisplayName("with the route open the echo closes in rather than shooting")
	void closesInWhenItCan() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = closeInThenRangedPolicy();
		EchoBoss boss = rangedBoss(hero, policy);
		Blob.seed(hero.pos, 120, Inferno.class);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		EchoPlan plan = EchoPolicyMatcher.choose(policy, status, new HashMap<String, Integer>());

		Assertions.assertThat(plan).isNotNull();
		Assertions.assertThat(plan.useRole).isEqualTo(EchoRole.CLOSE_IN.id());
	}

	@Test
	@DisplayName("out of line of sight the echo has no shot at all, blocked route or not")
	void noShotWithoutLineOfSight() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = closeInThenRangedPolicy();
		EchoBoss boss = rangedBoss(hero, policy);
		burnColumn(3);
		Blob.seed(hero.pos, 120, Inferno.class);
		boss.fieldOfView[hero.pos] = false;

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		EchoPlan plan = EchoPolicyMatcher.choose(policy, status, new HashMap<String, Integer>());

		Assertions.assertThat(status.enemyInLos).isFalse();
		Assertions.assertThat(status.isRoleReady(EchoRole.RANGED.id())).isFalse();
		// Falls to MELEE, which the executor refuses at range: the turn drops to
		// the hunting AI, which cannot path either. This is the stall.
		Assertions.assertThat(plan).isNotNull();
		Assertions.assertThat(plan.useRole).isEqualTo(EchoRole.MELEE.id());
		Assertions.assertThat(EchoRoleExecutor.execute(boss, policy, status, plan)).isFalse();
	}

	/** Hero at (3,1), echo at (3,5): distance 4, wand in the kit. */
	private static EchoBoss rangedBoss(Hero hero, EchoPolicy policy) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		boss.state = boss.HUNTING;
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		int width = Dungeon.level.width();
		hero.pos = 3 * width + 1;
		boss.pos = 3 * width + 5;
		Arrays.fill(boss.fieldOfView, true);
		Arrays.fill(Dungeon.level.heroFOV, true);
		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.identify();
		wand.curCharges = 5;
		wand.collect(boss.getEchoHero().belongings.backpack);
		boss.timeToNow();
		return boss;
	}

	private static void burnColumn(int column) {
		int width = Dungeon.level.width();
		for (int row = 0; row < Dungeon.level.height(); row++) {
			Blob.seed(row * width + column, 120, Inferno.class);
		}
	}

	/** Melee ideal distance, so CLOSE_IN owns the turn whenever it is ready. */
	private static EchoPolicy closeInThenRangedPolicy() {
		JSONObject root = new JSONObject(EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put(EchoRole.CLOSE_IN.id(), EchoTestSupport.capability("*move_closer"))
				.put(EchoRole.RANGED.id(), EchoTestSupport.capability("WandOfMagicMissile"))
				.put(EchoRole.MELEE.id(), EchoTestSupport.capability("*melee"))).root().toString());
		root.put("positioning", new JSONObject()
				.put("DEFAULT", new JSONObject()
						.put("ideal_distance", 1)
						.put("if_farther", EchoRole.CLOSE_IN.id())));
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("positioning").put("default"))
				.put("default_roles", new JSONArray()
						.put(EchoRole.RANGED.id())
						.put(EchoRole.MELEE.id())));
		return EchoPolicy.fromJson(root);
	}
}
