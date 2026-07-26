package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfFear;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ExtendWith(GdxTestExtension.class)
class DebugStrategyKitTest {

	@Test
	@DisplayName("strategy kit is a small balanced loadout for kite/shield/finish tests")
	void strategyKitIsBalancedAndCompact() {
		List<Item> items = DebugStrategyKit.createItems();

		Assertions.assertThat(items.size()).isLessThan(20);
		Assertions.assertThat(items.size()).isGreaterThanOrEqualTo(10);

		Set<Class<?>> classes = new HashSet<>();
		for (Item item : items) {
			classes.add(item.getClass());
		}
		Assertions.assertThat(classes).contains(
				PotionOfHealing.class,
				PotionOfShielding.class,
				PotionOfHaste.class,
				PotionOfInvisibility.class,
				PotionOfParalyticGas.class,
				PotionOfLiquidFlame.class,
				PotionOfCorrosiveGas.class,
				StoneOfBlink.class,
				StoneOfFear.class,
				WandOfFireblast.class,
				WandOfMagicMissile.class,
				WandOfBlastWave.class);
	}

	@Test
	@DisplayName("strategy policy arms finish, heal, blink, kite, and ranged roles")
	void strategyPolicyArmsKeyRoles() {
		EchoPolicy policy = DebugStrategyKit.policy();
		JSONObject caps = policy.root().getJSONObject("capabilities");

		Assertions.assertThat(caps.has("FINISHER")).isTrue();
		Assertions.assertThat(caps.has("HEAL")).isTrue();
		Assertions.assertThat(caps.has("BLINK")).isTrue();
		Assertions.assertThat(caps.has("RANGED")).isTrue();
		Assertions.assertThat(caps.has("KNOCKBACK")).isTrue();
		Assertions.assertThat(caps.has("SETUP_CC")).isTrue();
		Assertions.assertThat(caps.has("PAYOFF_AOE")).isTrue();

		String reactions = policy.root().getJSONArray("reactions").toString();
		Assertions.assertThat(reactions).contains("finish_him");
		Assertions.assertThat(reactions).contains("enemy_shield_below");
		Assertions.assertThat(reactions).contains("blink_escape");
		Assertions.assertThat(reactions).contains("kite_step");
		Assertions.assertThat(reactions).contains("ranged_poke");
	}
}
