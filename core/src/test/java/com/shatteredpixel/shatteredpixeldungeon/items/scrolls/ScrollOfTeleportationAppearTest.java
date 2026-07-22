package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.watabou.utils.PathFinder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class ScrollOfTeleportationAppearTest {

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
	@DisplayName("appear moves EchoBoss body without requiring GameScene emitters")
	void appearMovesEchoBossWithoutEmitterScene() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		int dest = -1;
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = boss.pos + n;
			if (c >= 0 && c < Dungeon.level.length()
					&& Dungeon.level.passable[c]
					&& Actor.findChar(c) == null) {
				dest = c;
				break;
			}
		}
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		int start = boss.pos;

		ScrollOfTeleportation.appear(boss, dest);

		Assertions.assertThat(boss.pos).isEqualTo(dest);
		Assertions.assertThat(boss.pos).isNotEqualTo(start);
	}
}
