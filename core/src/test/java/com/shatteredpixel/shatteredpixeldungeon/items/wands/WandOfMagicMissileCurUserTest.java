package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * ANDROID-1K residual: MagicMissile charge buff must tolerate null curUser.
 */
@ExtendWith(GdxTestExtension.class)
class WandOfMagicMissileCurUserTest {

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
	@DisplayName("MagicMissile onZap damages when curUser is null")
	void onZapDamagesWhenCurUserNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		int hpBefore = player.HP;

		WandOfMagicMissile wand = new WandOfMagicMissile();
		Item.clearCurrent();
		Ballistica shot = new Ballistica(boss.pos, player.pos, Ballistica.MAGIC_BOLT);

		Assertions.assertThatCode(() -> wand.onZap(shot)).doesNotThrowAnyException();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}
}
