package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
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
 * Reproduces Sentry NPEs when Lightning VFX touches a headless Echo kit or
 * target
 * ({@code ANDROID-1T} onHit, {@code ANDROID-1N} onZap).
 */
@ExtendWith(GdxTestExtension.class)
class WandOfLightningTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("onHit applies LightningCharge when attacker sprite is null")
	void onHitAppliesChargeWhenAttackerSpriteNull() {
		Hero player = mageHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 1);

		Hero kit = boss.getEchoHero();
		Assertions.assertThat(kit.sprite).isNull();

		MagesStaff staff = new MagesStaff(new WandOfLightning());
		WandOfLightning wand = (WandOfLightning) staff.wand();
		Assertions.assertThat(wand).isNotNull();

		boolean charged = false;
		for (int i = 0; i < 200; i++) {
			Assertions.assertThatCode(() -> wand.onHit(staff, kit, player, 10))
					.doesNotThrowAnyException();
			if (kit.buff(WandOfLightning.LightningCharge.class) != null) {
				charged = true;
				break;
			}
		}

		Assertions.assertThat(charged)
				.as("staff lightning proc must land within 200 rolls")
				.isTrue();
	}

	@Test
	@DisplayName("onZap damages when an affected target sprite is null")
	void onZapDamagesWhenAffectedSpriteNull() {
		Hero player = mageHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, lightningPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		Hero kit = boss.getEchoHero();
		WandOfLightning wand = new WandOfLightning();
		wand.curCharges = 3;
		wand.setCurrent(kit);
		// Borrow body sprite so fx can build affected arcs (Echo zapAs borrow).
		kit.sprite = boss.sprite;

		Ballistica shot = new Ballistica(boss.pos, player.pos, Ballistica.MAGIC_BOLT);
		Assertions.assertThatCode(() -> wand.fx(shot, () -> {
		})).doesNotThrowAnyException();

		player.sprite = null;
		int hpBefore = player.HP;

		Assertions.assertThatCode(() -> wand.onZap(shot)).doesNotThrowAnyException();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("fx skips Lightning VFX but still callbacks when curUser has no world fx")
	void fxSkipsLightningWhenCurUserHasNoWorldFx() {
		Hero player = mageHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Assertions.assertThat(kit.sprite).isNull();

		WandOfLightning wand = new WandOfLightning();
		wand.setCurrent(kit);
		Ballistica bolt = new Ballistica(boss.pos, player.pos, Ballistica.MAGIC_BOLT);
		boolean[] called = { false };

		Assertions.assertThatCode(() -> wand.fx(bolt, () -> called[0] = true))
				.doesNotThrowAnyException();
		Assertions.assertThat(called[0]).isTrue();
	}

	@Test
	@DisplayName("Echo zapAs damages the player without NPE on headless kit")
	void echoZapAsDamagesPlayerWithoutNpe() {
		Hero player = mageHero();
		WandOfLightning seed = new WandOfLightning();
		seed.curCharges = 3;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, lightningPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Assertions.assertThat(boss.getEchoHero().sprite).isNull();

		Wand wand = boss.getEchoHero().belongings.getItem(WandOfLightning.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 3;
		int hpBefore = player.HP;
		player.invisible = 1;

		Assertions.assertThatCode(() -> wand.zapAs(UseContext.echo(boss), player.pos))
				.doesNotThrowAnyException();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
		Assertions.assertThat(boss.getEchoHero().sprite).isNull();
	}

	@Test
	@DisplayName("Echo zapAs with body parent fires without NPE and damages")
	void echoZapAsWithBodyParentDamagesWithoutNpe() {
		Hero player = mageHero();
		WandOfLightning seed = new WandOfLightning();
		seed.curCharges = 3;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, lightningPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		Wand wand = boss.getEchoHero().belongings.getItem(WandOfLightning.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 3;
		int hpBefore = player.HP;
		player.invisible = 1;

		Assertions.assertThatCode(() -> wand.zapAs(UseContext.echo(boss), player.pos))
				.doesNotThrowAnyException();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	private static Hero mageHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static EchoPolicy lightningPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("WandOfLightning")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}
}
