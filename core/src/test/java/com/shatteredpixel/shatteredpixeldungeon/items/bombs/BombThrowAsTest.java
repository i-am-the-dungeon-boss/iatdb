package com.shatteredpixel.shatteredpixeldungeon.items.bombs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class BombThrowAsTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() throws Exception {
		TargetHealthIndicator.instance = null;
		setLightingFuse(false);
	}

	@Test
	@DisplayName("Hero plain THROW leaves bomb unlit (unlike LIGHTTHROW)")
	void heroPlainThrowLeavesBombUnlit() throws Exception {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		Bomb bomb = new Bomb();
		bomb.collect(hero.belongings.backpack);
		// Same intent as Bomb.execute(hero, AC_THROW): do not light.
		setLightingFuse(false);
		float before = hero.cooldown();

		boolean spent = bomb.throwAs(UseContext.hero(hero), target.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
		Assertions.assertThat(hero.belongings.getItem(Bomb.class)).isNull();
		Bomb landed = findBombAt(target.pos);
		Assertions.assertThat(landed).isNotNull();
		Assertions.assertThat(landed.fuse)
				.as("Hero THROW must drop an unlit bomb, not light+throw")
				.isNull();
	}

	@Test
	@DisplayName("Hero LIGHTTHROW lights fuse, detaches, and spends the hero turn")
	void heroLightThrowLightsFuseAndSpendsTurn() throws Exception {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		Bomb bomb = new Bomb();
		bomb.collect(hero.belongings.backpack);
		// Same intent as Bomb.execute(hero, AC_LIGHTTHROW) before cast.
		setLightingFuse(true);
		float before = hero.cooldown();

		boolean spent = bomb.throwAs(UseContext.hero(hero), target.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
		Assertions.assertThat(hero.belongings.getItem(Bomb.class)).isNull();
		Bomb landed = findBombAt(target.pos);
		Assertions.assertThat(landed).isNotNull();
		Assertions.assertThat(landed.fuse)
				.as("Hero LIGHTTHROW must land a lit bomb")
				.isNotNull();
	}

	@Test
	@DisplayName("Hero cast after THROW intent stays unlit while LIGHTTHROW intent lights")
	void heroThrowVersusLightThrowDifferOnFuse() throws Exception {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		Bomb unlit = new Bomb();
		unlit.quantity(2);
		unlit.collect(hero.belongings.backpack);

		setLightingFuse(false);
		Assertions.assertThat(unlit.throwAs(UseContext.hero(hero), target.pos)).isTrue();
		Bomb plainLanded = findBombAt(target.pos);
		Assertions.assertThat(plainLanded).isNotNull();
		Assertions.assertThat(plainLanded.fuse).isNull();

		// Clear heap so the next land is unambiguous.
		Dungeon.level.heaps.remove(target.pos);

		Bomb remaining = hero.belongings.getItem(Bomb.class);
		Assertions.assertThat(remaining).isNotNull();
		setLightingFuse(true);
		Assertions.assertThat(remaining.throwAs(UseContext.hero(hero), target.pos)).isTrue();
		Bomb litLanded = findBombAt(target.pos);
		Assertions.assertThat(litLanded).isNotNull();
		Assertions.assertThat(litLanded.fuse).isNotNull();
	}

	@Test
	@DisplayName("Echo bomb throwAs lights fuse, detaches from kit, no phantom spend")
	void echoThrowAsLightsFuseWithoutPhantomSpend() {
		Hero player = EchoTestSupport.warriorHero();
		Bomb bomb = new Bomb();
		bomb.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Bomb kitBomb = kit.belongings.getItem(Bomb.class);
		Assertions.assertThat(kitBomb).isNotNull();
		float kitBefore = kit.cooldown();

		boolean spent = kitBomb.throwAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
		Assertions.assertThat(kit.belongings.getItem(Bomb.class)).isNull();

		Bomb landed = findBombAt(player.pos);
		Assertions.assertThat(landed).isNotNull();
		Assertions.assertThat(landed.fuse)
				.as("Echo throwAs must light the fuse like LIGHTTHROW")
				.isNotNull();
	}

	@Test
	@DisplayName("Echo deferred bomb throw still lights fuse if lightingFuse was cleared mid-flight")
	void echoDeferredThrowLightsFuseDespiteClearedStatic() throws Exception {
		Hero player = EchoTestSupport.warriorHero();
		Bomb bomb = new Bomb();
		bomb.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.DeferredProjectileGroup fx = EchoTestSupport.attachDeferredProjectileParent(boss);

		Bomb kitBomb = boss.getEchoHero().belongings.getItem(Bomb.class);
		Assertions.assertThat(kitBomb.throwAs(UseContext.echo(boss), player.pos)).isTrue();
		Assertions.assertThat(fx.hasPending()).isTrue();

		// Simulate interleaving UI/action that clears the master static mid-VFX.
		setLightingFuse(false);

		fx.complete();

		Bomb landed = findBombAt(player.pos);
		Assertions.assertThat(landed).isNotNull();
		Assertions.assertThat(landed.fuse)
				.as("deferred Echo LIGHTTHROW must not depend on static lightingFuse lasting the VFX")
				.isNotNull();
	}

	@Test
	@DisplayName("Echo deferred stacked bomb still lights the split thrown copy")
	void echoDeferredStackedThrowLightsSplitCopy() throws Exception {
		Hero player = EchoTestSupport.warriorHero();
		Bomb bomb = new Bomb();
		bomb.quantity(3);
		bomb.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.DeferredProjectileGroup fx = EchoTestSupport.attachDeferredProjectileParent(boss);

		Bomb kitBomb = boss.getEchoHero().belongings.getItem(Bomb.class);
		Assertions.assertThat(kitBomb.quantity()).isGreaterThan(1);
		Assertions.assertThat(kitBomb.throwAs(UseContext.echo(boss), player.pos)).isTrue();
		Assertions.assertThat(fx.hasPending()).isTrue();

		setLightingFuse(false);

		fx.complete();

		Bomb landed = findBombAt(player.pos);
		Assertions.assertThat(landed).isNotNull();
		Assertions.assertThat(landed.quantity()).isEqualTo(1);
		Assertions.assertThat(landed.fuse)
				.as("split thrown bomb must ignite even if static lightingFuse was cleared")
				.isNotNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(Bomb.class).quantity())
				.isEqualTo(2);
	}

	private static void setLightingFuse(boolean value) throws Exception {
		java.lang.reflect.Field lightingFuse = Bomb.class.getDeclaredField("lightingFuse");
		lightingFuse.setAccessible(true);
		lightingFuse.setBoolean(null, value);
	}

	private static Bomb findBombAt(int cell) {
		Heap heap = Dungeon.level.heaps.get(cell);
		if (heap == null) {
			return null;
		}
		for (com.shatteredpixel.shatteredpixeldungeon.items.Item i : heap.items) {
			if (i instanceof Bomb) {
				return (Bomb) i;
			}
		}
		return null;
	}
}
