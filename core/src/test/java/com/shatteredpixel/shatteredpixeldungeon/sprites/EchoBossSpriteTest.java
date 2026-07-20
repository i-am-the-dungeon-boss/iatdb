package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;

@ExtendWith(GdxTestExtension.class)
class EchoBossSpriteTest {

	@Test
	@DisplayName("EchoBossSprite defines zap after armor setup for ranged shots")
	void definesZapAfterArmorSetup() throws Exception {
		EchoBossSprite sprite = new EchoBossSprite();
		sprite.setup(HeroClass.HUNTRESS, 1);

		Field zap = CharSprite.class.getDeclaredField("zap");
		zap.setAccessible(true);

		Assertions.assertThat(zap.get(sprite))
				.as("zap must match HeroSprite so SpiritBow can play a shoot pose")
				.isNotNull();
	}
}
