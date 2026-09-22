package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The generated playbook's {@code kite_step} reaction is armed by
 * {@code haste} / {@code stamina} / speed, and it spends the turn stepping away
 * only because RANGED is ready to use the space. In a doorway every step away
 * lands behind the door: the shot is gone, the turn falls through to the
 * hunting AI, and the AI walks the echo back into melee — the door dance, which
 * only ever showed up on a hasted echo because that is what arms the reaction.
 * <p>
 * A kite step that cannot buy a shot is refused, so the echo stands and fights
 * instead.
 */
@ExtendWith(GdxTestExtension.class)
class EchoKiteStepLineOfFireTest {

	@Test
	@DisplayName("a hasted echo in a doorway refuses the kite step that would blind it")
	void kiteStepRefusedWhenEveryRetreatIsBlind() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = kitePolicy();
		EchoBoss boss = rangedBoss(hero, policy);
		int door = putEchoInDoorwayNorthOfHero(hero, boss);
		Buff.affect(boss, Haste.class, 20f);

		// Premise: nowhere farther to stand that can still see the hero.
		for (int cell : fartherNeighbours(boss, hero)) {
			Assertions.assertThat(hasLine(cell, hero.pos))
					.as("cell %s should be blind", cell)
					.isFalse();
		}

		boolean stepped = boss.policyStepFurther(hero.pos, true, true);

		Assertions.assertThat(stepped).isFalse();
		Assertions.assertThat(boss.pos).isEqualTo(door);
	}

	@Test
	@DisplayName("the same echo still kites in the open, where the step buys a shot")
	void kiteStepStillTakenWhenItKeepsTheShot() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = kitePolicy();
		EchoBoss boss = rangedBoss(hero, policy);
		Buff.affect(boss, Haste.class, 20f);
		boss.pos = hero.pos + 1;
		int before = Dungeon.level.distance(boss.pos, hero.pos);

		boolean stepped = boss.policyStepFurther(hero.pos, true, true);

		Assertions.assertThat(stepped).isTrue();
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isGreaterThan(before);
		Assertions.assertThat(hasLine(boss.pos, hero.pos)).isTrue();
	}

	@Test
	@DisplayName("KEEP_DISTANCE does not spend the turn when the kite step is refused")
	void keepDistanceRoleFallsThroughInTheDoorway() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = kitePolicy();
		EchoBoss boss = rangedBoss(hero, policy);
		int door = putEchoInDoorwayNorthOfHero(hero, boss);
		Buff.affect(boss, Haste.class, 20f);
		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.rolesReady(set("KEEP_DISTANCE", "RANGED"))
				.distance(1)
				.build();

		boolean spent = EchoRoleExecutor.execute(
				boss, policy, status, new EchoPlan("KEEP_DISTANCE", "reactions", null));

		Assertions.assertThat(spent).isFalse();
		Assertions.assertThat(boss.pos).isEqualTo(door);
	}

	@Test
	@DisplayName("disengaging from an untouchable hero may still break the line")
	void disengageStillAllowedToBreakLine() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = kitePolicy();
		EchoBoss boss = rangedBoss(hero, policy);
		putEchoInDoorwayNorthOfHero(hero, boss);
		int before = Dungeon.level.distance(boss.pos, hero.pos);

		// The RUN stance's own step: losing the line is the point there.
		boolean stepped = boss.policyStepFurther(hero.pos, true);

		Assertions.assertThat(stepped).isTrue();
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isGreaterThan(before);
	}

	/**
	 * Walls the row above the hero except one door cell, and stands the echo in
	 * it with the hero directly below: every cell farther from the hero is on
	 * the far side of the door. Returns the door cell.
	 */
	private static int putEchoInDoorwayNorthOfHero(Hero hero, EchoBoss boss) {
		Level level = Dungeon.level;
		int width = level.width();
		int door = hero.pos - width;
		int rowStart = door - (door % width);
		for (int x = 0; x < width; x++) {
			int cell = rowStart + x;
			if (cell != door) {
				level.map[cell] = Terrain.WALL;
			}
		}
		level.map[door] = Terrain.DOOR;
		level.buildFlagMaps();
		boss.pos = door;
		return door;
	}

	private static Set<Integer> fartherNeighbours(EchoBoss boss, Hero hero) {
		Level level = Dungeon.level;
		int current = level.distance(boss.pos, hero.pos);
		Set<Integer> out = new HashSet<>();
		for (int i = 0; i < com.watabou.utils.PathFinder.NEIGHBOURS8.length; i++) {
			int cell = boss.pos + com.watabou.utils.PathFinder.NEIGHBOURS8[i];
			if (level.insideMap(cell)
					&& boss.policyCellPathable(cell)
					&& level.distance(cell, hero.pos) > current) {
				out.add(cell);
			}
		}
		return out;
	}

	/** Mirrors the echo's own line-of-fire rule: no solid and no sight blocker between. */
	private static boolean hasLine(int from, int to) {
		Level level = Dungeon.level;
		com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica line = new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(
				from, to,
				com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.STOP_TARGET
						| com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.STOP_SOLID);
		if (line.collisionPos != to) {
			return false;
		}
		for (int cell : line.subPath(1, line.dist - 1)) {
			if (level.losBlocking[cell]) {
				return false;
			}
		}
		return true;
	}

	private static EchoBoss rangedBoss(Hero hero, EchoPolicy policy) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.identify();
		wand.curCharges = 5;
		wand.collect(boss.getEchoHero().belongings.backpack);
		return boss;
	}

	private static EchoPolicy kitePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", EchoTestSupport.capability("WandOfMagicMissile"))
				.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static Set<String> set(String... roles) {
		Set<String> out = new HashSet<>();
		for (String role : roles) {
			out.add(role);
		}
		return out;
	}
}
