package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoThrowAdapter;
import com.shatteredpixel.shatteredpixeldungeon.items.AiItemActions;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class HeavyBoomerangCircleBackThrowerTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
		Item.clearCurrent();
	}

	@Test
	@DisplayName("player rangedHit attaches CircleBack to Dungeon.hero")
	void playerRangedHitAttachesCircleBackToHero() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		HeavyBoomerang boom = new HeavyBoomerang();
		AiItemActions.withUser(hero, boom, () -> boom.rangedHit(target, target.pos));

		HeavyBoomerang.CircleBack circling = hero.buff(HeavyBoomerang.CircleBack.class);
		Assertions.assertThat(circling).isNotNull();
		Assertions.assertThat(circling.returnPos()).isEqualTo(hero.pos);
		Assertions.assertThat(target.buff(HeavyBoomerang.CircleBack.class)).isNull();
	}

	@Test
	@DisplayName("Echo throw attaches CircleBack to kit with body returnPos")
	void echoThrowAttachesCircleBackToKit() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		Hero kit = boss.getEchoHero();
		HeavyBoomerang boom = new HeavyBoomerang();
		boom.identify();
		Assertions.assertThat(boom.collect(kit.belongings.backpack)).isTrue();
		player.invisible = 1;

		Assertions.assertThat(EchoThrowAdapter.throwItem(boss, boom, player.pos)).isTrue();

		Assertions.assertThat(kit.buff(HeavyBoomerang.CircleBack.class))
				.as("CircleBack must host on Echo thrower (kit), not Dungeon.hero")
				.isNotNull();
		Assertions.assertThat(player.buff(HeavyBoomerang.CircleBack.class)).isNull();
		Assertions.assertThat(kit.buff(HeavyBoomerang.CircleBack.class).returnPos())
				.isEqualTo(boss.pos);
	}
}
