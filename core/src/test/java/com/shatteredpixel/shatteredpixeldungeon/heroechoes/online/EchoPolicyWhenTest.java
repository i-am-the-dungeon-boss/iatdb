package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class EchoPolicyWhenTest {

	@Test
	@DisplayName("all requires every child to match")
	void allRequiresEveryChild() {
		EchoPolicyStatus status = statusWith(0.7f, 0.04f, 1, set("RANGED"));
		JSONObject when = new JSONObject().put("all", new JSONArray()
				.put(new JSONObject().put("enemy_hp_below", 0.05))
				.put(new JSONObject().put("distance_lte", 1)));

		Assertions.assertThat(EchoPolicyWhen.matches(when, status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(when, statusWith(0.7f, 0.04f, 3, set())))
				.isFalse();
	}

	@Test
	@DisplayName("any matches when one child matches")
	void anyMatchesOneChild() {
		EchoPolicyStatus status = statusWith(0.7f, 0.5f, 3, set("RANGED"));
		JSONObject when = new JSONObject().put("any", new JSONArray()
				.put(new JSONObject().put("distance_lte", 1))
				.put(new JSONObject().put("role_ready", "RANGED")));

		Assertions.assertThat(EchoPolicyWhen.matches(when, status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(when, statusWith(0.7f, 0.5f, 3, set())))
				.isFalse();
	}

	@Test
	@DisplayName("role_ready follows status.rolesReady")
	void roleReadyFollowsStatus() {
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.rolesReady(set("MOVE_TO_WATER"))
				.build();

		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("role_ready", "MOVE_TO_WATER"), status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("role_ready", "CLEANSE_BURN"), status)).isFalse();
	}

	@Test
	@DisplayName("terrain_near is true within terrainNearTiles")
	void terrainNearWithinThreshold() {
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.terrainNearTiles(3)
				.terrainNear("water", 2)
				.build();

		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("terrain_near", "water"), status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("terrain_near", "grass"), status)).isFalse();
	}

	@Test
	@DisplayName("terrain_near_none is true when terrain is absent or too far")
	void terrainNearNoneWhenAbsent() {
		EchoPolicyStatus near = new EchoPolicyStatus.Builder()
				.terrainNearTiles(3)
				.terrainNear("water", 2)
				.build();
		EchoPolicyStatus far = new EchoPolicyStatus.Builder()
				.terrainNearTiles(3)
				.terrainNear("water", 5)
				.build();

		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("terrain_near_none", "water"), near)).isFalse();
		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("terrain_near_none", "water"), far)).isTrue();
	}

	@Test
	@DisplayName("self_status matches buff name on self")
	void selfStatusMatches() {
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.selfStatuses(set("burning"))
				.build();

		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("self_status", "burning"), status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("self_status", "paralysed"), status)).isFalse();
	}

	@Test
	@DisplayName("enemy_status_none is true when none of the listed statuses are present")
	void enemyStatusNone() {
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.enemyStatuses(Collections.emptySet())
				.build();

		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("enemy_status_none", new JSONArray()
						.put("paralysed").put("frozen")),
				status)).isTrue();
	}

	@Test
	@DisplayName("enemy_status_any is true when any listed status is present")
	void enemyStatusAny() {
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.enemyStatuses(set("paralysed"))
				.build();

		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("enemy_status_any", new JSONArray().put("paralysed")),
				status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("enemy_status_any", new JSONArray().put("frozen")),
				status)).isFalse();
	}

	@Test
	@DisplayName("enemy_hp_below and self_hp_below compare ratios")
	void hpBelowPredicates() {
		EchoPolicyStatus status = statusWith(0.2f, 0.04f, 1, set());

		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("enemy_hp_below", 0.05), status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("self_hp_below", 0.35), status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("enemy_hp_below", 0.03), status)).isFalse();
	}

	@Test
	@DisplayName("distance_gte matches when far enough")
	void distanceGte() {
		EchoPolicyStatus status = statusWith(1f, 1f, 3, set());

		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("distance_gte", 2), status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("distance_gte", 4), status)).isFalse();
	}

	@Test
	@DisplayName("enemy_class matches case-insensitively")
	void enemyClassMatches() {
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.enemyClass("HUNTRESS")
				.build();

		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("enemy_class", "huntress"), status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("enemy_class", "MAGE"), status)).isFalse();
	}

	@Test
	@DisplayName("on_terrain matches tile under echo")
	void onTerrainMatches() {
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.onTerrain("water")
				.build();

		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("on_terrain", "water"), status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("on_terrain", "grass"), status)).isFalse();
	}

	@Test
	@DisplayName("enemy_in_los matches boolean LOS flag")
	void enemyInLos() {
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.enemyInLos(true)
				.build();

		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("enemy_in_los", true), status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("enemy_in_los", false), status)).isFalse();
	}

	@Test
	@DisplayName("self_safe_for and self_unsafe_for follow hazard sets")
	void safetyPredicates() {
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.safeHazards(set("PAYOFF_AOE", "fire_aoe"))
				.unsafeHazards(set("toxic_aoe"))
				.build();

		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("self_safe_for", "PAYOFF_AOE"), status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("self_unsafe_for", "toxic_aoe"), status)).isTrue();
		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("self_safe_for", "toxic_aoe"), status)).isFalse();
	}

	@Test
	@DisplayName("unknown when keys are ignored for forward compatibility")
	void unknownKeysIgnored() {
		EchoPolicyStatus status = statusWith(1f, 1f, 1, set());

		Assertions.assertThat(EchoPolicyWhen.matches(
				new JSONObject().put("future_predicate", "x"), status)).isTrue();
	}

	private static EchoPolicyStatus statusWith(
			float selfHp, float enemyHp, int distance, Set<String> roles) {
		return new EchoPolicyStatus.Builder()
				.selfHpRatio(selfHp)
				.enemyHpRatio(enemyHp)
				.distance(distance)
				.rolesReady(roles)
				.build();
	}

	private static Set<String> set(String... values) {
		return new HashSet<>(Arrays.asList(values));
	}
}
