package com.shatteredpixel.shatteredpixeldungeon.heroechoes.inspect;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoHeroSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greataxe;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;

/**
 * An echo is a dead hero laid out for inspection: what it carries reads as
 * known. The knowing happens on a throwaway copy, so the echo's own kit — which
 * may be the one an {@code EchoBoss} is fighting with — is left as it was, and
 * reading it teaches the living player nothing.
 */
@ExtendWith(GdxTestExtension.class)
class EchoItemIdentificationTest {

	@BeforeEach
	void reset() {
		EchoTestSupport.resetWorkflowState();
		Notes.reset();
		Scroll.initLabels();
		Potion.initColors();
		Ring.initGems();
	}

	@Test
	@DisplayName("A previewed echo's equipment reads as identified")
	void previewedKitIsIdentified() {
		Hero restored = EchoHeroSnapshot.restoreHero(echoWearingAnUnknownRing());

		Assertions.assertThat(ItemPreview.of(restored.belongings.weapon(), restored).isIdentified())
				.isTrue();
		Assertions.assertThat(ItemPreview.of(restored.belongings.ring(), restored).isIdentified())
				.isTrue();
	}

	@Test
	@DisplayName("A previewed echo's ring shows its true name")
	void previewedRingShowsItsTrueName() {
		Hero restored = EchoHeroSnapshot.restoreHero(echoWearingAnUnknownRing());

		Assertions.assertThat(ItemPreview.of(restored.belongings.ring(), restored).name())
				.contains(Messages.get(RingOfHaste.class, "name"));
	}

	@Test
	@DisplayName("Previewing leaves the echo's own fighting kit exactly as it was")
	void previewingLeavesTheLiveKitAlone() {
		Hero restored = EchoHeroSnapshot.restoreHero(echoWearingAnUnknownRing());
		Item liveWeapon = restored.belongings.weapon();
		Item liveRing = restored.belongings.ring();

		ItemPreview.of(liveWeapon, restored);
		ItemPreview.of(liveRing, restored);

		Assertions.assertThat(liveWeapon.isIdentified()).isFalse();
		Assertions.assertThat(liveRing.name())
				.doesNotContain(Messages.get(RingOfHaste.class, "name"));
	}

	@Test
	@DisplayName("Reading an echo's ring adds nothing to the player's known rings")
	void inspectingTeachesThePlayerNothing() {
		Echo foreign = echoWearingAnUnknownRing();
		EchoTestSupport.warriorHero();
		HashSet<Class<? extends Ring>> knownBefore = new HashSet<>(Ring.getKnown());

		Hero restored = EchoHeroSnapshot.restoreHero(foreign);
		ItemPreview.of(restored.belongings.ring(), restored);

		Assertions.assertThat(Ring.getKnown()).isEqualTo(knownBefore);
	}

	/** Another player's hero, wearing gear they never identified. */
	private static Echo echoWearingAnUnknownRing() {
		Hero owner = new Hero();
		Dungeon.hero = owner;
		HeroClass.WARRIOR.initHero(owner);

		Greataxe axe = new Greataxe();
		axe.upgrade(2);
		owner.belongings.weapon = axe;

		RingOfHaste ring = new RingOfHaste();
		owner.belongings.ring = ring;

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
