package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stamina;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.WarriorArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlast;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDragonsBreath;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfEarthenArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStamina;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoRoleExecutorTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("WAIT virtual tag spends the turn without attacking")
	void waitVirtualSpendsTurn() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, movePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("WAIT")).build(),
				new EchoPolicyChoice("WAIT", "default", null));

		Assertions.assertThat(spent).isTrue();
	}

	@Test
	@DisplayName("MELEE virtual tag falls through to mob attack AI")
	void meleeFallsThrough() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, movePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("MELEE")).build(),
				new EchoPolicyChoice("MELEE", "default", null));

		Assertions.assertThat(spent).isFalse();
	}

	@Test
	@DisplayName("KEEP_DISTANCE steps further from the enemy")
	void keepDistanceStepsFurther() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, movePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		int start = boss.pos;

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("KEEP_DISTANCE")).build(),
				new EchoPolicyChoice("KEEP_DISTANCE", "positioning", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos))
				.isGreaterThanOrEqualTo(Dungeon.level.distance(start, hero.pos));
	}

	@Test
	@DisplayName("HEAL can drink every PotionOfHealing in inventory")
	void healDrinksEveryPotionInInventory() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		potion.quantity(2);
		potion.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoTestSupport.healCapabilityPolicy(), 5);
		boss.HP = Math.max(1, boss.HT / 10);

		boolean first = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("HEAL")).build(),
				new EchoPolicyChoice("HEAL", "reactions", null));
		boolean second = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("HEAL")).build(),
				new EchoPolicyChoice("HEAL", "reactions", null));

		Assertions.assertThat(first).isTrue();
		Assertions.assertThat(second).isTrue();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfHealing.class)).isNull();
	}

	@Test
	@DisplayName("HASTE drinks PotionOfHaste onto the boss")
	void hasteDrinksOntoBoss() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfHaste haste = new PotionOfHaste();
		haste.identify();
		haste.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, hastePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("HASTE")).build(),
				new EchoPolicyChoice("HASTE", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Haste.class)).isNotNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfHaste.class)).isNull();
	}

	@Test
	@DisplayName("INVIS drinks PotionOfInvisibility onto the boss")
	void invisDrinksOntoBoss() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfInvisibility invis = new PotionOfInvisibility();
		invis.identify();
		invis.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, invisPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("INVIS")).build(),
				new EchoPolicyChoice("INVIS", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Invisibility.class)).isNotNull();
		Assertions.assertThat(boss.invisible).isGreaterThan(0);
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfInvisibility.class)).isNull();
	}

	@Test
	@DisplayName("CLEANSE strips poison from the boss")
	void cleanseStripsPoison() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfCleansing cleansing = new PotionOfCleansing();
		cleansing.identify();
		cleansing.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, cleanseDebuffPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		Buff.affect(boss, Poison.class).set(6f);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("CLEANSE")).build(),
				new EchoPolicyChoice("CLEANSE", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Poison.class)).isNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfCleansing.class)).isNull();
	}

	@Test
	@DisplayName("HEAL drinks PotionOfShielding as barrier on the boss")
	void healDrinksShieldingAsBarrier() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfShielding shielding = new PotionOfShielding();
		shielding.identify();
		shielding.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, shieldingHealPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("HEAL")).build(),
				new EchoPolicyChoice("HEAL", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Barrier.class)).isNotNull();
		Assertions.assertThat(boss.shielding()).isGreaterThan(0);
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfShielding.class)).isNull();
	}

	@Test
	@DisplayName("drink-default PotionOfStamina buffs Stamina on the boss not the kit")
	void staminaDrinksOntoBossViaDrinkDefault() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfStamina stamina = new PotionOfStamina();
		stamina.identify();
		stamina.collect(hero.belongings.backpack);
		// Role name is not in the legacy self-drink role list — drink via AC_DRINK
		// default.
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, staminaPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("STAMINA")).build(),
				new EchoPolicyChoice("STAMINA", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Stamina.class)).isNotNull();
		Assertions.assertThat(boss.getEchoHero().buff(Stamina.class)).isNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfStamina.class)).isNull();
	}

	@Test
	@DisplayName("drink-default ElixirOfArcaneArmor buffs ArcaneArmor on the boss")
	void arcaneArmorDrinksOntoBossViaDrinkDefault() {
		Hero hero = EchoTestSupport.warriorHero();
		ElixirOfArcaneArmor elixir = new ElixirOfArcaneArmor();
		elixir.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, arcaneArmorPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("ARCANE_ARMOR")).build(),
				new EchoPolicyChoice("ARCANE_ARMOR", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(ArcaneArmor.class)).isNotNull();
		Assertions.assertThat(boss.getEchoHero().buff(ArcaneArmor.class)).isNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(ElixirOfArcaneArmor.class)).isNull();
	}

	@Test
	@DisplayName("drink-default PotionOfEarthenArmor buffs Barkskin on the boss")
	void earthenArmorDrinksOntoBossViaDrinkDefault() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfEarthenArmor earthen = new PotionOfEarthenArmor();
		earthen.identify();
		earthen.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, earthenArmorPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("EARTHEN_ARMOR")).build(),
				new EchoPolicyChoice("EARTHEN_ARMOR", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Barkskin.class)).isNotNull();
		Assertions.assertThat(boss.getEchoHero().buff(Barkskin.class)).isNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfEarthenArmor.class)).isNull();
	}

	@Test
	@DisplayName("Hero-only PotionOfStrength self-drink fails without consuming")
	void strengthSelfDrinkFailsWithoutConsuming() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfStrength strength = new PotionOfStrength();
		strength.identify();
		strength.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, strengthPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("STRENGTH")).build(),
				new EchoPolicyChoice("STRENGTH", "reactions", null));

		Assertions.assertThat(spent).isFalse();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfStrength.class)).isNotNull();
	}

	@Test
	@DisplayName("PURITY drinks blob immunity onto the boss")
	void purityDrinksBlobImmunity() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfPurity purity = new PotionOfPurity();
		purity.identify();
		purity.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, purityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("PURITY")).build(),
				new EchoPolicyChoice("PURITY", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(BlobImmunity.class)).isNotNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfPurity.class)).isNull();
	}

	@Test
	@DisplayName("CLEANSE_BURN drinks frost via shared apply (shatter at boss)")
	void cleanseBurnDrinksFrost() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfFrost frost = new PotionOfFrost();
		frost.identify();
		frost.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, cleansePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		Buff.affect(boss, Burning.class);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("CLEANSE_BURN")).build(),
				new EchoPolicyChoice("CLEANSE_BURN", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		// Same as Hero drink: apply → shatter. No Echo-only Burning.detach.
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfFrost.class)).isNull();
	}

	@Test
	@DisplayName("DRAGONS_BREATH cones Burning and Cripple onto the player from the boss")
	void dragonsBreathBreathesViaExecutor() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfDragonsBreath breath = new PotionOfDragonsBreath();
		breath.identify();
		breath.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, dragonsBreathPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Assertions.assertThat(hero.sprite.ch).isSameAs(hero);
		Assertions.assertThat(boss.sprite.ch).isSameAs(boss);
		Assertions.assertThat(boss.getEchoHero().sprite).isNull();

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("DRAGONS_BREATH")).build(),
				new EchoPolicyChoice("DRAGONS_BREATH", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hero.buff(Burning.class)).isNotNull();
		Assertions.assertThat(hero.buff(Cripple.class)).isNotNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfDragonsBreath.class)).isNull();
	}

	@Test
	@DisplayName("SETUP_CC throws paralytic gas at the picked cell")
	void setupCcThrowsPotion() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfParalyticGas gas = new PotionOfParalyticGas();
		gas.identify();
		gas.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, setupCcPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.rolesReady(java.util.Set.of("SETUP_CC"))
				.safeHazards(java.util.Set.of("SETUP_CC"))
				.build();
		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				status,
				new EchoPolicyChoice("SETUP_CC", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfParalyticGas.class)).isNull();
	}

	@Test
	@DisplayName("thrown potion queues MissileSprite showing that potion's image")
	void thrownPotionMissileUsesPotionImage() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfParalyticGas gas = new PotionOfParalyticGas();
		gas.identify();
		gas.collect(hero.belongings.backpack);
		int expectedImage = gas.image();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, setupCcPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		MissileCaptureGroup stage = new MissileCaptureGroup();
		stage.add(boss.sprite);
		stage.add(hero.sprite);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.rolesReady(java.util.Set.of("SETUP_CC"))
				.safeHazards(java.util.Set.of("SETUP_CC"))
				.build();
		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				status,
				new EchoPolicyChoice("SETUP_CC", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(stage.lastMissile)
				.as("thrown potion must play a MissileSprite from the boss")
				.isNotNull();

		com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite expected = new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite();
		expected.view(expectedImage, null);
		com.watabou.utils.RectF missileFrame = stage.lastMissile.frame();
		com.watabou.utils.RectF expectedFrame = expected.frame();
		Assertions.assertThat(missileFrame.left).isEqualTo(expectedFrame.left);
		Assertions.assertThat(missileFrame.top).isEqualTo(expectedFrame.top);
		Assertions.assertThat(missileFrame.right).isEqualTo(expectedFrame.right);
		Assertions.assertThat(missileFrame.bottom).isEqualTo(expectedFrame.bottom);
	}

	@Test
	@DisplayName("PAYOFF_AOE MissileSprite uses liquid flame image not a generic projectile")
	void payoffAoeMissileUsesLiquidFlameImage() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfLiquidFlame flame = new PotionOfLiquidFlame();
		flame.identify();
		flame.collect(hero.belongings.backpack);
		int expectedImage = flame.image();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, payoffPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		MissileCaptureGroup stage = new MissileCaptureGroup();
		stage.add(boss.sprite);
		stage.add(hero.sprite);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.rolesReady(java.util.Set.of("PAYOFF_AOE"))
				.safeHazards(java.util.Set.of("fire_aoe", "PAYOFF_AOE"))
				.build();
		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				status,
				new EchoPolicyChoice("PAYOFF_AOE", "recipes", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(stage.lastMissile).isNotNull();

		com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite expected = new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite();
		expected.view(expectedImage, null);
		com.watabou.utils.RectF missileFrame = stage.lastMissile.frame();
		com.watabou.utils.RectF expectedFrame = expected.frame();
		Assertions.assertThat(missileFrame.left).isEqualTo(expectedFrame.left);
		Assertions.assertThat(missileFrame.top).isEqualTo(expectedFrame.top);
		Assertions.assertThat(missileFrame.right).isEqualTo(expectedFrame.right);
		Assertions.assertThat(missileFrame.bottom).isEqualTo(expectedFrame.bottom);
	}

	@Test
	@DisplayName("RANGED MissileWeapon spends turn, damages player, and leaves kit sprite-less")
	void rangedMissileWeaponHitsAndLeavesKitSpriteNull() {
		Hero hero = EchoTestSupport.warriorHero();
		ThrowingKnife knives = new ThrowingKnife();
		knives.identify();
		knives.quantity(3);
		knives.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, rangedMissilePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Hero kit = boss.getEchoHero();
		ThrowingKnife kitKnives = kit.belongings.getItem(ThrowingKnife.class);
		Assertions.assertThat(kitKnives).isNotNull();
		Assertions.assertThat(kit.sprite).isNull();
		Assertions.assertThat(hero.sprite.ch).isSameAs(hero);
		Assertions.assertThat(boss.sprite.ch).isSameAs(boss);

		int hpBefore = hero.HP;
		int qtyBefore = kitKnives.quantity();
		kit.invisible = 1; // guarantee hit (surprise-accuracy path)

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("RANGED")).build(),
				new EchoPolicyChoice("RANGED", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hero.HP).isLessThan(hpBefore);
		Assertions.assertThat(kit.sprite).isNull();
		ThrowingKnife after = kit.belongings.getItem(ThrowingKnife.class);
		Assertions.assertThat(after == null ? 0 : after.quantity()).isLessThan(qtyBefore);
	}

	@Test
	@DisplayName("RANGED SpiritBow spends turn and survives a real hit VFX path")
	void rangedSpiritBowSpendsTurnAndHitsWithSprites() {
		Hero hero = huntressHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, rangedBowPolicy(), 5);
		// Offset 2: inside Level.insideMap on the 7×7 fixture (offset 3 is the border).
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		// Mirror a live fight: player + boss have sprites; phantom echo hero does not.
		Assertions.assertThat(hero.sprite).isNotNull();
		Assertions.assertThat(hero.sprite.ch).isSameAs(hero);
		Assertions.assertThat(boss.sprite).isNotNull();
		Assertions.assertThat(boss.sprite.ch).isSameAs(boss);
		Assertions.assertThat(boss.getEchoHero().sprite).isNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(SpiritBow.class)).isNotNull();

		int hpBefore = hero.HP;
		boss.getEchoHero().invisible = 1; // guarantee hit (surprise-accuracy path)

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("RANGED")).build(),
				new EchoPolicyChoice("RANGED", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hero.HP).isLessThan(hpBefore);
		// Borrowed boss sprite must not stick on the phantom hero.
		Assertions.assertThat(boss.getEchoHero().sprite).isNull();
	}

	@Test
	@DisplayName("RANGED SpiritBow queues a MissileSprite when the boss sprite is on stage")
	void rangedSpiritBowQueuesMissileSprite() {
		Hero hero = huntressHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, rangedBowPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		MissileCaptureGroup stage = new MissileCaptureGroup();
		stage.add(boss.sprite);
		stage.add(hero.sprite);
		boss.getEchoHero().invisible = 1;

		SpiritBow bow = boss.getEchoHero().belongings.getItem(SpiritBow.class);
		Assertions.assertThat(bow).isNotNull();
		int expectedImage = bow.knockArrow().image();

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("RANGED")).build(),
				new EchoPolicyChoice("RANGED", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(stage.lastMissile)
				.as("SpiritBow should recycle a MissileSprite onto the boss sprite parent")
				.isNotNull();

		com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite expected = new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite();
		expected.view(expectedImage, null);
		com.watabou.utils.RectF missileFrame = stage.lastMissile.frame();
		com.watabou.utils.RectF expectedFrame = expected.frame();
		Assertions.assertThat(missileFrame.left).isEqualTo(expectedFrame.left);
		Assertions.assertThat(missileFrame.top).isEqualTo(expectedFrame.top);
		Assertions.assertThat(missileFrame.right).isEqualTo(expectedFrame.right);
		Assertions.assertThat(missileFrame.bottom).isEqualTo(expectedFrame.bottom);
	}

	@Test
	@DisplayName("AOE throw is refused when target picker returns none")
	void aoeThrowRefusedWithoutSafeTarget() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfLiquidFlame flame = new PotionOfLiquidFlame();
		flame.identify();
		flame.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, payoffPolicy(), 5);
		// No level → TargetPicker returns -1
		Dungeon.level = null;

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.rolesReady(java.util.Set.of("PAYOFF_AOE"))
				.unsafeHazards(java.util.Set.of("fire_aoe", "PAYOFF_AOE"))
				.build();
		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				status,
				new EchoPolicyChoice("PAYOFF_AOE", "recipes", null));

		Assertions.assertThat(spent).isFalse();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfLiquidFlame.class)).isNotNull();
	}

	@Test
	@DisplayName("PAYOFF_AOE Bomb throwAs lights fuse via executor")
	void payoffAoeBombLightsFuseViaExecutor() {
		Hero hero = EchoTestSupport.warriorHero();
		Bomb bomb = new Bomb();
		bomb.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, bombPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.rolesReady(java.util.Set.of("PAYOFF_AOE"))
				.safeHazards(java.util.Set.of("fire_aoe", "PAYOFF_AOE"))
				.build();
		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				status,
				new EchoPolicyChoice("PAYOFF_AOE", "recipes", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(Bomb.class)).isNull();

		Bomb landed = null;
		for (Heap heap : Dungeon.level.heaps.valueList()) {
			for (Item i : heap.items) {
				if (i instanceof Bomb) {
					landed = (Bomb) i;
					break;
				}
			}
		}
		Assertions.assertThat(landed).isNotNull();
		Assertions.assertThat(landed.fuse).isNotNull();
	}

	@Test
	@DisplayName("PAYOFF_AOE StoneOfBlast throwAs damages hero via executor")
	void payoffAoeStoneOfBlastDamagesHeroViaExecutor() {
		Hero hero = EchoTestSupport.warriorHero();
		StoneOfBlast stone = new StoneOfBlast();
		stone.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, stoneBlastPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		java.util.Arrays.fill(Dungeon.level.heroFOV, false);
		int hpBefore = hero.HP;

		EchoPolicyStatus status = new EchoPolicyStatus.Builder()
				.rolesReady(java.util.Set.of("PAYOFF_AOE"))
				.safeHazards(java.util.Set.of("fire_aoe", "PAYOFF_AOE"))
				.build();
		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				status,
				new EchoPolicyChoice("PAYOFF_AOE", "recipes", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(StoneOfBlast.class)).isNull();
		Assertions.assertThat(hero.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("SCROLL reads ScrollOfRecharging onto the boss body")
	void scrollRechargingBuffsBossViaExecutor() {
		Hero hero = EchoTestSupport.warriorHero();
		ScrollOfRecharging scroll = new ScrollOfRecharging();
		scroll.identify();
		scroll.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, scrollRechargePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("SCROLL")).build(),
				new EchoPolicyChoice("SCROLL", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Recharging.class)).isNotNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(ScrollOfRecharging.class)).isNull();
	}

	@Test
	@DisplayName("SCROLL Upgrade auto-upgrades kit weapon via executor")
	void scrollUpgradeViaExecutor() {
		Hero hero = EchoTestSupport.warriorHero();
		ScrollOfUpgrade scroll = new ScrollOfUpgrade();
		scroll.identify();
		scroll.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, scrollUpgradePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Hero kit = boss.getEchoHero();
		com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword sword = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword();
		sword.identify();
		kit.belongings.weapon = sword;
		int levelBefore = sword.level();

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("SCROLL")).build(),
				new EchoPolicyChoice("SCROLL", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(sword.level()).isEqualTo(levelBefore + 1);
		Assertions.assertThat(kit.belongings.getItem(ScrollOfUpgrade.class)).isNull();
	}

	@Test
	@DisplayName("ARMOR_ABILITY Endure activates ClassArmor on the boss body")
	void armorAbilityEndureBuffsBossViaExecutor() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, armorEndurePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Hero kit = boss.getEchoHero();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;
		kit.belongings.armor = armor;
		armor.activate(kit);
		kit.armorAbility = new Endure();
		float chargeBefore = armor.charge;

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("ARMOR_ABILITY")).build(),
				new EchoPolicyChoice("ARMOR_ABILITY", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Endure.EndureTracker.class)).isNotNull();
		Assertions.assertThat(kit.buff(Endure.EndureTracker.class)).isNull();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
	}

	@Test
	@DisplayName("STEALTH CloakOfShadows useAs buffs the boss body via executor")
	void stealthCloakBuffsBossViaExecutor() {
		Hero hero = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, stealthCloakPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Hero kit = boss.getEchoHero();
		CloakOfShadows cloak = kit.belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.directCharge(2);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("STEALTH")).build(),
				new EchoPolicyChoice("STEALTH", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(CloakOfShadows.cloakStealth.class)).isNotNull();
		Assertions.assertThat(kit.buff(CloakOfShadows.cloakStealth.class)).isNull();
		Assertions.assertThat(boss.invisible).isGreaterThan(0);
	}

	@Test
	@DisplayName("HOLY_WARD casts HolyWard via HolyTome capability spell field")
	void holyWardCastViaExecutor() {
		Hero hero = clericHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, holyWardPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		HolyTome tome = (HolyTome) boss.getEchoHero().belongings.artifact;
		tome.directCharge(5f);
		String chargeBefore = tome.status();

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("HOLY_WARD")).build(),
				new EchoPolicyChoice("HOLY_WARD", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions
				.assertThat(boss
						.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.HolyArmBuff.class))
				.isNotNull();
		Assertions.assertThat(boss.getEchoHero().buff(
				com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.HolyArmBuff.class)).isNull();
		Assertions.assertThat(tome.status()).isNotEqualTo(chargeBefore);
	}

	@Test
	@DisplayName("ARMOR_ABILITY refuses when ClassArmor charge is too low")
	void armorAbilityRefusesLowCharge() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, armorEndurePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Hero kit = boss.getEchoHero();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 0;
		kit.belongings.armor = armor;
		armor.activate(kit);
		kit.armorAbility = new Endure();

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("ARMOR_ABILITY")).build(),
				new EchoPolicyChoice("ARMOR_ABILITY", "default", null));

		Assertions.assertThat(spent).isFalse();
		Assertions.assertThat(boss.buff(Endure.EndureTracker.class)).isNull();
	}

	private static Hero clericHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.CLERIC.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static EchoPolicy holyWardPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("HOLY_WARD", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("HolyTome"))
						.put("spell", "HolyWard"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static Hero rogueHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.ROGUE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static EchoPolicy stealthCloakPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("STEALTH", EchoTestSupport.capability("CloakOfShadows"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static Hero huntressHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.HUNTRESS.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static EchoPolicy rangedBowPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "MAX_DAMAGE")
						.put("items", new JSONArray().put("SpiritBow")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy rangedMissilePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "MAX_DAMAGE")
						.put("items", new JSONArray().put("ThrowingKnife")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	@Test
	@DisplayName("WEAPON_ABILITY Scimitar abilityAs buffs the boss body via executor")
	void weaponAbilityScimitarViaExecutor() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, scimitarAbilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Scimitar scimitar = new Scimitar();
		scimitar.identify();
		kit.belongings.weapon = scimitar;
		scimitar.activate(kit);
		kit.STR = Math.max(kit.STR(), scimitar.STRReq());
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
				.affect(kit,
						com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.Charger.class).charges = 5;

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("WEAPON_ABILITY")).build(),
				new EchoPolicyChoice("WEAPON_ABILITY", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Scimitar.SwordDance.class)).isNotNull();
		Assertions.assertThat(kit.buff(Scimitar.SwordDance.class)).isNull();
	}

	@Test
	@DisplayName("WEAPON_ABILITY refuses when Charger has insufficient charges")
	void weaponAbilityRefusesLowCharge() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, scimitarAbilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Scimitar scimitar = new Scimitar();
		scimitar.identify();
		kit.belongings.weapon = scimitar;
		scimitar.activate(kit);
		kit.STR = Math.max(kit.STR(), scimitar.STRReq());
		com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.Charger charger = com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff
				.affect(kit, com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon.Charger.class);
		charger.charges = 0;
		charger.partialCharge = 0;

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("WEAPON_ABILITY")).build(),
				new EchoPolicyChoice("WEAPON_ABILITY", "default", null));

		Assertions.assertThat(spent).isFalse();
		Assertions.assertThat(boss.buff(Scimitar.SwordDance.class)).isNull();
	}

	private static Hero duelistHero() {
		Hero hero = new Hero();
		HeroClass.DUELIST.initHero(hero);
		hero.lvl = 10;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static EchoPolicy scimitarAbilityPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("WEAPON_ABILITY", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("Scimitar")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy armorEndurePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("ARMOR_ABILITY", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("WarriorArmor")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy scrollRechargePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("SCROLL", EchoTestSupport.capability("ScrollOfRecharging"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy scrollUpgradePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("SCROLL", EchoTestSupport.capability("ScrollOfUpgrade"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy movePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
				.put("MELEE", EchoTestSupport.capability("*melee"))
				.put("WAIT", EchoTestSupport.capability("*wait")));
	}

	/** Captures MissileSprites added/recycled onto the boss stage. */
	private static final class MissileCaptureGroup extends com.watabou.noosa.Group {
		com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite lastMissile;

		@Override
		public synchronized com.watabou.noosa.Gizmo add(com.watabou.noosa.Gizmo g) {
			if (g instanceof com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite) {
				lastMissile = (com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite) g;
			}
			return super.add(g);
		}
	}

	private static EchoPolicy hastePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("HASTE", EchoTestSupport.capability("PotionOfHaste"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy invisPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("INVIS", EchoTestSupport.capability("PotionOfInvisibility"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy cleanseDebuffPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("CLEANSE", EchoTestSupport.capability("PotionOfCleansing"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy shieldingHealPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("HEAL", EchoTestSupport.capability("PotionOfShielding"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy staminaPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("STAMINA", EchoTestSupport.capability("PotionOfStamina"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy arcaneArmorPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("ARCANE_ARMOR", EchoTestSupport.capability("ElixirOfArcaneArmor"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy earthenArmorPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("EARTHEN_ARMOR", EchoTestSupport.capability("PotionOfEarthenArmor"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy strengthPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("STRENGTH", EchoTestSupport.capability("PotionOfStrength"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy purityPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("PURITY", EchoTestSupport.capability("PotionOfPurity"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy cleansePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("CLEANSE_BURN", EchoTestSupport.capability("PotionOfFrost"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy dragonsBreathPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("DRAGONS_BREATH", EchoTestSupport.capability("PotionOfDragonsBreath"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy setupCcPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("SETUP_CC", EchoTestSupport.capability("PotionOfParalyticGas"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy payoffPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("PAYOFF_AOE", new JSONObject()
						.put("pick", "MAX_DAMAGE")
						.put("items", new JSONArray().put("PotionOfLiquidFlame"))
						.put("hazard", EchoPolicyHazards.FIRE_AOE))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy bombPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("PAYOFF_AOE", new JSONObject()
						.put("pick", "MAX_DAMAGE")
						.put("items", new JSONArray().put("Bomb"))
						.put("hazard", EchoPolicyHazards.FIRE_AOE))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy stoneBlastPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("PAYOFF_AOE", new JSONObject()
						.put("pick", "MAX_DAMAGE")
						.put("items", new JSONArray().put("StoneOfBlast"))
						.put("hazard", EchoPolicyHazards.FIRE_AOE))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}
}
