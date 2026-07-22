package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class AscensionChallengeBlockTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("ascension challenge is disabled for this game")
	void ascensionChallengeIsDisabled() {
		Assertions.assertThat(AscensionChallenge.enabled()).isFalse();
	}

	@Test
	@DisplayName("Buff.affect does not attach AscensionChallenge when disabled")
	void affectDoesNotAttachWhenDisabled() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);

		Buff.affect(hero, AscensionChallenge.class);

		Assertions.assertThat(hero.buff(AscensionChallenge.class)).isNull();
	}

	@Test
	@DisplayName("ascent offer is blocked even when hero holds the amulet")
	void ascentOfferBlockedWithAmulet() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		Assertions.assertThat(new Amulet().collect(hero.belongings.backpack)).isTrue();

		Assertions.assertThat(
				AscensionChallenge.shouldOfferAscent(hero, LevelTransition.Type.REGULAR_ENTRANCE))
				.isFalse();
	}
}
