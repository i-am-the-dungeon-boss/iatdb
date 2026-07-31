package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
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
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Cudgel;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
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

	@ParameterizedTest(name = "{0} starts knowing {1}")
	@MethodSource("startingKnownTypes")
	@DisplayName("initHero marks each default-known potion and scroll type as known")
	void initHeroMarksDefaultKnownTypeAsKnown(
			HeroClass heroClass,
			Class<? extends Item> knownType) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		heroClass.initHero(hero);

		Assertions.assertThat(isTypeKnown(knownType))
				.as("%s must start knowing %s", heroClass, knownType.getSimpleName())
				.isTrue();
	}

	@ParameterizedTest(name = "{0} still starts knowing {1} when curUser is a stale Echo kit")
	@MethodSource("startingKnownTypes")
	@DisplayName("initHero teaches default-known types even when Item.curUser is a leftover Echo")
	void initHeroTeachesDefaultKnownTypesDespiteStaleEchoCurUser(
			HeroClass heroClass,
			Class<? extends Item> knownType) {
		Hero living = new Hero();
		Dungeon.hero = living;

		Hero staleEchoKit = new Hero();
		staleEchoKit.heroClass = HeroClass.WARRIOR;
		// Simulate Echo combat leaving Item.curUser set after returning to a new run.
		new ClothArmor().setCurrent(staleEchoKit);

		heroClass.initHero(living);

		Assertions.assertThat(isTypeKnown(knownType))
				.as("%s must start knowing %s despite stale Echo curUser", heroClass, knownType.getSimpleName())
				.isTrue();
	}

	@ParameterizedTest(name = "{0} starts with identified {1}")
	@MethodSource("startingIdentifiedEquipment")
	@DisplayName("initHero leaves each default starting equipment item identified")
	void initHeroLeavesStartingEquipmentIdentified(
			HeroClass heroClass,
			Class<? extends Item> itemClass) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		heroClass.initHero(hero);

		Item item = hero.belongings.getItem(itemClass);
		Assertions.assertThat(item)
				.as("%s must start with %s", heroClass, itemClass.getSimpleName())
				.isNotNull();
		Assertions.assertThat(item.isIdentified())
				.as("%s starting %s must be identified", heroClass, itemClass.getSimpleName())
				.isTrue();
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

		Assertions.assertThat(new ScrollOfIdentify().isKnown()).isTrue();
		Assertions.assertThat(Reflection.newInstance(startingPotion).isKnown()).isTrue();
		Assertions.assertThat(Reflection.newInstance(startingScroll).isKnown()).isTrue();
	}

	@ParameterizedTest(name = "{0} does not start knowing {1}")
	@MethodSource("otherClassTypeKnowledge")
	@DisplayName("initHero does not identify another class's starting potion or scroll")
	void initHeroDoesNotIdentifyOtherClassStartingType(
			HeroClass heroClass,
			Class<? extends Item> otherClassType) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		heroClass.initHero(hero);

		Assertions.assertThat(isTypeKnown(otherClassType))
				.as("%s must not start knowing %s", heroClass, otherClassType.getSimpleName())
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

	private static boolean isTypeKnown(Class<? extends Item> type) {
		Item sample = Reflection.newInstance(type);
		if (sample instanceof Potion) {
			return ((Potion) sample).isKnown();
		}
		if (sample instanceof Scroll) {
			return ((Scroll) sample).isKnown();
		}
		throw new IllegalArgumentException("expected potion or scroll type, got " + type);
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

	/**
	 * Potion/scroll types marked known via throwaway {@code identify()} in
	 * {@link HeroClass#initHero}.
	 */
	static Stream<Arguments> startingKnownTypes() {
		return Stream.of(
				Arguments.of(HeroClass.WARRIOR, ScrollOfIdentify.class),
				Arguments.of(HeroClass.WARRIOR, PotionOfHealing.class),
				Arguments.of(HeroClass.WARRIOR, ScrollOfRage.class),
				Arguments.of(HeroClass.MAGE, ScrollOfIdentify.class),
				Arguments.of(HeroClass.MAGE, PotionOfLiquidFlame.class),
				Arguments.of(HeroClass.MAGE, ScrollOfUpgrade.class),
				Arguments.of(HeroClass.ROGUE, ScrollOfIdentify.class),
				Arguments.of(HeroClass.ROGUE, PotionOfInvisibility.class),
				Arguments.of(HeroClass.ROGUE, ScrollOfMagicMapping.class),
				Arguments.of(HeroClass.HUNTRESS, ScrollOfIdentify.class),
				Arguments.of(HeroClass.HUNTRESS, PotionOfMindVision.class),
				Arguments.of(HeroClass.HUNTRESS, ScrollOfLullaby.class),
				Arguments.of(HeroClass.DUELIST, ScrollOfIdentify.class),
				Arguments.of(HeroClass.DUELIST, PotionOfStrength.class),
				Arguments.of(HeroClass.DUELIST, ScrollOfMirrorImage.class),
				Arguments.of(HeroClass.CLERIC, ScrollOfIdentify.class),
				Arguments.of(HeroClass.CLERIC, PotionOfPurity.class),
				Arguments.of(HeroClass.CLERIC, ScrollOfRemoveCurse.class));
	}

	/** Instances given to the hero that {@link HeroClass#initHero} identifies. */
	static Stream<Arguments> startingIdentifiedEquipment() {
		return Stream.of(
				Arguments.of(HeroClass.WARRIOR, ClothArmor.class),
				Arguments.of(HeroClass.WARRIOR, WornShortsword.class),
				Arguments.of(HeroClass.WARRIOR, ThrowingStone.class),
				Arguments.of(HeroClass.MAGE, ClothArmor.class),
				Arguments.of(HeroClass.MAGE, MagesStaff.class),
				Arguments.of(HeroClass.ROGUE, ClothArmor.class),
				Arguments.of(HeroClass.ROGUE, Dagger.class),
				Arguments.of(HeroClass.ROGUE, CloakOfShadows.class),
				Arguments.of(HeroClass.ROGUE, ThrowingKnife.class),
				Arguments.of(HeroClass.HUNTRESS, ClothArmor.class),
				Arguments.of(HeroClass.HUNTRESS, Gloves.class),
				Arguments.of(HeroClass.HUNTRESS, SpiritBow.class),
				Arguments.of(HeroClass.DUELIST, ClothArmor.class),
				Arguments.of(HeroClass.DUELIST, Rapier.class),
				Arguments.of(HeroClass.DUELIST, ThrowingSpike.class),
				Arguments.of(HeroClass.CLERIC, ClothArmor.class),
				Arguments.of(HeroClass.CLERIC, Cudgel.class),
				Arguments.of(HeroClass.CLERIC, HolyTome.class));
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

	static Stream<Arguments> otherClassTypeKnowledge() {
		return Stream.of(
				Arguments.of(HeroClass.WARRIOR, PotionOfLiquidFlame.class),
				Arguments.of(HeroClass.WARRIOR, ScrollOfUpgrade.class),
				Arguments.of(HeroClass.MAGE, PotionOfHealing.class),
				Arguments.of(HeroClass.MAGE, ScrollOfRage.class),
				Arguments.of(HeroClass.ROGUE, PotionOfStrength.class),
				Arguments.of(HeroClass.ROGUE, ScrollOfMirrorImage.class),
				Arguments.of(HeroClass.HUNTRESS, PotionOfInvisibility.class),
				Arguments.of(HeroClass.HUNTRESS, ScrollOfMagicMapping.class),
				Arguments.of(HeroClass.DUELIST, PotionOfMindVision.class),
				Arguments.of(HeroClass.DUELIST, ScrollOfLullaby.class),
				Arguments.of(HeroClass.CLERIC, PotionOfHealing.class),
				Arguments.of(HeroClass.CLERIC, ScrollOfRage.class));
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
