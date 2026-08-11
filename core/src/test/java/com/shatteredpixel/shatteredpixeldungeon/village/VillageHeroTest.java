package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class VillageHeroTest {

    @Test
    @DisplayName("The town avatar arrives with no gear at all")
    void villageHeroCarriesNothing() {
        VillageHero hero = new VillageHero(HeroClass.WARRIOR);

        Assertions.assertThat(hero.belongings.armor()).isNull();
        Assertions.assertThat(hero.belongings.weapon()).isNull();
        Assertions.assertThat(hero.belongings.backpack.items).isEmpty();
    }

    @Test
    @DisplayName("Nothing in the village can hurt the town avatar")
    void villageHeroTakesNoDamage() {
        VillageHero hero = new VillageHero(HeroClass.WARRIOR);
        int hp = hero.HP;

        hero.damage(999, this);

        Assertions.assertThat(hero.HP).isEqualTo(hp);
        Assertions.assertThat(hero.isAlive()).isTrue();
    }

    @Test
    @DisplayName("The town avatar cannot die, so no rankings or resurrect windows fire")
    void villageHeroCannotDie() {
        VillageHero hero = new VillageHero(HeroClass.WARRIOR);

        hero.die(this);

        Assertions.assertThat(hero.isAlive()).isTrue();
    }

    @Test
    @DisplayName("The town avatar earns no experience and stays at level 1")
    void villageHeroEarnsNoExperience() {
        VillageHero hero = new VillageHero(HeroClass.WARRIOR);

        hero.earnExp(500, VillageHeroTest.class);

        Assertions.assertThat(hero.exp).isZero();
        Assertions.assertThat(hero.lvl).isEqualTo(1);
    }

    @Test
    @DisplayName("Town has neither hunger nor regeneration on the turn loop")
    void villageHeroHasNoUpkeepBuffs() {
        VillageHero hero = new VillageHero(HeroClass.WARRIOR);

        hero.live();

        Assertions.assertThat(hero.buff(Hunger.class)).isNull();
        Assertions.assertThat(hero.buff(Regeneration.class)).isNull();
    }

    @Test
    @DisplayName("Searching finds nothing in a fully revealed village")
    void villageHeroNeverSearches() {
        Assertions.assertThat(new VillageHero(HeroClass.WARRIOR).search(true)).isFalse();
    }

    @Test
    @DisplayName("The hero pane is safe in town: talent tiers exist even unearned")
    void talentTiersExistSoWndHeroCannotThrow() {
        VillageHero hero = new VillageHero(HeroClass.WARRIOR);

        Assertions.assertThatCode(() -> {
            for (int tier = 1; tier <= hero.talents.size(); tier++) {
                hero.talentPointsAvailable(tier);
                hero.talentPointsSpent(tier);
            }
        }).doesNotThrowAnyException();

        Assertions.assertThat(hero.talents).isNotEmpty();
    }

    @Test
    @DisplayName("A null hero class still yields a drawable avatar")
    void heroClassAlwaysSet() {
        Assertions.assertThat(new VillageHero(null).heroClass).isNotNull();
    }

}
