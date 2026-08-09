package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * ANDROID-S: BlastWave push must tolerate a null {@code ch.sprite}
 * (Pushing skips VFX; cancel-path {@code place} is also null-gated).
 */
@ExtendWith(GdxTestExtension.class)
class WandOfBlastWaveNullSpriteTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("throwChar does not NPE when pushed char sprite is null")
	void throwCharDoesNotNpeWhenSpriteNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Ballistica trajectory = new Ballistica(boss.pos, player.pos, Ballistica.STOP_TARGET);
		Assertions.assertThat(trajectory.dist).isGreaterThan(0);

		// Push the boss (not Dungeon.hero) so Pushing skips Camera.main.panFollow.
		boss.sprite = null;

		Assertions.assertThatCode(() -> WandOfBlastWave.throwChar(
				boss, trajectory, 2, false, false, new WandOfBlastWave()))
				.doesNotThrowAnyException();
	}
}
