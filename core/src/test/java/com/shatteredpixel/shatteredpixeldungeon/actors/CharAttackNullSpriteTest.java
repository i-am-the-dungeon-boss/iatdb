package com.shatteredpixel.shatteredpixeldungeon.actors;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class CharAttackNullSpriteTest {

	@Test
	@DisplayName("attack with null attacker sprite does not NPE when enemy has a sprite")
	void attackWithNullAttackerSpriteDoesNotNpe() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Assertions.assertThat(kit.sprite).isNull();
		Assertions.assertThat(player.sprite).isNotNull();

		kit.pos = boss.pos;
		player.invisible = 1;

		Assertions.assertThatCode(() -> kit.attack(player, 1f, 0f, Char.INFINITE_ACCURACY))
				.doesNotThrowAnyException();
	}
}
