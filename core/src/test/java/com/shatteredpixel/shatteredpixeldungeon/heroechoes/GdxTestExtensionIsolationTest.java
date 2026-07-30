package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies {@link GdxTestExtension} clears shared statics between tests so
 * leftovers from one method cannot leak into the next.
 */
@ExtendWith(GdxTestExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GdxTestExtensionIsolationTest {

	@Test
	@Order(1)
	@DisplayName("pollutes shared Dungeon/Actor state for the next test")
	void pollutesSharedState() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Assertions.assertThat(Dungeon.level).isNotNull();
		Assertions.assertThat(Dungeon.hero).isSameAs(hero);
		Assertions.assertThat(Actor.all()).isNotEmpty();
	}

	@Test
	@Order(2)
	@DisplayName("starts with clean Dungeon/Actor state after previous test")
	void startsCleanAfterPreviousTest() {
		Assertions.assertThat(Dungeon.level).isNull();
		Assertions.assertThat(Dungeon.hero).isNull();
		Assertions.assertThat(Actor.all()).isEmpty();
	}
}
