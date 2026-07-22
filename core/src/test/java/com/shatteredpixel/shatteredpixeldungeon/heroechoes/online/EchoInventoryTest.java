package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoInventoryTest {

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

}
