package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.PointF;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;

@ExtendWith(GdxTestExtension.class)
class EchoBossSpriteTest {

	private Group stage;

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
		stage = null;
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

	/**
	 * Linked sprite whose char is stealthed — hide logic keys off
	 * {@code ch.invisible}.
	 */
	private EchoBossSprite cloakedEchoSprite() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.state = boss.HUNTING;
		boss.invisible = 1;
		return linkedStageSprite(boss);
	}

	private EchoBossSprite linkedStageSprite(EchoBoss boss) {
		EchoBossSprite sprite = new EchoBossSprite() {
			@Override
			public PointF worldToCamera(int cell) {
				return new PointF(cell, 0);
			}
		};
		stage = new Group();
		stage.add(sprite);
		sprite.ch = boss;
		boss.sprite = sprite;
		sprite.visible = true;
		sprite.alpha(1f);
		return sprite;
	}

	private void tickSprite(EchoBossSprite sprite) {
		Game.elapsed = 1f;
		sprite.update();
		if (stage != null) {
			stage.update();
		}
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
		EchoBossSprite sprite = cloakedEchoSprite();

		sprite.add(CharSprite.State.INVISIBLE);
		tickSprite(sprite);

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
		EchoBossSprite sprite = cloakedEchoSprite();
		sprite.add(CharSprite.State.INVISIBLE);
		tickSprite(sprite);

		sprite.resetColor();

		Assertions.assertThat(sprite.alpha()).isZero();
	}

	@Test
	@DisplayName("invisible EchoBossSprite hides attached status effects despite FOV")
	void invisibleHidesAttachedEffectsDespiteFov() {
		EchoBossSprite sprite = cloakedEchoSprite();
		sprite.burning = new Emitter();
		sprite.burning.visible = true;
		sprite.healing = new Emitter();
		sprite.healing.visible = true;
		sprite.add(CharSprite.State.INVISIBLE);
		tickSprite(sprite);

		// GameScene.afterObserve / Char.move flip this back on while still in FOV
		sprite.visible = true;
		sprite.burning.visible = true;
		sprite.healing.visible = true;
		tickSprite(sprite);

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
		boss.invisible = 1;
		sprite.add(CharSprite.State.INVISIBLE);
		sprite.update();
		Assertions.assertThat(sprite.visible).isFalse();

		boss.invisible = 0;
		sprite.remove(CharSprite.State.INVISIBLE);
		sprite.update();

		Assertions.assertThat(sprite.alpha()).isEqualTo(1f);
		Assertions.assertThat(sprite.visible)
				.as("Echo in hero FOV must render again after invisibility")
				.isTrue();
	}

	@Test
	@DisplayName("PotionOfInvisibility fully hides EchoBossSprite like cloak stealth")
	void potionOfInvisibilityFullyHidesEchoBossSprite() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.state = boss.HUNTING;

		EchoBossSprite sprite = linkedStageSprite(boss);
		PotionOfInvisibility pot = new PotionOfInvisibility();
		pot.identify();
		pot.collect(boss.getEchoHero().belongings.backpack);

		Assertions.assertThat(pot.drinkAs(UseContext.echo(boss))).isTrue();
		tickSprite(sprite);

		Assertions.assertThat(boss.buff(Invisibility.class)).isNotNull();
		Assertions.assertThat(boss.invisible).isGreaterThan(0);
		Assertions.assertThat(sprite.visible)
				.as("potion invis must fully hide Echo like cloak")
				.isFalse();
		Assertions.assertThat(sprite.alpha()).isZero();
	}

	@Test
	@DisplayName("wand hit after PotionOfInvisibility re-renders EchoBossSprite")
	void wandHitAfterPotionInvisibilityRerendersEchoBossSprite() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.state = boss.HUNTING;

		EchoBossSprite sprite = linkedStageSprite(boss);
		Buff.affect(boss, Invisibility.class, Invisibility.DURATION);
		tickSprite(sprite);
		Assertions.assertThat(sprite.visible).isFalse();

		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.identify();
		wand.curCharges = wand.maxCharges;
		Assertions.assertThat(wand.collect(player.belongings.backpack)).isTrue();
		Assertions.assertThat(wand.zapAs(UseContext.hero(player), boss.pos)).isTrue();

		tickSprite(sprite);

		Assertions.assertThat(boss.buff(Invisibility.class)).isNull();
		Assertions.assertThat(boss.invisible).isEqualTo(0);
		Assertions.assertThat(sprite.alpha()).isEqualTo(1f);
		Assertions.assertThat(sprite.visible)
				.as("potion invis dispel must re-render Echo like cloak")
				.isTrue();
	}

	@Test
	@DisplayName("EchoBossSprite reappears when stealth ends even if INVISIBLE fx was skipped")
	void reappearsWhenStealthEndsEvenIfSpriteStateRemovalSkipped() throws Exception {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.state = boss.HUNTING;

		EchoBossSprite sprite = new EchoBossSprite() {
			@Override
			public PointF worldToCamera(int cell) {
				return new PointF(cell, 0);
			}
		};
		sprite.ch = boss;
		boss.sprite = sprite;
		boss.invisible = 1;
		sprite.add(CharSprite.State.INVISIBLE);
		sprite.update();
		Assertions.assertThat(sprite.visible).isFalse();

		// Finish the fade tweener the way the game loop does (kill, leave field).
		Field invisField = CharSprite.class.getDeclaredField("invisible");
		invisField.setAccessible(true);
		AlphaTweener tweener = (AlphaTweener) invisField.get(sprite);
		Assertions.assertThat(tweener).isNotNull();
		tweener.kill();

		// Buff/counter cleared (dispel) but sprite.remove(INVISIBLE) never ran.
		boss.invisible = 0;
		sprite.visible = true; // afterObserve would restore FOV visibility
		sprite.update();

		Assertions.assertThat(sprite.alpha())
				.as("dispel must restore alpha even when INVISIBLE state fx was skipped")
				.isEqualTo(1f);
		Assertions.assertThat(sprite.visible)
				.as("dispel must re-render EchoBoss after full hide")
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
