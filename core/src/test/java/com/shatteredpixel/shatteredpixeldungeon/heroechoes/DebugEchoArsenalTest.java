package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoInventory;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(GdxTestExtension.class)
class DebugEchoArsenalTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

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
		Assertions.assertThat(caps.has(DebugEchoArsenal.ROLE_DRINK)).isTrue();
		Assertions.assertThat(caps.has(DebugEchoArsenal.ROLE_THROW)).isTrue();
		Assertions.assertThat(caps.has(DebugEchoArsenal.ROLE)).isTrue();
		Assertions.assertThat(boss.getEchoPolicy().root()
				.getJSONObject("selection").getJSONArray("default_roles").getString(0))
				.isEqualTo(DebugEchoArsenal.ROLE_DRINK);
		Assertions.assertThat(boss.state).isSameAs(boss.HUNTING);
	}

	@Test
	@DisplayName("grantAndCycle gives bombs and lists them on the throw role")
	void grantAndCycleGivesBombsOnThrowRole() {
		DebugSettings.setDebugBuildOverride(true);
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		DebugEchoArsenal.grantAndCycle(boss);

		Assertions.assertThat(EchoInventory.count(boss.getEchoHero(), "Bomb")).isEqualTo(1);
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(Bomb.class)).isNotNull();

		org.json.JSONArray throwItems = boss.getEchoPolicy().root()
				.getJSONObject("capabilities")
				.getJSONObject(DebugEchoArsenal.ROLE_THROW)
				.getJSONArray("items");
		List<String> ids = new ArrayList<>();
		for (int i = 0; i < throwItems.length(); i++) {
			ids.add(throwItems.getString(i));
		}
		Assertions.assertThat(ids).contains("Bomb", "Firebomb", "ArcaneBomb");
		Assertions.assertThat(ids).doesNotContain("WandOfMagicMissile", "ScrollOfIdentify");
	}

	@Test
	@DisplayName("grantAndCycleAll is a no-op outside debug builds")
	void grantAndCycleAllNoOpOutsideDebugBuilds() {
		DebugSettings.setDebugBuildOverride(false);
		Assertions.assertThat(DebugEchoArsenal.grantAndCycleAll()).isZero();
	}
}
