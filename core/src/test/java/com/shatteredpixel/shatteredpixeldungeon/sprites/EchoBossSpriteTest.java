package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.watabou.noosa.Group;
import com.watabou.noosa.MovieClip;
import com.watabou.utils.PointF;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;

@ExtendWith(GdxTestExtension.class)
class EchoBossSpriteTest {

	@AfterEach
	void cleanup() {
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

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

	@Test
	@DisplayName("EchoBossSprite defines fly after armor setup like HeroSprite")
	void definesFlyAfterArmorSetup() throws Exception {
		EchoBossSprite sprite = new EchoBossSprite();
		sprite.setup(HeroClass.DUELIST, 1);

		Field fly = EchoBossSprite.class.getDeclaredField("fly");
		fly.setAccessible(true);

		Assertions.assertThat(fly.get(sprite))
				.as("fly pose is required so Rapier/HeroicLeap jumps look like a lunge, not a teleport")
				.isNotNull();
	}

	@Test
	@DisplayName("EchoBossSprite defines read after armor setup like HeroSprite")
	void definesReadAfterArmorSetup() throws Exception {
		EchoBossSprite sprite = new EchoBossSprite();
		sprite.setup(HeroClass.MAGE, 1);

		Field read = EchoBossSprite.class.getDeclaredField("read");
		read.setAccessible(true);

		Assertions.assertThat(read.get(sprite))
				.as("read pose is required so scroll readAnimation matches Hero")
				.isNotNull();
	}

	@Test
	@DisplayName("EchoBossSprite jump plays the fly pose like HeroSprite")
	void jumpPlaysFlyPose() throws Exception {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		EchoBossSprite sprite = new EchoBossSprite() {
			@Override
			public PointF worldToCamera(int cell) {
				return new PointF(cell, 0);
			}
		};
		sprite.setup(HeroClass.DUELIST, 1);
		sprite.parent = new Group();

		Field fly = EchoBossSprite.class.getDeclaredField("fly");
		fly.setAccessible(true);
		Object flyAnim = fly.get(sprite);
		Assertions.assertThat(flyAnim).isNotNull();

		sprite.jump(boss.pos, player.pos, 0, 0.1f, null);

		Field curAnim = MovieClip.class.getDeclaredField("curAnim");
		curAnim.setAccessible(true);
		Assertions.assertThat(curAnim.get(sprite))
				.as("jump must switch to fly so Echo lunges match the Hero")
				.isSameAs(flyAnim);
	}
}
