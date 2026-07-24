package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.BlizzardBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlast;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(GdxTestExtension.class)
class DebugEchoArsenalPotionRolesTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("healing potions are classified as drink, gas potions and brews as throw")
	void classifiesDrinkAndThrowPotionsByType() {
		Assertions.assertThat(DebugEchoArsenal.isThrowPotion(new PotionOfLiquidFlame())).isTrue();
		Assertions.assertThat(DebugEchoArsenal.isThrowPotion(new BlizzardBrew())).isTrue();
		Assertions.assertThat(DebugEchoArsenal.isThrowPotion(new PotionOfHealing())).isFalse();
		Assertions.assertThat(DebugEchoArsenal.isDrinkPotion(new PotionOfHealing())).isTrue();
		Assertions.assertThat(DebugEchoArsenal.isDrinkPotion(new PotionOfLiquidFlame())).isFalse();
	}

	@Test
	@DisplayName("cycle policy puts drink and throw potions in separate roles")
	void cyclePolicySplitsDrinkAndThrowPotionRoles() {
		List<Item> items = new ArrayList<>();
		items.add(new PotionOfHealing());
		items.add(new PotionOfLiquidFlame());
		items.add(new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile());

		JSONObject root = DebugEchoArsenal.cyclePolicy(items).root();
		JSONObject caps = root.getJSONObject("capabilities");

		Assertions.assertThat(caps.has(DebugEchoArsenal.ROLE_DRINK)).isTrue();
		Assertions.assertThat(caps.has(DebugEchoArsenal.ROLE_THROW)).isTrue();
		Assertions.assertThat(caps.has(DebugEchoArsenal.ROLE)).isTrue();

		JSONArray drink = caps.getJSONObject(DebugEchoArsenal.ROLE_DRINK).getJSONArray("items");
		JSONArray throwItems = caps.getJSONObject(DebugEchoArsenal.ROLE_THROW).getJSONArray("items");
		JSONArray other = caps.getJSONObject(DebugEchoArsenal.ROLE).getJSONArray("items");

		Assertions.assertThat(jsonStrings(drink)).contains("PotionOfHealing");
		Assertions.assertThat(jsonStrings(throwItems)).contains("PotionOfLiquidFlame");
		Assertions.assertThat(jsonStrings(other)).contains("WandOfMagicMissile");
		Assertions.assertThat(jsonStrings(drink)).doesNotContain("PotionOfLiquidFlame");
		Assertions.assertThat(jsonStrings(throwItems)).doesNotContain("PotionOfHealing");
	}

	@Test
	@DisplayName("cycle policy puts bombs, missiles, and throwable stones on THROW")
	void cyclePolicyPutsNonPotionThrowablesOnThrowRole() {
		List<Item> items = new ArrayList<>();
		items.add(new Bomb());
		items.add(new ThrowingStone());
		items.add(new StoneOfBlast());
		items.add(new com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile());
		items.add(new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify());

		JSONObject caps = DebugEchoArsenal.cyclePolicy(items).root().getJSONObject("capabilities");
		JSONArray throwItems = caps.getJSONObject(DebugEchoArsenal.ROLE_THROW).getJSONArray("items");
		JSONArray other = caps.getJSONObject(DebugEchoArsenal.ROLE).getJSONArray("items");

		Assertions.assertThat(jsonStrings(throwItems))
				.containsExactlyInAnyOrder("Bomb", "ThrowingStone", "StoneOfBlast");
		Assertions.assertThat(jsonStrings(other))
				.containsExactlyInAnyOrder("WandOfMagicMissile", "ScrollOfIdentify");
	}

	@Test
	@DisplayName("cycle policy THROW role has no AOE hazard so point throwables aim at the hero")
	void cyclePolicyThrowRoleHasNoAoeHazard() {
		List<Item> items = new ArrayList<>();
		items.add(new ThrowingStone());
		items.add(new PotionOfLiquidFlame());
		items.add(new StoneOfBlast());

		JSONObject throwCap = DebugEchoArsenal.cyclePolicy(items).root()
				.getJSONObject("capabilities")
				.getJSONObject(DebugEchoArsenal.ROLE_THROW);

		Assertions.assertThat(throwCap.optString("hazard", "")).isEmpty();
	}

	private static List<String> jsonStrings(JSONArray arr) {
		List<String> out = new ArrayList<>();
		for (int i = 0; i < arr.length(); i++) {
			out.add(arr.getString(i));
		}
		return out;
	}
}
