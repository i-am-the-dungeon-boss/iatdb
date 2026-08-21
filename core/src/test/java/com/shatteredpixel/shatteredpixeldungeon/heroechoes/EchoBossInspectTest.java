package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greataxe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.EchoKitSlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemSlot;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEchoBossInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoBossInspectTest {

	@BeforeEach
	void reset() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("hasInspectableKit is true for an EchoBoss with a restored hero")
	void hasInspectableKitWhenEchoHeroPresent() {
		EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(5), 5);

		Assertions.assertThat(WndEchoBossInfo.hasInspectableKit(boss)).isTrue();
	}

	@Test
	@DisplayName("hasInspectableKit is false for a plain Mob and for null")
	void hasInspectableKitFalseForPlainMobAndNull() {
		Assertions.assertThat(WndEchoBossInfo.hasInspectableKit(new Goo())).isFalse();
		Assertions.assertThat(WndEchoBossInfo.hasInspectableKit(null)).isFalse();
	}

	@Test
	@DisplayName("hasInspectableKit accepts any EchoInspectable, not only an EchoBoss")
	void hasInspectableKitAcceptsAnyEchoInspectable() {
		Hero echoHero = EchoTestSupport.warriorHero();

		Assertions.assertThat(WndEchoBossInfo.hasInspectableKit(new StubEchoMob(echoHero))).isTrue();
	}

	@Test
	@DisplayName("hasInspectableKit is false for an EchoInspectable whose hero has not arrived")
	void hasInspectableKitFalseWhenEchoHeroMissing() {
		Assertions.assertThat(WndEchoBossInfo.hasInspectableKit(new StubEchoMob(null))).isFalse();
	}

	@Test
	@DisplayName("statRows reads any EchoInspectable's own health, not the boss class'")
	void statRowsReadsAnyEchoInspectable() {
		Hero echoHero = EchoTestSupport.warriorHero();
		echoHero.STR = 14;
		StubEchoMob body = new StubEchoMob(echoHero);
		body.HP = 3;
		body.HT = 21;

		String[][] rows = WndEchoBossInfo.statRows(body);

		Assertions.assertThat(values(rows)).contains("14", "3/21");
	}

	/** A non-boss echo body, standing in for the village figures. */
	private static class StubEchoMob extends Mob implements EchoInspectable {
		private final Hero echoHero;

		StubEchoMob(Hero echoHero) {
			this.echoHero = echoHero;
			this.HP = this.HT = 10;
		}

		@Override
		public Hero getEchoHero() {
			return echoHero;
		}

		@Override
		public Echo getEcho() {
			return null;
		}
	}

	@Test
	@DisplayName("equippedKit returns weapon / armor / artifact / misc / ring in that order, with a placeholder for each empty slot")
	void equippedKitOrderAndPlaceholders() {
		Hero echoHero = new Hero();
		WornShortsword weapon = new WornShortsword();
		ClothArmor armor = new ClothArmor();
		echoHero.belongings.weapon = weapon;
		echoHero.belongings.armor = armor;

		Item[] kit = WndEchoBossInfo.equippedKit(echoHero);

		Assertions.assertThat(kit).hasSize(5);
		Assertions.assertThat(kit[0]).isSameAs(weapon);
		Assertions.assertThat(kit[1]).isSameAs(armor);
		assertPlaceholder(kit[2], ItemSpriteSheet.ARTIFACT_HOLDER);
		assertPlaceholder(kit[3], ItemSpriteSheet.SOMETHING);
		assertPlaceholder(kit[4], ItemSpriteSheet.RING_HOLDER);
	}

	@Test
	@DisplayName("equippedKit appends secondWep only when the echo has one")
	void equippedKitAppendsSecondWepOnlyWhenPresent() {
		Hero withoutSecond = new Hero();
		Assertions.assertThat(WndEchoBossInfo.equippedKit(withoutSecond)).hasSize(5);

		Hero withSecond = new Hero();
		WornShortsword second = new WornShortsword();
		withSecond.belongings.secondWep = second;

		Item[] kit = WndEchoBossInfo.equippedKit(withSecond);
		Assertions.assertThat(kit).hasSize(6);
		Assertions.assertThat(kit[5]).isSameAs(second);
	}

	@Test
	@DisplayName("equippedKit reads the echo, not the player")
	void equippedKitReadsEchoNotPlayer() {
		Hero echoHero = new Hero();
		WornShortsword echoWeapon = new WornShortsword();
		ClothArmor echoArmor = new ClothArmor();
		echoHero.belongings.weapon = echoWeapon;
		echoHero.belongings.armor = echoArmor;

		Hero player = EchoTestSupport.warriorHero();
		Greataxe playerWeapon = new Greataxe();
		player.belongings.weapon = playerWeapon;
		player.belongings.armor = null;

		Item[] kit = WndEchoBossInfo.equippedKit(echoHero);

		Assertions.assertThat(kit[0]).isSameAs(echoWeapon);
		Assertions.assertThat(kit[1]).isSameAs(echoArmor);
		Assertions.assertThat(kit[0]).isNotSameAs(playerWeapon);
	}

	@Test
	@DisplayName("equippedKit does not disturb the player")
	void equippedKitDoesNotDisturbPlayer() {
		Hero player = EchoTestSupport.warriorHero();
		WornShortsword playerWeapon = new WornShortsword();
		ClothArmor playerArmor = new ClothArmor();
		player.belongings.weapon = playerWeapon;
		player.belongings.armor = playerArmor;
		Item playerArtifact = player.belongings.artifact;
		Item playerMisc = player.belongings.misc;
		Item playerRing = player.belongings.ring;
		Item playerSecond = player.belongings.secondWep;

		Hero echoHero = new Hero();
		echoHero.belongings.weapon = new Greataxe();

		WndEchoBossInfo.equippedKit(echoHero);

		Assertions.assertThat(Dungeon.hero).isSameAs(player);
		Assertions.assertThat(player.belongings.weapon).isSameAs(playerWeapon);
		Assertions.assertThat(player.belongings.armor).isSameAs(playerArmor);
		Assertions.assertThat(player.belongings.artifact).isSameAs(playerArtifact);
		Assertions.assertThat(player.belongings.misc).isSameAs(playerMisc);
		Assertions.assertThat(player.belongings.ring).isSameAs(playerRing);
		Assertions.assertThat(player.belongings.secondWep).isSameAs(playerSecond);
	}

	@Test
	@DisplayName("statRows lists echo STR, boss health, and echo exp — not player gold")
	void statRowsListsEchoStatsNotPlayerGold() {
		Hero kit = EchoTestSupport.warriorHero();
		kit.STR = 16;
		kit.exp = 7;
		EchoBoss boss = echoBossFrom(kit);

		Hero player = EchoTestSupport.warriorHero();
		player.STR = 8;
		player.HP = player.HT = 20;
		Statistics.goldCollected = 9999;

		String[][] rows = WndEchoBossInfo.statRows(boss);
		Assertions.assertThat(values(rows)).contains(
				"16",
				boss.HP + "/" + boss.HT,
				kit.exp + "/" + boss.getEchoHero().maxExp());
		Assertions.assertThat(values(rows)).doesNotContain("9999", "8", "20/20");
		Assertions.assertThat(labels(rows)).doesNotContain("Gold Collected");
	}

	@Test
	@DisplayName("statRows includes subclass when the echo has one")
	void statRowsIncludesSubclassWhenPresent() {
		Hero kit = EchoTestSupport.warriorHero();
		kit.subClass = HeroSubClass.BERSERKER;
		EchoBoss boss = echoBossFrom(kit);

		String[][] rows = WndEchoBossInfo.statRows(boss);

		Assertions.assertThat(values(rows)).contains(HeroSubClass.BERSERKER.title());
	}

	@Test
	@DisplayName("statRows does not disturb the player")
	void statRowsDoesNotDisturbPlayer() {
		Hero kit = EchoTestSupport.warriorHero();
		kit.STR = 16;
		EchoBoss boss = echoBossFrom(kit);

		Hero player = EchoTestSupport.warriorHero();
		int playerStr = player.STR;
		Statistics.goldCollected = 42;

		WndEchoBossInfo.statRows(boss);

		Assertions.assertThat(Dungeon.hero).isSameAs(player);
		Assertions.assertThat(player.STR).isEqualTo(playerStr);
		Assertions.assertThat(Statistics.goldCollected).isEqualTo(42);
	}

	@Test
	@DisplayName("talentTiersToShow follows the echo's level and subclass, not the player's")
	void talentTiersToShowFollowsEchoNotPlayer() {
		Hero echoHero = EchoTestSupport.warriorHero();
		echoHero.lvl = 1;
		Hero player = EchoTestSupport.warriorHero();
		player.lvl = 30;

		Assertions.assertThat(WndEchoBossInfo.talentTiersToShow(echoHero)).isEqualTo(1);

		echoHero.lvl = 6;
		Assertions.assertThat(WndEchoBossInfo.talentTiersToShow(echoHero)).isEqualTo(2);

		echoHero.lvl = 20;
		echoHero.subClass = HeroSubClass.NONE;
		Assertions.assertThat(WndEchoBossInfo.talentTiersToShow(echoHero)).isEqualTo(2);

		echoHero.subClass = HeroSubClass.BERSERKER;
		Assertions.assertThat(WndEchoBossInfo.talentTiersToShow(echoHero)).isEqualTo(3);
	}

	@Test
	@DisplayName("strengthColor is owner-driven even when the player has the opposite STR")
	void strengthColorUsesOwnerNotPlayer() {
		Greataxe axe = new Greataxe();
		Hero owner = new Hero();
		Hero player = new Hero();
		Dungeon.hero = player;

		owner.STR = 20;
		player.STR = 10;
		Assertions.assertThat(EchoKitSlot.strengthColor(axe, owner))
				.isEqualTo(EchoKitSlot.NO_TINT);

		owner.STR = 10;
		player.STR = 20;
		Assertions.assertThat(EchoKitSlot.strengthColor(axe, owner))
				.isEqualTo(ItemSlot.DEGRADED);
	}

	private static EchoBoss echoBossFrom(Hero kit) {
		Echo echo = Echo.create(
				5,
				EchoTestSupport.TEST_GAME_VERSION,
				1L,
				kit.heroClass.name(),
				kit.lvl,
				kit.HP,
				kit.HT,
				EchoTestSupport.bundleHero(kit));
		return EchoTestSupport.createBoss(echo, 5);
	}

	private static String[] labels(String[][] rows) {
		String[] out = new String[rows.length];
		for (int i = 0; i < rows.length; i++) {
			out[i] = rows[i][0];
		}
		return out;
	}

	private static String[] values(String[][] rows) {
		String[] out = new String[rows.length];
		for (int i = 0; i < rows.length; i++) {
			out[i] = rows[i][1];
		}
		return out;
	}

	private static void assertPlaceholder(Item item, int image) {
		Assertions.assertThat(item).isInstanceOf(WndBag.Placeholder.class);
		Assertions.assertThat(item.image()).isEqualTo(image);
	}
}
