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
	void cleanup() {
		TargetHealthIndicator.instance = null;
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Hero bomb throwAs lights fuse, detaches, and spends the hero turn")
	void heroThrowAsLightsFuseAndSpendsTurn() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		Bomb bomb = new Bomb();
		bomb.collect(hero.belongings.backpack);
		float before = hero.cooldown();

		boolean spent = bomb.throwAs(UseContext.hero(hero), target.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
		Assertions.assertThat(hero.belongings.getItem(Bomb.class)).isNull();
		Heap heap = Dungeon.level.heaps.get(target.pos);
		Assertions.assertThat(heap).isNotNull();
		Bomb landed = null;
		for (com.shatteredpixel.shatteredpixeldungeon.items.Item i : heap.items) {
			if (i instanceof Bomb) {
				landed = (Bomb) i;
				break;
			}
		}
		Assertions.assertThat(landed).isNotNull();
		Assertions.assertThat(landed.fuse).isNotNull();
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

		Heap heap = Dungeon.level.heaps.get(player.pos);
		Assertions.assertThat(heap).isNotNull();
		Bomb landed = null;
		for (com.shatteredpixel.shatteredpixeldungeon.items.Item i : heap.items) {
			if (i instanceof Bomb) {
				landed = (Bomb) i;
				break;
			}
		}
		Assertions.assertThat(landed).isNotNull();
		Assertions.assertThat(landed.fuse)
				.as("Echo throwAs must light the fuse like LIGHTTHROW")
				.isNotNull();
	}
}
