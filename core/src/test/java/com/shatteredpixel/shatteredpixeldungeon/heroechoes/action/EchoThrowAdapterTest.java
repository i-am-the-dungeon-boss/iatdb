package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoThrowAdapterTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("Echo throw adapter damages the player without phantom kit spend")
	void echoThrowAdapterDamagesWithoutPhantomSpend() {
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
	@DisplayName("Echo throw adapter fires MissileSprite when the body sprite has a parent")
	void echoThrowAdapterFiresMissileSpriteWhenSpriteHasParent() {
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

		boolean spent = EchoThrowAdapter.throwItem(boss, kitKnives, player.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(fx.missileSpriteRecycles).isGreaterThan(0);
	}

	@Test
	@DisplayName("Echo throw adapter light-throws bombs with a lit fuse")
	void echoThrowAdapterLightThrowsBombs() {
		Hero player = EchoTestSupport.warriorHero();
		Bomb seed = new Bomb();
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Bomb bomb = boss.getEchoHero().belongings.getItem(Bomb.class);
		Assertions.assertThat(bomb).isNotNull();

		boolean spent = EchoThrowAdapter.throwItem(boss, bomb, player.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(Bomb.class)).isNull();
	}
}
