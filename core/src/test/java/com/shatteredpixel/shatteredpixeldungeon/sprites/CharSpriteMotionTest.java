package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class CharSpriteMotionTest {

	@Test
	@DisplayName("an in-flight slide cannot leave the sprite off the character tile")
	void inFlightSlideCannotParkOffTile() {
		Camera.reset(new Camera(0, 0, 240, 400, 1f));
		Hero hero = new Hero();
		Rat rat = new Rat();
		EchoTestSupport.installEchoBossLevel(hero, rat, 0);

		rat.state = rat.HUNTING;
		int near = rat.pos;
		int far = near + 3;
		EchoBossSprite sprite = new EchoBossSprite();
		sprite.ch = rat;
		rat.sprite = sprite;
		Group stage = new Group();
		stage.add(sprite);

		sprite.place(near);
		sprite.move(near, far);
		sprite.place(near);

		Game.elapsed = 1f;
		stage.update();

		PointF onTile = sprite.worldToCamera(rat.pos);
		Assertions.assertThat(sprite.point().x).isEqualTo(onTile.x);
		Assertions.assertThat(sprite.point().y).isEqualTo(onTile.y);
		Assertions.assertThat(rat.pos).isEqualTo(near);
	}

	@Test
	@DisplayName("a finished slide lands on the character tile")
	void finishedSlideLandsOnCharacterTile() {
		Camera.reset(new Camera(0, 0, 240, 400, 1f));
		Hero hero = new Hero();
		Rat rat = new Rat();
		EchoTestSupport.installEchoBossLevel(hero, rat, 0);

		rat.state = rat.HUNTING;
		int near = rat.pos;
		int far = near + 3;
		EchoBossSprite sprite = new EchoBossSprite();
		sprite.ch = rat;
		rat.sprite = sprite;
		Group stage = new Group();
		stage.add(sprite);

		sprite.place(near);
		sprite.move(near, far);

		Game.elapsed = 1f;
		stage.update();

		PointF onTile = sprite.worldToCamera(rat.pos);
		Assertions.assertThat(sprite.point().x).isEqualTo(onTile.x);
		Assertions.assertThat(sprite.point().y).isEqualTo(onTile.y);
		Assertions.assertThat(rat.pos).isEqualTo(near);
	}
}
