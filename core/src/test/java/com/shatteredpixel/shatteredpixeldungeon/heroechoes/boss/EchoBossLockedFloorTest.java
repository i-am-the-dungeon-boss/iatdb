package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(GdxTestExtension.class)
class EchoBossLockedFloorTest {

	@AfterEach
	void cleanup() {
		Dungeon.challenges = 0;
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@ParameterizedTest(name = "depth {0} restores LockedFloor regen like the regional boss")
	@CsvSource({
			"5",
			"10",
			"15",
			"20",
			"25",
	})
	@DisplayName("EchoBoss damage refreshes LockedFloor regen window at boss depths")
	void damageRefreshesLockedFloorAtBossDepths(int depth) {
		Hero hero = warrior();
		Dungeon.depth = depth;
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), depth);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		boss.defenseSkill = 0;

		LockedFloor lock = Buff.affect(hero, LockedFloor.class);
		exhaustLockedFloor(lock);
		Assertions.assertThat(lock.regenOn())
				.as("precondition: regen window spent")
				.isFalse();

		boss.damage(20, hero);

		Assertions.assertThat(lock.regenOn())
				.as("depth %d EchoBoss must add LockedFloor time like the regional boss", depth)
				.isTrue();
	}

	@ParameterizedTest(name = "depth {0} with Badder Bosses still refreshes LockedFloor")
	@CsvSource({
			"5",
			"15",
			"25",
	})
	@DisplayName("EchoBoss damage refreshes LockedFloor under STRONGER_BOSSES")
	void damageRefreshesLockedFloorWithStrongerBosses(int depth) {
		Dungeon.challenges = Challenges.STRONGER_BOSSES;
		Hero hero = warrior();
		Dungeon.depth = depth;
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), depth);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		boss.defenseSkill = 0;

		LockedFloor lock = Buff.affect(hero, LockedFloor.class);
		exhaustLockedFloor(lock);
		Assertions.assertThat(lock.regenOn()).isFalse();

		boss.damage(30, hero);

		Assertions.assertThat(lock.regenOn())
				.as("depth %d under Badder Bosses", depth)
				.isTrue();
	}

	/** Spends the initial window (50 normally, 20 with Badder Bosses). */
	private static void exhaustLockedFloor(LockedFloor lock) {
		lock.removeTime(Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 20f : 50f);
	}

	private static Hero warrior() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		hero.lvl = 10;
		hero.HP = hero.HT = 40;
		return hero;
	}
}
