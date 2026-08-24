package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The Java half of the "break invisibility with a strike, not a wand"
 * preference. The backend keys its reactions on the echo's own
 * {@code invisible} self status and on a bow-only {@code SURPRISE_SHOT} role,
 * so both have to be things Java actually emits and can execute.
 */
@ExtendWith(GdxTestExtension.class)
class EchoInvisibleSurpriseTest {

	@Test
	@DisplayName("an invisible echo reports invisible as a self status, not only the hero")
	void invisibleEchoSensesItsOwnStatus() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = surprisePolicy();
		EchoBoss boss = surpriseBoss(hero, policy);
		Buff.affect(boss, Invisibility.class, 20f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);

		Assertions.assertThat(boss.invisible).isGreaterThan(0);
		Assertions.assertThat(status.selfStatuses).contains("invisible");
	}

	@Test
	@DisplayName("a cloaked echo next to the hero picks the melee strike over its wand")
	void cloakedEchoStrikesInsteadOfZapping() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = surprisePolicy();
		EchoBoss boss = surpriseBoss(hero, policy);
		giveEchoItem(boss, new WandOfMagicMissile());
		Buff.affect(boss, Invisibility.class, 20f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		EchoPlan choice = EchoPolicyMatcher.choose(policy, status, new java.util.HashMap<>());

		Assertions.assertThat(status.isRoleReady("RANGED")).isTrue();
		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("MELEE");
	}

	@Test
	@DisplayName("a visible echo goes back to its wand: the preference is invisibility-only")
	void visibleEchoStillUsesItsWand() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = surprisePolicy();
		EchoBoss boss = surpriseBoss(hero, policy);
		giveEchoItem(boss, new WandOfMagicMissile());

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		EchoPlan choice = EchoPolicyMatcher.choose(policy, status, new java.util.HashMap<>());

		Assertions.assertThat(status.selfStatuses).doesNotContain("invisible");
		Assertions.assertThat(choice).isNotNull();
		Assertions.assertThat(choice.useRole).isEqualTo("RANGED");
	}

	@Test
	@DisplayName("a bow-only SURPRISE_SHOT role resolves to the bow and spends the turn")
	void surpriseShotFiresTheBow() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = surprisePolicy();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		// Offset 2: inside Level.insideMap on the 7x7 fixture (offset 3 is the border).
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		EchoTestSupport.attachInstantProjectileParent(boss);
		giveEchoItem(boss, new com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow());
		Buff.affect(boss, Invisibility.class, 20f);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, policy);
		boolean spent = EchoRoleExecutor.execute(boss, policy, status,
				new EchoPlan("SURPRISE_SHOT", "reactions", null));

		Assertions.assertThat(status.isRoleReady("SURPRISE_SHOT")).isTrue();
		Assertions.assertThat(spent).isTrue();
	}

	/**
	 * Mirrors the generated shape: a wand-first RANGED role, a bow-only
	 * SURPRISE_SHOT, and the surprise block outranking the ordinary attacks.
	 */
	private static EchoPolicy surprisePolicy() {
		JSONObject invisible = new JSONObject().put("self_status", "invisible");
		return EchoPolicy.fromJson(new JSONObject()
				.put("policy_schema_version", EchoTestSupport.TEST_GAME_VERSION)
				.put("capabilities", new JSONObject()
						.put("MELEE", EchoTestSupport.capability("*melee"))
						.put("CLOSE_IN", EchoTestSupport.capability("*move_closer"))
						.put("RANGED", EchoTestSupport.capability("WandOfMagicMissile"))
						.put("SURPRISE_SHOT", EchoTestSupport.capability("SpiritBow")))
				.put("reactions", new JSONArray()
						.put(reaction("invis_surprise_melee", 93, new JSONObject()
								.put("all", new JSONArray()
										.put(invisible)
										.put(new JSONObject().put("distance_lte", 1))
										.put(new JSONObject().put("role_ready", "MELEE"))),
								"MELEE", null))
						.put(reaction("invis_surprise_close", 91, new JSONObject()
								.put("all", new JSONArray()
										.put(invisible)
										.put(new JSONObject().put("distance_gte", 2))
										.put(new JSONObject().put("role_ready", "CLOSE_IN"))),
								"CLOSE_IN", null))
						.put(reaction("ranged_poke", 74, new JSONObject(), "RANGED", "enemy_cell")))
				.put("recipes", new JSONArray())
				.put("positioning", new JSONObject())
				.put("matchups", new JSONObject())
				.put("selection", new JSONObject()
						.put("order", new JSONArray().put("reactions").put("default"))
						.put("default_roles", new JSONArray().put("MELEE")))
				.put("tuning", new JSONObject()));
	}

	private static EchoBoss surpriseBoss(Hero hero, EchoPolicy policy) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		fillFov(boss);
		return boss;
	}

	private static void fillFov(EchoBoss boss) {
		boss.fieldOfView =
				new boolean[com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.length()];
		java.util.Arrays.fill(boss.fieldOfView, true);
	}

	private static JSONObject reaction(
			String id, int priority, JSONObject when, String useRole, String target) {
		JSONObject dof = new JSONObject().put("use_role", useRole);
		if (target != null) {
			dof.put("target", target);
		}
		return new JSONObject()
				.put("id", id)
				.put("priority", priority)
				.put("when", when)
				.put("do", dof);
	}

	private static void giveEchoItem(EchoBoss boss, Item item) {
		item.identify();
		item.collect(boss.getEchoHero().belongings.backpack);
	}
}
