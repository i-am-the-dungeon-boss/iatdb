package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class WandZapAsTest {

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
	@DisplayName("Hero zapAs spends a charge and the hero turn")
	void heroZapAsSpendsChargeAndTurn() {
		Hero hero = mageHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		MagesStaff staff = hero.belongings.getItem(MagesStaff.class);
		Assertions.assertThat(staff).isNotNull();
		Wand wand = staff.wand();
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 3;
		float before = hero.cooldown();
		int chargesBefore = wand.curCharges;

		boolean ok = wand.zapAs(UseContext.hero(hero), target.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(wand.curCharges).isEqualTo(chargesBefore - 1);
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
	}

	@Test
	@DisplayName("Echo zapAs spends charges without phantom hero turn")
	void echoZapAsSpendsChargesWithoutPhantomTurn() {
		Hero player = mageHero();
		WandOfMagicMissile seed = new WandOfMagicMissile();
		seed.curCharges = 3;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, wandPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Wand wand = kit.belongings.getItem(WandOfMagicMissile.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 3;
		float kitBefore = kit.cooldown();
		int hpBefore = player.HP;

		boolean ok = wand.zapAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(wand.curCharges).isEqualTo(2);
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
		Assertions.assertThat(player.HP).isLessThanOrEqualTo(hpBefore);
	}

	@Test
	@DisplayName("Hero zapAs dispels Invisibility on the hero")
	void heroZapAsDispelsInvisibility() {
		Hero hero = mageHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		MagesStaff staff = hero.belongings.getItem(MagesStaff.class);
		Assertions.assertThat(staff).isNotNull();
		Wand wand = staff.wand();
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 3;

		Buff.affect(hero, Invisibility.class, Invisibility.DURATION);
		Buff.affect(target, Invisibility.class, Invisibility.DURATION);

		boolean ok = wand.zapAs(UseContext.hero(hero), target.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(hero.buff(Invisibility.class)).isNull();
		Assertions.assertThat(target.buff(Invisibility.class)).isNotNull();
	}

	@Test
	@DisplayName("Echo zapAs dispels Invisibility on the boss body")
	void echoZapAsDispelsBossInvisibility() {
		Hero player = mageHero();
		WandOfMagicMissile seed = new WandOfMagicMissile();
		seed.curCharges = 3;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, wandPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Wand wand = boss.getEchoHero().belongings.getItem(WandOfMagicMissile.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 3;

		Buff.affect(boss, Invisibility.class, Invisibility.DURATION);
		Buff.affect(player, Invisibility.class, Invisibility.DURATION);

		boolean ok = wand.zapAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(Invisibility.class)).isNull();
		Assertions.assertThat(player.buff(Invisibility.class)).isNotNull();
	}

	@Test
	@DisplayName("Echo zapAs damages the player on a real linked-sprite hit path")
	void echoZapAsDamagesPlayerWithLinkedSprites() {
		Hero player = mageHero();
		WandOfMagicMissile seed = new WandOfMagicMissile();
		seed.curCharges = 3;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, wandPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Assertions.assertThat(player.sprite.ch).isSameAs(player);
		Assertions.assertThat(boss.sprite.ch).isSameAs(boss);
		Assertions.assertThat(boss.getEchoHero().sprite).isNull();

		Wand wand = boss.getEchoHero().belongings.getItem(WandOfMagicMissile.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 3;
		int hpBefore = player.HP;
		player.invisible = 1;

		boolean ok = wand.zapAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
		Assertions.assertThat(boss.getEchoHero().sprite).isNull();
	}

	@Test
	@DisplayName("Echo zapAs fires MagicMissile VFX when the body sprite has a parent")
	void echoZapAsFiresMagicMissileWhenSpriteHasParent() {
		Hero player = mageHero();
		WandOfMagicMissile seed = new WandOfMagicMissile();
		seed.curCharges = 3;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, wandPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(boss);
		Wand wand = boss.getEchoHero().belongings.getItem(WandOfMagicMissile.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 3;
		int hpBefore = player.HP;
		player.invisible = 1;

		boolean ok = wand.zapAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(fx.magicMissileRecycles).isGreaterThan(0);
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo zapAs refuses when wand has no charges")
	void echoZapAsRefusesEmptyWand() {
		Hero player = mageHero();
		WandOfMagicMissile seed = new WandOfMagicMissile();
		seed.curCharges = 0;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, wandPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Wand wand = boss.getEchoHero().belongings.getItem(WandOfMagicMissile.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 0;

		Assertions.assertThat(wand.zapAs(UseContext.echo(boss), player.pos)).isFalse();
	}

	@Test
	@DisplayName("Hero MagesStaff zapAs spends imbued wand charge and the hero turn")
	void heroStaffZapAsSpendsChargeAndTurn() {
		Hero hero = mageHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		MagesStaff staff = hero.belongings.getItem(MagesStaff.class);
		Assertions.assertThat(staff).isNotNull();
		Assertions.assertThat(staff.wand()).isNotNull();
		staff.setWandCharges(3);
		float before = hero.cooldown();

		boolean ok = staff.zapAs(UseContext.hero(hero), target.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(staff.wand().curCharges).isEqualTo(2);
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
	}

	@Test
	@DisplayName("Echo MagesStaff zapAs spends imbued wand charge without phantom turn")
	void echoStaffZapAsSpendsChargesWithoutPhantomTurn() {
		Hero player = mageHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, staffPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		MagesStaff staff = boss.getEchoHero().belongings.getItem(MagesStaff.class);
		Assertions.assertThat(staff).isNotNull();
		Assertions.assertThat(staff.wand()).isNotNull();
		staff.setWandCharges(2);
		float kitBefore = boss.getEchoHero().cooldown();

		boolean ok = staff.zapAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(staff.wand().curCharges).isEqualTo(1);
		Assertions.assertThat(boss.getEchoHero().cooldown()).isEqualTo(kitBefore);
	}

	private static Hero mageHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static EchoPolicy wandPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("WandOfMagicMissile")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy staffPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("MagesStaff")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}
}
