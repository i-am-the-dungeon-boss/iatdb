package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class TitleHeroLogoFlareLayerTest {

	@Test
	@DisplayName("credits flare attaches behind the hero visual")
	void creditsFlareAttachesBehindHero() {
		Group group = new Group();
		Image hero = new Image();
		group.add(hero);

		Flare flare = new Flare(7, 24);
		TitleHeroLogo.attachCreditsFlareBehind(flare, hero);

		Assertions.assertThat(flare.parent).isSameAs(group);
		Assertions.assertThat(group.indexOf(flare)).isLessThan(group.indexOf(hero));
	}
}
