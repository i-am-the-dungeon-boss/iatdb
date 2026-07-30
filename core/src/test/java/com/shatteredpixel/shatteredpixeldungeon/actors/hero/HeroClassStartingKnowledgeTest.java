package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

@ExtendWith(GdxTestExtension.class)
class HeroClassStartingKnowledgeTest {

	@BeforeEach
	void resetItemKnowledge() {
		Item.clearCurrent();
		Potion.initColors();
		Scroll.initLabels();
	}

	@AfterEach
	void cleanup() {
		Potion.initColors();
		Scroll.initLabels();
	}

	@ParameterizedTest(name = "{0} starts knowing {1} and {2}")
	@MethodSource("classStartingKnowledge")
	@DisplayName("initHero identifies each class's starting potion and scroll")
	void initHeroIdentifiesClassStartingPotionAndScroll(
			HeroClass heroClass,
			Class<? extends Potion> startingPotion,
			Class<? extends Scroll> startingScroll) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		heroClass.initHero(hero);

		assertStartingKnowledge(heroClass, startingPotion, startingScroll);
	}

	@ParameterizedTest(name = "{0} still knows {1} and {2} after EchoBoss load")
	@MethodSource("classStartingKnowledge")
	@DisplayName("class starting knowledge survives EchoBoss bundle restore")
	void classStartingKnowledgeSurvivesEchoBossLoad(
			HeroClass heroClass,
			Class<? extends Potion> startingPotion,
			Class<? extends Scroll> startingScroll) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		heroClass.initHero(hero);
		hero.live();

		loadEchoBossRoundTrip(hero);

		assertStartingKnowledge(heroClass, startingPotion, startingScroll);
	}

	@ParameterizedTest(name = "{0} does not start knowing {1}")
	@MethodSource("otherClassPotionKnowledge")
	@DisplayName("initHero does not identify another class's starting potion")
	void initHeroDoesNotIdentifyOtherClassStartingPotion(
			HeroClass heroClass,
			Class<? extends Potion> otherClassPotion) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		heroClass.initHero(hero);

		Assertions.assertThat(Reflection.newInstance(otherClassPotion).isKnown())
				.as("%s must not start knowing %s", heroClass, otherClassPotion.getSimpleName())
				.isFalse();
	}

	@ParameterizedTest(name = "{0} still does not know {1} after loading {2} EchoBoss")
	@MethodSource("otherClassEchoBossKnowledge")
	@DisplayName("loading another class EchoBoss does not teach their starting potion")
	void loadingOtherClassEchoBossDoesNotTeachStartingPotion(
			HeroClass livingClass,
			Class<? extends Potion> otherClassPotion,
			HeroClass echoClass) {
		Bundle echoBossBundle = captureEchoBossBundle(echoClass);

		Item.clearCurrent();
		Potion.initColors();
		Scroll.initLabels();

		Hero hero = new Hero();
		Dungeon.hero = hero;
		livingClass.initHero(hero);
		hero.live();
		Assertions.assertThat(Reflection.newInstance(otherClassPotion).isKnown())
				.as("precondition: living %s does not know %s", livingClass, otherClassPotion.getSimpleName())
				.isFalse();

		Dungeon.depth = 5;
		EchoBoss restored = new EchoBoss();
		restored.restoreFromBundle(echoBossBundle);

		Assertions.assertThat(restored.getEchoHero().heroClass).isEqualTo(echoClass);
		Assertions.assertThat(Reflection.newInstance(otherClassPotion).isKnown())
				.as("EchoBoss restore must not teach living hero %s", otherClassPotion.getSimpleName())
				.isFalse();
	}

	private static void assertStartingKnowledge(
			HeroClass heroClass,
			Class<? extends Potion> startingPotion,
			Class<? extends Scroll> startingScroll) {
		Assertions.assertThat(new ScrollOfIdentify().isKnown())
				.as("every class starts knowing identify")
				.isTrue();
		Assertions.assertThat(Reflection.newInstance(startingPotion).isKnown())
				.as("%s starting potion", heroClass)
				.isTrue();
		Assertions.assertThat(Reflection.newInstance(startingScroll).isKnown())
				.as("%s starting scroll", heroClass)
				.isTrue();
	}

	private static void loadEchoBossRoundTrip(Hero hero) {
		EchoBoss original = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		Bundle bundle = new Bundle();
		original.storeInBundle(bundle);

		EchoBoss restored = new EchoBoss();
		restored.restoreFromBundle(bundle);
		Assertions.assertThat(restored.getEchoHero()).isNotNull();
	}

	private static Bundle captureEchoBossBundle(HeroClass echoClass) {
		Hero echoSource = new Hero();
		Dungeon.hero = echoSource;
		echoClass.initHero(echoSource);
		echoSource.live();
		Echo echo = Echo.fromHero(echoSource, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);
		EchoBoss original = EchoTestSupport.createBoss(echo, 5);
		Bundle bundle = new Bundle();
		original.storeInBundle(bundle);
		return bundle;
	}

	static Stream<Arguments> classStartingKnowledge() {
		return Stream.of(
				Arguments.of(HeroClass.WARRIOR, PotionOfHealing.class, ScrollOfRage.class),
				Arguments.of(HeroClass.MAGE, PotionOfLiquidFlame.class, ScrollOfUpgrade.class),
				Arguments.of(HeroClass.ROGUE, PotionOfInvisibility.class, ScrollOfMagicMapping.class),
				Arguments.of(HeroClass.HUNTRESS, PotionOfMindVision.class, ScrollOfLullaby.class),
				Arguments.of(HeroClass.DUELIST, PotionOfStrength.class, ScrollOfMirrorImage.class),
				Arguments.of(HeroClass.CLERIC, PotionOfPurity.class, ScrollOfRemoveCurse.class));
	}

	static Stream<Arguments> otherClassPotionKnowledge() {
		return Stream.of(
				Arguments.of(HeroClass.WARRIOR, PotionOfLiquidFlame.class),
				Arguments.of(HeroClass.MAGE, PotionOfHealing.class),
				Arguments.of(HeroClass.ROGUE, PotionOfStrength.class),
				Arguments.of(HeroClass.HUNTRESS, PotionOfInvisibility.class),
				Arguments.of(HeroClass.DUELIST, PotionOfMindVision.class),
				Arguments.of(HeroClass.CLERIC, PotionOfHealing.class));
	}

	static Stream<Arguments> otherClassEchoBossKnowledge() {
		return Stream.of(
				Arguments.of(HeroClass.WARRIOR, PotionOfLiquidFlame.class, HeroClass.MAGE),
				Arguments.of(HeroClass.MAGE, PotionOfHealing.class, HeroClass.WARRIOR),
				Arguments.of(HeroClass.ROGUE, PotionOfStrength.class, HeroClass.DUELIST),
				Arguments.of(HeroClass.HUNTRESS, PotionOfInvisibility.class, HeroClass.ROGUE),
				Arguments.of(HeroClass.DUELIST, PotionOfMindVision.class, HeroClass.HUNTRESS),
				Arguments.of(HeroClass.CLERIC, PotionOfHealing.class, HeroClass.WARRIOR));
	}
}
