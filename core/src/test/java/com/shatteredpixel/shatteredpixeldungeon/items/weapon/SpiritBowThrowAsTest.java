package com.shatteredpixel.shatteredpixeldungeon.items.weapon;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Shared {@link UseContext} / {@code throwAs} seam for Hero + Echo SpiritBow.
 */
@ExtendWith(GdxTestExtension.class)
class SpiritBowThrowAsTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Hero throwAs spends turn and can hit via UseContext.hero")
	void heroThrowAsSpendsTurnAndHits() {
		Hero hero = huntressHero();
		EchoBoss target = installFight(hero);
		SpiritBow bow = hero.belongings.getItem(SpiritBow.class);
		Assertions.assertThat(bow).isNotNull();

		float cooldownBefore = hero.cooldown();
		int hpBefore = target.HP;
		hero.invisible = 1;

		boolean spent = bow.knockArrow().throwAs(UseContext.hero(hero), target.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hero.cooldown()).isGreaterThan(cooldownBefore);
		Assertions.assertThat(target.HP).isLessThan(hpBefore);
		Assertions.assertThat(hero.belongings.getItem(SpiritBow.class)).isSameAs(bow);
	}

	@Test
	@DisplayName("Echo throwAs hits via UseContext.echo without spending phantom hero time")
	void echoThrowAsHitsWithoutPhantomSpend() {
		Hero player = huntressHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, rangedBowPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		SpiritBow bow = kit.belongings.getItem(SpiritBow.class);
		Assertions.assertThat(bow).isNotNull();
		Assertions.assertThat(kit.sprite).isNull();

		float kitCooldownBefore = kit.cooldown();
		int hpBefore = player.HP;
		kit.invisible = 1;

		boolean spent = bow.knockArrow().throwAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitCooldownBefore);
		Assertions.assertThat(kit.sprite).isNull();
		Assertions.assertThat(kit.belongings.getItem(SpiritBow.class)).isSameAs(bow);
	}

	@Test
	@DisplayName("Echo throwAs returns false when there is no valid target")
	void echoThrowAsRefusesWithoutTarget() {
		Hero player = huntressHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, rangedBowPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		SpiritBow bow = boss.getEchoHero().belongings.getItem(SpiritBow.class);
		Assertions.assertThat(bow).isNotNull();

		// Empty adjacent cell — no Char there
		int empty = boss.pos + 1;
		Assertions.assertThat(com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(empty))
				.isNull();

		boolean spent = bow.knockArrow().throwAs(UseContext.echo(boss), empty);

		Assertions.assertThat(spent).isFalse();
	}

	private static Hero huntressHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.HUNTRESS.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static EchoBoss installFight(Hero hero) {
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);
		return target;
	}

	private static com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy rangedBowPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "MAX_DAMAGE")
						.put("items", new JSONArray().put("SpiritBow")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}
}
