package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Covers the "unlock everything" settings toggle end to end: the persisted
 * flag itself, and its effect on {@link HeroClass#isUnlocked()} with the
 * flag on and off, without ever touching {@link Badges} unlock state.
 */
@ExtendWith(GdxTestExtension.class)
class UnlockEverythingSettingTest {

	@AfterEach
	void cleanup() {
		SPDSettings.unlockEverything(false);
		Badges.disown(Badges.Badge.UNLOCK_MAGE);
		Badges.disown(Badges.Badge.UNLOCK_ROGUE);
		Badges.disown(Badges.Badge.UNLOCK_HUNTRESS);
		Badges.disown(Badges.Badge.UNLOCK_DUELIST);
		Badges.disown(Badges.Badge.UNLOCK_CLERIC);
		Badges.reset();
	}

	@Test
	@DisplayName("unlock everything setting defaults to off and persists when toggled")
	void unlockEverythingDefaultsOffAndPersists() {
		Assertions.assertThat(SPDSettings.unlockEverything()).isFalse();

		SPDSettings.unlockEverything(true);
		Assertions.assertThat(SPDSettings.unlockEverything()).isTrue();

		SPDSettings.unlockEverything(false);
		Assertions.assertThat(SPDSettings.unlockEverything()).isFalse();
	}

	@ParameterizedTest(name = "{0} is locked without its badge or the setting")
	@EnumSource(value = HeroClass.class, names = { "MAGE", "ROGUE", "HUNTRESS", "DUELIST", "CLERIC" })
	@DisplayName("locked classes stay locked when the setting is off")
	void lockedClassStaysLockedWithSettingOff(HeroClass heroClass) {
		SPDSettings.unlockEverything(false);

		Assertions.assertThat(heroClass.isUnlocked()).isFalse();
	}

	@ParameterizedTest(name = "{0} is unlocked by the setting without earning its badge")
	@EnumSource(value = HeroClass.class, names = { "MAGE", "ROGUE", "HUNTRESS", "DUELIST", "CLERIC" })
	@DisplayName("unlock everything setting bypasses class-unlock badges")
	void unlockEverythingBypassesClassBadges(HeroClass heroClass) {
		SPDSettings.unlockEverything(true);

		Assertions.assertThat(heroClass.isUnlocked()).isTrue();

		// The setting must not have actually granted the badge.
		SPDSettings.unlockEverything(false);
		Assertions.assertThat(heroClass.isUnlocked())
				.as("%s must re-lock once the setting is turned back off", heroClass)
				.isFalse();
	}

	@Test
	@DisplayName("warrior is always unlocked regardless of the setting")
	void warriorAlwaysUnlocked() {
		SPDSettings.unlockEverything(false);
		Assertions.assertThat(HeroClass.WARRIOR.isUnlocked()).isTrue();

		SPDSettings.unlockEverything(true);
		Assertions.assertThat(HeroClass.WARRIOR.isUnlocked()).isTrue();
	}

	@Test
	@DisplayName("a genuinely earned class badge keeps that class unlocked after the setting is turned off")
	void genuinelyEarnedBadgeSurvivesSettingBeingTurnedOff() {
		Badges.unlock(Badges.Badge.UNLOCK_MAGE);

		SPDSettings.unlockEverything(true);
		Assertions.assertThat(HeroClass.MAGE.isUnlocked()).isTrue();

		SPDSettings.unlockEverything(false);
		Assertions.assertThat(HeroClass.MAGE.isUnlocked())
				.as("a real badge unlock must not depend on the setting")
				.isTrue();
	}
}
