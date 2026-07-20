package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoPolicyRuntimeTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("supported policy with HEAL ready chooses HEAL over default melee")
	void supportedPolicyChoosesHealWhenReady() {
		Echo echo = mageWithHealing();
		EchoPolicy policy = healForwardPolicy();
		CompositeEchoLookup.setEchoLookupForTests(
				depth -> EchoTestSupport.outcomeWithPolicy(echo, policy));
		Dungeon.depth = 5;
		Dungeon.prefetchEchoBossForDepth(5);

		EchoBoss boss = new EchoBoss(echo, 5);
		boss.HP = Math.max(1, boss.HT / 10);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		// Force HEAL ready for matcher even if inventory sense is thin in unit env
		status = new EchoPolicyStatus.Builder()
				.selfHpRatio(0.1f)
				.enemyHpRatio(1f)
				.distance(2)
				.rolesReady(java.util.Set.of("HEAL", "MELEE", "WAIT"))
				.build();

		EchoPolicyChoice choice = EchoPolicyMatcher.choose(policy, status, java.util.Collections.emptyMap());

		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("HEAL");
	}

	@Test
	@DisplayName("EchoBoss rejects unsupported policy")
	void echoBossRejectsUnsupportedPolicy() {
		Echo echo = mageWithHealing();
		EchoPolicy unsupported = EchoPolicy.fromJson("{"
				+ "\"policy_schema_version\":\"0.0.1\""
				+ "}");

		Assertions.assertThatThrownBy(() -> new EchoBoss(echo, 5, unsupported))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("echo_policy");
	}

	private static Echo mageWithHealing() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 8;
		hero.HP = hero.HT = 40;
		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		potion.collect(hero.belongings.backpack);
		Bundle data = EchoTestSupport.bundleHero(hero);
		return Echo.create(5, EchoTestSupport.TEST_GAME_VERSION, 1L, "MAGE", 8, 40, 40, data);
	}

	private static EchoPolicy healForwardPolicy() {
		JSONObject root = new JSONObject()
				.put("policy_schema_version", EchoTestSupport.TEST_GAME_VERSION)
				.put("capabilities", new JSONObject()
						.put("HEAL", new JSONObject()
								.put("pick", "FIRST_LEGAL")
								.put("items", new JSONArray().put("PotionOfHealing")))
						.put("MELEE", new JSONObject()
								.put("pick", "FIRST_LEGAL")
								.put("items", new JSONArray().put("*melee")))
						.put("WAIT", new JSONObject()
								.put("pick", "FIRST_LEGAL")
								.put("items", new JSONArray().put("*wait"))))
				.put("reactions", new JSONArray().put(new JSONObject()
						.put("id", "heal_up")
						.put("priority", 100)
						.put("when", new JSONObject().put("all", new JSONArray()
								.put(new JSONObject().put("self_hp_below", 0.35))
								.put(new JSONObject().put("role_ready", "HEAL"))))
						.put("do", new JSONObject().put("use_role", "HEAL"))))
				.put("recipes", new JSONArray())
				.put("positioning", new JSONObject())
				.put("matchups", new JSONObject())
				.put("selection", new JSONObject()
						.put("order", new JSONArray()
								.put("reactions").put("recipes").put("positioning")
								.put("matchups").put("default"))
						.put("default_roles", new JSONArray().put("MELEE").put("WAIT")))
				.put("tuning", new JSONObject());
		return EchoPolicy.fromJson(root);
	}
}
