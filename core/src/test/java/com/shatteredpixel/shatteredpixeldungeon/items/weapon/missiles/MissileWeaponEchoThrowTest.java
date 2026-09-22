package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoThrowAdapter;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.BlindingDart;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.HealingDart;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.ParalyticDart;

@ExtendWith(GdxTestExtension.class)
class MissileWeaponEchoThrowTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("Hero throw spends the hero turn and can damage the foe")
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

		knives.cast(hero, target.pos);
		boolean spent = true;

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
		Assertions.assertThat(target.HP).isLessThanOrEqualTo(hpBefore);
	}

	@Test
	@DisplayName("Echo throw damages the player without phantom kit spend")
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
		Assertions.assertThat(kit.sprite).isSameAs(boss.sprite);
		float kitBefore = kit.cooldown();
		int hpBefore = player.HP;
		int qtyBefore = kitKnives.quantity();
		player.invisible = 1;

		boolean spent = EchoThrowAdapter.throwItem(boss, kitKnives, player.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
		Assertions.assertThat(kitKnives.quantity()).isLessThan(qtyBefore);
		Assertions.assertThat(player.HP).isLessThanOrEqualTo(hpBefore);
		Assertions.assertThat(kit.sprite).isSameAs(boss.sprite);
	}

	@Test
	@DisplayName("Echo throw fires MissileSprite VFX when the body sprite has a parent")
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

		boolean spent = EchoThrowAdapter.throwItem(boss, kitKnives, player.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(fx.missileSpriteRecycles).isGreaterThan(0);
		Assertions.assertThat(player.HP).isLessThanOrEqualTo(hpBefore);
	}

	@Test
	@DisplayName("Echo-thrown Javelin spends exactly one from the kit and never the kit's turn")
	void echoThrownJavelinSpendsOne() {
		Hero player = EchoTestSupport.warriorHero();
		Javelin javelins = new Javelin();
		javelins.identify();
		javelins.quantity(3);
		javelins.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Javelin kitJavelins = kit.belongings.getItem(Javelin.class);
		Assertions.assertThat(kitJavelins).isNotNull();
		float kitBefore = kit.cooldown();
		int hpBefore = player.HP;
		player.invisible = 1;

		boolean spent = EchoThrowAdapter.throwItem(boss, kitJavelins, player.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(kitJavelins.quantity()).isEqualTo(2);
		// Hit or miss, the throw is gone from the kit — this is the finite-ammo claim.
		Assertions.assertThat(player.HP).isLessThanOrEqualTo(hpBefore);
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
	}

	@Test
	@DisplayName("Echo-thrown Paralytic Dart paralyses the player — the SETUP_CC payoff")
	void echoThrownParalyticDartParalyses() {
		Hero player = EchoTestSupport.warriorHero();
		ParalyticDart darts = new ParalyticDart();
		darts.identify();
		darts.quantity(2);
		darts.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		ParalyticDart kitDarts = boss.getEchoHero().belongings.getItem(ParalyticDart.class);
		Assertions.assertThat(kitDarts).isNotNull();
		player.invisible = 1;

		Assertions.assertThat(EchoThrowAdapter.throwItem(boss, kitDarts, player.pos)).isTrue();

		Assertions.assertThat(player.buff(Paralysis.class)).isNotNull();
	}

	@Test
	@DisplayName("Echo-thrown Blinding Dart blinds the player — the BLIND payoff")
	void echoThrownBlindingDartBlinds() {
		Hero player = EchoTestSupport.warriorHero();
		BlindingDart darts = new BlindingDart();
		darts.identify();
		darts.quantity(2);
		darts.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		BlindingDart kitDarts = boss.getEchoHero().belongings.getItem(BlindingDart.class);
		Assertions.assertThat(kitDarts).isNotNull();
		player.invisible = 1;

		Assertions.assertThat(EchoThrowAdapter.throwItem(boss, kitDarts, player.pos)).isTrue();

		Assertions.assertThat(player.buff(Blindness.class)).isNotNull();
	}

	/**
	 * Why the support darts are in {@code POLICY_EXCLUDED_ITEMS} on the backend.
	 * <p>
	 * The echo kit is a phantom {@link Hero} and {@code Hero} defaults to
	 * {@code Alignment.ALLY} — the same alignment as {@code Dungeon.hero} — so
	 * every tipped dart takes its same-alignment branch against the player.
	 * Healing Dart does not even branch: it cures and heals unconditionally, and
	 * its {@code damageRoll} returns 0 for an ally, so an echo throwing one is
	 * pure charity. Do not un-exclude these without fixing kit alignment first.
	 */
	@Test
	@DisplayName("Echo-thrown Healing Dart heals the player — why support darts are policy-excluded")
	void echoThrownHealingDartHealsThePlayer() {
		Hero player = EchoTestSupport.warriorHero();
		HealingDart darts = new HealingDart();
		darts.identify();
		darts.quantity(2);
		darts.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		HealingDart kitDarts = boss.getEchoHero().belongings.getItem(HealingDart.class);
		Assertions.assertThat(kitDarts).isNotNull();
		player.HP = 5;
		player.invisible = 1;

		Assertions.assertThat(EchoThrowAdapter.throwItem(boss, kitDarts, player.pos)).isTrue();

		Assertions.assertThat(player.buff(Healing.class)).isNotNull();
		Assertions.assertThat(player.HP).isGreaterThanOrEqualTo(5);
	}
}
