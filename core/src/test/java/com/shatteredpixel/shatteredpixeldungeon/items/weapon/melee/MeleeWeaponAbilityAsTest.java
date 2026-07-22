package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class MeleeWeaponAbilityAsTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Hero abilityAs applies Scimitar SwordDance on the hero")
	void heroScimitarBuffsHero() {
		Hero hero = duelistHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		Scimitar scimitar = equipScimitar(hero, 5);
		float before = hero.cooldown();

		boolean ok = scimitar.abilityAs(UseContext.hero(hero), null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(hero.buff(Scimitar.SwordDance.class)).isNotNull();
		Assertions.assertThat(chargerCharges(hero)).isLessThan(5);
		Assertions.assertThat(hero.cooldown()).isGreaterThanOrEqualTo(before);
	}

	@Test
	@DisplayName("Echo abilityAs applies Scimitar SwordDance on the boss body")
	void echoScimitarBuffsBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Scimitar scimitar = equipScimitar(kit, 5);

		boolean ok = scimitar.abilityAs(UseContext.echo(boss), null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(Scimitar.SwordDance.class)).isNotNull();
		Assertions.assertThat(kit.buff(Scimitar.SwordDance.class)).isNull();
		Assertions.assertThat(chargerCharges(kit)).isLessThan(5);
	}

	@Test
	@DisplayName("Echo abilityAs applies Quarterstaff DefensiveStance on the boss body")
	void echoQuarterstaffBuffsBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Quarterstaff staff = equipQuarterstaff(kit, 5);

		boolean ok = staff.abilityAs(UseContext.echo(boss), null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(Quarterstaff.DefensiveStance.class)).isNotNull();
		Assertions.assertThat(kit.buff(Quarterstaff.DefensiveStance.class)).isNull();
	}

	@Test
	@DisplayName("abilityAs refuses when kit is not a duelist")
	void refusesNonDuelist() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Scimitar scimitar = equipScimitar(kit, 5);

		Assertions.assertThat(scimitar.abilityAs(UseContext.echo(boss), null)).isFalse();
		Assertions.assertThat(boss.buff(Scimitar.SwordDance.class)).isNull();
	}

	@Test
	@DisplayName("abilityAs refuses when Charger has insufficient charges")
	void refusesLowCharge() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Scimitar scimitar = equipScimitar(kit, 0);
		MeleeWeapon.Charger charger = Buff.affect(kit, MeleeWeapon.Charger.class);
		charger.charges = 0;
		charger.partialCharge = 0;

		Assertions.assertThat(scimitar.abilityAs(UseContext.echo(boss), null)).isFalse();
		Assertions.assertThat(boss.buff(Scimitar.SwordDance.class)).isNull();
	}

	@Test
	@DisplayName("Echo abilityAs applies RoundShield GuardTracker on the boss body")
	void echoRoundShieldBuffsBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		RoundShield shield = new RoundShield();
		shield.identify();
		kit.belongings.weapon = shield;
		shield.activate(kit);
		kit.STR = Math.max(kit.STR(), shield.STRReq());
		Buff.affect(kit, MeleeWeapon.Charger.class).charges = 5;

		boolean ok = shield.abilityAs(UseContext.echo(boss), null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(RoundShield.GuardTracker.class)).isNotNull();
		Assertions.assertThat(kit.buff(RoundShield.GuardTracker.class)).isNull();
	}

	@Test
	@DisplayName("Hero abilityAs Rapier lunge moves adjacent and can damage the foe")
	void heroRapierLungeHits() {
		Hero hero = duelistHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		Rapier rapier = equipRapier(hero, 5);
		target.invisible = 1;
		int hpBefore = target.HP;
		int heroBefore = hero.pos;

		boolean ok = rapier.abilityAs(UseContext.hero(hero), target.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(hero.pos).isNotEqualTo(heroBefore);
		Assertions.assertThat(Dungeon.level.distance(hero.pos, target.pos))
				.isLessThanOrEqualTo(rapier.reachFactor(hero));
		Assertions.assertThat(target.HP).isLessThanOrEqualTo(hpBefore);
	}

	@Test
	@DisplayName("Hero abilityAs Rapier out of range refuses without freezing the hero")
	void heroRapierOutOfRangeKeepsHeroReady() {
		Hero hero = duelistHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		// Place boss three cells away — too far for Rapier lunge (needs gap of 1 within
		// reach)
		EchoTestSupport.installEchoBossLevel(hero, target, 3);

		Rapier rapier = equipRapier(hero, 5);
		hero.ready = true;
		int chargeBefore = Buff.affect(hero, MeleeWeapon.Charger.class).charges;

		boolean ok = rapier.abilityAs(UseContext.hero(hero), target.pos);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(hero.ready).isTrue();
		Assertions.assertThat(Buff.affect(hero, MeleeWeapon.Charger.class).charges)
				.isEqualTo(chargeBefore);
	}

	@Test
	@DisplayName("Echo abilityAs Rapier lunge moves boss body and damages the player")
	void echoRapierLungeHitsFromBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Rapier rapier = equipRapier(kit, 5);

		boss.fieldOfView = new boolean[Dungeon.level.length()];
		java.util.Arrays.fill(boss.fieldOfView, true);
		player.invisible = 1;
		int hpBefore = player.HP;
		int bossBefore = boss.pos;

		boolean ok = rapier.abilityAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.pos).isNotEqualTo(bossBefore);
		Assertions.assertThat(Dungeon.level.distance(boss.pos, player.pos))
				.isLessThanOrEqualTo(rapier.reachFactor(kit));
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
		Assertions.assertThat(kit.pos).isNotEqualTo(boss.pos);
	}

	@Test
	@DisplayName("Echo Rapier lunge places boss sprite at the destination before the hit")
	void echoRapierLungePlacesSpriteAtDest() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Rapier rapier = equipRapier(kit, 5);

		boss.fieldOfView = new boolean[Dungeon.level.length()];
		java.util.Arrays.fill(boss.fieldOfView, true);
		player.invisible = 1;
		int bossBefore = boss.pos;

		boolean ok = rapier.abilityAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.pos).isNotEqualTo(bossBefore);
		Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(boss)).isEqualTo(boss.pos);
	}

	@Test
	@DisplayName("Echo Rapier lunge plays jump VFX when the body sprite has a parent")
	void echoRapierLungePlaysJumpWhenSpriteHasParent() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		Hero kit = boss.getEchoHero();
		Rapier rapier = equipRapier(kit, 5);

		boss.fieldOfView = new boolean[Dungeon.level.length()];
		java.util.Arrays.fill(boss.fieldOfView, true);
		player.invisible = 1;
		int jumpsBefore = EchoTestSupport.stubSpriteJumpCalls(boss);

		boolean ok = rapier.abilityAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(EchoTestSupport.stubSpriteJumpCalls(boss))
				.isGreaterThan(jumpsBefore);
	}

	@Test
	@DisplayName("Echo abilityAs Spear spike damages the player from the boss body")
	void echoSpearSpikeHitsFromBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Spear spear = equipSpear(kit, 5);

		int reachCell = cellAtDistanceTwo(player.pos);
		Assertions.assertThat(reachCell).isGreaterThanOrEqualTo(0);
		boss.pos = reachCell;
		Dungeon.level.occupyCell(boss);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		java.util.Arrays.fill(boss.fieldOfView, true);
		player.invisible = 0;

		int hpBefore = player.HP;
		boolean ok = spear.abilityAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo abilityAs Sword cleave damages the player from the boss body")
	void echoSwordCleaveHitsFromBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Sword sword = new Sword();
		sword.identify();
		kit.belongings.weapon = sword;
		sword.activate(kit);
		kit.STR = Math.max(kit.STR(), sword.STRReq());
		Buff.affect(kit, MeleeWeapon.Charger.class).charges = 5;

		// Place boss adjacent to player for cleave reach
		int adj = -1;
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8) {
			int c = player.pos + n;
			if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c]
					&& com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(c) == null) {
				adj = c;
				break;
			}
		}
		Assertions.assertThat(adj).isGreaterThanOrEqualTo(0);
		boss.pos = adj;
		Dungeon.level.occupyCell(boss);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		java.util.Arrays.fill(boss.fieldOfView, true);
		player.invisible = 0;
		kit.invisible = 1; // guarantee hit via kit.attack surprise if used

		int hpBefore = player.HP;
		boolean ok = sword.abilityAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo Sword cleave plays attack VFX when the body sprite has a parent")
	void echoSwordCleavePlaysAttackWhenSpriteHasParent() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		Hero kit = boss.getEchoHero();
		Sword sword = new Sword();
		sword.identify();
		kit.belongings.weapon = sword;
		sword.activate(kit);
		kit.STR = Math.max(kit.STR(), sword.STRReq());
		Buff.affect(kit, MeleeWeapon.Charger.class).charges = 5;

		int adj = -1;
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8) {
			int c = player.pos + n;
			if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c]
					&& com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(c) == null) {
				adj = c;
				break;
			}
		}
		Assertions.assertThat(adj).isGreaterThanOrEqualTo(0);
		boss.pos = adj;
		Dungeon.level.occupyCell(boss);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		java.util.Arrays.fill(boss.fieldOfView, true);
		player.invisible = 0;
		kit.invisible = 1;

		int attacksBefore = EchoTestSupport.stubSpriteAttackCalls(boss);
		boolean ok = sword.abilityAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(EchoTestSupport.stubSpriteAttackCalls(boss))
				.isGreaterThan(attacksBefore);
	}

	@Test
	@DisplayName("Echo Scimitar ability plays operate VFX when the body sprite has a parent")
	void echoScimitarPlaysOperateWhenSpriteHasParent() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		Hero kit = boss.getEchoHero();
		Scimitar scimitar = equipScimitar(kit, 5);
		int operatesBefore = EchoTestSupport.stubSpriteOperateCalls(boss);

		boolean ok = scimitar.abilityAs(UseContext.echo(boss), null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(EchoTestSupport.stubSpriteOperateCalls(boss))
				.isGreaterThan(operatesBefore);
	}

	@Test
	@DisplayName("Echo abilityAs Sai combo strike damages the player from the boss body")
	void echoSaiComboStrikeHitsFromBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Sai sai = equipSai(kit, 5);
		placeBossAdjacentTo(boss, player);

		int hpBefore = player.HP;
		boolean ok = sai.abilityAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo abilityAs Sickle harvest damages the player from the boss body")
	void echoSickleHarvestHitsFromBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Sickle sickle = equipSickle(kit, 5);
		placeBossAdjacentTo(boss, player);

		boolean ok = sickle.abilityAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.buff(Bleeding.class)).isNotNull();
	}

	@Test
	@DisplayName("Echo abilityAs Mace heavy blow damages the player from the boss body")
	void echoMaceHeavyBlowHitsFromBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Mace mace = equipMace(kit, 5);
		placeBossAdjacentTo(boss, player);
		player.invisible = 1;

		int hpBefore = player.HP;
		boolean ok = mace.abilityAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo abilityAs Dagger sneak applies Invisibility on the boss body")
	void echoDaggerSneakBuffsBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Dagger dagger = equipDagger(kit, 5);
		int dest = adjacentEmptyCell(boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);

		boolean ok = dagger.abilityAs(UseContext.echo(boss), dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility.class))
				.isNotNull();
		Assertions.assertThat(boss.pos).isEqualTo(dest);
		Assertions.assertThat(kit.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility.class))
				.isNull();
	}

	@Test
	@DisplayName("Echo Dagger sneak places the sprite at the land cell when parent is attached")
	void echoDaggerSneakPlacesSpriteWhenParentAttached() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		Hero kit = boss.getEchoHero();
		Dagger dagger = equipDagger(kit, 5);
		int dest = adjacentEmptyCell(boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);

		boolean ok = dagger.abilityAs(UseContext.echo(boss), dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(boss))
				.as("sneak relocate must place sprite; wool/puff must also run when canWorldFx")
				.isEqualTo(dest);
	}

	@Test
	@DisplayName("Echo abilityAs Whip damages the player from the boss body")
	void echoWhipHitsFromBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Whip whip = equipWhip(kit, 5);
		placeBossAdjacentTo(boss, player);

		int hpBefore = player.HP;
		boolean ok = whip.abilityAs(UseContext.echo(boss), null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo abilityAs RunicBlade damages the player from the boss body")
	void echoRunicBladeHitsFromBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		RunicBlade blade = equipRunicBlade(kit, 5);
		placeBossAdjacentTo(boss, player);

		int hpBefore = player.HP;
		boolean ok = blade.abilityAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo abilityAs Flail spin stacks SpinAbilityTracker on the kit")
	void echoFlailSpinBuffsKit() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Flail flail = equipFlail(kit, 5);

		boolean ok = flail.abilityAs(UseContext.echo(boss), null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(kit.buff(Flail.SpinAbilityTracker.class)).isNotNull();
		Assertions.assertThat(kit.buff(Flail.SpinAbilityTracker.class).spins).isEqualTo(1);
	}

	@Test
	@DisplayName("Echo abilityAs Crossbow charged shot buffs the kit")
	void echoCrossbowChargedShotBuffsKit() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Crossbow crossbow = equipCrossbow(kit, 5);

		boolean ok = crossbow.abilityAs(UseContext.echo(boss), null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(kit.buff(Crossbow.ChargedShot.class)).isNotNull();
	}

	private static Hero duelistHero() {
		Hero previous = Dungeon.hero;
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.DUELIST.initHero(hero);
		hero.lvl = 10;
		hero.HP = hero.HT = 30;
		if (previous != null) {
			Dungeon.hero = previous;
		}
		return hero;
	}

	private static Scimitar equipScimitar(Hero kit, int charges) {
		Scimitar scimitar = new Scimitar();
		scimitar.identify();
		kit.belongings.weapon = scimitar;
		scimitar.activate(kit);
		kit.STR = Math.max(kit.STR(), scimitar.STRReq());
		MeleeWeapon.Charger charger = Buff.affect(kit, MeleeWeapon.Charger.class);
		charger.charges = charges;
		charger.partialCharge = 0;
		return scimitar;
	}

	private static Spear equipSpear(Hero kit, int charges) {
		Spear spear = new Spear();
		spear.identify();
		kit.belongings.weapon = spear;
		spear.activate(kit);
		kit.STR = Math.max(kit.STR(), spear.STRReq());
		MeleeWeapon.Charger charger = Buff.affect(kit, MeleeWeapon.Charger.class);
		charger.charges = charges;
		charger.partialCharge = 0;
		return spear;
	}

	private static int cellAtDistanceTwo(int target) {
		for (int i = 0; i < Dungeon.level.length(); i++) {
			if (!Dungeon.level.passable[i]
					|| com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(i) != null) {
				continue;
			}
			if (Dungeon.level.distance(i, target) == 2) {
				return i;
			}
		}
		return -1;
	}

	private static void placeBossAdjacentTo(EchoBoss boss, Hero player) {
		int adj = adjacentEmptyCell(player.pos);
		Assertions.assertThat(adj).isGreaterThanOrEqualTo(0);
		boss.pos = adj;
		Dungeon.level.occupyCell(boss);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		java.util.Arrays.fill(boss.fieldOfView, true);
		player.invisible = 0;
	}

	private static int adjacentEmptyCell(int from) {
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8) {
			int c = from + n;
			if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c]
					&& com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(c) == null) {
				return c;
			}
		}
		return -1;
	}

	private static MeleeWeapon equipWeapon(Hero kit, MeleeWeapon weapon, int charges) {
		weapon.identify();
		kit.belongings.weapon = weapon;
		weapon.activate(kit);
		kit.STR = Math.max(kit.STR(), weapon.STRReq());
		MeleeWeapon.Charger charger = Buff.affect(kit, MeleeWeapon.Charger.class);
		charger.charges = charges;
		charger.partialCharge = 0;
		return weapon;
	}

	private static Sai equipSai(Hero kit, int charges) {
		return (Sai) equipWeapon(kit, new Sai(), charges);
	}

	private static Sickle equipSickle(Hero kit, int charges) {
		return (Sickle) equipWeapon(kit, new Sickle(), charges);
	}

	private static Mace equipMace(Hero kit, int charges) {
		return (Mace) equipWeapon(kit, new Mace(), charges);
	}

	private static Dagger equipDagger(Hero kit, int charges) {
		return (Dagger) equipWeapon(kit, new Dagger(), charges);
	}

	private static Whip equipWhip(Hero kit, int charges) {
		return (Whip) equipWeapon(kit, new Whip(), charges);
	}

	private static RunicBlade equipRunicBlade(Hero kit, int charges) {
		return (RunicBlade) equipWeapon(kit, new RunicBlade(), charges);
	}

	private static Flail equipFlail(Hero kit, int charges) {
		return (Flail) equipWeapon(kit, new Flail(), charges);
	}

	private static Crossbow equipCrossbow(Hero kit, int charges) {
		return (Crossbow) equipWeapon(kit, new Crossbow(), charges);
	}

	private static Rapier equipRapier(Hero kit, int charges) {
		Rapier rapier = new Rapier();
		rapier.identify();
		kit.belongings.weapon = rapier;
		rapier.activate(kit);
		kit.STR = Math.max(kit.STR(), rapier.STRReq());
		MeleeWeapon.Charger charger = Buff.affect(kit, MeleeWeapon.Charger.class);
		charger.charges = charges;
		charger.partialCharge = 0;
		return rapier;
	}

	private static Quarterstaff equipQuarterstaff(Hero kit, int charges) {
		Quarterstaff staff = new Quarterstaff();
		staff.identify();
		kit.belongings.weapon = staff;
		staff.activate(kit);
		kit.STR = Math.max(kit.STR(), staff.STRReq());
		MeleeWeapon.Charger charger = Buff.affect(kit, MeleeWeapon.Charger.class);
		charger.charges = charges;
		charger.partialCharge = 0;
		return staff;
	}

	private static int chargerCharges(Hero kit) {
		MeleeWeapon.Charger charger = kit.buff(MeleeWeapon.Charger.class);
		return charger != null ? charger.charges : 0;
	}
}
