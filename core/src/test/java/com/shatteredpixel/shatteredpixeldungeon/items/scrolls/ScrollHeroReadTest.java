package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.AiItemActions;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Phase 1 characterization: Hero scroll read via master {@link Scroll#doRead()}
 * (same path as {@link Scroll#execute} AC_READ after guards).
 */
@ExtendWith(GdxTestExtension.class)
class ScrollHeroReadTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
		Scroll.initLabels();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("Hero read consumes the scroll and runs doRead")
	void heroReadConsumesScrollAndRunsDoRead() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		TestRechargingScroll scroll = new TestRechargingScroll();
		scroll.identify();
		scroll.collect(hero.belongings.backpack);

		AiItemActions.withUser(hero, scroll, scroll::doRead);

		Assertions.assertThat(scroll.readAnimated).isTrue();
		Assertions.assertThat(hero.buff(Recharging.class)).isNotNull();
		Assertions.assertThat(hero.belongings.getItem(ScrollOfRecharging.class)).isNull();
	}

	@Test
	@DisplayName("Hero read preserves upstream animation and busy timing")
	void heroReadPreservesUpstreamAnimationAndBusyTiming() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		TestRechargingScroll scroll = new TestRechargingScroll();
		scroll.identify();
		scroll.collect(hero.belongings.backpack);
		float before = hero.cooldown();

		AiItemActions.withUser(hero, scroll, scroll::doRead);

		Assertions.assertThat(scroll.readAnimated).isTrue();
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
	}

	/** Avoids HeroSprite texture setup; still exercises master {@link #doRead()}. */
	private static final class TestRechargingScroll extends ScrollOfRecharging {
		boolean readAnimated;

		@Override
		public void readAnimation() {
			readAnimated = true;
			curUser.spend(TIME_TO_READ);
			curUser.busy();
		}
	}
}
