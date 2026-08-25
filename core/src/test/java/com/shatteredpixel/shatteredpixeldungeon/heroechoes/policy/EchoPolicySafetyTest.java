package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The client-owned half of the split: rules the generated playbook cannot get a
 * vote on because they depend on the board this turn, not on the kit card.
 */
@ExtendWith(GdxTestExtension.class)
class EchoPolicySafetyTest {

	@Test
	@DisplayName("a bomb is dropped from the pick list when the echo is inside its own blast")
	void bombDroppedAtPointBlank() {
		Hero hero = heroWith(new Bomb());
		JSONObject cap = EchoTestSupport.capability("Bomb", "WandOfFireblast");

		JSONObject narrowed = EchoPolicySafety.withoutSelfBlast(cap, hero, 1);

		Assertions.assertThat(EchoRoleResolver.resolveItemId(narrowed, ids("Bomb", "WandOfFireblast")))
				.isEqualTo("WandOfFireblast");
	}

	@Test
	@DisplayName("a blast stone counts as a bomb")
	void blastStoneDroppedAtPointBlank() {
		Hero hero = heroWith(new StoneOfBlast());
		JSONObject cap = EchoTestSupport.capability("StoneOfBlast");

		JSONObject narrowed = EchoPolicySafety.withoutSelfBlast(cap, hero, 0);

		Assertions.assertThat(EchoRoleResolver.resolveItemId(narrowed, ids("StoneOfBlast"))).isNull();
	}

	@Test
	@DisplayName("the same bomb is kept once the target is outside the blast")
	void bombKeptAtRange() {
		Hero hero = heroWith(new Bomb());
		JSONObject cap = EchoTestSupport.capability("Bomb");

		JSONObject narrowed = EchoPolicySafety.withoutSelfBlast(cap, hero, 2);

		Assertions.assertThat(EchoRoleResolver.resolveItemId(narrowed, ids("Bomb"))).isEqualTo("Bomb");
	}

	@Test
	@DisplayName("non-blast kit is untouched at point blank")
	void fireToolKeptAtPointBlank() {
		Hero hero = heroWith(new WandOfFireblast());
		JSONObject cap = EchoTestSupport.capability("WandOfFireblast");

		JSONObject narrowed = EchoPolicySafety.withoutSelfBlast(cap, hero, 1);

		Assertions.assertThat(EchoRoleResolver.resolveItemId(narrowed, ids("WandOfFireblast")))
				.isEqualTo("WandOfFireblast");
	}

	@Test
	@DisplayName("hazard and spell survive the copy")
	void metadataSurvivesNarrowing() {
		Hero hero = heroWith(new Bomb());
		JSONObject cap = EchoTestSupport.capability("Bomb", "HolyTome")
				.put("hazard", EchoPolicyHazards.FIRE_AOE)
				.put("spell", "GuidingLight");

		JSONObject narrowed = EchoPolicySafety.withoutSelfBlast(cap, hero, 1);

		Assertions.assertThat(narrowed.optString("hazard")).isEqualTo(EchoPolicyHazards.FIRE_AOE);
		Assertions.assertThat(narrowed.optString("spell")).isEqualTo("GuidingLight");
		Assertions.assertThat(narrowed.optString("pick")).isEqualTo(cap.optString("pick"));
	}

	@Test
	@DisplayName("a missing capability narrows to nothing rather than throwing")
	void nullCapability() {
		Assertions.assertThat(EchoPolicySafety.withoutSelfBlast(null, null, 0)).isNull();
		Assertions.assertThat(EchoPolicySafety.withoutParalyticGas(null)).isNull();
	}

	@Test
	@DisplayName("stun items are dropped while the hero is under the stun lockout")
	void stunItemsDroppedUnderLockout() {
		JSONObject cap = EchoTestSupport.capability("PotionOfParalyticGas", "PotionOfCorrosiveGas");

		JSONObject narrowed = EchoPolicySafety.withoutParalyticGas(cap);

		Assertions.assertThat(
				EchoRoleResolver.resolveItemId(narrowed, ids("PotionOfParalyticGas", "PotionOfCorrosiveGas")))
				.isEqualTo("PotionOfCorrosiveGas");
	}

	private static Hero heroWith(com.shatteredpixel.shatteredpixeldungeon.items.Item item) {
		Hero hero = EchoTestSupport.warriorHero();
		item.collect(hero.belongings.backpack);
		return hero;
	}

	private static Set<String> ids(String... itemIds) {
		Set<String> set = new HashSet<>();
		Collections.addAll(set, itemIds);
		return set;
	}
}
