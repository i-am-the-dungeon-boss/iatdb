package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.SparseArray;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

@ExtendWith(GdxTestExtension.class)
class EchoBossBehaviorTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("EchoBoss does not use custom AI while sleeping")
	void doesNotUseCustomAiWhileSleeping() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		potion.collect(hero.belongings.backpack);
		Echo echo = Echo.create(
				5, EchoTestSupport.TEST_GAME_VERSION, 1L,
				"WARRIOR", 6, 8, 30, EchoTestSupport.bundleHero(hero));

		EchoBoss boss = new EchoBoss(echo, 5);
		boss.state = boss.SLEEPING;
		boss.HP = 8;
		installMinimalActLevel(hero, boss);

		boss.act();

		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfHealing.class)).isNotNull();
		Assertions.assertThat(boss.healingPotionsUsed()).isZero();
	}

	/**
	 * Bare level + stub sprite so {@link Mob#act()} can run without a full game
	 * scene.
	 */
	private static void installMinimalActLevel(Hero hero, Mob boss) {
		Level level = new Level() {
			@Override
			public String tilesTex() {
				return null;
			}

			@Override
			public String waterTex() {
				return null;
			}

			@Override
			protected boolean build() {
				return true;
			}

			@Override
			protected void createMobs() {
			}

			@Override
			protected void createItems() {
			}
		};
		level.setSize(7, 7);
		Arrays.fill(level.map, Terrain.EMPTY);
		level.mobs = new HashSet<>();
		level.heaps = new SparseArray<>();
		level.blobs = new HashMap<>();
		level.plants = new SparseArray<>();
		level.traps = new SparseArray<>();
		level.buildFlagMaps();

		int center = 3 * level.width() + 3;
		hero.pos = center;
		boss.pos = center + 1;
		boss.sprite = new StubSprite();

		Dungeon.level = level;
	}

	private static final class StubSprite extends CharSprite {
		@Override
		public void showAlert() {
		}

		@Override
		public void hideAlert() {
		}

		@Override
		public void hideLost() {
		}

		@Override
		public void hideInvestigate() {
		}
	}
}
