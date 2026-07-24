package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class UiUxAndPolishTest {

    @AfterEach
    void reset() {
        BossHealthBar.assignBoss(null);
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("Intro banner text includes hero class and level from snapshot")
    void introBannerTextUsesSnapshotMetadata() {
        String text = EchoBossSpawner.introBannerText(EchoTestSupport.warriorEcho(5));

        Assertions.assertThat(text).isNotBlank();
        Assertions.assertThat(text.toLowerCase()).contains("warrior");
    }

    @Test
    @DisplayName("EchoBoss name is the echo username")
    void echoBossNameIsUsername() {
        Echo echo = EchoTestSupport.warriorEchoWithData(5);
        echo.userName = "Marwan";
        EchoBoss boss = EchoTestSupport.createBoss(echo, 5);

        Assertions.assertThat(boss.name()).isEqualTo("Marwan");
    }

    @Test
    @DisplayName("Boss health bar label renders EchoBoss username")
    void bossHealthBarLabelRendersEchoUsername() {
        Echo echo = EchoTestSupport.warriorEchoWithData(5);
        echo.userName = "Marwan";
        EchoBoss boss = EchoTestSupport.createBoss(echo, 5);

        Assertions.assertThat(BossHealthBar.nameLabelFor(boss))
                .isEqualTo(Messages.titleCase("Marwan"));
    }

    @Test
    @DisplayName("Boss health bar label stays empty for non-echo bosses")
    void bossHealthBarLabelEmptyForNonEchoBoss() {
        Assertions.assertThat(BossHealthBar.nameLabelFor(new Goo())).isNull();
    }

    @Test
    @DisplayName("Defeat banner text includes the echo username")
    void defeatBannerTextIncludesUsername() {
        Echo echo = EchoTestSupport.warriorEcho(5);
        echo.userName = "Marwan";

        String text = EchoBossSpawner.defeatBannerText(echo);

        Assertions.assertThat(text).contains("Marwan");
        Assertions.assertThat(text.toLowerCase()).contains("defeated");
    }

    @Test
    @DisplayName("Defeat banner text falls back when echo is missing")
    void defeatBannerTextFallsBackWithoutEcho() {
        String text = EchoBossSpawner.defeatBannerText(null);

        Assertions.assertThat(text).isNotBlank();
        Assertions.assertThat(text.toLowerCase()).contains("defeated");
    }

    @Test
    @DisplayName("EchoBossSprite resolves armor tier from echo hero equipment")
    void resolvesArmorTierFromEchoHero() {
        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        PlateArmor armor = new PlateArmor();
        armor.identify();
        hero.belongings.armor = armor;

        Assertions.assertThat(EchoBossSprite.armorTierFor(hero, null)).isEqualTo(armor.tier);
    }

    @Test
    @DisplayName("EchoBossSprite falls back to snapshot level for armor tier")
    void fallsBackToSnapshotLevelForArmorTier() {
        Echo echo = EchoTestSupport.warriorEcho(5);
        echo.lvl = 12;

        Assertions.assertThat(EchoBossSprite.armorTierFor(null, echo)).isEqualTo(3);
    }

    @Test
    @DisplayName("EchoBossSprite falls back to snapshot level when echo hero has no armor")
    void fallsBackWhenEchoHeroHasNoArmor() {
        Echo echo = EchoTestSupport.warriorEcho(5);
        echo.lvl = 12;

        Hero hero = new Hero();
        hero.live();
        hero.lvl = echo.lvl;

        Assertions.assertThat(hero.tier()).isZero();
        Assertions.assertThat(EchoBossSprite.armorTierFor(hero, echo)).isEqualTo(3);
    }
}
