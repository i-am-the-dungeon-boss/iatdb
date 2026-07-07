package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoBossAiAndMechanicsTest {

    @Test
    @DisplayName("EchoBoss spawns sleeping until the player engages it")
    void spawnsSleepingUntilPlayerEngages() {
        EchoBoss boss = new EchoBoss(EchoTestSupport.warriorEchoWithData(5), 5);

        Assertions.assertThat(boss.state).isEqualTo(boss.SLEEPING);
    }

    @Test
    @DisplayName("EchoBoss from snapshot applies boss HP scaling")
    void heroicBossFromSnapshotAppliesBossHpScaling() {
        Echo snap = EchoTestSupport.warriorEchoWithData(5);

        EchoBoss boss = new EchoBoss(snap, 5);

        Assertions.assertThat(boss.HT).isEqualTo(EchoBoss.scaledHT(snap, 5));
        Assertions.assertThat(boss.HP).isEqualTo(boss.HT);
        Assertions.assertThat(boss.getEcho()).isEqualTo(snap);
    }

    @Test
    @DisplayName("EchoBoss from snapshot exposes hero class metadata")
    void heroicBossFromSnapshotExposesClassMetadata() {
        Echo snap = EchoTestSupport.warriorEchoWithData(5);

        EchoBoss boss = new EchoBoss(snap, 5);

        Assertions.assertThat(boss.getEcho().heroClass).isEqualTo("WARRIOR");
    }

    @Test
    @DisplayName("EchoBoss uses healing when low HP and has a potion")
    void usesHealingWhenLowAndHasPotion() {
        EchoBoss boss = new EchoBoss(EchoTestSupport.warriorEchoWithData(5), 5);

        Assertions.assertThat(boss.wantsToHeal(30, true, false)).isTrue();
        Assertions.assertThat(boss.decideAction(30, true, false, 0))
                .isEqualTo(EchoBoss.IntendedAction.HEAL);
    }

    @Test
    @DisplayName("EchoBoss skips healing when threatened in melee")
    void skipsHealingWhenThreatened() {
        EchoBoss boss = new EchoBoss(EchoTestSupport.warriorEchoWithData(5), 5);

        Assertions.assertThat(boss.wantsToHeal(20, true, true)).isFalse();
    }

    @Test
    @DisplayName("EchoBoss respects healing potion cap")
    void respectsHealingPotionCap() {
        EchoBoss boss = new EchoBoss(EchoTestSupport.warriorEchoWithData(5), 5);
        boss.consumeHealingPotion();
        boss.consumeHealingPotion();

        Assertions.assertThat(boss.wantsToHeal(10, true, false)).isFalse();
        Assertions.assertThat(boss.healingPotionsUsed()).isEqualTo(EchoBoss.MAX_HEALING_POTIONS);
    }

    @Test
    @DisplayName("EchoBoss rejects echo without hero combat data")
    void rejectsEchoWithoutCombatData() {
        Assertions.assertThatThrownBy(() -> new EchoBoss(EchoTestSupport.warriorEcho(5), 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("combat data");
        Assertions.assertThatThrownBy(() -> new EchoBoss(null, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("EchoBoss combat delegation works without echo hero sprite")
    void combatDelegationWorksWithoutSprite() {
        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        hero.lvl = 6;
        hero.HP = hero.HT = 30;
        Bundle data = EchoTestSupport.bundleHero(hero);
        Echo echo = Echo.create(
                5, EchoTestSupport.TEST_GAME_VERSION, 1L,
                "WARRIOR", 6, 30, 30, data);

        EchoBoss boss = new EchoBoss(echo, 5);
        Hero target = new Hero();
        HeroClass.WARRIOR.initHero(target);

        Assertions.assertThat(boss.getEchoHero().sprite).isNull();

        Assertions.assertThat(boss.speed()).isEqualTo(hero.combatSpeed());
        Assertions.assertThat(boss.attackSkill(target)).isEqualTo(hero.attackSkill(target));
        Assertions.assertThat(boss.defenseSkill(target)).isEqualTo(hero.defenseSkill(target));
        Assertions.assertThat(boss.attackDelay()).isEqualTo(hero.attackDelay());

        Assertions.assertThatCode(() -> {
            boss.damageRoll();
            boss.drRoll();
            boss.attackProc(target, 10);
            boss.defenseProc(target, 10);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("EchoBoss defense skill delegates to echo hero")
    void defenseSkillDelegatesToEchoHero() {
        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        hero.lvl = 6;
        hero.HP = hero.HT = 30;
        Bundle data = EchoTestSupport.bundleHero(hero);
        Echo echo = Echo.create(
                5, EchoTestSupport.TEST_GAME_VERSION, 1L,
                "WARRIOR", 6, 30, 30, data);

        EchoBoss boss = new EchoBoss(echo, 5);
        Hero target = new Hero();
        HeroClass.WARRIOR.initHero(target);

        Assertions.assertThat(boss.defenseSkill(target))
                .isEqualTo(hero.defenseSkill(target));
    }

    @Test
    @DisplayName("EchoBoss consumes healing potion from echo hero inventory")
    void consumesHealingPotionFromInventory() {
        Hero hero = new Hero();
        Dungeon.hero = hero;
        HeroClass.WARRIOR.initHero(hero);
        hero.lvl = 6;
        hero.HP = hero.HT = 30;
        PotionOfHealing potion = new PotionOfHealing();
        potion.identify();
        potion.collect(hero.belongings.backpack);
        Bundle data = EchoTestSupport.bundleHero(hero);
        Echo echo = Echo.create(
                5, EchoTestSupport.TEST_GAME_VERSION, 1L,
                "WARRIOR", 6, 8, 30, data);

        EchoBoss boss = new EchoBoss(echo, 5);
        boss.HP = 8;

        Assertions.assertThat(boss.tryHealFromInventory()).isTrue();
        Assertions.assertThat(boss.healingPotionsUsed()).isEqualTo(1);
        Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfHealing.class)).isNull();
    }
}
