package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * Kiting only works if the echo gains ground. At speed parity the hero simply
 * walks after it, so a retreat step is paid for at a Haste-sized advantage over
 * the hero's current speed rather than at the echo's own speed.
 */
@ExtendWith(GdxTestExtension.class)
class EchoBossKiteSpeedTest {

	private static final Offset<Float> EPS = Offset.offset(0.0001f);

	@Test
	@DisplayName("a kite step costs a fraction of what the hero pays to follow it")
	void kiteStepOutpacesTheHero() {
		Hero hero = huntressHero();
		EchoBoss boss = kitingBoss(hero);

		boss.act();

		float heroMove = 1f / hero.combatSpeed();
		Assertions.assertThat(boss.cooldown())
				.isCloseTo(heroMove / EchoBoss.KITE_SPEED_ADVANTAGE, EPS);
		Assertions.assertThat(EchoBoss.KITE_SPEED_ADVANTAGE).isGreaterThanOrEqualTo(2.5f);
	}

	@Test
	@DisplayName("the kite advantage is measured against the hero's current speed, so Haste cannot close it")
	void hastedHeroCannotOutrunTheKite() {
		Hero hero = huntressHero();
		EchoBoss boss = kitingBoss(hero);
		Buff.affect(hero, Haste.class, Haste.DURATION);

		boss.act();

		float heroMove = 1f / hero.combatSpeed();
		Assertions.assertThat(boss.cooldown())
				.isCloseTo(heroMove / EchoBoss.KITE_SPEED_ADVANTAGE, EPS);
	}

	@Test
	@DisplayName("an echo already faster than the kite advantage keeps its own speed")
	void ownSpeedWinsWhenItIsHigher() {
		Hero hero = huntressHero();
		EchoBoss boss = kitingBoss(hero);
		Buff.affect(boss.getEchoHero(), Haste.class, Haste.DURATION);

		boss.act();

		Assertions.assertThat(boss.speed())
				.isGreaterThan(hero.combatSpeed() * EchoBoss.KITE_SPEED_ADVANTAGE);
		Assertions.assertThat(boss.cooldown()).isCloseTo(1f / boss.speed(), EPS);
	}

	@Test
	@DisplayName("stepping toward the hero still costs the echo's own speed")
	void closingInIsNotHastened() {
		Hero hero = huntressHero();
		EchoBoss boss = kitingBoss(hero);
		// Farther than ideal: the same policy closes in instead of kiting.
		placeOnRow(hero, boss, 1, 5);
		boss.timeToNow();
		int distBefore = Dungeon.level.distance(boss.pos, hero.pos);

		boss.act();

		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isLessThan(distBefore);
		Assertions.assertThat(boss.cooldown()).isCloseTo(1f / boss.speed(), EPS);
	}

	/** Huntress echo on a kite playbook, standing one cell from the hero. */
	private static EchoBoss kitingBoss(Hero hero) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, kitePolicy(), 5);
		boss.state = boss.HUNTING;
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		placeOnRow(hero, boss, 1, 2); // distance 1, ideal 3
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		boss.timeToNow();
		return boss;
	}

	private static Hero huntressHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.HUNTRESS.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static void placeOnRow(Hero hero, EchoBoss boss, int heroX, int bossX) {
		int y = 3;
		hero.pos = y * Dungeon.level.width() + heroX;
		boss.pos = y * Dungeon.level.width() + bossX;
	}

	private static EchoPolicy kitePolicy() {
		return EchoPolicy.fromJson(new JSONObject()
				.put("policy_schema_version", EchoTestSupport.TEST_GAME_VERSION)
				.put("capabilities", new JSONObject()
						.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
						.put("CLOSE_IN", EchoTestSupport.capability("*move_closer"))
						.put("MELEE", EchoTestSupport.capability("*melee"))
						.put("HOLD", EchoTestSupport.capability("*wait")))
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
						.put("default_roles", new JSONArray().put("MELEE").put("HOLD")))
				.put("tuning", new JSONObject()));
	}
}
