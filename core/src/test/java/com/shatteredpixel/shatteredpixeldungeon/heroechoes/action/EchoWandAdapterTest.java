package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoWandAdapterTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("Echo wand adapter spends charges without phantom hero turn")
	void echoWandAdapterSpendsChargesWithoutPhantomTurn() {
		Hero player = mageHero();
		WandOfMagicMissile seed = new WandOfMagicMissile();
		seed.curCharges = 3;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Wand wand = kit.belongings.getItem(WandOfMagicMissile.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 3;
		float kitBefore = kit.cooldown();
		int hpBefore = player.HP;

		boolean ok = EchoWandAdapter.zap(boss, wand, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(wand.curCharges).isEqualTo(2);
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
		Assertions.assertThat(player.HP).isLessThanOrEqualTo(hpBefore);
		Assertions.assertThat(kit.sprite).isSameAs(boss.sprite);
	}

	@Test
	@DisplayName("Echo wand adapter dispels boss invisibility and reveals the hit hero")
	void echoWandAdapterDispelsBossAndHitHeroInvisibility() {
		Hero player = mageHero();
		WandOfMagicMissile seed = new WandOfMagicMissile();
		seed.curCharges = 3;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Wand wand = boss.getEchoHero().belongings.getItem(WandOfMagicMissile.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 3;

		Buff.affect(boss, Invisibility.class, Invisibility.DURATION);
		Buff.affect(player, Invisibility.class, Invisibility.DURATION);

		boolean ok = EchoWandAdapter.zap(boss, wand, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(Invisibility.class)).isNull();
		Assertions.assertThat(player.buff(Invisibility.class)).isNull();
	}

	@Test
	@DisplayName("Echo wand adapter fires MagicMissile when the body sprite has a parent")
	void echoWandAdapterFiresMagicMissileWhenSpriteHasParent() {
		Hero player = mageHero();
		WandOfMagicMissile seed = new WandOfMagicMissile();
		seed.curCharges = 3;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(boss);

		Wand wand = boss.getEchoHero().belongings.getItem(WandOfMagicMissile.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 3;

		boolean ok = EchoWandAdapter.zap(boss, wand, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(fx.magicMissileRecycles).isGreaterThan(0);
	}

	private static Hero mageHero() {
		Hero hero = new Hero();
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}
}
