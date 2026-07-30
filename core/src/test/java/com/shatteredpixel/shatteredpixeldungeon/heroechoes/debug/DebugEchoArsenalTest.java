package com.shatteredpixel.shatteredpixeldungeon.heroechoes.debug;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoInventory;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(GdxTestExtension.class)
class DebugEchoArsenalTest {

	@Test
	@DisplayName("usable items are potions, scrolls, wands, and throwables")
	void usableItemsArePotionsScrollsWandsAndThrowables() {
		List<Item> usable = DebugEchoArsenal.usableItems();

		Assertions.assertThat(usable).isNotEmpty();
		Assertions.assertThat(usable.stream().anyMatch(i -> i instanceof PotionOfHealing)).isTrue();
		Assertions.assertThat(usable.stream().anyMatch(i -> i instanceof ScrollOfIdentify)).isTrue();
		Assertions.assertThat(usable.stream().anyMatch(i -> i instanceof Wand)).isTrue();
		Assertions.assertThat(usable.stream().anyMatch(i -> i instanceof Bomb)).isTrue();
		Assertions.assertThat(usable.stream().anyMatch(i -> i instanceof Runestone || i instanceof MissileWeapon))
				.isTrue();
		Assertions.assertThat(usable.stream().filter(i -> i instanceof Bomb).count())
				.isGreaterThanOrEqualTo(11); // Catalog.BOMBS size
		Assertions.assertThat(usable.stream().noneMatch(i -> i.getClass().getSimpleName().equals("PotionOfStrength")))
				.isTrue();
		for (Item item : usable) {
			Assertions.assertThat(item instanceof Potion
					|| item instanceof Scroll
					|| item instanceof Wand
					|| item instanceof Runestone
					|| item instanceof com.shatteredpixel.shatteredpixeldungeon.items.stones.InventoryStone
					|| item instanceof MissileWeapon
					|| item instanceof Bomb)
					.as(item.getClass().getSimpleName())
					.isTrue();
		}
	}

	@Test
	@DisplayName("usable items are granted with quantity 1 and a single wand charge")
	void usableItemsHaveSingleUseCharges() {
		List<Item> usable = DebugEchoArsenal.usableItems();

		Assertions.assertThat(usable).isNotEmpty();
		for (Item item : usable) {
			Assertions.assertThat(item.quantity())
					.as("%s quantity", item.getClass().getSimpleName())
					.isEqualTo(1);
			if (item instanceof Wand) {
				Assertions.assertThat(((Wand) item).curCharges)
						.as("%s charges", item.getClass().getSimpleName())
						.isEqualTo(1);
			}
			if (item instanceof Runestone || item instanceof MissileWeapon || item instanceof Bomb
					|| item instanceof Scroll) {
				Assertions.assertThat(item.quantity())
						.as("%s single use", item.getClass().getSimpleName())
						.isEqualTo(1);
			}
			if (item instanceof MissileWeapon) {
				MissileWeapon missile = (MissileWeapon) item;
				float perUse = missile.durabilityPerUse();
				// Infinite-durability missiles (e.g. Dart) are qty-limited only.
				if (perUse > 0f) {
					Assertions.assertThat(missile.durabilityLeft())
							.as("%s single-throw durability", item.getClass().getSimpleName())
							.isLessThanOrEqualTo(perUse);
				}
			}
		}
	}

	@Test
	@DisplayName("grantAndCycle preserves potions the hero already identified")
	void grantAndCyclePreservesHeroPotionKnowledge() {
		DebugSettings.setDebugBuildOverride(true);
		Hero hero = EchoTestSupport.warriorHero();
		Item.clearCurrent();
		PotionOfLiquidFlame known = new PotionOfLiquidFlame();
		known.identify();
		Assertions.assertThat(known.isKnown()).isTrue();
		known.collect(hero.belongings.backpack);

		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		DebugEchoArsenal.grantAndCycle(boss);

		Assertions.assertThat(new PotionOfLiquidFlame().isKnown())
				.as("boss arsenal setup must not wipe potions the hero already knew")
				.isTrue();
		Assertions.assertThat(hero.belongings.getItem(PotionOfLiquidFlame.class).isIdentified())
				.isTrue();
	}

