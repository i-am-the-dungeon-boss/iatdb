package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sheep;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * The two blocked-route senses the backend has always been allowed to key
 * reactions on but the game never wrote: {@code los_blocked} (burnable grass
 * eating the sightline, which {@code CLEAR_LOS} can open) and
 * {@code path_blocked} (a soft blocker standing in the shot, which
 * {@code PATH_THROUGH} pierces).
 * <p>
 * Both used to live as never-written {@code EchoBoss} fields, so
 * {@code CLEAR_LOS} reported permanently ready and fired an aimless shot at the
 * hero. See {@code docs/hero-echoes/echo-boss-code-patterns.md} § 3.1 / § 3.8.
 */
@ExtendWith(GdxTestExtension.class)
class EchoBlockedRouteSenseTest {

	@Test
	@DisplayName("burnable grass eating the sightline sets los_blocked and aims CLEAR_LOS at it")
	void grassOnSightlineSetsLosBlocked() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = blockedRouteBoss(hero);
		int between = placeTwoRowsNorth(hero, boss);
		Dungeon.level.map[between] = Terrain.HIGH_GRASS;
		Dungeon.level.buildFlagMaps();
		giveEchoItem(boss, new PotionOfLiquidFlame());
		fovWithHeroHidden(boss, hero);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, blockedRoutePolicy());

		Assertions.assertThat(status.selfStatuses).contains(EchoPolicyHazards.LOS_BLOCKED);
		Assertions.assertThat(status.isRoleReady("CLEAR_LOS")).isTrue();
		Assertions.assertThat(status.targetCellFor("CLEAR_LOS")).isEqualTo(between);
	}

	@Test
	@DisplayName("a wall eating the sightline is not los_blocked — no fire opens a wall")
	void wallOnSightlineIsNotLosBlocked() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = blockedRouteBoss(hero);
		int between = placeTwoRowsNorth(hero, boss);
		Dungeon.level.map[between] = Terrain.WALL;
		Dungeon.level.buildFlagMaps();
		giveEchoItem(boss, new PotionOfLiquidFlame());
		fovWithHeroHidden(boss, hero);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, blockedRoutePolicy());

		Assertions.assertThat(status.selfStatuses).doesNotContain(EchoPolicyHazards.LOS_BLOCKED);
		Assertions.assertThat(status.isRoleReady("CLEAR_LOS")).isFalse();
		Assertions.assertThat(status.targetCellFor("CLEAR_LOS")).isEqualTo(-1);
	}

	@Test
	@DisplayName("CLEAR_LOS stays unready while the hero is plainly visible")
	void visibleHeroIsNeverLosBlocked() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = blockedRouteBoss(hero);
		placeTwoRowsNorth(hero, boss);
		giveEchoItem(boss, new PotionOfLiquidFlame());
		fillFov(boss);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, blockedRoutePolicy());

		Assertions.assertThat(status.selfStatuses).doesNotContain(EchoPolicyHazards.LOS_BLOCKED);
		Assertions.assertThat(status.isRoleReady("CLEAR_LOS")).isFalse();
	}

	@Test
	@DisplayName("a soft blocker standing in the shot sets path_blocked")
	void softBlockerInTheShotSetsPathBlocked() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = blockedRouteBoss(hero);
		int between = placeTwoRowsNorth(hero, boss);
		Sheep sheep = new Sheep();
		sheep.pos = between;
		Actor.add(sheep);
		giveEchoItem(boss, new Dart());
		fillFov(boss);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, blockedRoutePolicy());

		Assertions.assertThat(status.selfStatuses).contains(EchoPolicyHazards.PATH_BLOCKED);
		Assertions.assertThat(status.isRoleReady("PATH_THROUGH")).isTrue();
	}

	@Test
	@DisplayName("a clear shot at the hero is not path_blocked")
	void clearShotIsNotPathBlocked() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = blockedRouteBoss(hero);
		placeTwoRowsNorth(hero, boss);
		giveEchoItem(boss, new Dart());
		fillFov(boss);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, blockedRoutePolicy());

		Assertions.assertThat(status.selfStatuses).doesNotContain(EchoPolicyHazards.PATH_BLOCKED);
	}

	/**
	 * Puts the boss two rows straight north of the hero on the otherwise open
	 * test level and returns the single cell between them.
	 */
	private static int placeTwoRowsNorth(Hero hero, EchoBoss boss) {
		Level level = Dungeon.level;
		boss.pos = hero.pos - 2 * level.width();
		return hero.pos - level.width();
	}

	private static void fillFov(EchoBoss boss) {
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
	}

	/** Everything visible except the hero, as grass or a wall would leave it. */
	private static void fovWithHeroHidden(EchoBoss boss, Hero hero) {
		fillFov(boss);
		boss.fieldOfView[hero.pos] = false;
		boss.noteEnemySeenAt(hero.pos);
	}

	private static void giveEchoItem(EchoBoss boss, Item item) {
		item.identify();
		item.collect(boss.getEchoHero().belongings.backpack);
	}

	private static EchoBoss blockedRouteBoss(Hero hero) {
		EchoPolicy policy = blockedRoutePolicy();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 4);
		return boss;
	}

	private static EchoPolicy blockedRoutePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("CLEAR_LOS", EchoTestSupport.capability("PotionOfLiquidFlame"))
				.put("PATH_THROUGH", EchoTestSupport.capability("Dart"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}
}
