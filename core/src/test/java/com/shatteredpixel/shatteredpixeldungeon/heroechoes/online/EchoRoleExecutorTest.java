package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
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
	@DisplayName("CLEANSE_BURN detaches burning from the boss")
	void cleanseBurnDetachesBurning() {
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
		Assertions.assertThat(boss.buff(Burning.class)).isNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfFrost.class)).isNull();
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

		com.watabou.noosa.Group stage = new com.watabou.noosa.Group();
		stage.add(boss.sprite);
		stage.add(hero.sprite);
		int lengthBefore = stage.length;
		boss.getEchoHero().invisible = 1;

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of("RANGED")).build(),
				new EchoPolicyChoice("RANGED", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(stage.length)
				.as("SpiritBow should recycle a MissileSprite onto the boss sprite parent")
				.isGreaterThan(lengthBefore);
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

	private static EchoPolicy movePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
				.put("MELEE", EchoTestSupport.capability("*melee"))
				.put("WAIT", EchoTestSupport.capability("*wait")));
	}

	private static EchoPolicy cleansePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("CLEANSE_BURN", EchoTestSupport.capability("PotionOfFrost"))
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
}
