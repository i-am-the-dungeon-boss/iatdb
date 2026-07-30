package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

@ExtendWith(GdxTestExtension.class)
class HeroCheckVisibleMobsTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
		QuickSlotButton.reset();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
		QuickSlotButton.reset();
	}

	@Test
	@DisplayName("checkVisibleMobs does not auto-target an invisible EchoBoss")
	void doesNotAutoTargetInvisibleEcho() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		installVisibleFight(hero, boss);
		boss.invisible = 1;
		QuickSlotButton.lastTarget = null;

		hero.checkVisibleMobs();

		Assertions.assertThat(QuickSlotButton.lastTarget)
				.as("invisible Echo must not become the quickslot aim target")
				.isNotSameAs(boss);
		Assertions.assertThat(hero.getVisibleEnemies()).doesNotContain(boss);
	}

	@Test
	@DisplayName("checkVisibleMobs clears quickslot aim when EchoBoss turns invisible")
	void clearsAutoTargetWhenEchoTurnsInvisible() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		installVisibleFight(hero, boss);
		QuickSlotButton.target(boss);
		Assertions.assertThat(QuickSlotButton.lastTarget).isSameAs(boss);

		boss.invisible = 1;
		hero.checkVisibleMobs();

		Assertions.assertThat(QuickSlotButton.lastTarget)
				.as("quickslot aim must drop when the Echo goes invisible")
				.isNotSameAs(boss);
	}

	private static void installVisibleFight(Hero hero, EchoBoss boss) {
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		hero.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(hero.fieldOfView, true);
	}
}
