package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.PointF;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Characterizes {@link EchoBoss#policyStepFurther(int, boolean)}: while
 * kiting, a retreat step that puts a harmful plant between the echo and the
 * hero is preferred over one that merely maximizes raw distance/clearance,
 * the plant itself is never a legal landing cell, and retreating never fails
 * just because cover happens to be unavailable.
 */
@ExtendWith(GdxTestExtension.class)
class EchoPlantCoverRetreatTest {

	@Test
	@DisplayName("kiting prefers a plant-covered retreat cell over one with better raw clearance")
	void prefersPlantCoverOverRawClearance() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = openRoomBoss(hero);
		int width = Dungeon.level.width();
		// Hero at (3,3), boss at (2,2): five neighbours of boss are farther than
		// the current distance. Wall two of them off and gas-disqualify a third,
		// leaving exactly (3,1) [straight north] and (1,3) [straight west].
		Level level = Dungeon.level;
		level.map[hero.pos - 2 * width - 2] = Terrain.WALL; // (1,1)
		level.map[hero.pos - width - 2] = Terrain.WALL; // (1,2)
		Blob.seed(hero.pos - 2 * width - 1, 10, ToxicGas.class); // (2,1): disqualifies it
		Blob.seed(hero.pos - 2 * width + 1, 10, ToxicGas.class); // (4,1): thins (3,1)'s clearance
		level.buildFlagMaps();
		// (3,2) is the intermediate cell on (3,1)'s straight line back to the hero.
		plant(new Firebloom(), hero.pos - width);
		linkSprite(boss);

		int candidateWithCover = hero.pos - 2 * width; // (3,1)
		int candidateWithoutCover = hero.pos - 2; // (1,3)

		boolean movedKiting = boss.policyStepFurther(hero.pos, true);

		Assertions.assertThat(movedKiting).isTrue();
		Assertions.assertThat(boss.pos).isEqualTo(candidateWithCover);
		Assertions.assertThat(boss.pos).isNotEqualTo(candidateWithoutCover);
	}

	@Test
	@DisplayName("without preferring cover, the same layout picks the cell with better clearance instead")
	void ignoresCoverWhenNotKiting() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = openRoomBoss(hero);
		int width = Dungeon.level.width();
		Level level = Dungeon.level;
		level.map[hero.pos - 2 * width - 2] = Terrain.WALL; // (1,1)
		level.map[hero.pos - width - 2] = Terrain.WALL; // (1,2)
		Blob.seed(hero.pos - 2 * width - 1, 10, ToxicGas.class); // (2,1)
		Blob.seed(hero.pos - 2 * width + 1, 10, ToxicGas.class); // (4,1)
		level.buildFlagMaps();
		plant(new Firebloom(), hero.pos - width);
		linkSprite(boss);

		int candidateWithCover = hero.pos - 2 * width; // (3,1), lower raw clearance
		int candidateWithoutCover = hero.pos - 2; // (1,3), higher raw clearance

		boolean moved = boss.policyStepFurther(hero.pos, false);

		Assertions.assertThat(moved).isTrue();
		Assertions.assertThat(boss.pos).isEqualTo(candidateWithoutCover);
		Assertions.assertThat(boss.pos).isNotEqualTo(candidateWithCover);
	}

	@Test
	@DisplayName("never lands on the harmful plant itself, even as the only nearby farther cell")
	void neverLandsOnThePlant() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = openRoomBoss(hero);
		int width = Dungeon.level.width();
		boss.pos = hero.pos - width; // adjacent, straight north
		Level level = Dungeon.level;
		// Wall every other neighbour of the boss so the only geometrically
		// farther cell is the one about to hold the plant.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0) {
					continue;
				}
				int n = boss.pos + dy * width + dx;
				boolean isTheFartherCell = dx == 0 && dy == -1;
				if (!isTheFartherCell && level.insideMap(n)) {
					level.map[n] = Terrain.WALL;
				}
			}
		}
		level.buildFlagMaps();
		int onlyFartherCell = boss.pos - width;
		plant(new Firebloom(), onlyFartherCell);
		linkSprite(boss);

		boss.policyStepFurther(hero.pos, true);

		Assertions.assertThat(boss.pos).isNotEqualTo(onlyFartherCell);
	}

	@Test
	@DisplayName("still retreats while kiting when no plant cover is anywhere nearby")
	void stillRetreatsWithNoCoverAvailable() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = openRoomBoss(hero);
		int start = boss.pos;
		linkSprite(boss);

		boolean moved = boss.policyStepFurther(hero.pos, true);

		Assertions.assertThat(moved).isTrue();
		Assertions.assertThat(boss.pos).isNotEqualTo(start);
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos))
				.isGreaterThan(Dungeon.level.distance(start, hero.pos));
	}

	private static void plant(Plant plant, int cell) {
		plant.pos = cell;
		Dungeon.level.plants.put(cell, plant);
	}

	private static EchoBoss openRoomBoss(Hero hero) {
		com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy policy =
				EchoTestSupport.policyWithCapabilities(
						new org.json.JSONObject().put("MELEE", EchoTestSupport.capability("*melee")));
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 0);
		// Boss one cell north-west of the hero (2,2) on the 7x7 test level.
		// Level.insideMap excludes the outer ring, and both the retreat scan and
		// Ballistica assume endpoints stay off it, so this leaves a full ring of
		// interior room (1..5) in every direction to retreat into.
		boss.pos = hero.pos - Dungeon.level.width() - 1;
		return boss;
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
