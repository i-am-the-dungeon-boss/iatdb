package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MageArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Echo {@link WildMagic} via
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityWildMagicTest {

	private static Fight fight(int bossOffset) {
		Hero player = magePlayer();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, bossOffset);
		Assertions.assertThat(player.sprite.ch).isSameAs(player);
		Assertions.assertThat(boss.sprite.ch).isSameAs(boss);
		Assertions.assertThat(boss.getEchoHero().sprite).isNull();
		return new Fight(player, boss);
	}

	private static Fight fight() {
		return fight(2);
	}

	private static Hero magePlayer() {
		Hero player = new Hero();
		Dungeon.hero = player;
		HeroClass.MAGE.initHero(player);
		player.lvl = 6;
		player.HP = player.HT = 40;
		return player;
	}

	private static void grantKitWand(Fight f, WandOfMagicMissile wand) {
		wand.cursed = false;
		wand.curCharges = 5;
		wand.collect(f.boss.getEchoHero().belongings.backpack);
	}

	private static void grantTalent(Hero kit, Talent talent, int points) {
		while (kit.talents.size() < 4) {
			kit.talents.add(new java.util.LinkedHashMap<>());
		}
		kit.talents.get(3).put(talent, points);
	}

	private static final class Fight {
		final Hero player;
		final EchoBoss boss;

		Fight(Hero player, EchoBoss boss) {
			this.player = player;
			this.boss = boss;
		}

		UseContext echo() {
			return UseContext.echo(boss);
		}
	}

	@Test
	@DisplayName("Echo WildMagic activateAs damages the player from kit wands")
	void wildMagicDamagesPlayerFromKitWands() {
		Fight f = fight();
		grantKitWand(f, new WandOfMagicMissile());
		MageArmor armor = new MageArmor();
		armor.charge = 100;
		int hpBefore = f.player.HP;

		boolean ok = new WildMagic().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo WildMagic activateAs spends ClassArmor charge from the kit")
	void wildMagicSpendsCharge() {
		Fight f = fight();
		grantKitWand(f, new WandOfMagicMissile());
		MageArmor armor = new MageArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new WildMagic().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore - 25f);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo WildMagic activateAs clears busy so the boss turn can resume")
	void wildMagicClearsBusy() {
		Fight f = fight();
		grantKitWand(f, new WandOfMagicMissile());
		MageArmor armor = new MageArmor();
		armor.charge = 100;

		boolean ok = new WildMagic().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo WildMagic with Conserved Magic free finish clears busy")
	void wildMagicConservedMagicClearsBusy() {
		Fight f = fight();
		grantKitWand(f, new WandOfMagicMissile());
		grantTalent(f.boss.getEchoHero(), Talent.CONSERVED_MAGIC, 4);
		EchoTestSupport.attachInstantProjectileParent(f.boss);
		MageArmor armor = new MageArmor();
		armor.charge = 100;

		boolean ok = new WildMagic().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo WildMagic activateAs fires frost wand VFX without NPE when kit is headless")
	void wildMagicFrostWandDoesNotNpeWhenKitHeadless() {
		Hero player = magePlayer();
		WandOfFrost seed = new WandOfFrost();
		seed.curCharges = 5;
		seed.collect(player.belongings.backpack);

		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, frostWandPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(boss);

		Assertions.assertThat(boss.getEchoHero().sprite).isNull();
		WandOfFrost wand = boss.getEchoHero().belongings.getItem(WandOfFrost.class);
		Assertions.assertThat(wand).isNotNull();
		wand.cursed = false;
		wand.curCharges = 5;

		MageArmor armor = new MageArmor();
		armor.charge = 100;

		Assertions.assertThatCode(() -> new WildMagic().activateAs(UseContext.echo(boss), armor, player.pos))
				.doesNotThrowAnyException();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(fx.magicMissileRecycles).isGreaterThan(0);
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
	}

	@Test
	@DisplayName("Echo WildMagic activateAs fires disintegration DeathRay without NPE when kit is headless")
	void wildMagicDisintegrationDoesNotNpeWhenKitHeadless() {
		Hero player = magePlayer();
		WandOfDisintegration seed = new WandOfDisintegration();
		seed.curCharges = 5;
		seed.collect(player.belongings.backpack);

		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, disintegrationWandPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		Assertions.assertThat(boss.getEchoHero().sprite).isNull();
		WandOfDisintegration wand = boss.getEchoHero().belongings.getItem(WandOfDisintegration.class);
		Assertions.assertThat(wand).isNotNull();
		wand.cursed = false;
		wand.curCharges = 5;

		MageArmor armor = new MageArmor();
		armor.charge = 100;
		int hpBefore = player.HP;

		Assertions.assertThatCode(() -> new WildMagic().activateAs(UseContext.echo(boss), armor, player.pos))
				.doesNotThrowAnyException();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
	}

	@Test
	@DisplayName("Echo WildMagic activateAs fires MagicMissile VFX when the body sprite has a parent")
	void wildMagicFiresMagicMissileWhenParentLive() {
		Fight f = fight();
		grantKitWand(f, new WandOfMagicMissile());
		EchoTestSupport.InstantProjectileGroup fx =
				EchoTestSupport.attachInstantProjectileParent(f.boss);
		MageArmor armor = new MageArmor();
		armor.charge = 100;

		boolean ok = new WildMagic().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(fx.magicMissileRecycles).isGreaterThan(0);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo WildMagic activateAs applies wand damage synchronously without a sprite parent")
	void wildMagicHeadlessStillDamagesPlayer() {
		Fight f = fight();
		grantKitWand(f, new WandOfMagicMissile());
		MageArmor armor = new MageArmor();
		armor.charge = 100;
		int hpBefore = f.player.HP;
		Assertions.assertThat(UseContext.canWorldFx(f.boss)).isFalse();

		boolean ok = new WildMagic().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo WildMagic activateAs spends wand charge from the kit")
	void wildMagicSpendsWandCharge() {
		Fight f = fight();
		WandOfMagicMissile wand = new WandOfMagicMissile();
		grantKitWand(f, wand);
		int chargesBefore = wand.curCharges;
		MageArmor armor = new MageArmor();
		armor.charge = 100;

		boolean ok = new WildMagic().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(wand.curCharges).isLessThan(chargesBefore);
	
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo WildMagic activateAs refuses when ClassArmor charge is too low")
	void wildMagicRefusesLowCharge() {
		Fight f = fight();
		grantKitWand(f, new WandOfMagicMissile());
		MageArmor armor = new MageArmor();
		armor.charge = 0;
		int hpBefore = f.player.HP;

		boolean ok = new WildMagic().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(f.player.HP).isEqualTo(hpBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo WildMagic activateAs skips effect when aimed at the boss body")
	void wildMagicSkipsSelfTarget() {
		Fight f = fight();
		grantKitWand(f, new WandOfMagicMissile());
		MageArmor armor = new MageArmor();
		armor.charge = 100;
		int hpBefore = f.player.HP;

		boolean ok = new WildMagic().activateAs(f.echo(), armor, f.boss.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isEqualTo(100f);
		Assertions.assertThat(f.player.HP).isEqualTo(hpBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	private static EchoPolicy frostWandPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("WandOfFrost")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy disintegrationWandPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("WandOfDisintegration")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}
}
