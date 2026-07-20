package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
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
	@DisplayName("find returns the first item with the matching class name")
	void findReturnsMatchingItem() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		potion.collect(hero.belongings.backpack);

		Assertions.assertThat(EchoInventory.find(hero, "PotionOfHealing")).isSameAs(potion);
		Assertions.assertThat(EchoInventory.find(hero, "PotionOfFrost")).isNull();
	}

}
