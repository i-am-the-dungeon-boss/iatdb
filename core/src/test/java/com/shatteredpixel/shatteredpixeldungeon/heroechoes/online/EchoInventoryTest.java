package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoInventoryTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("availableIds omits CloakOfShadows when charge is zero")
	void availableIdsOmitsUnchargedCloak() {
		Hero hero = rogueHero();
		CloakOfShadows cloak = hero.belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.directCharge(-100);

		Assertions.assertThat(EchoInventory.availableIds(hero)).doesNotContain("CloakOfShadows");
	}

	@Test
	@DisplayName("availableIds omits CloakOfShadows while already stealthed on the boss")
	void availableIdsOmitsCloakWhileStealthed() {
		Hero player = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		CloakOfShadows cloak = kit.belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.directCharge(2);
		Assertions.assertThat(cloak.useAs(UseContext.echo(boss))).isTrue();

		Assertions.assertThat(EchoInventory.availableIds(kit)).doesNotContain("CloakOfShadows");
	}

	@Test
	@DisplayName("availableIds keeps CloakOfShadows when restored stealth is stuck on the kit")
	void availableIdsKeepsCloakWhenStealthStuckOnKit() {
		Hero player = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		CloakOfShadows live = kit.belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(live).isNotNull();
		live.directCharge(2);
		Assertions.assertThat(live.useAs(UseContext.echo(boss))).isTrue();

		Bundle bundle = new Bundle();
		live.storeInBundle(bundle);
		live.useAs(UseContext.echo(boss)); // clear boss stealth

		CloakOfShadows restored = new CloakOfShadows();
		restored.restoreFromBundle(bundle);
		kit.belongings.artifact = restored;
		restored.activate(kit); // mid-stealth restore attaches to phantom kit

		Assertions.assertThat(EchoInventory.availableIds(kit)).contains("CloakOfShadows");
	}

	@Test
	@DisplayName("count sums quantity for a matching item id")
	void countSumsQuantityForItemId() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		potion.quantity(3);
		potion.collect(hero.belongings.backpack);

		Assertions.assertThat(EchoInventory.count(hero, "PotionOfHealing")).isEqualTo(3);
		Assertions.assertThat(EchoInventory.count(hero, "PotionOfFrost")).isZero();
	}

	@Test
	@DisplayName("countMatching sums quantities across capability item ids")
	void countMatchingSumsAcrossCapabilityIds() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		potion.quantity(2);
		potion.collect(hero.belongings.backpack);

		int n = EchoInventory.countMatching(hero, new JSONArray()
				.put("PotionOfFrost")
				.put("PotionOfHealing"));

		Assertions.assertThat(n).isEqualTo(2);
	}

	@Test
	@DisplayName("availableIds omits wands with no charges")
	void availableIdsOmitsEmptyWands() {
		Hero hero = EchoTestSupport.warriorHero();
		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.identify();
		wand.curCharges = 0;
		wand.collect(hero.belongings.backpack);
		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		potion.collect(hero.belongings.backpack);

		Assertions.assertThat(EchoInventory.availableIds(hero))
				.contains("PotionOfHealing")
				.doesNotContain("WandOfMagicMissile");
	}

	@Test
	@DisplayName("availableIds omits HornOfPlenty when charge is zero")
	void availableIdsOmitsUnchargedHorn() {
		Hero hero = EchoTestSupport.warriorHero();
		HornOfPlenty horn = new HornOfPlenty();
		horn.identify();
		horn.collect(hero.belongings.backpack);

		Assertions.assertThat(EchoInventory.availableIds(hero)).doesNotContain("HornOfPlenty");
	}

	@Test
	@DisplayName("availableIds omits EtherealChains when charge is zero")
	void availableIdsOmitsUnchargedChains() {
		Hero hero = EchoTestSupport.warriorHero();
		EtherealChains chains = new EtherealChains();
		chains.identify();
		Bundle bundle = new Bundle();
		chains.storeInBundle(bundle);
		bundle.put("charge", 0);
		chains.restoreFromBundle(bundle);
		chains.collect(hero.belongings.backpack);

		Assertions.assertThat(EchoInventory.availableIds(hero)).doesNotContain("EtherealChains");
	}

	@Test
	@DisplayName("availableIds omits WandOfWarding when it cannot sustain more wards")
	void availableIdsOmitsWandOfWardingWithoutWardCapacity() {
		Hero hero = EchoTestSupport.warriorHero();
		WandOfWarding wand = new WandOfWarding();
		wand.identify();
		wand.curCharges = 1;
		// No Wand.charge — no Charger means ward energy cap is 0.
		hero.belongings.backpack.items.add(wand);

		Assertions.assertThat(EchoInventory.availableIds(hero)).doesNotContain("WandOfWarding");
	}

	@Test
	@DisplayName("availableIds includes WandOfWarding when charged with ward capacity")
	void availableIdsIncludesChargedWandOfWarding() {
		Hero hero = EchoTestSupport.warriorHero();
		WandOfWarding wand = new WandOfWarding();
		wand.identify();
		wand.curCharges = 1;
		wand.collect(hero.belongings.backpack);

		Assertions.assertThat(EchoInventory.availableIds(hero)).contains("WandOfWarding");
	}

	@Test
	@DisplayName("availableIds omits mage staff when imbued wand has no charges")
	void availableIdsOmitsEmptyMagesStaff() {
		Hero hero = EchoTestSupport.warriorHero();
		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.identify();
		MagesStaff staff = new MagesStaff(wand);
		staff.setWandCharges(0);
		hero.belongings.weapon = staff;

		Assertions.assertThat(staff.canZap()).isFalse();
		Assertions.assertThat(EchoInventory.availableIds(hero)).doesNotContain("MagesStaff");
	}

	@Test
	@DisplayName("availableIds includes MissileWeapon when quantity is positive")
	void availableIdsIncludesMissileWithQuantity() {
		Hero hero = EchoTestSupport.warriorHero();
		ThrowingKnife knives = new ThrowingKnife();
		knives.identify();
		knives.quantity(2);
		knives.collect(hero.belongings.backpack);

		Assertions.assertThat(EchoInventory.availableIds(hero)).contains("ThrowingKnife");
	}

	@Test
	@DisplayName("availableIds omits MissileWeapon when quantity is zero")
	void availableIdsOmitsMissileWithZeroQuantity() {
		Hero hero = EchoTestSupport.warriorHero();
		ThrowingKnife knives = new ThrowingKnife();
		knives.identify();
		knives.quantity(1);
		knives.collect(hero.belongings.backpack);
		knives.quantity(0);

		Assertions.assertThat(EchoInventory.availableIds(hero)).doesNotContain("ThrowingKnife");
	}

	@Test
	@DisplayName("find returns the first item with the matching class name")
	void findReturnsMatchingItem() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		potion.collect(hero.belongings.backpack);

		Assertions.assertThat(EchoInventory.find(hero, "PotionOfHealing")).isSameAs(potion);
		Assertions.assertThat(EchoInventory.find(hero, "PotionOfFrost")).isNull();
	}

	@Test
	@DisplayName("availableIds includes equipped duelist melee weapon when Charger has charges")
	void availableIdsIncludesChargedEquippedDuelistWeapon() {
		Hero hero = duelistHero();
		Scimitar scimitar = new Scimitar();
		scimitar.identify();
		hero.belongings.weapon = scimitar;
		scimitar.activate(hero);
		hero.STR = Math.max(hero.STR(), scimitar.STRReq());
		Buff.affect(hero, MeleeWeapon.Charger.class).charges = 2;

		Assertions.assertThat(EchoInventory.availableIds(hero)).contains("Scimitar");
	}

	@Test
	@DisplayName("availableIds omits duelist melee weapon when Charger has insufficient charges")
	void availableIdsOmitsUnchargedDuelistWeapon() {
		Hero hero = duelistHero();
		Scimitar scimitar = new Scimitar();
		scimitar.identify();
		hero.belongings.weapon = scimitar;
		scimitar.activate(hero);
		hero.STR = Math.max(hero.STR(), scimitar.STRReq());
		MeleeWeapon.Charger charger = Buff.affect(hero, MeleeWeapon.Charger.class);
		charger.charges = 0;
		charger.partialCharge = 0;

		Assertions.assertThat(EchoInventory.availableIds(hero)).doesNotContain("Scimitar");
	}

	@Test
	@DisplayName("availableIds omits unequipped duelist melee weapon even with charges")
	void availableIdsOmitsUnequippedDuelistWeapon() {
		Hero hero = duelistHero();
		Scimitar scimitar = new Scimitar();
		scimitar.identify();
		scimitar.collect(hero.belongings.backpack);
		Buff.affect(hero, MeleeWeapon.Charger.class).charges = 5;

		Assertions.assertThat(EchoInventory.availableIds(hero)).doesNotContain("Scimitar");
	}

	private static Hero duelistHero() {
		Hero previous = Dungeon.hero;
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.DUELIST.initHero(hero);
		hero.lvl = 10;
		hero.HP = hero.HT = 30;
		if (previous != null) {
			Dungeon.hero = previous;
		}
		return hero;
	}

	private static Hero rogueHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.ROGUE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

}
