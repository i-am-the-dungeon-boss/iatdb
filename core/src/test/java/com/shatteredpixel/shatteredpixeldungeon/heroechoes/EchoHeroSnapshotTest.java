package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SnipersMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoHeroSnapshotTest {

	@AfterEach
	void cleanup() {
		Dungeon.quickslot.reset();
		ActionIndicator.clearAction();
		GameScene.updateItemDisplays = false;
		Belongings.bundleRestoring = false;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("fromHero records equipped armor in echo data")
	void fromHeroRecordsEquippedArmor() {
		Hero hero = heroWithPlateArmor();

		Echo echo = Echo.fromHero(hero, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);

		Assertions.assertThat(EchoHeroSnapshot.hasEquippedItems(echo.echoData)).isTrue();
		Assertions.assertThat(EchoHeroSnapshot.restoreHero(echo).belongings.armor())
				.isInstanceOf(PlateArmor.class);
	}

	@Test
	@DisplayName("fromHero records equipped weapon in echo data")
	void fromHeroRecordsEquippedWeapon() {
		Hero hero = heroWithPlateArmor();
		WornShortsword sword = new WornShortsword();
		sword.identify();
		hero.belongings.weapon = sword;

		Echo echo = Echo.fromHero(hero, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);

		Assertions.assertThat(EchoHeroSnapshot.hasEquippedItems(echo.echoData)).isTrue();
		Assertions.assertThat(EchoHeroSnapshot.restoreHero(echo).belongings.weapon())
				.isInstanceOf(WornShortsword.class);
	}

	@Test
	@DisplayName("recordEquippedItems backfills missing equipment into partial echo data")
	void recordEquippedItemsBackfillsPartialEchoData() {
		Hero hero = heroWithPlateArmor();
		Bundle partial = new Bundle();
		partial.put("lvl", hero.lvl);

		EchoHeroSnapshot.recordEquippedItems(hero, partial);

		Assertions.assertThat(EchoHeroSnapshot.hasEquippedItems(partial)).isTrue();
		Hero restored = new Hero();
		restored.live();
		restored.restoreFromBundle(partial);
		Assertions.assertThat(restored.belongings.armor()).isInstanceOf(PlateArmor.class);
	}

	@Test
	@DisplayName("restoreHero does not overwrite the player's quickslot bar")
	void restoreHeroPreservesPlayerQuickslots() {
		Hero echoSource = heroWithPlateArmor();
		PotionOfHealing echoPotion = new PotionOfHealing();
		echoPotion.identify();
		echoSource.belongings.backpack.items.add(echoPotion);
		Dungeon.quickslot.reset();
		Dungeon.quickslot.setSlot(0, echoPotion);

		Echo echo = Echo.fromHero(echoSource, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);

		Hero player = new Hero();
		Dungeon.hero = player;
		HeroClass.MAGE.initHero(player);
		PotionOfHealing playerPotion = new PotionOfHealing();
		playerPotion.identify();
		player.belongings.backpack.items.add(playerPotion);
		Dungeon.quickslot.reset();
		Dungeon.quickslot.setSlot(0, playerPotion);
		Dungeon.quickslot.setSlot(1, player.belongings.weapon);

		EchoHeroSnapshot.restoreHero(echo);

		Assertions.assertThat(Dungeon.quickslot.getItem(0)).isSameAs(playerPotion);
		Assertions.assertThat(Dungeon.quickslot.getItem(1)).isSameAs(player.belongings.weapon);
		for (int i = 2; i < QuickSlot.SIZE; i++) {
			Assertions.assertThat(Dungeon.quickslot.getItem(i)).isNull();
		}
	}

	@Test
	@DisplayName("restoreHero does not overwrite the player's ActionIndicator")
	void restoreHeroPreservesActionIndicator() {
		Hero echoSource = heroWithPlateArmor();
		SnipersMark mark = Buff.affect(echoSource, SnipersMark.class);
		mark.set(42, 0.25f);
		Echo echo = Echo.fromHero(echoSource, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);

		Hero player = playerHero();
		ActionIndicator.Action playerAction = stubAction("player-action");
		ActionIndicator.setAction(playerAction);

		EchoHeroSnapshot.restoreHero(echo);

		Assertions.assertThat(ActionIndicator.action).isSameAs(playerAction);
		Assertions.assertThat(Dungeon.hero).isSameAs(player);
	}

	@Test
	@DisplayName("restoreHero preserves GameScene.updateItemDisplays")
	void restoreHeroPreservesUpdateItemDisplays() {
		Hero echoSource = heroWithPlateArmor();
		echoSource.belongings.armor().upgrade(3);
		Echo echo = Echo.fromHero(echoSource, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);

		Dungeon.hero = playerHero();
		GameScene.updateItemDisplays = false;

		EchoHeroSnapshot.restoreHero(echo);

		Assertions.assertThat(GameScene.updateItemDisplays).isFalse();
	}

	@Test
	@DisplayName("restoreHero leaves Belongings.bundleRestoring false")
	void restoreHeroClearsBundleRestoringFlag() {
		Hero echoSource = heroWithPlateArmor();
		Echo echo = Echo.fromHero(echoSource, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);
		Dungeon.hero = playerHero();
		Belongings.bundleRestoring = true;

		EchoHeroSnapshot.restoreHero(echo);

		Assertions.assertThat(Belongings.bundleRestoring).isFalse();
	}

	@Test
	@DisplayName("EchoBoss load does not overwrite player ActionIndicator or quickslots")
	void echoBossLoadPreservesPlayerGlobals() {
		Hero echoSource = heroWithPlateArmor();
		PotionOfHealing echoPotion = new PotionOfHealing();
		echoPotion.identify();
		echoSource.belongings.backpack.items.add(echoPotion);
		Dungeon.quickslot.reset();
		Dungeon.quickslot.setSlot(0, echoPotion);
		SnipersMark mark = Buff.affect(echoSource, SnipersMark.class);
		mark.set(7, 0.5f);
		Echo echo = Echo.fromHero(echoSource, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);

		Hero player = playerHero();
		PotionOfHealing playerPotion = new PotionOfHealing();
		playerPotion.identify();
		player.belongings.backpack.items.add(playerPotion);
		Dungeon.quickslot.reset();
		Dungeon.quickslot.setSlot(0, playerPotion);
		ActionIndicator.Action playerAction = stubAction("boss-load-action");
		ActionIndicator.setAction(playerAction);
		GameScene.updateItemDisplays = false;

		EchoTestSupport.createBoss(echo, 5);

		Assertions.assertThat(Dungeon.quickslot.getItem(0)).isSameAs(playerPotion);
		Assertions.assertThat(ActionIndicator.action).isSameAs(playerAction);
		Assertions.assertThat(GameScene.updateItemDisplays).isFalse();
		Assertions.assertThat(Belongings.bundleRestoring).isFalse();
		Assertions.assertThat(Dungeon.hero).isSameAs(player);
	}

	@Test
	@DisplayName("EchoBoss restores equipped items from snapshot for sprite and combat")
	void echoBossUsesEquippedItemsFromSnapshot() {
		Hero hero = heroWithPlateArmor();
		Echo echo = Echo.fromHero(hero, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);

		EchoBoss boss = EchoTestSupport.createBoss(echo, 5);

		Assertions.assertThat(boss.getEchoHero().belongings.armor()).isInstanceOf(PlateArmor.class);
		Assertions.assertThat(EchoBossSprite.armorTierFor(boss.getEchoHero(), echo))
				.isEqualTo(((PlateArmor) hero.belongings.armor()).tier);
	}

	@Test
	@DisplayName("captureBossVictory persists equipped items when boss dies")
	void captureBossVictoryPersistsEquippedItems() {
		Hero hero = heroWithPlateArmor();
		EchoStorage storage = new EchoStorage();

		EchoCaptureTrigger.captureBossVictory(hero, 5, storage);

		Echo loaded = storage.loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION).orElseThrow();
		Assertions.assertThat(EchoHeroSnapshot.hasEquippedItems(loaded.echoData)).isTrue();
		Assertions.assertThat(EchoHeroSnapshot.restoreHero(loaded).belongings.armor())
				.isInstanceOf(PlateArmor.class);
	}

	private static Hero heroWithPlateArmor() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		hero.lvl = 12;
		hero.HP = hero.HT = 80;
		PlateArmor armor = new PlateArmor();
		armor.identify();
		hero.belongings.armor = armor;
		return hero;
	}

	private static Hero playerHero() {
		Hero player = new Hero();
		Dungeon.hero = player;
		HeroClass.MAGE.initHero(player);
		return player;
	}

	private static ActionIndicator.Action stubAction(String name) {
		return new ActionIndicator.Action() {
			@Override
			public String actionName() {
				return name;
			}

			@Override
			public int indicatorColor() {
				return 0xFFFFFF;
			}

			@Override
			public void doAction() {
			}
		};
	}
}
