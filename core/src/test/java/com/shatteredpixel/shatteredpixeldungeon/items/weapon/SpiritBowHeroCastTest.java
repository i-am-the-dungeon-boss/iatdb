package com.shatteredpixel.shatteredpixeldungeon.items.weapon;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.RectF;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Step 0 characterization: today's Hero {@code knockArrow().cast(hero, dst)}
 * path
 * before UseContext / throwAs migration.
 */
@ExtendWith(GdxTestExtension.class)
class SpiritBowHeroCastTest {

	@BeforeEach
	void installUiStubs() {
		// Item.cast → QuickSlotButton.target needs a live indicator instance.
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Hero SpiritBow cast spends the hero turn")
	void castSpendsHeroTurn() {
		Hero hero = huntressHero();
		EchoBoss target = installFight(hero);
		SpiritBow bow = hero.belongings.getItem(SpiritBow.class);
		Assertions.assertThat(bow).isNotNull();

		float cooldownBefore = hero.cooldown();
		bow.knockArrow().cast(hero, target.pos);

		Assertions.assertThat(hero.cooldown()).isGreaterThan(cooldownBefore);
	}

	@Test
	@DisplayName("Hero SpiritBow cast queues MissileSprite with SpiritArrow image when on stage")
	void castQueuesMissileSpriteWithArrowImage() {
		Hero hero = huntressHero();
		EchoBoss target = installFight(hero);
		SpiritBow bow = hero.belongings.getItem(SpiritBow.class);
		Assertions.assertThat(bow).isNotNull();

		int expectedImage = bow.knockArrow().image();
		MissileCaptureGroup stage = new MissileCaptureGroup();
		stage.add(hero.sprite);
		stage.add(target.sprite);

		bow.knockArrow().cast(hero, target.pos);

		Assertions.assertThat(stage.lastMissile).isNotNull();
		ItemSprite expected = new ItemSprite();
		expected.view(expectedImage, null);
		RectF missileFrame = stage.lastMissile.frame();
		RectF expectedFrame = expected.frame();
		Assertions.assertThat(missileFrame.left).isEqualTo(expectedFrame.left);
		Assertions.assertThat(missileFrame.top).isEqualTo(expectedFrame.top);
		Assertions.assertThat(missileFrame.right).isEqualTo(expectedFrame.right);
		Assertions.assertThat(missileFrame.bottom).isEqualTo(expectedFrame.bottom);
	}

	@Test
	@DisplayName("Hero SpiritBow cast can hit when accuracy is forced")
	void castHitsWhenAccuracyForced() {
		Hero hero = huntressHero();
		EchoBoss target = installFight(hero);
		SpiritBow bow = hero.belongings.getItem(SpiritBow.class);
		Assertions.assertThat(bow).isNotNull();
		Assertions.assertThat(hero.sprite.ch).isSameAs(hero);
		Assertions.assertThat(target.sprite.ch).isSameAs(target);

		int hpBefore = target.HP;
		hero.invisible = 1; // guarantee hit (surprise-accuracy path)

		bow.knockArrow().cast(hero, target.pos);

		Assertions.assertThat(target.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Hero SpiritBow cast does not consume the bow")
	void castDoesNotConsumeBow() {
		Hero hero = huntressHero();
		EchoBoss target = installFight(hero);
		SpiritBow bow = hero.belongings.getItem(SpiritBow.class);
		Assertions.assertThat(bow).isNotNull();

		bow.knockArrow().cast(hero, target.pos);

		Assertions.assertThat(hero.belongings.getItem(SpiritBow.class)).isSameAs(bow);
	}

	private static Hero huntressHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.HUNTRESS.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static EchoBoss installFight(Hero hero) {
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);
		return target;
	}

	private static final class MissileCaptureGroup extends Group {
		MissileSprite lastMissile;

		@Override
		public synchronized Gizmo add(Gizmo g) {
			if (g instanceof MissileSprite) {
				lastMissile = (MissileSprite) g;
			}
			return super.add(g);
		}
	}
}
