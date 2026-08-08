package com.shatteredpixel.shatteredpixeldungeon.actors;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoActionContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class CharCanWorldFxTest {

	@Test
	@DisplayName("canWorldFx is false when sprite has no parent")
	void canWorldFxFalseWithoutParent() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoTestSupport.linkStubSprite(hero);
		Assertions.assertThat(hero.sprite.parent).isNull();
		Assertions.assertThat(Char.canWorldFx(hero)).isFalse();
		Assertions.assertThat(EchoActionContext.canWorldFx(hero)).isFalse();
	}

	@Test
	@DisplayName("canWorldFx is true when sprite has a scene parent")
	void canWorldFxTrueWithParent() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoTestSupport.linkStubSprite(hero);
		EchoTestSupport.attachInstantProjectileParent(hero);
		Assertions.assertThat(Char.canWorldFx(hero)).isTrue();
		Assertions.assertThat(EchoActionContext.canWorldFx(hero)).isTrue();
	}

	@Test
	@DisplayName("canWorldFx is false for null char")
	void canWorldFxFalseForNull() {
		Assertions.assertThat(Char.canWorldFx(null)).isFalse();
	}
}
