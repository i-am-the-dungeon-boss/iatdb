package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.items.remains.SealShard;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Locks the badge awards that real regional bosses (Goo, Tengu, DM-300, Dwarf
 * King, Yog)
 * grant via {@link Badges#validateBossSlain()} and related validators.
 */
@ExtendWith(GdxTestExtension.class)
class RegionalBossBadgeAwardTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
		Dungeon.customSeedText = "";
		Statistics.qualifiedForBossRemainsBadge = false;
		Statistics.qualifiedForBossChallengeBadge = false;
	}

	@ParameterizedTest(name = "depth {0} awards {1}")
	@CsvSource({
			"5, BOSS_SLAIN_1",
			"10, BOSS_SLAIN_2",
			"15, BOSS_SLAIN_3",
			"20, BOSS_SLAIN_4"
	})
	@DisplayName("real regional boss slain validation awards depth badge")
	void realRegionalBossSlainAwardsDepthBadge(int depth, Badges.Badge badge) {
		Badges.disown(badge);
		Badges.reset();

		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		Dungeon.depth = depth;

		Badges.validateBossSlain();

		Assertions.assertThat(Badges.filterReplacedBadges(false)).contains(badge);
	}

	@Test
	@DisplayName("real goo-region boss slain awards warrior class badge")
	void realGooRegionBossSlainAwardsWarriorClassBadge() {
		Badges.disown(Badges.Badge.BOSS_SLAIN_1_WARRIOR);
		Badges.reset();

		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		Dungeon.depth = 5;

		Badges.validateBossSlain();

		Assertions.assertThat(Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_1_WARRIOR)).isTrue();
	}

	@Test
	@DisplayName("real dm300-region boss slain awards berserker subclass badge")
	void realDm300RegionBossSlainAwardsBerserkerSubclassBadge() {
		Badges.disown(Badges.Badge.BOSS_SLAIN_3_BERSERKER);
		Badges.reset();

		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		hero.subClass = HeroSubClass.BERSERKER;
		Dungeon.depth = 15;

		Badges.validateBossSlain();

		Assertions.assertThat(Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_3_BERSERKER)).isTrue();
	}

	@Test
	@DisplayName("real regional boss slain awards remains badge when qualified")
	void realRegionalBossSlainAwardsRemainsBadgeWhenQualified() {
		Badges.disown(Badges.Badge.BOSS_SLAIN_REMAINS);
		Badges.reset();
		Statistics.qualifiedForBossRemainsBadge = true;

		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		Assertions.assertThat(new SealShard().collect(hero.belongings.backpack)).isTrue();
		Dungeon.depth = 5;

		Badges.validateBossSlain();

		Assertions.assertThat(Badges.filterReplacedBadges(false)).contains(Badges.Badge.BOSS_SLAIN_REMAINS);
	}

	@ParameterizedTest(name = "depth {0} awards {1}")
	@CsvSource({
			"5, BOSS_CHALLENGE_1",
			"10, BOSS_CHALLENGE_2",
			"15, BOSS_CHALLENGE_3",
			"20, BOSS_CHALLENGE_4",
			"25, BOSS_CHALLENGE_5"
	})
	@DisplayName("real regional boss challenge validation awards challenge badge")
	void realRegionalBossChallengeAwardsBadge(int depth, Badges.Badge badge) {
		Badges.disown(badge);
		Badges.reset();

		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		Dungeon.depth = depth;

		Badges.validateBossChallengeCompleted();

		Assertions.assertThat(Badges.filterReplacedBadges(false)).contains(badge);
	}

	@Test
	@DisplayName("real yog kill with +20 pickaxe awards Taking the Mick")
	void realYogKillWithPickaxeAwardsTakingTheMick() {
		Badges.disown(Badges.Badge.TAKING_THE_MICK);
		Badges.reset();

		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		Pickaxe pickaxe = new Pickaxe();
		pickaxe.level(20);
		hero.belongings.weapon = pickaxe;
		Dungeon.depth = 25;

		Badges.validateTakingTheMick(hero);

		Assertions.assertThat(Badges.filterReplacedBadges(false)).contains(Badges.Badge.TAKING_THE_MICK);
	}
}
