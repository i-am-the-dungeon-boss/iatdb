package com.shatteredpixel.shatteredpixeldungeon.heroechoes.debug;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoInventory;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(GdxTestExtension.class)
class DebugClericSpellsTest {

	private static EchoBoss arenaBoss() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		return boss;
	}

	private static List<String> recipeRoles(EchoPolicy policy) {
		JSONArray recipes = policy.root().optJSONArray("recipes");
		Assertions.assertThat(recipes.length()).isEqualTo(1);
		JSONArray steps = recipes.optJSONObject(0).optJSONArray("steps");
		List<String> roles = new ArrayList<>();
		for (int i = 0; i < steps.length(); i++) {
			roles.add(steps.optJSONObject(i).optJSONObject("do").optString("use_role"));
		}
		return roles;
	}

	@Test
	@DisplayName("grantTomeSpells equips a charged HolyTome the policy can see")
	void grantTomeSpellsEquipsChargedTome() {
		EchoBoss boss = arenaBoss();

		DebugClericSpells.grantTomeSpells(boss, HeroSubClass.PRIEST);

		Hero kit = boss.getEchoHero();
		Assertions.assertThat(kit.belongings.artifact).isInstanceOf(HolyTome.class);
		HolyTome tome = (HolyTome) kit.belongings.artifact;
		Assertions.assertThat(tome.isEquipped(kit)).isTrue();
		Assertions.assertThat(EchoInventory.availableIds(kit)).contains("HolyTome");
	}

	@Test
	@DisplayName("grantTomeSpells installs one recipe step per castable spell")
	void grantTomeSpellsInstallsOneStepPerCastableSpell() {
		EchoBoss boss = arenaBoss();

		DebugClericSpells.grantTomeSpells(boss, HeroSubClass.PRIEST);

		EchoPolicy policy = boss.getEchoPolicy();
		List<String> roles = recipeRoles(policy);
		Assertions.assertThat(roles).hasSizeGreaterThan(10);
		Assertions.assertThat(roles).doesNotHaveDuplicates();

		JSONObject caps = policy.root().optJSONObject("capabilities");
		Hero kit = boss.getEchoHero();
		for (String role : roles) {
			JSONObject cap = caps.optJSONObject(role);
			Assertions.assertThat(cap).as(role).isNotNull();
			Assertions.assertThat(cap.optJSONArray("items").optString(0)).isEqualTo("HolyTome");
			ClericSpell spell = ClericSpell.bySimpleName(cap.optString("spell"));
			Assertions.assertThat(spell).as(role).isNotNull();
			Assertions.assertThat(spell.canCast(kit)).as(role + " castable").isTrue();
		}
	}

	@Test
	@DisplayName("grantTomeSpells only lists spells the echo cleric handlers implement")
	void grantTomeSpellsOnlyListsHandledSpells() {
		EchoBoss boss = arenaBoss();

		DebugClericSpells.grantTomeSpells(boss, HeroSubClass.PRIEST);

		JSONObject caps = boss.getEchoPolicy().root().optJSONObject("capabilities");
		for (String role : recipeRoles(boss.getEchoPolicy())) {
			String name = caps.optJSONObject(role).optString("spell");
			Assertions.assertThat(DebugClericSpells.handledSpells())
					.as(name)
					.anyMatch(s -> s.getClass().getSimpleName().equals(name));
		}
	}

	@Test
	@DisplayName("PRIEST gets Radiance, PALADIN gets Smite")
	void subclassDecidesSubclassOnlySpells() {
		EchoBoss priest = arenaBoss();
		DebugClericSpells.grantTomeSpells(priest, HeroSubClass.PRIEST);
		EchoBoss paladin = arenaBoss();
		DebugClericSpells.grantTomeSpells(paladin, HeroSubClass.PALADIN);

		Assertions.assertThat(spellNames(priest)).contains("Radiance").doesNotContain("Smite");
		Assertions.assertThat(spellNames(paladin)).contains("Smite").doesNotContain("Radiance");
	}

	private static List<String> spellNames(EchoBoss boss) {
		JSONObject caps = boss.getEchoPolicy().root().optJSONObject("capabilities");
		List<String> names = new ArrayList<>();
		for (String role : recipeRoles(boss.getEchoPolicy())) {
			names.add(caps.optJSONObject(role).optString("spell"));
		}
		return names;
	}

	@Test
	@DisplayName("the tome recipe runs before default roles so every spell gets a turn")
	void recipeLayerRunsBeforeDefaults() {
		EchoBoss boss = arenaBoss();

		DebugClericSpells.grantTomeSpells(boss, HeroSubClass.PRIEST);

		JSONArray order = boss.getEchoPolicy().root().optJSONObject("selection").optJSONArray("order");
		List<String> layers = new ArrayList<>();
		for (int i = 0; i < order.length(); i++) {
			layers.add(order.optString(i));
		}
		Assertions.assertThat(layers).containsSubsequence("recipes", "default");
	}
}
