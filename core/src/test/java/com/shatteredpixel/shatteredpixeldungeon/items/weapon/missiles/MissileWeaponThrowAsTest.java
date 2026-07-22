package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class MissileWeaponThrowAsTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Hero throwAs spends the hero turn and can damage the foe")
	void heroThrowAsSpendsTurnAndDamages() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		ThrowingKnife knives = new ThrowingKnife();
		knives.identify();
		knives.quantity(3);
		knives.collect(hero.belongings.backpack);
		float before = hero.cooldown();
		int hpBefore = target.HP;
		target.invisible = 1; // guarantee hit

		boolean spent = knives.throwAs(UseContext.hero(hero), target.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
		Assertions.assertThat(target.HP).isLessThanOrEqualTo(hpBefore);
	}

	@Test
	@DisplayName("Echo throwAs damages the player without phantom kit spend")
	void echoThrowAsDamagesWithoutPhantomSpend() {
		Hero player = EchoTestSupport.warriorHero();
		ThrowingKnife knives = new ThrowingKnife();
		knives.identify();
		knives.quantity(3);
		knives.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		ThrowingKnife kitKnives = kit.belongings.getItem(ThrowingKnife.class);
		Assertions.assertThat(kitKnives).isNotNull();
		Assertions.assertThat(kit.sprite).isNull();
		float kitBefore = kit.cooldown();
		int hpBefore = player.HP;
		int qtyBefore = kitKnives.quantity();
		player.invisible = 1;

		boolean spent = kitKnives.throwAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
		Assertions.assertThat(kitKnives.quantity()).isLessThan(qtyBefore);
		Assertions.assertThat(player.HP).isLessThanOrEqualTo(hpBefore);
		Assertions.assertThat(kit.sprite).isNull();
	}

	@Test
	@DisplayName("Echo throwAs fires MissileSprite VFX when the body sprite has a parent")
	void echoThrowAsFiresMissileSpriteWhenSpriteHasParent() {
		Hero player = EchoTestSupport.warriorHero();
		ThrowingKnife knives = new ThrowingKnife();
		knives.identify();
		knives.quantity(3);
		knives.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(boss);
		ThrowingKnife kitKnives = boss.getEchoHero().belongings.getItem(ThrowingKnife.class);
		Assertions.assertThat(kitKnives).isNotNull();
		player.invisible = 1;
		int hpBefore = player.HP;

		boolean spent = kitKnives.throwAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(fx.missileSpriteRecycles).isGreaterThan(0);
		Assertions.assertThat(player.HP).isLessThanOrEqualTo(hpBefore);
	}
}
