package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Inferno;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Engulfed by a blob with no clear neighbour — an Infernal Brew lands on the
 * echo and seeds its whole 3x3. {@code LEAVE_AOE} stays unready there, so the
 * playbook's trapped rules keep their turn; when none of them can act either,
 * the last-resort escape crosses one hazard tile rather than let the echo stand
 * and burn.
 */
@ExtendWith(GdxTestExtension.class)
class EchoEngulfedEscapeTest {

	@Test
	@DisplayName("engulfed leaves LEAVE_AOE unready but the last resort still escapes")
	void engulfedEchoCrossesTheBlob() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, escapePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		engulf(boss.pos);
		int start = boss.pos;

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, escapePolicy());

		Assertions.assertThat(status.selfStatuses).contains(EchoAoeDots.STATUS);
		// Trapped rules (purity / ranged / melee) still own the turn first.
		Assertions.assertThat(status.isRoleReady("LEAVE_AOE")).isFalse();
		Assertions.assertThat(boss.policyStepOutOfAoe(hero.pos, false)).isFalse();

		Assertions.assertThat(boss.escapeAoeLastResort(hero.pos, false)).isTrue();
		Assertions.assertThat(boss.pos).isNotEqualTo(start);
		Assertions.assertThat(Dungeon.level.adjacent(start, boss.pos)).isTrue();
		Assertions.assertThat(hasClearNeighbour(boss, boss.pos)).isTrue();
	}

	@Test
	@DisplayName("last resort takes a clear neighbour when one exists rather than crossing")
	void lastResortPrefersClearGround() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, escapePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		Blob.seed(boss.pos, 120, Inferno.class);

		Assertions.assertThat(boss.escapeAoeLastResort(hero.pos, false)).isTrue();
		Assertions.assertThat(EchoAoeDots.isAoeDotAt(boss, boss.pos)).isFalse();
	}

	@Test
	@DisplayName("last resort does nothing when the echo is not standing in a hazard")
	void lastResortNoopOutsideHazard() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, escapePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		int start = boss.pos;

		Assertions.assertThat(boss.escapeAoeLastResort(hero.pos, false)).isFalse();
		Assertions.assertThat(boss.pos).isEqualTo(start);
	}

	@Test
	@DisplayName("burning and engulfed: MOVE_TO_WATER cannot step, the last resort moves anyway")
	void burningEngulfedStillMoves() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = escapePolicy();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		Dungeon.level.map[hero.pos - Dungeon.level.width()] = Terrain.WATER;
		Dungeon.level.buildFlagMaps();
		engulf(boss.pos);
		Buff.affect(boss, Burning.class);
		int start = boss.pos;

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		EchoPlan plan = EchoPolicyMatcher.choose(policy, status, new HashMap<String, Integer>());

		// This is the logged fallthrough: the water reaction matches and spends nothing.
		Assertions.assertThat(plan).isNotNull();
		Assertions.assertThat(plan.useRole).isEqualTo("MOVE_TO_WATER");
		Assertions.assertThat(EchoRoleExecutor.execute(boss, policy, status, plan)).isFalse();
		Assertions.assertThat(boss.pos).isEqualTo(start);

		Assertions.assertThat(boss.escapeAoeLastResort(hero.pos, false)).isTrue();
		Assertions.assertThat(boss.pos).isNotEqualTo(start);
	}

	/** Infernal Brew pattern: the cell and every neighbour of it. */
	private static void engulf(int cell) {
		Blob.seed(cell, 120, Inferno.class);
		for (int i : PathFinder.NEIGHBOURS8) {
			if (Dungeon.level.insideMap(cell + i)) {
				Blob.seed(cell + i, 120, Inferno.class);
			}
		}
	}

	private static boolean hasClearNeighbour(EchoBoss boss, int cell) {
		for (int i : PathFinder.NEIGHBOURS8) {
			int n = cell + i;
			if (Dungeon.level.insideMap(n)
					&& Dungeon.level.passable[n]
					&& !EchoAoeDots.isAoeHazardForPath(boss, n, false)) {
				return true;
			}
		}
		return false;
	}

	private static void fillFov(EchoBoss boss) {
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		Arrays.fill(Dungeon.level.heroFOV, true);
	}

	/** leave_aoe_dot (109) over burn_step_into_water (108), as the backend ranks them. */
	private static EchoPolicy escapePolicy() {
		JSONObject root = new JSONObject(EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("LEAVE_AOE", EchoTestSupport.capability("*leave_aoe"))
				.put("MOVE_TO_WATER", EchoTestSupport.capability("*move_to_terrain:water"))
				.put("MELEE", EchoTestSupport.capability("*melee"))).root().toString());
		root.put("reactions", new JSONArray()
				.put(new JSONObject()
						.put("id", "leave_aoe_dot")
						.put("priority", 109)
						.put("when", new JSONObject().put("all", new JSONArray()
								.put(new JSONObject().put("self_status", EchoAoeDots.STATUS))
								.put(new JSONObject().put("role_ready", "LEAVE_AOE"))))
						.put("do", new JSONObject().put("use_role", "LEAVE_AOE")))
				.put(new JSONObject()
						.put("id", "burn_step_into_water")
						.put("priority", 108)
						.put("when", new JSONObject().put("all", new JSONArray()
								.put(new JSONObject().put("self_status", "burning"))
								.put(new JSONObject().put("terrain_near", "water"))
								.put(new JSONObject().put("role_ready", "MOVE_TO_WATER"))))
						.put("do", new JSONObject().put("use_role", "MOVE_TO_WATER"))));
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("reactions").put("default"))
				.put("default_roles", new JSONArray().put("MELEE")));
		return EchoPolicy.fromJson(root);
	}
}