	@Test
	@DisplayName("grantAndCycle replaces prior arsenal so each stone stays at one use")
	void grantAndCycleDoesNotStackStones() {
		DebugSettings.setDebugBuildOverride(true);
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		DebugEchoArsenal.grantAndCycle(boss);
		DebugEchoArsenal.grantAndCycle(boss);

		Assertions.assertThat(EchoInventory.count(boss.getEchoHero(), "StoneOfBlink")).isEqualTo(1);
		Assertions.assertThat(EchoInventory.count(boss.getEchoHero(), "PotionOfHealing")).isEqualTo(1);
		Assertions.assertThat(EchoInventory.count(boss.getEchoHero(), "ScrollOfIdentify")).isEqualTo(1);
	}

	@Test
	@DisplayName("grantAndCycle fills kit and installs FIRST_LEGAL arsenal policy")
	void grantAndCycleFillsKitAndInstallsArsenalPolicy() {
		DebugSettings.setDebugBuildOverride(true);
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		DebugEchoArsenal.grantAndCycle(boss);

		Assertions.assertThat(EchoInventory.count(boss.getEchoHero(), "PotionOfHealing")).isGreaterThan(0);
		Assertions.assertThat(EchoInventory.count(boss.getEchoHero(), "ScrollOfIdentify")).isGreaterThan(0);
		JSONObject caps = boss.getEchoPolicy().root().getJSONObject("capabilities");
		Assertions.assertThat(caps.has(DebugEchoArsenal.ROLE_BOMB)).isTrue();
		Assertions.assertThat(caps.has(DebugEchoArsenal.ROLE_DRINK)).isTrue();
		Assertions.assertThat(caps.has(DebugEchoArsenal.ROLE_THROW)).isTrue();
		Assertions.assertThat(caps.has(DebugEchoArsenal.ROLE)).isTrue();
		Assertions.assertThat(boss.getEchoPolicy().root()
				.getJSONObject("selection").getJSONArray("default_roles").getString(0))
				.isEqualTo(DebugEchoArsenal.ROLE_BOMB);
		Assertions.assertThat(boss.state).isSameAs(boss.HUNTING);
	}

	@Test
	@DisplayName("grantAndCycle gives bombs and lists them on the BOMB light-throw role")
	void grantAndCycleGivesBombsOnBombRole() {
		DebugSettings.setDebugBuildOverride(true);
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		DebugEchoArsenal.grantAndCycle(boss);

		Assertions.assertThat(EchoInventory.count(boss.getEchoHero(), "Bomb")).isEqualTo(1);
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(Bomb.class)).isNotNull();

