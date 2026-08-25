package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfEnchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Executor integration for artifact / inventory-stone / wand adapters that were
 * only covered at the adapter layer.
 */
@ExtendWith(GdxTestExtension.class)
class EchoRoleExecutorArtifactStoneWandTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("RANGED WandOfMagicMissile zaps via executor and spends kit charges")
	void rangedWandZapsViaExecutor() {
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

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("RANGED")).build(),
				new EchoPlan("RANGED", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(wand.curCharges).isEqualTo(2);
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
		Assertions.assertThat(player.HP).isLessThanOrEqualTo(hpBefore);
	}

	@Test
	@DisplayName("SNACK HornOfPlenty snacks via executor on boss Hunger")
	void snackHornViaExecutor() {
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

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("SNACK")).build(),
				new EchoPlan("SNACK", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(hunger.hunger()).isLessThan(hungerBefore);
	}

	@Test
	@DisplayName("PULL EtherealChains pulls the player via executor")
	void pullChainsViaExecutor() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, chainsPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		EtherealChains chains = new EtherealChains();
		kit.belongings.artifact = chains;
		chains.activate(kit);

		int playerBefore = player.pos;
		int distBefore = Dungeon.level.distance(boss.pos, player.pos);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("PULL")).build(),
				new EchoPlan("PULL", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(player.pos).isNotEqualTo(playerBefore);
		Assertions.assertThat(Dungeon.level.distance(boss.pos, player.pos)).isLessThan(distBefore);
	}

	@Test
	@DisplayName("ENCHANT StoneOfEnchantment enchants kit weapon via executor")
	void enchantStoneViaExecutor() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, enchantPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		WornShortsword sword = new WornShortsword();
		sword.identify();
		kit.belongings.weapon = sword;

		StoneOfEnchantment stone = new StoneOfEnchantment();
		stone.collect(kit.belongings.backpack);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("ENCHANT")).build(),
				new EchoPlan("ENCHANT", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(sword.enchantment).isNotNull();
		Assertions.assertThat(kit.belongings.getItem(StoneOfEnchantment.class)).isNull();
	}

	private static Hero mageHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.MAGE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static EchoPolicy wandPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", EchoTestSupport.capability("WandOfMagicMissile"))
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

	private static EchoPolicy enchantPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("ENCHANT", EchoTestSupport.capability("StoneOfEnchantment"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}
}
