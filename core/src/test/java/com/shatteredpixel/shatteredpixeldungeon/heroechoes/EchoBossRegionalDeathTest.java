package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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

        EchoBoss boss = new EchoBoss(EchoTestSupport.warriorEchoWithData(5), 5);
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

        EchoBoss boss = new EchoBoss(EchoTestSupport.warriorEchoWithData(5), 5);
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
        EchoBoss boss = new EchoBoss(EchoTestSupport.warriorEchoWithData(5), 5);
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
        EchoBoss boss = new EchoBoss(EchoTestSupport.warriorEchoWithData(10), 10);
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
        EchoBoss boss = new EchoBoss(EchoTestSupport.warriorEchoWithData(15), 15);
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
        EchoBoss boss = new EchoBoss(EchoTestSupport.warriorEchoWithData(20), 20);
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
        EchoBoss boss = new EchoBoss(EchoTestSupport.warriorEchoWithData(25), 25);
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
}