		org.json.JSONArray bombItems = boss.getEchoPolicy().root()
				.getJSONObject("capabilities")
				.getJSONObject(DebugEchoArsenal.ROLE_BOMB)
				.getJSONArray("items");
		List<String> ids = new ArrayList<>();
		for (int i = 0; i < bombItems.length(); i++) {
			ids.add(bombItems.getString(i));
		}
		Assertions.assertThat(ids).contains("Bomb", "Firebomb", "ArcaneBomb", "FrostBomb", "SmokeBomb",
				"HolyBomb", "Noisemaker", "FlashBangBomb", "RegrowthBomb", "WoollyBomb", "ShrapnelBomb");
		Assertions.assertThat(ids).doesNotContain("WandOfMagicMissile", "ScrollOfIdentify", "ThrowingStone");
		Assertions.assertThat(boss.getEchoPolicy().root()
				.getJSONObject("selection").getJSONArray("default_roles").getString(0))
				.isEqualTo(DebugEchoArsenal.ROLE_BOMB);
	}

	@Test
	@DisplayName("grantAndCycle lists the potion of invisibility on the drink role")
	void grantAndCycleGivesInvisibilityOnDrinkRole() {
		DebugSettings.setDebugBuildOverride(true);
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		DebugEchoArsenal.grantAndCycle(boss);

		Assertions.assertThat(EchoInventory.count(boss.getEchoHero(), "PotionOfInvisibility"))
				.isEqualTo(1);
		org.json.JSONArray drinkItems = boss.getEchoPolicy().root()
				.getJSONObject("capabilities")
				.getJSONObject(DebugEchoArsenal.ROLE_DRINK)
				.getJSONArray("items");
		List<String> ids = new ArrayList<>();
		for (int i = 0; i < drinkItems.length(); i++) {
			ids.add(drinkItems.getString(i));
		}
		Assertions.assertThat(ids).contains("PotionOfInvisibility");
	}

	@Test
	@DisplayName("grantAndCycle does not install ClassArmor on the arsenal policy")
	void grantAndCycleDoesNotInstallClassArmor() {
		DebugSettings.setDebugBuildOverride(true);
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		Class<?> abilityBefore = boss.getEchoHero().armorAbility != null
				? boss.getEchoHero().armorAbility.getClass()
				: null;

		DebugEchoArsenal.grantAndCycle(boss);

		org.json.JSONArray arsenal = boss.getEchoPolicy().root()
				.getJSONObject("capabilities")
				.getJSONObject(DebugEchoArsenal.ROLE)
				.getJSONArray("items");
		List<String> ids = new ArrayList<>();
		for (int i = 0; i < arsenal.length(); i++) {
			ids.add(arsenal.getString(i));
		}
		Assertions.assertThat(ids).doesNotContain("WarriorArmor", "MageArmor", "RogueArmor",
				"HuntressArmor", "DuelistArmor", "ClericArmor");
		Class<?> abilityAfter = boss.getEchoHero().armorAbility != null
				? boss.getEchoHero().armorAbility.getClass()
				: null;
		Assertions.assertThat(abilityAfter).isEqualTo(abilityBefore);
	}

	@Test
	@DisplayName("grantArmorAbility equips charged ClassArmor and lists it on ARMOR role")
	void grantArmorAbilityEquipsClassArmorOnArmorRole() {
		DebugSettings.setDebugBuildOverride(true);
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		String abilityName = DebugEchoArsenal.grantArmorAbility(boss);

		Hero kit = boss.getEchoHero();
		Assertions.assertThat(kit.belongings.armor)
				.isInstanceOf(com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor.class);
		com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor armor = (com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor) kit.belongings.armor;
		Assertions.assertThat(armor.charge).isGreaterThanOrEqualTo(100f);
		Assertions.assertThat(kit.armorAbility).isNotNull();
		Assertions.assertThat(abilityName).isEqualTo(kit.armorAbility.name());
		Assertions.assertThat(EchoInventory.availableIds(kit))
				.contains(armor.getClass().getSimpleName());

		org.json.JSONArray armorItems = boss.getEchoPolicy().root()
				.getJSONObject("capabilities")
				.getJSONObject(DebugEchoArsenal.ROLE_ARMOR)
				.getJSONArray("items");
		List<String> ids = new ArrayList<>();
		for (int i = 0; i < armorItems.length(); i++) {
			ids.add(armorItems.getString(i));
		}
		Assertions.assertThat(ids).containsExactly(armor.getClass().getSimpleName());
		Assertions.assertThat(boss.getEchoPolicy().root()
				.getJSONObject("selection").getJSONArray("default_roles").getString(0))
				.isEqualTo(DebugEchoArsenal.ROLE_ARMOR);
	}

	@Test
	@DisplayName("grantArmorAbility rotates the echo kit armor ability each press")
	void grantArmorAbilityRotatesArmorAbility() {
		DebugSettings.setDebugBuildOverride(true);
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		DebugEchoArsenal.grantArmorAbility(boss);
		Class<?> first = boss.getEchoHero().armorAbility.getClass();

		DebugEchoArsenal.grantArmorAbility(boss);
		Class<?> second = boss.getEchoHero().armorAbility.getClass();

		Assertions.assertThat(second).isNotEqualTo(first);
		List<Class<?>> allowed = new ArrayList<>();
		for (com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility ability : boss
				.getEchoHero().heroClass.armorAbilities()) {
			allowed.add(ability.getClass());
		}
		Assertions.assertThat(allowed).contains(first, second);
	}

	@Test
	@DisplayName("grantAndCycleAll is a no-op outside debug builds")
	void grantAndCycleAllNoOpOutsideDebugBuilds() {
		DebugSettings.setDebugBuildOverride(false);
		Assertions.assertThat(DebugEchoArsenal.grantAndCycleAll()).isZero();
	}

	@Test
	@DisplayName("grantArmorAbilityAll is a no-op outside debug builds")
	void grantArmorAbilityAllNoOpOutsideDebugBuilds() {
		DebugSettings.setDebugBuildOverride(false);
		Assertions.assertThat(DebugEchoArsenal.grantArmorAbilityAll()).isZero();
	}
}
