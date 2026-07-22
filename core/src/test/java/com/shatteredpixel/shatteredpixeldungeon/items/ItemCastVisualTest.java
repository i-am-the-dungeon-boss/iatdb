package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.Callback;
import com.watabou.utils.RectF;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class ItemCastVisualTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("castVisual queues MissileSprite using this item's image")
	void castVisualUsesItemImage() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		PotionOfParalyticGas potion = new PotionOfParalyticGas();
		potion.identify();
		int expectedImage = potion.image();

		MissileCaptureGroup stage = new MissileCaptureGroup();
		stage.add(boss.sprite);
		stage.add(hero.sprite);

		boolean[] arrived = { false };
		int cell = potion.castVisual(boss.sprite, boss.pos, hero.pos, () -> arrived[0] = true);

		Assertions.assertThat(cell).isEqualTo(hero.pos);
		Assertions.assertThat(stage.lastMissile).isNotNull();
		Assertions.assertThat(arrived[0]).isFalse();

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
	@DisplayName("castVisual invokes callback immediately when sprite has no parent")
	void castVisualImmediateWhenOffStage() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		PotionOfParalyticGas potion = new PotionOfParalyticGas();
		boolean[] arrived = { false };
		potion.castVisual(boss.sprite, boss.pos, hero.pos, () -> arrived[0] = true);

		Assertions.assertThat(arrived[0]).isTrue();
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
