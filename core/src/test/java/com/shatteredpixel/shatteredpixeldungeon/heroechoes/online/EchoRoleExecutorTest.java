package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding;
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

		com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite expected =
				new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite();
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

		com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite expected =
				new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite();
		expected.view(expectedImage, null);
		com.watabou.utils.RectF missileFrame = stage.lastMissile.frame();
		com.watabou.utils.RectF expectedFrame = expected.frame();
		Assertions.assertThat(missileFrame.left).isEqualTo(expectedFrame.left);
		Assertions.assertThat(missileFrame.top).isEqualTo(expectedFrame.top);
		Assertions.assertThat(missileFrame.right).isEqualTo(expectedFrame.right);
		Assertions.assertThat(missileFrame.bottom).isEqualTo(expectedFrame.bottom);
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

		com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite expected =
				new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite();
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
