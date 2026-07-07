package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite;
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
	@DisplayName("EchoBoss restores equipped items from snapshot for sprite and combat")
	void echoBossUsesEquippedItemsFromSnapshot() {
		Hero hero = heroWithPlateArmor();
		Echo echo = Echo.fromHero(hero, 5, EchoTestSupport.TEST_GAME_VERSION, 1L);

		EchoBoss boss = new EchoBoss(echo, 5);

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
}
