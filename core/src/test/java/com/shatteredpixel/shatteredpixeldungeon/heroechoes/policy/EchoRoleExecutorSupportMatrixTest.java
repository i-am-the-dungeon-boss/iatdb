package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
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
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
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
 * Phase-10 support matrix: one representative item per executor handler family.
 * Each supported path must spend the turn and mutate state; unsupported classes
 * must fail without consuming inventory.
 */
@ExtendWith(GdxTestExtension.class)
class EchoRoleExecutorSupportMatrixTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("support matrix: potion drink spends turn and consumes item")
	void potionDrinkSpendsAndConsumes() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		potion.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, healPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		boss.HP = Math.max(1, boss.HT / 10);

		boolean spent = execute(boss, "HEAL", "reactions");

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfHealing.class)).isNull();
	}

	@Test
	@DisplayName("support matrix: wand zap spends turn and reduces charges")
	void wandZapSpendsAndReducesCharges() {
		Hero player = mageHero();
		WandOfMagicMissile seed = new WandOfMagicMissile();
		seed.curCharges = 3;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, wandPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		WandOfMagicMissile wand = boss.getEchoHero().belongings.getItem(WandOfMagicMissile.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 3;

		boolean spent = execute(boss, "RANGED", "default");

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(wand.curCharges).isEqualTo(2);
	}

	@Test
	@DisplayName("support matrix: scroll read spends turn and consumes scroll")
	void scrollReadSpendsAndConsumes() {
		Hero hero = EchoTestSupport.warriorHero();
		ScrollOfRecharging scroll = new ScrollOfRecharging();
		scroll.identify();
		scroll.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, scrollPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		boolean spent = execute(boss, "SCROLL", "default");

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Recharging.class)).isNotNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(ScrollOfRecharging.class)).isNull();
	}

	@Test
	@DisplayName("support matrix: inventory stone spends turn and consumes stone")
	void inventoryStoneSpendsAndConsumes() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, enchantPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Hero kit = boss.getEchoHero();
		WornShortsword sword = new WornShortsword();
		sword.identify();
		kit.belongings.weapon = sword;
		StoneOfEnchantment stone = new StoneOfEnchantment();
		stone.collect(kit.belongings.backpack);

		boolean spent = execute(boss, "ENCHANT", "default");

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(sword.enchantment).isNotNull();
		Assertions.assertThat(kit.belongings.getItem(StoneOfEnchantment.class)).isNull();
	}

	@Test
	@DisplayName("support matrix: HornOfPlenty snack spends turn and reduces hunger")
	void hornSnackSpendsAndReducesHunger() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, hornPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		HornOfPlenty horn = new HornOfPlenty();
		horn.fullyCharge();
		kit.belongings.artifact = horn;
		horn.activate(kit);

		Hunger hunger = boss.buff(Hunger.class);
		if (hunger == null) {
			hunger = new Hunger();
			hunger.attachTo(boss);
		}
		hunger.satisfy(-Hunger.STARVING);
		int hungerBefore = hunger.hunger();

		boolean spent = execute(boss, "SNACK", "default");

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hunger.hunger()).isLessThan(hungerBefore);
	}

	@Test
	@DisplayName("support matrix: EtherealChains pull spends turn and moves player")
	void chainsPullSpendsAndMovesPlayer() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, chainsPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		EtherealChains chains = new EtherealChains();
		kit.belongings.artifact = chains;
		chains.activate(kit);

		int playerBefore = player.pos;
		int distBefore = Dungeon.level.distance(boss.pos, player.pos);

		boolean spent = execute(boss, "PULL", "default");

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(player.pos).isNotEqualTo(playerBefore);
		Assertions.assertThat(Dungeon.level.distance(boss.pos, player.pos)).isLessThan(distBefore);
	}

	@Test
	@DisplayName("support matrix: CloakOfShadows stealth spends turn and buffs boss")
	void cloakStealthSpendsAndBuffsBoss() {
		Hero hero = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, stealthPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		CloakOfShadows cloak = boss.getEchoHero().belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.directCharge(2);

		boolean spent = execute(boss, "STEALTH", "default");

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(CloakOfShadows.cloakStealth.class)).isNotNull();
		Assertions.assertThat(boss.invisible).isGreaterThan(0);
	}

	@Test
	@DisplayName("support matrix: HolyTome spell spends turn and buffs boss")
	void holyTomeSpellSpendsAndBuffsBoss() {
		Hero hero = clericHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, holyWardPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		HolyTome tome = (HolyTome) boss.getEchoHero().belongings.artifact;
		tome.directCharge(5f);
		String chargeBefore = tome.status();

		boolean spent = execute(boss, "HOLY_WARD", "default");

		Assertions.assertThat(spent).isTrue();
		Assertions
				.assertThat(boss
						.buff(com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard.HolyArmBuff.class))
				.isNotNull();
		Assertions.assertThat(tome.status()).isNotEqualTo(chargeBefore);
	}

	@Test
	@DisplayName("support matrix: ClassArmor ability spends turn and buffs boss")
	void armorAbilitySpendsAndBuffsBoss() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, endurePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Hero kit = boss.getEchoHero();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;
		kit.belongings.armor = armor;
		armor.activate(kit);
		kit.armorAbility = new Endure();
		float chargeBefore = armor.charge;

		boolean spent = execute(boss, "ARMOR_ABILITY", "default");

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Endure.EndureTracker.class)).isNotNull();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
	}

	@Test
	@DisplayName("support matrix: Duelist weapon ability spends turn and buffs boss")
	void duelistWeaponAbilitySpendsAndBuffsBoss() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(template, scimitarPolicy(), 5);
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

		boolean spent = execute(boss, "WEAPON_ABILITY", "default");

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Scimitar.SwordDance.class)).isNotNull();
	}

	@Test
	@DisplayName("support matrix: MissileWeapon throw spends turn and reduces quantity")
	void missileWeaponSpendsAndReducesQuantity() {
		Hero hero = EchoTestSupport.warriorHero();
		ThrowingKnife knives = new ThrowingKnife();
		knives.identify();
		knives.quantity(3);
		knives.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, missilePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		ThrowingKnife kitKnives = boss.getEchoHero().belongings.getItem(ThrowingKnife.class);
		Assertions.assertThat(kitKnives).isNotNull();
		int qtyBefore = kitKnives.quantity();
		boss.getEchoHero().invisible = 1;

		boolean spent = execute(boss, "RANGED", "default");

		Assertions.assertThat(spent).isTrue();
		ThrowingKnife after = boss.getEchoHero().belongings.getItem(ThrowingKnife.class);
		Assertions.assertThat(after == null ? 0 : after.quantity()).isLessThan(qtyBefore);
	}

	@Test
	@DisplayName("support matrix: Bomb throw spends turn and consumes bomb")
	void bombThrowSpendsAndConsumes() {
		Hero hero = EchoTestSupport.warriorHero();
		Bomb bomb = new Bomb();
		bomb.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, bombPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		boolean spent = execute(boss, "PAYOFF_AOE", "recipes");

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
	@DisplayName("support matrix: SpiritBow ranged spends turn and damages player")
	void spiritBowSpendsAndDamagesPlayer() {
		Hero hero = huntressHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, bowPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Assertions.assertThat(boss.getEchoHero().belongings.getItem(SpiritBow.class)).isNotNull();
		int hpBefore = hero.HP;
		boss.getEchoHero().invisible = 1;

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().distance(2).rolesReady(java.util.Set.of("RANGED")).build(),
				new EchoPlan("RANGED", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hero.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("support matrix: unsupported item class refuses without consuming inventory")
	void unsupportedItemRefusesWithoutMutation() {
		Hero hero = EchoTestSupport.warriorHero();
		RingOfMight ring = new RingOfMight();
		ring.collect(hero.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, ringPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		int qtyBefore = EchoInventory.count(boss.getEchoHero(), "RingOfMight");

		boolean spent = execute(boss, "RING", "default");

		Assertions.assertThat(spent).isFalse();
		Assertions.assertThat(EchoInventory.count(boss.getEchoHero(), "RingOfMight")).isEqualTo(qtyBefore);
	}

	private static boolean execute(EchoBoss boss, String role, String layer) {
		return EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of(role)).build(),
				new EchoPlan(role, layer, null));
	}

	private static Hero mageHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static Hero rogueHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.ROGUE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static Hero clericHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.CLERIC.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static Hero huntressHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.HUNTRESS.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static Hero duelistHero() {
		Hero hero = new Hero();
		HeroClass.DUELIST.initHero(hero);
		hero.lvl = 10;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static EchoPolicy healPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("HEAL", EchoTestSupport.capability("PotionOfHealing"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy wandPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", EchoTestSupport.capability("WandOfMagicMissile"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy scrollPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("SCROLL", EchoTestSupport.capability("ScrollOfRecharging"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy enchantPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("ENCHANT", EchoTestSupport.capability("StoneOfEnchantment"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy hornPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("SNACK", EchoTestSupport.capability("HornOfPlenty"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy chainsPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("PULL", EchoTestSupport.capability("EtherealChains"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy stealthPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("STEALTH", EchoTestSupport.capability("CloakOfShadows"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy holyWardPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("HOLY_WARD", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("HolyTome"))
						.put("spell", "HolyWard"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy endurePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("ARMOR_ABILITY", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("WarriorArmor")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy scimitarPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("WEAPON_ABILITY", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("Scimitar")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy missilePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "MAX_DAMAGE")
						.put("items", new JSONArray().put("ThrowingKnife")))
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

	private static EchoPolicy bowPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "MAX_DAMAGE")
						.put("items", new JSONArray().put("SpiritBow")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy ringPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RING", EchoTestSupport.capability("RingOfMight"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}
}
