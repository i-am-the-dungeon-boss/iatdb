package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoBossAiAndMechanicsTest {

    @AfterEach
    void cleanup() {
        Dungeon.level = null;
        EchoTestSupport.resetWorkflowState();
    }

    @Test
    @DisplayName("EchoBoss spawns sleeping until the player engages it")
    void spawnsSleepingUntilPlayerEngages() {
        EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(5), 5);

        Assertions.assertThat(boss.state).isEqualTo(boss.SLEEPING);
    }

    @Test
    @DisplayName("EchoBoss HT matches captured hero HT with no boss multiplier")
    void heroicBossFromSnapshotMatchesCapturedHt() {
        Echo snap = EchoTestSupport.warriorEchoWithData(5);

        EchoBoss boss = EchoTestSupport.createBoss(snap, 5);

        Assertions.assertThat(EchoBoss.scaledHT(snap, 5)).isEqualTo(snap.ht);
        Assertions.assertThat(EchoBoss.scaledHT(snap, 25)).isEqualTo(snap.ht);
        Assertions.assertThat(boss.HT).isEqualTo(snap.ht);
        Assertions.assertThat(boss.HP).isEqualTo(boss.HT);
        Assertions.assertThat(boss.getEcho()).isEqualTo(snap);
        Assertions.assertThat(boss.getEcho().heroClass).isEqualTo("WARRIOR");
    }

    @Test
    @DisplayName("EchoBoss rejects missing policy")
    void rejectsMissingPolicy() {
        Assertions.assertThatThrownBy(
                () -> new EchoBoss(EchoTestSupport.warriorEchoWithData(5), 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("echo_policy");
    }

    @Test
    @DisplayName("EchoBoss rejects unsupported policy")
    void rejectsUnsupportedPolicy() {
        Echo echo = EchoTestSupport.warriorEchoWithData(5);
        EchoPolicy unsupported = EchoPolicy.fromJson("{"
                + "\"policy_schema_version\":\"0.0.1\""
                + "}");

        Assertions.assertThatThrownBy(() -> new EchoBoss(echo, 5, unsupported))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("echo_policy");
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
    @DisplayName("ToxicGas damages EchoBoss and Poison can attach")
    void toxicGasDamagesEchoBoss() {
        Hero hero = EchoTestSupport.warriorHero();
        EchoBoss boss = EchoTestSupport.createBossWithPolicy(
                hero, EchoTestSupport.healCapabilityPolicy(), 5);
        EchoTestSupport.installEchoBossLevel(hero, boss, 2);
        Dungeon.depth = 5;

        Assertions.assertThat(boss.isImmune(ToxicGas.class)).isFalse();
        Assertions.assertThat(boss.isImmune(Poison.class)).isFalse();

        Buff.affect(boss, Poison.class).set(6f);
        Assertions.assertThat(boss.buff(Poison.class)).isNotNull();

        int hpBefore = boss.HP;
        ToxicGas gas = Blob.seed(boss.pos, 1000, ToxicGas.class);
        Actor.add(gas);
        gas.act();

        Assertions.assertThat(boss.HP).isLessThan(hpBefore);
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

        EchoBoss boss = EchoTestSupport.createBoss(echo, 5);
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

}
