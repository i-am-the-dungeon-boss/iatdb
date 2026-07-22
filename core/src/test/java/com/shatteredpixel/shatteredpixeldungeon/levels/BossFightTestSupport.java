package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.watabou.noosa.Camera;

import java.util.Arrays;
import java.util.function.Supplier;

/** Shared fixtures for regional / echo boss fight integration tests. */
final class BossFightTestSupport {

	private BossFightTestSupport() {
	}

	static void setUpUiStubs() {
		new TargetHealthIndicator();
		Camera.reset();
		BossHealthBar.assignBoss(null);
	}

	static void tearDownUiStubs() {
		EchoTestSupport.resetWorkflowState();
		TargetHealthIndicator.instance = null;
		BossHealthBar.assignBoss(null);
		Camera.reset();
	}

	static void prepareLevel(Level level, Hero hero) {
		Dungeon.level = level;
		Dungeon.hero = hero;
		hero.pos = level.entrance();
		level.heroFOV = new boolean[level.length()];
		Arrays.fill(level.heroFOV, true);
		EchoTestSupport.linkStubSprite(hero);
	}

	static <T extends Level> T createWithoutEcho(int depth, Supplier<T> factory) {
		Hero hero = EchoTestSupport.warriorHero();
		CompositeEchoLookup.setEchoLookupForTests(d -> EchoLookupOutcome.notFound());
		Dungeon.depth = depth;
		Dungeon.seed = 1L;
		Dungeon.prefetchEchoBossForDepth(depth);
		T level = factory.get();
		level.create();
		prepareLevel(level, hero);
		return level;
	}

	static <T extends Level> T createWithPendingEcho(int depth, Supplier<T> factory) {
		// warriorEchoWithData installs Dungeon.hero — capture that instance for
		// fixtures.
		Echo echo = EchoTestSupport.warriorEchoWithData(depth);
		Hero hero = Dungeon.hero;
		CompositeEchoLookup.setEchoLookupForTests(d -> EchoTestSupport.outcomeWithPolicy(echo));
		Dungeon.depth = depth;
		Dungeon.seed = 1L;
		Dungeon.prefetchEchoBossForDepth(depth);
		T level = factory.get();
		level.create();
		prepareLevel(level, hero);
		return level;
	}

	static <T extends Mob> T findMob(Level level, Class<T> type) {
		for (Mob mob : level.mobs) {
			if (type.isInstance(mob)) {
				return type.cast(mob);
			}
		}
		return null;
	}
}
