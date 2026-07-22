package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Group;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.particles.Emitter;
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
	@DisplayName("EchoBoss.sprite() uses echo hero class for boss bar and info icons")
	void spriteFactoryUsesEchoHeroClass() {
		EchoBoss boss = huntressEchoBoss();

		CharSprite icon = boss.sprite();

		Assertions.assertThat(icon).isInstanceOf(EchoBossSprite.class);
		Assertions.assertThat(icon.texture)
				.as("BossHealthBar / WndInfoMob use Mob.sprite() without link()")
				.isSameAs(TextureCache.get(HeroClass.HUNTRESS.spritesheet()));
	}

	@Test
	@DisplayName("EchoBossSprite.linkVisuals uses echo hero class for attack target icon")
	void linkVisualsUsesEchoHeroClass() {
		EchoBoss boss = huntressEchoBoss();
		EchoBossSprite sprite = new EchoBossSprite();

		sprite.linkVisuals(boss);

		Assertions.assertThat(sprite.texture)
				.as("AttackIndicator builds via Reflection + linkVisuals, not Mob.sprite()")
				.isSameAs(TextureCache.get(HeroClass.HUNTRESS.spritesheet()));
	}

	private static EchoBoss huntressEchoBoss() {
		Hero hero = new Hero();
		com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero = hero;
		HeroClass.HUNTRESS.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		Echo echo = Echo.create(
				5, EchoTestSupport.TEST_GAME_VERSION, 1L,
				"HUNTRESS", 6, 30, 30, EchoTestSupport.bundleHero(hero));
		return EchoTestSupport.createBoss(echo, 5);
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
	@DisplayName("invisible EchoBossSprite is fully hidden from the hero")
	void invisibleFullyHiddenFromHero() {
		EchoBossSprite sprite = new EchoBossSprite();
		sprite.visible = true;
		sprite.alpha(1f);

		sprite.add(CharSprite.State.INVISIBLE);
		sprite.update();

		Assertions.assertThat(sprite.alpha())
				.as("Echo invisibility must not leave a translucent silhouette")
				.isZero();
		Assertions.assertThat(sprite.visible)
				.as("Echo must not be rendered while invisible")
				.isFalse();
	}

	@Test
	@DisplayName("EchoBossSprite stays fully hidden after resetColor while invisible")
	void staysHiddenAfterResetColorWhileInvisible() {
		EchoBossSprite sprite = new EchoBossSprite();
		sprite.add(CharSprite.State.INVISIBLE);
		sprite.update();

		sprite.resetColor();

		Assertions.assertThat(sprite.alpha()).isZero();
	}

	@Test
	@DisplayName("invisible EchoBossSprite hides attached status effects despite FOV")
	void invisibleHidesAttachedEffectsDespiteFov() {
		EchoBossSprite sprite = new EchoBossSprite();
		sprite.burning = new Emitter();
		sprite.burning.visible = true;
		sprite.healing = new Emitter();
		sprite.healing.visible = true;
		sprite.add(CharSprite.State.INVISIBLE);
		sprite.update();

		// GameScene.afterObserve / Char.move flip this back on while still in FOV
		sprite.visible = true;
		sprite.burning.visible = true;
		sprite.healing.visible = true;
		sprite.update();

		Assertions.assertThat(sprite.visible).isFalse();
		Assertions.assertThat(sprite.burning.visible)
				.as("burning must not reveal an invisible Echo")
				.isFalse();
		Assertions.assertThat(sprite.healing.visible)
				.as("healing must not reveal an invisible Echo")
				.isFalse();
	}

	@Test
	@DisplayName("EchoBossSprite reappears in FOV when invisibility ends")
	void reappearsInFovWhenInvisibilityEnds() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.state = boss.HUNTING; // avoid sleep emo (needs GameScene)

		EchoBossSprite sprite = new EchoBossSprite() {
			@Override
			public PointF worldToCamera(int cell) {
				return new PointF(cell, 0);
			}
		};
		sprite.ch = boss;
		boss.sprite = sprite;
		sprite.visible = true;
		sprite.add(CharSprite.State.INVISIBLE);
		sprite.update();
		Assertions.assertThat(sprite.visible).isFalse();

		sprite.remove(CharSprite.State.INVISIBLE);
		sprite.update();

		Assertions.assertThat(sprite.alpha()).isEqualTo(1f);
		Assertions.assertThat(sprite.visible)
				.as("Echo in hero FOV must render again after invisibility")
				.isTrue();
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
