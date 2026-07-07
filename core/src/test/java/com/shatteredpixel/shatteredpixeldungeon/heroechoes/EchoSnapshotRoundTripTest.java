package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies boss-capture snapshots survive storage round-trip and feed EchoBoss correctly.
 */
@ExtendWith(GdxTestExtension.class)
class EchoSnapshotRoundTripTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("storage round-trip preserves full hero combat data from boss capture")
	void storageRoundTripPreservesFullHeroCombatData() {
		Hero hero = richHero();
		EchoStorage storage = new EchoStorage();

		EchoCaptureTrigger.captureBossVictory(hero, 5, storage);

		Echo loaded = storage.loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION).orElseThrow();
		Hero restored = EchoHeroSnapshot.restoreHero(loaded);

		Assertions.assertThat(loaded.heroClass).isEqualTo("MAGE");
		Assertions.assertThat(loaded.lvl).isEqualTo(hero.lvl);
		Assertions.assertThat(loaded.hp).isEqualTo(hero.HP);
		Assertions.assertThat(loaded.ht).isEqualTo(hero.HT);
		Assertions.assertThat(restored.heroClass).isEqualTo(HeroClass.MAGE);
		Assertions.assertThat(restored.lvl).isEqualTo(hero.lvl);
		Assertions.assertThat(restored.STR).isEqualTo(hero.STR);
		Assertions.assertThat(restored.belongings.armor()).isInstanceOf(PlateArmor.class);
		Assertions.assertThat(restored.belongings.weapon()).isInstanceOf(WornShortsword.class);
		Assertions.assertThat(restored.belongings.getItem(PotionOfHealing.class)).isNotNull();
	}

	@Test
	@DisplayName("EchoBoss from storage-loaded snapshot uses restored hero for combat and inventory")
	void echoBossUsesStorageLoadedSnapshot() {
		Hero hero = richHero();
		EchoStorage storage = new EchoStorage();
		EchoCaptureTrigger.captureBossVictory(hero, 5, storage);

		Echo loaded = storage.loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION).orElseThrow();
		EchoBoss boss = new EchoBoss(loaded, 5);
		Hero echoHero = boss.getEchoHero();

		Assertions.assertThat(echoHero.heroClass).isEqualTo(HeroClass.MAGE);
		Assertions.assertThat(echoHero.belongings.armor()).isInstanceOf(PlateArmor.class);
		Assertions.assertThat(echoHero.belongings.weapon()).isInstanceOf(WornShortsword.class);
		Assertions.assertThat(echoHero.belongings.getItem(PotionOfHealing.class)).isNotNull();
		Assertions.assertThat(boss.defenseSkill(hero)).isEqualTo(echoHero.defenseSkill(hero));
		Assertions.assertThat(EchoBossSprite.armorTierFor(echoHero, loaded))
				.isEqualTo(((PlateArmor) hero.belongings.armor()).tier);
	}

	@Test
	@DisplayName("EchoBoss from storage-loaded snapshot can heal using backpack potion")
	void echoBossHealsFromStorageLoadedBackpack() {
		Hero hero = richHero();
		EchoStorage storage = new EchoStorage();
		EchoCaptureTrigger.captureBossVictory(hero, 5, storage);

		Echo loaded = storage.loadForDepth(5, EchoTestSupport.TEST_GAME_VERSION).orElseThrow();
		EchoBoss boss = new EchoBoss(loaded, 5);
		boss.HP = 10;

		Assertions.assertThat(boss.tryHealFromInventory()).isTrue();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfHealing.class)).isNull();
	}

	@Test
	@DisplayName("prefetch and spawner create EchoBoss from storage-loaded snapshot")
	void prefetchAndSpawnerUseStorageLoadedSnapshot() {
		Hero hero = richHero();
		EchoStorage storage = new EchoStorage();
		EchoCaptureTrigger.captureBossVictory(hero, 5, storage);

		Dungeon.setEchoLookup(storage);
		Dungeon.depth = 5;
		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(5)).isTrue();

		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isTrue();
		EchoBoss boss = (EchoBoss) EchoBossSpawner.createRegionalBoss(new Goo());

		Assertions.assertThat(boss.getEchoHero().heroClass).isEqualTo(HeroClass.MAGE);
		Assertions.assertThat(boss.getEchoHero().belongings.armor()).isInstanceOf(PlateArmor.class);
		Assertions.assertThat(boss.HT).isEqualTo(EchoBoss.scaledHT(boss.getEcho(), 5));
	}

	private static Hero richHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 14;
		hero.HP = 65;
		hero.HT = 70;
		hero.STR = 18;

		PlateArmor armor = new PlateArmor();
		armor.identify();
		hero.belongings.armor = armor;

		WornShortsword sword = new WornShortsword();
		sword.identify();
		hero.belongings.weapon = sword;

		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		potion.collect(hero.belongings.backpack);

		return hero;
	}
}
