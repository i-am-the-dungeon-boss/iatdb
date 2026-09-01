package com.shatteredpixel.shatteredpixeldungeon.heroechoes.inspect;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoHeroSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greataxe;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Foreign hero data is view-only: restoring somebody else's echo must not move
 * the local player's badges, catalog or statistics.
 */
@ExtendWith(GdxTestExtension.class)
class ForeignRestoreIsolationTest {

	@BeforeEach
	void reset() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Restoring a foreign echo awards the local player no badges")
	void foreignRestoreAwardsNoBadges() {
		Echo foreign = echoCarrying(upgradedAxe());

		EchoTestSupport.warriorHero();
		Badges.reset();

		Hero restored = EchoHeroSnapshot.restoreHero(foreign);

		Assertions.assertThat(restored).isNotNull();
		Assertions.assertThat(Badges.totalUnlocked(false)).isZero();
	}

	@Test
	@DisplayName("Restoring a foreign echo discovers no item types for the local player")
	void foreignRestoreDiscoversNoItemTypes() {
		Echo foreign = echoCarrying(upgradedAxe());

		EchoTestSupport.warriorHero();
		Statistics.itemTypesDiscovered.clear();

		EchoHeroSnapshot.restoreHero(foreign);

		Assertions.assertThat(Statistics.itemTypesDiscovered).isEmpty();
	}

	private static Item upgradedAxe() {
		Greataxe axe = new Greataxe();
		axe.upgrade(6);
		axe.levelKnown = true;
		axe.cursedKnown = true;
		return axe;
	}

	/** An echo whose owner is carrying {@code carried} in their backpack. */
	private static Echo echoCarrying(Item carried) {
		Hero owner = EchoTestSupport.warriorHero();
		carried.collect(owner.belongings.backpack);
		Echo echo = Echo.create(
				5,
				EchoTestSupport.TEST_GAME_VERSION,
				1L,
				"WARRIOR",
				owner.lvl,
				owner.HP,
				owner.HT,
				EchoTestSupport.bundleHero(owner));
		Dungeon.hero = null;
		return echo;
	}
}
