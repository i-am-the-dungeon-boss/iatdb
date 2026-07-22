package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class UiUxAndPolishTest {

    @AfterEach
    void reset() {
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
    @DisplayName("Intro banner text includes username, class name, and kill count")
    void introBannerTextIncludesUsernameClassAndKillCount() {
        Echo echo = EchoTestSupport.warriorEcho(5);
        echo.userName = "Marwan";
        echo.killCount = 12;

        String text = EchoBossSpawner.introBannerText(echo);

        Assertions.assertThat(text).contains("Marwan");
        Assertions.assertThat(text.toLowerCase()).contains("warrior");
        Assertions.assertThat(text).contains("12");
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
