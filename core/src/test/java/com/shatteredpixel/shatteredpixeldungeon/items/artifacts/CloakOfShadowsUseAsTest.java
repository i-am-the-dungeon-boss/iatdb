package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class CloakOfShadowsUseAsTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Echo useAs applies cloak stealth on the boss body not the kit")
	void echoUseAsAppliesStealthToBossBody() {
		Hero player = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		CloakOfShadows cloak = kit.belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.charge = 5;
		float kitBefore = kit.cooldown();

		boolean ok = cloak.useAs(UseContext.echo(boss));

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(CloakOfShadows.cloakStealth.class)).isNotNull();
		Assertions.assertThat(kit.buff(CloakOfShadows.cloakStealth.class)).isNull();
		Assertions.assertThat(boss.invisible).isGreaterThan(0);
		Assertions.assertThat(kit.invisible).isEqualTo(0);
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
		Assertions.assertThat(cloak.charge).isEqualTo(5);
	}

	@Test
	@DisplayName("Echo cloak-on plays operate VFX when the body sprite has a parent")
	void echoCloakOnPlaysOperateWhenSpriteHasParent() {
		Hero player = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		CloakOfShadows cloak = boss.getEchoHero().belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.charge = 5;
		int operatesBefore = EchoTestSupport.stubSpriteOperateCalls(boss);

		boolean ok = cloak.useAs(UseContext.echo(boss));

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(EchoTestSupport.stubSpriteOperateCalls(boss))
				.as("cloak activate must operate like Hero, not only on deactivate")
				.isGreaterThan(operatesBefore);
	}

	@Test
	@DisplayName("Echo useAs refuses when cloak has no charge")
	void echoUseAsRefusesWhenNoCharge() {
		Hero player = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		CloakOfShadows cloak = boss.getEchoHero().belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.charge = 0;

		boolean ok = cloak.useAs(UseContext.echo(boss));

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(boss.buff(CloakOfShadows.cloakStealth.class)).isNull();
		Assertions.assertThat(boss.invisible).isEqualTo(0);
	}

	@Test
	@DisplayName("Hero useAs spends turn and applies cloak stealth on the hero")
	void heroUseAsSpendsTurnAndAppliesStealth() {
		Hero hero = rogueHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		CloakOfShadows cloak = hero.belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.charge = 5;
		float before = hero.cooldown();

		boolean ok = cloak.useAs(UseContext.hero(hero));

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(hero.buff(CloakOfShadows.cloakStealth.class)).isNotNull();
		Assertions.assertThat(hero.invisible).isGreaterThan(0);
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
	}

	private static Hero rogueHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.ROGUE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}
}
