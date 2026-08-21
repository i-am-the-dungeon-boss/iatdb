package com.shatteredpixel.shatteredpixeldungeon.actors;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Momentum;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WellFed;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.AntiEntropy;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Affection;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Potential;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Annoying;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Explosive;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Friendly;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.HeavyBoomerang;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.watabou.utils.Bundle;
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Reproduces production NPEs when combat VFX runs on a char with null sprite
 * (echo kit / headless actors). Gameplay must still apply.
 */
@ExtendWith(GdxTestExtension.class)
class NullSpriteVfxSafetyTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("Berserk start skips status VFX when target sprite is null")
	void berserkStartSkipsStatusWhenSpriteNull() {
		Hero hero = EchoTestSupport.warriorHero();
		hero.sprite = null;
		if (hero.belongings.armor() != null && hero.belongings.armor().checkSeal() == null) {
			hero.belongings.armor().affixSeal(new BrokenSeal());
		}
		hero.belongings.armor().activate(hero);

		Berserk berserk = Buff.affect(hero, Berserk.class);
		berserk.damage(hero.HT * 4);
		Assertions.assertThat(hero.buff(BrokenSeal.WarriorShield.class)).isNotNull();

		Assertions.assertThatCode(berserk::doAction).doesNotThrowAnyException();
		Assertions.assertThat(hero.shielding()).isGreaterThan(0);
	}

	@Test
	@DisplayName("WellFed heal tick skips status VFX when target sprite is null")
	void wellFedHealTickSkipsStatusWhenSpriteNull() {
		Hero hero = EchoTestSupport.warriorHero();
		hero.sprite = null;
		hero.HP = 10;
		hero.HT = 30;

		WellFed buff = Buff.affect(hero, WellFed.class);
		Bundle state = new Bundle();
		state.put("left", 19);
		buff.restoreFromBundle(state);

		Assertions.assertThatCode(buff::act).doesNotThrowAnyException();
		Assertions.assertThat(hero.HP).isEqualTo(11);
	}

	@Test
	@DisplayName("Blocking proc applies shield when attacker sprite is null")
	void blockingProcAppliesShieldWhenAttackerSpriteNull() {
		Hero attacker = EchoTestSupport.warriorHero();
		Hero defender = EchoTestSupport.warriorHero();
		attacker.sprite = null;
		WornShortsword weapon = new WornShortsword();
		Blocking enchant = new Blocking();

		boolean shielded = false;
		for (int i = 0; i < 200; i++) {
			enchant.proc(weapon, attacker, defender, 8);
			if (attacker.buff(Blocking.BlockBuff.class) != null) {
				shielded = true;
				break;
			}
		}

		Assertions.assertThat(shielded).isTrue();
	}

	@Test
	@DisplayName("Potential proc charges wands when defender sprite is null")
	void potentialProcChargesWhenDefenderSpriteNull() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		hero.sprite = null;
		Hero attacker = EchoTestSupport.warriorHero();
		ClothArmor armor = new ClothArmor();
		Potential glyph = new Potential();

		Assertions.assertThat(hero.belongings.charge(0f))
				.as("mage fixture must own a wand charger so Potential reaches VFX")
				.isGreaterThan(0);

		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 200; i++) {
				glyph.proc(armor, attacker, hero, 5);
			}
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Bleeding tick damages when target sprite is null")
	void bleedingTickDamagesWhenSpriteNull() {
		Hero hero = EchoTestSupport.warriorHero();
		hero.sprite = null;
		hero.HP = hero.HT = 200;
		Bleeding bleeding = Buff.affect(hero, Bleeding.class);
		// High enough that Math.round(NormalFloat(level/2, level)) cannot be 0.
		bleeding.set(100f);

		Assertions.assertThatCode(bleeding::act).doesNotThrowAnyException();
		Assertions.assertThat(hero.HP).isLessThan(200);
	}

	@Test
	@DisplayName("MagicMissile onZap damages when target sprite is null")
	void magicMissileOnZapWhenTargetSpriteNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		player.sprite = null;
		int hpBefore = player.HP;

		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.setCurrent(boss.getEchoHero());
		Ballistica shot = new Ballistica(boss.pos, player.pos, Ballistica.MAGIC_BOLT);

		Assertions.assertThatCode(() -> wand.onZap(shot)).doesNotThrowAnyException();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("CursedWand HealthTransfer heals when sprites are null")
	void cursedHealthTransferWhenSpritesNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero kit = boss.getEchoHero();
		kit.sprite = null;
		player.sprite = null;
		kit.pos = boss.pos;
		boss.HP = 5;
		boss.HT = 100;
		kit.HP = 5;
		kit.HT = 100;
		int bodyHpBefore = boss.HP;

		Ballistica bolt = new Ballistica(boss.pos, player.pos, Ballistica.MAGIC_BOLT);
		CursedWand.HealthTransfer effect = new CursedWand.HealthTransfer();

		Assertions.assertThatCode(() -> effect.effect(null, kit, bolt, true))
				.doesNotThrowAnyException();
		Assertions.assertThat(boss.HP).isGreaterThan(bodyHpBefore);
	}

	@Test
	@DisplayName("Shocking proc applies arc damage when sprites are null")
	void shockingProcAppliesDamageWhenSpritesNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.HP = boss.HT = 500;

		Hero attacker = boss.getEchoHero();
		// The kit mirrors its body's sprite; drop it to exercise the headless path.
		attacker.sprite = null;
		player.sprite = null;

		WornShortsword weapon = new WornShortsword();
		Shocking enchant = new Shocking();

		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 200; i++) {
				enchant.proc(weapon, attacker, player, 12);
			}
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Annoying curse proc skips scream VFX when attacker sprite is null")
	void annoyingProcSkipsScreamWhenAttackerSpriteNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero attacker = boss.getEchoHero();
		// The kit mirrors its body's sprite; drop it to exercise the headless path.
		attacker.sprite = null;

		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 400; i++) {
				new Annoying().proc(new WornShortsword(), attacker, player, 5);
			}
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Explosive curse warm/hot VFX skips when attacker sprite is null")
	void explosiveWarmHotSkipsWhenAttackerSpriteNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero attacker = boss.getEchoHero();
		// The kit mirrors its body's sprite; drop it to exercise the headless path.
		attacker.sprite = null;

		Explosive curse = new Explosive();
		Bundle state = new Bundle();
		// Warm threshold is 50; keep above explosion (0) and preferably above hot (10).
		state.put("durability", 55);
		curse.restoreFromBundle(state);

		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 3; i++) {
				curse.proc(new WornShortsword(), attacker, player, 5);
			}
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Friendly curse proc skips heart VFX when sprites are null")
	void friendlyProcSkipsHeartsWhenSpritesNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero attacker = boss.getEchoHero();
		// The kit mirrors its body's sprite; drop it to exercise the headless path.
		attacker.sprite = null;
		player.sprite = null;

		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 200; i++) {
				new Friendly().proc(new WornShortsword(), attacker, player, 5);
			}
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("AntiEntropy curse proc skips flame VFX when defender sprite is null")
	void antiEntropyProcSkipsFlameWhenDefenderSpriteNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero defender = boss.getEchoHero();
		defender.pos = boss.pos;
		// The kit mirrors its body's sprite; drop it to exercise the headless path.
		defender.sprite = null;
		// Neighbour Freezing VFX needs CellEmitter (GameScene); keep FOV false.
		Arrays.fill(Dungeon.level.heroFOV, false);

		ClothArmor armor = new ClothArmor();
		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 100; i++) {
				new AntiEntropy().proc(armor, player, defender, 5);
			}
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Viscosity deferred damage skips status when target sprite is null")
	void viscosityDeferredSkipsStatusWhenSpriteNull() {
		Hero hero = EchoTestSupport.warriorHero();
		hero.sprite = null;
		Viscosity.ViscosityTracker tracker = Buff.affect(hero, Viscosity.ViscosityTracker.class);

		Assertions.assertThatCode(() -> tracker.deferDamage(20)).doesNotThrowAnyException();
		Assertions.assertThat(hero.buff(Viscosity.DeferedDamage.class)).isNotNull();
	}

	@Test
	@DisplayName("Affection glyph proc skips heart VFX when attacker sprite is null")
	void affectionProcSkipsHeartsWhenAttackerSpriteNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero attacker = boss.getEchoHero();
		// The kit mirrors its body's sprite; drop it to exercise the headless path.
		attacker.sprite = null;

		ClothArmor armor = new ClothArmor();
		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 200; i++) {
				new Affection().proc(armor, attacker, player, 5);
			}
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Paralysis breakout skips status VFX when target sprite is null")
	void paralysisBreakoutSkipsStatusWhenSpriteNull() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		hero.sprite = null;
		hero.HP = 10;

		Paralysis paralysis = Buff.affect(hero, Paralysis.class);
		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 50; i++) {
				if (hero.buff(Paralysis.class) == null) {
					break;
				}
				paralysis.processDamage(1000);
			}
		}).doesNotThrowAnyException();
		Assertions.assertThat(hero.buff(Paralysis.class)).isNull();
	}

	@Test
	@DisplayName("Momentum freerun action skips emitter when target sprite is null")
	void momentumActionSkipsEmitterWhenSpriteNull() {
		Hero hero = EchoTestSupport.warriorHero();
		hero.sprite = null;
		Momentum momentum = Buff.affect(hero, Momentum.class);
		Bundle state = new Bundle();
		state.put("stacks", 4);
		state.put("freerun_turns", 0);
		state.put("freerun_CD", 0);
		momentum.restoreFromBundle(state);

		Assertions.assertThatCode(momentum::doAction).doesNotThrowAnyException();
		Assertions.assertThat(momentum.freerunning()).isTrue();
	}

	@Test
	@DisplayName("HeavyBoomerang CircleBack returns without VFX when sprites have no parent")
	void heavyBoomerangCircleBackWithoutParentDoesNotNpe() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		hero.sprite = null;

		HeavyBoomerang boomerang = new HeavyBoomerang();
		boomerang.spawnedForEffect = true;
		HeavyBoomerang.CircleBack circling = Buff.append(hero, HeavyBoomerang.CircleBack.class);
		circling.setup(boomerang, hero.pos + 1, hero.pos, Dungeon.depth, Dungeon.branch);

		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 8; i++) {
				if (hero.buff(HeavyBoomerang.CircleBack.class) == null) {
					break;
				}
				circling.act();
			}
		}).doesNotThrowAnyException();
		Assertions.assertThat(hero.buff(HeavyBoomerang.CircleBack.class)).isNull();
	}

	@Test
	@DisplayName("Blazing proc applies burning when defender sprite is null")
	void blazingProcAppliesBurningWhenDefenderSpriteNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		player.sprite = null;

		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 200; i++) {
				new Blazing().proc(new WornShortsword(), boss.getEchoHero(), player, 10);
			}
		}).doesNotThrowAnyException();
		Assertions.assertThat(player.buff(Burning.class)).isNotNull();
	}

	@Test
	@DisplayName("Chilling proc applies chill when defender sprite is null")
	void chillingProcAppliesChillWhenDefenderSpriteNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		player.sprite = null;

		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 200; i++) {
				new Chilling().proc(new WornShortsword(), boss.getEchoHero(), player, 10);
			}
		}).doesNotThrowAnyException();
		Assertions.assertThat(player.buff(Chill.class)).isNotNull();
	}

	@Test
	@DisplayName("LivingEarth onHit grants rock armor when attacker sprite is null")
	void livingEarthOnHitWhenAttackerSpriteNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero kit = boss.getEchoHero();
		Assertions.assertThat(kit.sprite).isSameAs(boss.sprite);

		WandOfLivingEarth wand = new WandOfLivingEarth();
		MagesStaff staff = new MagesStaff(wand);
		Assertions.assertThatCode(() -> wand.onHit(staff, kit, player, 20))
				.doesNotThrowAnyException();
		Assertions.assertThat(kit.buff(WandOfLivingEarth.RockArmor.class)).isNotNull();
	}

	@Test
	@DisplayName("Transfusion onHit shields when attacker sprite is null")
	void transfusionOnHitShieldsWhenAttackerSpriteNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero kit = boss.getEchoHero();
		Assertions.assertThat(kit.sprite).isSameAs(boss.sprite);
		Buff.affect(player, Charm.class, Charm.DURATION).object = kit.id();

		WandOfTransfusion wand = new WandOfTransfusion();
		MagesStaff staff = new MagesStaff(wand);
		Assertions.assertThatCode(() -> wand.onHit(staff, kit, player, 10))
				.doesNotThrowAnyException();
		Assertions.assertThat(kit.buff(Barrier.class)).isNotNull();
	}

	@Test
	@DisplayName("BlastWave.blast is a no-op when hero cannot world FX")
	void blastWaveBlastNoopsWhenHeroCannotWorldFx() {
		Hero hero = EchoTestSupport.warriorHero();
		Dungeon.hero = hero;
		hero.sprite = null;
		Assertions.assertThatCode(() -> WandOfBlastWave.BlastWave.blast(0))
				.doesNotThrowAnyException();
	}
}
