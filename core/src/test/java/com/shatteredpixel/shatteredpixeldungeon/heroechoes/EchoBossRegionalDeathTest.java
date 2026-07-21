package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.items.remains.SealShard;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;

@ExtendWith(GdxTestExtension.class)
class EchoBossRegionalDeathTest {

    @AfterEach
    void cleanup() {
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("regional death applies sewer boss score at depth 5")
    void appliesSewerBossScoreAtDepth5() {
        Statistics.bossScores[0] = 0;
        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);

        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(5), 5);
        boss.pos = 10;
        Dungeon.depth = 5;

        EchoBossRegionalDeath.apply(boss, boss);

        Assertions.assertThat(Statistics.bossScores[0]).isEqualTo(1000);
    }

    @Test
    @DisplayName("regional death captures echo from victorious hero at boss depth")
    void capturesEchoFromVictoriousHero() {
        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        hero.lvl = 7;

        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(5), 5);
        boss.pos = 10;
        Dungeon.depth = 5;

        EchoBossRegionalDeath.apply(boss, boss);

        EchoStorage storage = new EchoStorage();
        Assertions.assertThat(storage.loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION))
                .isPresent();
    }

    @Test
    @DisplayName("solo regional boss death captures hero into echoes-solo")
    void soloRegionalDeathCapturesHeroIntoEchoesSolo() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(5), 5);
        boss.pos = 10;
        Dungeon.depth = 5;

        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        hero.lvl = 12;
        hero.HP = hero.HT = 80;
        PlateArmor armor = new PlateArmor();
        armor.identify();
        hero.belongings.armor = armor;

        EchoBossRegionalDeath.apply(boss, boss);

        Assertions.assertThat(new File("echoes-solo/depth-5.dat")).exists();
        Assertions.assertThat(new File("echoes/depth-5.dat")).doesNotExist();
        Echo loaded = new EchoStorage().loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION).orElseThrow();
        Assertions.assertThat(loaded.heroClass).isEqualTo("WARRIOR");
        Assertions.assertThat(EchoHeroSnapshot.restoreHero(loaded).belongings.armor())
                .isInstanceOf(PlateArmor.class);
    }

    @Test
    @DisplayName("regional death applies prison boss score at depth 10")
    void appliesPrisonBossScoreAtDepth10() {
        Statistics.bossScores[1] = 0;
        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(10), 10);
        Dungeon.depth = 10;

        EchoBossRegionalDeath.apply(boss, boss);

        Assertions.assertThat(Statistics.bossScores[1]).isEqualTo(2000);
    }

    @Test
    @DisplayName("regional death applies caves boss score at depth 15")
    void appliesCavesBossScoreAtDepth15() {
        Statistics.bossScores[2] = 0;
        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(15), 15);
        Dungeon.depth = 15;

        EchoBossRegionalDeath.apply(boss, boss);

        Assertions.assertThat(Statistics.bossScores[2]).isEqualTo(3000);
    }

    @Test
    @DisplayName("regional death applies city boss score at depth 20")
    void appliesCityBossScoreAtDepth20() {
        Statistics.bossScores[3] = 0;
        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(20), 20);
        Dungeon.depth = 20;

        EchoBossRegionalDeath.apply(boss, boss);

        Assertions.assertThat(Statistics.bossScores[3]).isEqualTo(4000);
    }

    @Test
    @DisplayName("regional death applies halls boss score at depth 25")
    void appliesHallsBossScoreAtDepth25() {
        Statistics.bossScores[4] = 0;
        Statistics.spawnersAlive = 2;
        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(25), 25);
        Dungeon.depth = 25;

        EchoBossRegionalDeath.apply(boss, boss);

        Assertions.assertThat(Statistics.bossScores[4]).isEqualTo(7500);
    }

    @Test
    @DisplayName("regional death ignores null boss")
    void ignoresNullBoss() {
        Statistics.bossScores[0] = 0;
        Dungeon.depth = 5;

        EchoBossRegionalDeath.apply(null, null);

        Assertions.assertThat(Statistics.bossScores[0]).isZero();
    }

    @ParameterizedTest(name = "depth {0} does not award {1}")
    @CsvSource({
            "5, BOSS_SLAIN_1",
            "10, BOSS_SLAIN_2",
            "15, BOSS_SLAIN_3",
            "20, BOSS_SLAIN_4"
    })
    @DisplayName("regional echo death does not award regional boss slain badge")
    void doesNotAwardRegionalBossSlainBadge(int depth, Badges.Badge badge) {
        Badges.disown(badge);
        Badges.reset();

        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(depth), depth);
        boss.pos = 10;
        Dungeon.depth = depth;

        EchoBossRegionalDeath.apply(boss, boss);

        Assertions.assertThat(Badges.isUnlocked(badge)).isFalse();
        Assertions.assertThat(Badges.filterReplacedBadges(false)).doesNotContain(badge);
    }

    @Test
    @DisplayName("regional echo death does not award boss challenge badge when qualified")
    void doesNotAwardBossChallengeBadgeWhenQualified() {
        Badges.disown(Badges.Badge.BOSS_CHALLENGE_1);
        Badges.reset();
        Statistics.qualifiedForBossChallengeBadge = true;

        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(5), 5);
        boss.pos = 10;
        Dungeon.depth = 5;

        EchoBossRegionalDeath.apply(boss, boss);

        Assertions.assertThat(Badges.isUnlocked(Badges.Badge.BOSS_CHALLENGE_1)).isFalse();
        Assertions.assertThat(Badges.filterReplacedBadges(false)).doesNotContain(Badges.Badge.BOSS_CHALLENGE_1);
    }

    @Test
    @DisplayName("halls echo death does not award boss challenge badge for full spawners")
    void hallsEchoDeathDoesNotAwardBossChallengeBadge() {
        Badges.disown(Badges.Badge.BOSS_CHALLENGE_5);
        Badges.reset();
        Statistics.spawnersAlive = 4;
        Dungeon.challenges = Challenges.STRONGER_BOSSES;

        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(25), 25);
        boss.pos = 10;
        Dungeon.depth = 25;

        EchoBossRegionalDeath.apply(boss, boss);

        Assertions.assertThat(Badges.isUnlocked(Badges.Badge.BOSS_CHALLENGE_5)).isFalse();
        Assertions.assertThat(Badges.filterReplacedBadges(false)).doesNotContain(Badges.Badge.BOSS_CHALLENGE_5);
    }

    @Test
    @DisplayName("regional echo death does not award first-boss class badge")
    void doesNotAwardFirstBossClassBadge() {
        Badges.disown(Badges.Badge.BOSS_SLAIN_1_WARRIOR);
        Badges.reset();

        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(5), 5);
        boss.pos = 10;
        Dungeon.depth = 5;

        EchoBossRegionalDeath.apply(boss, boss);

        Assertions.assertThat(Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_1_WARRIOR)).isFalse();
    }

    @Test
    @DisplayName("regional echo death does not award third-boss subclass badge")
    void doesNotAwardThirdBossSubclassBadge() {
        Badges.disown(Badges.Badge.BOSS_SLAIN_3_BERSERKER);
        Badges.reset();

        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        hero.subClass = HeroSubClass.BERSERKER;
        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(15), 15);
        boss.pos = 10;
        Dungeon.depth = 15;

        EchoBossRegionalDeath.apply(boss, boss);

        Assertions.assertThat(Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_3_BERSERKER)).isFalse();
    }

    @Test
    @DisplayName("regional echo death does not award remains boss badge when qualified")
    void doesNotAwardRemainsBossBadgeWhenQualified() {
        Badges.disown(Badges.Badge.BOSS_SLAIN_REMAINS);
        Badges.reset();
        Statistics.qualifiedForBossRemainsBadge = true;

        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        Assertions.assertThat(new SealShard().collect(hero.belongings.backpack)).isTrue();
        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(5), 5);
        boss.pos = 10;
        Dungeon.depth = 5;

        EchoBossRegionalDeath.apply(boss, boss);

        Assertions.assertThat(Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_REMAINS)).isFalse();
        Assertions.assertThat(Badges.filterReplacedBadges(false)).doesNotContain(Badges.Badge.BOSS_SLAIN_REMAINS);
    }

    @Test
    @DisplayName("halls echo death does not award Taking the Mick")
    void hallsEchoDeathDoesNotAwardTakingTheMick() {
        Badges.disown(Badges.Badge.TAKING_THE_MICK);
        Badges.reset();

        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        Pickaxe pickaxe = new Pickaxe();
        pickaxe.level(20);
        hero.belongings.weapon = pickaxe;
        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(25), 25);
        boss.pos = 10;
        Dungeon.depth = 25;

        EchoBossRegionalDeath.apply(boss, hero);

        Assertions.assertThat(Badges.isUnlocked(Badges.Badge.TAKING_THE_MICK)).isFalse();
        Assertions.assertThat(Badges.filterReplacedBadges(false)).doesNotContain(Badges.Badge.TAKING_THE_MICK);
    }
}
