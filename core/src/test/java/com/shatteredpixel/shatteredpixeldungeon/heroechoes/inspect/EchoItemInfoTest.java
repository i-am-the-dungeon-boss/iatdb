package com.shatteredpixel.shatteredpixeldungeon.heroechoes.inspect;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * An echo's equipment is described to the player as its own owner would read
 * it. The player's class, subclass, level and strength say nothing about
 * somebody else's kit.
 */
@ExtendWith(GdxTestExtension.class)
class EchoItemInfoTest {

	@BeforeEach
	void reset() {
		EchoTestSupport.resetWorkflowState();
		Notes.reset();
	}

	@Test
	@DisplayName("A duelist player reading a mage echo's sword sees no duelist ability text")
	void duelistTextDoesNotFollowThePlayer() {
		Sword sword = identifiedSword();
		playing(HeroClass.DUELIST);
		Hero echo = offstage(HeroClass.MAGE);

		Assertions.assertThat(infoOf(sword, echo))
				.doesNotContain(sword.abilityInfo());
	}

	@Test
	@DisplayName("A mage player reading a duelist echo's sword still sees the duelist ability text")
	void duelistTextFollowsTheEcho() {
		Sword sword = identifiedSword();
		playing(HeroClass.MAGE);
		Hero echo = offstage(HeroClass.DUELIST);

		Assertions.assertThat(infoOf(sword, echo))
				.contains(sword.abilityInfo());
	}

	@Test
	@DisplayName("Outside a preview, descriptions still read from the player")
	void thePlayerStillSeesTheirOwnDescriptions() {
		Sword sword = identifiedSword();
		playing(HeroClass.DUELIST);

		Assertions.assertThat(sword.info()).contains(sword.abilityInfo());
	}

	@Test
	@DisplayName("The strength verdict is the echo's, not the player's")
	void strengthVerdictFollowsTheEcho() {
		Sword sword = identifiedSword();
		Hero player = playing(HeroClass.WARRIOR);
		player.STR = 30;
		Hero echo = offstage(HeroClass.WARRIOR);
		echo.STR = 5;

		Assertions.assertThat(infoOf(sword, echo))
				.contains(Messages.get(Weapon.class, "too_heavy"));
	}

	@Test
	@DisplayName("A spirit bow's level is derived from the echo's level, not the player's")
	void spiritBowLevelFollowsTheEcho() {
		SpiritBow bow = new SpiritBow();
		Hero player = playing(HeroClass.HUNTRESS);
		player.lvl = 25;
		Hero echo = offstage(HeroClass.HUNTRESS);
		echo.lvl = 10;

		Assertions.assertThat(ItemPreview.of(bow, echo).level()).isEqualTo(2);
		Assertions.assertThat(bow.level()).isEqualTo(5);
	}

	@Test
	@DisplayName("A preview is a copy, so the echo's own item is never touched")
	void thePreviewNeverTouchesTheOriginal() {
		Sword sword = new Sword();
		playing(HeroClass.WARRIOR);
		Hero echo = offstage(HeroClass.DUELIST);

		Item preview = ItemPreview.of(sword, echo);

		Assertions.assertThat(preview).isNotSameAs(sword);
		Assertions.assertThat(sword.isIdentified()).isFalse();
	}

	private static String infoOf(Item item, Hero owner) {
		return ItemPreview.of(item, owner).info();
	}

	private static Sword identifiedSword() {
		Sword sword = new Sword();
		sword.levelKnown = true;
		sword.cursedKnown = true;
		return sword;
	}

	/** The hero actually playing this run. */
	private static Hero playing(HeroClass heroClass) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		heroClass.initHero(hero);
		return hero;
	}

	/** A restored hero that is not, and never becomes, {@link Dungeon#hero}. */
	private static Hero offstage(HeroClass heroClass) {
		Hero playing = Dungeon.hero;
		Hero hero = new Hero();
		heroClass.initHero(hero);
		Dungeon.hero = playing;
		return hero;
	}
}
