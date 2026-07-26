package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.PointF;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

@ExtendWith(GdxTestExtension.class)
class EchoLeaveAoeTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("status marks aoe_dot and LEAVE_AOE when standing in fire with a safe exit")
	void statusMarksAoeDotAndLeaveReady() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, leavePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		Blob.seed(boss.pos, 10, Fire.class);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, leavePolicy());

		Assertions.assertThat(status.selfStatuses).contains("aoe_dot");
		Assertions.assertThat(status.isRoleReady("LEAVE_AOE")).isTrue();
	}

	@Test
	@DisplayName("LEAVE_AOE is not ready when every adjacent cell is also aoe_dot")
	void leaveNotReadyWhenSurrounded() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, leavePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		seedFireAround(boss.pos);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, leavePolicy());

		Assertions.assertThat(status.selfStatuses).contains("aoe_dot");
		Assertions.assertThat(status.isRoleReady("LEAVE_AOE")).isFalse();
	}

	@Test
	@DisplayName("leave_aoe reaction wins when self has aoe_dot and LEAVE_AOE is ready")
	void leaveReactionMatches() {
		JSONObject when = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("self_status", "aoe_dot"))
				.put(new JSONObject().put("role_ready", "LEAVE_AOE")));
		JSONObject root = new JSONObject(leavePolicy().root().toString());
		root.put("reactions", new JSONArray().put(new JSONObject()
				.put("id", "leave_aoe_dot")
				.put("priority", 109)
				.put("when", when)
				.put("do", new JSONObject().put("use_role", "LEAVE_AOE"))));
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("reactions").put("default"))
				.put("default_roles", new JSONArray().put("MELEE")));
		EchoPolicy policy = EchoPolicy.fromJson(root);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.selfStatuses(Set.of("aoe_dot"))
				.rolesReady(Set.of("LEAVE_AOE", "MELEE"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, Collections.emptyMap());

		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("LEAVE_AOE");
		Assertions.assertThat(choice.layer).isEqualTo("reactions");
	}

	@Test
	@DisplayName("LEAVE_AOE steps out of fire toward the hero when not kiting")
	void leaveStepsTowardHeroWhenNotKiting() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, leavePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		linkSprite(boss);
		Blob.seed(boss.pos, 10, Fire.class);
		int distBefore = Dungeon.level.distance(boss.pos, hero.pos);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				leavePolicy(),
				readyLeave(),
				new EchoPolicyChoice("LEAVE_AOE", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(Blob.volumeAt(boss.pos, Fire.class)).isEqualTo(0);
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isLessThan(distBefore);
	}

	@Test
	@DisplayName("LEAVE_AOE steps out of fire away from the hero when kiting too close")
	void leaveStepsAwayWhenKiting() {
		Hero hero = huntressHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, kiteLeavePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		fillFov(boss);
		linkSprite(boss);
		Blob.seed(boss.pos, 10, Fire.class);
		int distBefore = Dungeon.level.distance(boss.pos, hero.pos);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, kiteLeavePolicy());
		boolean spent = EchoRoleExecutor.execute(
				boss,
				kiteLeavePolicy(),
				status,
				new EchoPolicyChoice("LEAVE_AOE", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(Blob.volumeAt(boss.pos, Fire.class)).isEqualTo(0);
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isGreaterThan(distBefore);
	}

	@Test
	@DisplayName("CLOSE_IN steps around fire on the path instead of into it")
	void closeInAvoidsFireOnPath() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, leavePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		placeOnRow(hero, boss, 1, 5);
		fillFov(boss);
		linkSprite(boss);
		int width = Dungeon.level.width();
		int y = 3;
		for (int x = 2; x <= 4; x++) {
			Blob.seed(y * width + x, 10, Fire.class);
		}
		int distBefore = Dungeon.level.distance(boss.pos, hero.pos);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				leavePolicy(),
				readyCloseIn(),
				new EchoPolicyChoice("CLOSE_IN", "positioning", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(Blob.volumeAt(boss.pos, Fire.class)).isEqualTo(0);
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isLessThan(distBefore);
	}

	@Test
	@DisplayName("CLOSE_IN steps around toxic gas and flaming grass")
	void closeInAvoidsToxicGasAndFlamingGrass() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, leavePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		placeOnRow(hero, boss, 1, 5);
		fillFov(boss);
		linkSprite(boss);
		int width = Dungeon.level.width();
		int y = 3;
		int grassFire = y * width + 2;
		Dungeon.level.map[grassFire] = Terrain.GRASS;
		Dungeon.level.buildFlagMaps();
		Blob.seed(grassFire, 10, Fire.class);
		Blob.seed(y * width + 3, 10, ToxicGas.class);
		Blob.seed(y * width + 4, 10, CorrosiveGas.class);
		int distBefore = Dungeon.level.distance(boss.pos, hero.pos);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				leavePolicy(),
				readyCloseIn(),
				new EchoPolicyChoice("CLOSE_IN", "positioning", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(EchoAoeDots.isAoeDotAt(boss, boss.pos)).isFalse();
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isLessThan(distBefore);
	}

	@Test
	@DisplayName("CLOSE_IN steps around frost and paralytic gas")
	void closeInAvoidsFrostAndParalyticGas() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, leavePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		placeOnRow(hero, boss, 1, 5);
		fillFov(boss);
		linkSprite(boss);
		int width = Dungeon.level.width();
		int y = 3;
		Blob.seed(y * width + 2, 10, Freezing.class);
		Blob.seed(y * width + 3, 10, Freezing.class);
		Blob.seed(y * width + 4, 10, ParalyticGas.class);
		int distBefore = Dungeon.level.distance(boss.pos, hero.pos);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				leavePolicy(),
				readyCloseIn(),
				new EchoPolicyChoice("CLOSE_IN", "positioning", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(Blob.volumeAt(boss.pos, Freezing.class)).isEqualTo(0);
		Assertions.assertThat(Blob.volumeAt(boss.pos, ParalyticGas.class)).isEqualTo(0);
		Assertions.assertThat(EchoAoeDots.isAoeDotAt(boss, boss.pos)).isFalse();
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isLessThan(distBefore);
	}

	@Test
	@DisplayName("LEAVE_AOE steps out of frost toward the hero when not kiting")
	void leaveStepsOutOfFrostTowardHero() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, leavePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		linkSprite(boss);
		Blob.seed(boss.pos, 10, Freezing.class);
		int distBefore = Dungeon.level.distance(boss.pos, hero.pos);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				leavePolicy(),
				readyLeave(),
				new EchoPolicyChoice("LEAVE_AOE", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(Blob.volumeAt(boss.pos, Freezing.class)).isEqualTo(0);
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isLessThan(distBefore);
	}

	@Test
	@DisplayName("KEEP_DISTANCE does not step further into fire")
	void keepDistanceAvoidsFire() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, leavePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		placeOnRow(hero, boss, 2, 3);
		fillFov(boss);
		linkSprite(boss);
		// Further along the row and the side-back cells that maximize distance.
		Blob.seed(boss.pos + 1, 10, Fire.class);
		Blob.seed(boss.pos + 1 - Dungeon.level.width(), 10, Fire.class);
		Blob.seed(boss.pos + 1 + Dungeon.level.width(), 10, Fire.class);
		int start = boss.pos;

		boolean spent = EchoRoleExecutor.execute(
				boss,
				leavePolicy(),
				readyKeepDistance(),
				new EchoPolicyChoice("KEEP_DISTANCE", "positioning", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.pos).isNotEqualTo(start);
		Assertions.assertThat(Blob.volumeAt(boss.pos, Fire.class)).isEqualTo(0);
	}

	private static EchoPolicyStatus readyLeave() {
		return new EchoPolicyStatus.Builder()
				.rolesReady(Set.of("LEAVE_AOE"))
				.selfStatuses(Set.of("aoe_dot"))
				.distance(2)
				.build();
	}

	private static EchoPolicyStatus readyCloseIn() {
		return new EchoPolicyStatus.Builder().rolesReady(Set.of("CLOSE_IN")).build();
	}

	private static EchoPolicyStatus readyKeepDistance() {
		return new EchoPolicyStatus.Builder().rolesReady(Set.of("KEEP_DISTANCE")).build();
	}

	private static EchoPolicy leavePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("LEAVE_AOE", EchoTestSupport.capability("*leave_aoe"))
				.put("CLOSE_IN", EchoTestSupport.capability("*move_closer"))
				.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
				.put("MELEE", EchoTestSupport.capability("*melee"))
				.put("WAIT", EchoTestSupport.capability("*wait")));
	}

	private static void placeOnRow(Hero hero, EchoBoss boss, int heroX, int bossX) {
		int y = 3;
		hero.pos = y * Dungeon.level.width() + heroX;
		boss.pos = y * Dungeon.level.width() + bossX;
	}

	private static EchoPolicy kiteLeavePolicy() {
		return EchoPolicy.fromJson(new JSONObject()
				.put("policy_schema_version", EchoTestSupport.TEST_GAME_VERSION)
				.put("capabilities", new JSONObject()
						.put("LEAVE_AOE", EchoTestSupport.capability("*leave_aoe"))
						.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
						.put("CLOSE_IN", EchoTestSupport.capability("*move_closer"))
						.put("RANGED", new JSONObject()
								.put("pick", "MAX_DAMAGE")
								.put("items", new JSONArray().put("SpiritBow")))
						.put("MELEE", EchoTestSupport.capability("*melee"))
						.put("WAIT", EchoTestSupport.capability("*wait")))
				.put("reactions", new JSONArray())
				.put("recipes", new JSONArray())
				.put("positioning", new JSONObject()
						.put("HUNTRESS", new JSONObject()
								.put("ideal_distance", 3)
								.put("if_closer", "KEEP_DISTANCE")
								.put("if_farther", "CLOSE_IN")))
				.put("matchups", new JSONObject())
				.put("selection", new JSONObject()
						.put("order", new JSONArray()
								.put("reactions").put("recipes").put("positioning")
								.put("matchups").put("default"))
						.put("default_roles", new JSONArray().put("RANGED").put("MELEE").put("WAIT")))
				.put("tuning", new JSONObject()));
	}

	private static Hero huntressHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.HUNTRESS.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static void fillFov(EchoBoss boss) {
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
	}

	private static void seedFireAround(int center) {
		Blob.seed(center, 10, Fire.class);
		for (int i = 0; i < com.watabou.utils.PathFinder.NEIGHBOURS8.length; i++) {
			int cell = center + com.watabou.utils.PathFinder.NEIGHBOURS8[i];
			if (Dungeon.level.insideMap(cell)) {
				Blob.seed(cell, 10, Fire.class);
			}
		}
	}

	private static void linkSprite(EchoBoss boss) {
		CharSprite sprite = new CharSprite() {
			@Override
			public void place(int cell) {
			}

			@Override
			public void turnTo(int from, int to) {
			}

			@Override
			public void move(int from, int to) {
				place(to);
			}

			@Override
			public void showAlert() {
			}

			@Override
			public void hideAlert() {
			}

			@Override
			public void hideLost() {
			}

			@Override
			public void hideInvestigate() {
			}

			@Override
			public void bloodBurstA(PointF from, int damage) {
			}

			@Override
			public void flash() {
			}

			@Override
			public void showStatus(int color, String text, Object... args) {
			}
		};
		sprite.ch = boss;
		boss.sprite = sprite;
	}
}
