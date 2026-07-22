package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpectralBlades;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClericArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.DuelistArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.HuntressArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MageArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.RogueArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.WarriorArmor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

@ExtendWith(GdxTestExtension.class)
class ArmorAbilityActivateAsTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Hero activateAs applies Endure tracker on the hero and spends charge")
	void heroEndureBuffsHeroAndSpendsCharge() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new Endure().activateAs(UseContext.hero(hero), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(hero.buff(
				com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure.EndureTracker.class))
				.isNotNull();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
	}

	@Test
	@DisplayName("Echo activateAs applies NaturesPower buff on the boss body")
	void echoActivateAsBuffsBody() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		HuntressArmor armor = new HuntressArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new NaturesPower().activateAs(UseContext.echo(boss), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(NaturesPower.naturesPowerTracker.class)).isNotNull();
		Assertions.assertThat(boss.getEchoHero().buff(NaturesPower.naturesPowerTracker.class)).isNull();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
	}

	@Test
	@DisplayName("Echo activateAs applies Endure tracker on the boss body")
	void echoEndureBuffsBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new Endure().activateAs(UseContext.echo(boss), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(Endure.EndureTracker.class)).isNotNull();
		Assertions.assertThat(boss.getEchoHero().buff(Endure.EndureTracker.class)).isNull();
	}

	@Test
	@DisplayName("Echo Shockwave damages the player along the cone")
	void echoShockwaveDamagesPlayer() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Assertions.assertThat(player.sprite.ch).isSameAs(player);
		Assertions.assertThat(boss.sprite.ch).isSameAs(boss);
		Assertions.assertThat(boss.getEchoHero().sprite).isNull();

		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;
		int hpBefore = player.HP;
		player.invisible = 1; // guarantee hit path side effects where applicable

		boolean ok = new Shockwave().activateAs(UseContext.echo(boss), armor, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
		Assertions.assertThat(
				player.buff(Paralysis.class) != null || player.buff(Cripple.class) != null)
				.isTrue();
		Assertions.assertThat(boss.getEchoHero().sprite).isNull();
	}

	@Test
	@DisplayName("Echo Shockwave fires cone MagicMissile when the body sprite has a parent")
	void echoShockwaveFiresMagicMissileWhenSpriteHasParent() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(boss);

		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;
		player.invisible = 1;

		boolean ok = new Shockwave().activateAs(UseContext.echo(boss), armor, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(fx.magicMissileRecycles).isGreaterThan(0);
	}

	@Test
	@DisplayName("Echo SmokeBomb teleports the boss body to an empty cell")
	void echoSmokeBombMovesBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		int dest = emptyAdjacent(boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		int start = boss.pos;

		RogueArmor armor = new RogueArmor();
		armor.charge = 100;
		int kitPosBefore = boss.getEchoHero().pos;

		boolean ok = new SmokeBomb().activateAs(UseContext.echo(boss), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.pos).isEqualTo(dest);
		Assertions.assertThat(boss.pos).isNotEqualTo(start);
		Assertions.assertThat(boss.getEchoHero().pos).isEqualTo(kitPosBefore);
		Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(boss)).isEqualTo(dest);
	}

	@Test
	@DisplayName("Echo HeroicLeap places boss sprite at the landing cell")
	void echoHeroicLeapPlacesSpriteAtDest() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		int dest = emptyAdjacent(boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);

		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new HeroicLeap().activateAs(UseContext.echo(boss), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.pos).isEqualTo(dest);
		Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(boss)).isEqualTo(dest);
	}

	@Test
	@DisplayName("Echo HeroicLeap plays jump VFX when the body sprite has a parent")
	void echoHeroicLeapPlaysJumpWhenSpriteHasParent() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		int dest = emptyAdjacent(boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		int jumpsBefore = EchoTestSupport.stubSpriteJumpCalls(boss);

		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new HeroicLeap().activateAs(UseContext.echo(boss), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(EchoTestSupport.stubSpriteJumpCalls(boss))
				.isGreaterThan(jumpsBefore);
	}

	@Test
	@DisplayName("Echo WarpBeacon recall teleports boss to beacon like Hero tele option")
	void echoWarpBeaconRecallTeleportsBody() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		int beacon = boss.pos;
		Dungeon.level.visited[beacon] = true;
		Dungeon.level.mapped[beacon] = true;

		MageArmor armor = new MageArmor();
		armor.charge = 100;
		Hero kit = boss.getEchoHero();

		boolean placed = new WarpBeacon().activateAs(UseContext.echo(boss), armor, beacon);
		Assertions.assertThat(placed).isTrue();
		Assertions.assertThat(kit.buff(WarpBeacon.WarpBeaconTracker.class)).isNotNull();

		int away = emptyAdjacent(boss.pos);
		Assertions.assertThat(away).isGreaterThanOrEqualTo(0);
		boss.pos = away;
		Dungeon.level.occupyCell(boss);

		boolean recalled = new WarpBeacon().activateAs(UseContext.echo(boss), armor, boss.pos);
		Assertions.assertThat(recalled).isTrue();
		Assertions.assertThat(boss.pos).isEqualTo(beacon);
		Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(boss)).isEqualTo(beacon);
	}

	@Test
	@DisplayName("Echo Feint moves boss body and leaves AfterImage at departure cell")
	void echoFeintMovesBodyAndLeavesImage() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		int dest = emptyAdjacent(boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		int start = boss.pos;

		DuelistArmor armor = new DuelistArmor();
		armor.charge = 100;

		boolean ok = new Feint().activateAs(UseContext.echo(boss), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.pos).isEqualTo(dest);
		Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(boss)).isEqualTo(dest);
		Feint.AfterImage image = null;
		for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : Dungeon.level.mobs) {
			if (m instanceof Feint.AfterImage) {
				image = (Feint.AfterImage) m;
				break;
			}
		}
		Assertions.assertThat(image).isNotNull();
		Assertions.assertThat(image.pos).isEqualTo(start);
		Assertions.assertThat(image.alignment).isEqualTo(boss.alignment);
	}

	@Test
	@DisplayName("Echo SpectralBlades hits the player from the boss body position")
	void echoSpectralBladesHitsFromBossPos() {
		Hero player = huntressHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(boss);
		Assertions.assertThat(player.sprite.ch).isSameAs(player);
		Assertions.assertThat(boss.getEchoHero().sprite).isNull();

		HuntressArmor armor = new HuntressArmor();
		armor.charge = 100;
		// Strip player DR so a guaranteed hit always reduces HP (cloth DR can absorb low rolls).
		player.belongings.armor = null;
		int hpBefore = player.HP;
		boss.getEchoHero().invisible = 1; // surprise accuracy for kit.attack
		player.invisible = 0;

		boolean ok = new SpectralBlades().activateAs(UseContext.echo(boss), armor, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(fx.missileSpriteRecycles).isGreaterThan(0);
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
		Assertions.assertThat(boss.getEchoHero().sprite).isNull();
		Assertions.assertThat(boss.getEchoHero().pos).isNotEqualTo(boss.pos);
	}

	@Test
	@DisplayName("Hero AscendedForm dispels Invisibility on the hero")
	void heroAscendedFormDispelsInvisibility() {
		assertHeroAbilityDispelsInvisibility(
				new AscendedForm(), new ClericArmor(), null);
	}

	@Test
	@DisplayName("Hero Endure dispels Invisibility on the hero")
	void heroEndureDispelsInvisibility() {
		assertHeroAbilityDispelsInvisibility(
				new Endure(), new WarriorArmor(), null);
	}

	@Test
	@DisplayName("Hero NaturesPower dispels Invisibility on the hero")
	void heroNaturesPowerDispelsInvisibility() {
		assertHeroAbilityDispelsInvisibility(
				new NaturesPower(), new HuntressArmor(), null);
	}

	@Test
	@DisplayName("Hero Shockwave dispels Invisibility on the hero")
	void heroShockwaveDispelsInvisibility() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		assertHeroAbilityDispelsInvisibility(
				new Shockwave(), new WarriorArmor(), boss.pos, hero, boss);
	}

	@Test
	@DisplayName("Echo AscendedForm dispels Invisibility on the boss body")
	void echoAscendedFormDispelsInvisibility() {
		assertEchoAbilityDispelsBossInvisibility(
				new AscendedForm(), new ClericArmor(), null);
	}

	@Test
	@DisplayName("Echo Endure dispels Invisibility on the boss body")
	void echoEndureDispelsInvisibility() {
		assertEchoAbilityDispelsBossInvisibility(
				new Endure(), new WarriorArmor(), null);
	}

	@Test
	@DisplayName("Echo NaturesPower dispels Invisibility on the boss body")
	void echoNaturesPowerDispelsInvisibility() {
		assertEchoAbilityDispelsBossInvisibility(
				new NaturesPower(), new HuntressArmor(), null);
	}

	@Test
	@DisplayName("Echo Shockwave dispels Invisibility on the boss body")
	void echoShockwaveDispelsInvisibility() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		assertEchoAbilityDispelsBossInvisibility(
				new Shockwave(), new WarriorArmor(), player.pos, player, boss);
	}

	private static void assertHeroAbilityDispelsInvisibility(
			ArmorAbility ability, ClassArmor armor, Integer target) {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		assertHeroAbilityDispelsInvisibility(ability, armor, target, hero, boss);
	}

	private static void assertHeroAbilityDispelsInvisibility(
			ArmorAbility ability,
			ClassArmor armor,
			Integer target,
			Hero hero,
			EchoBoss boss) {
		Buff.affect(hero, Invisibility.class, Invisibility.DURATION);
		Buff.affect(boss, Invisibility.class, Invisibility.DURATION);
		Assertions.assertThat(hero.buff(Invisibility.class)).isNotNull();

		armor.charge = 100;

		boolean ok = ability.activateAs(UseContext.hero(hero), armor, target);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(hero.buff(Invisibility.class)).isNull();
		Assertions.assertThat(boss.buff(Invisibility.class)).isNotNull();
	}

	private static void assertEchoAbilityDispelsBossInvisibility(
			ArmorAbility ability, ClassArmor armor, Integer target) {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		assertEchoAbilityDispelsBossInvisibility(ability, armor, target, player, boss);
	}

	private static void assertEchoAbilityDispelsBossInvisibility(
			ArmorAbility ability,
			ClassArmor armor,
			Integer target,
			Hero player,
			EchoBoss boss) {
		Buff.affect(boss, Invisibility.class, Invisibility.DURATION);
		Buff.affect(player, Invisibility.class, Invisibility.DURATION);
		Assertions.assertThat(boss.buff(Invisibility.class)).isNotNull();

		armor.charge = 100;

		boolean ok = ability.activateAs(UseContext.echo(boss), armor, target);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(Invisibility.class)).isNull();
		Assertions.assertThat(player.buff(Invisibility.class)).isNotNull();
	}

	@Test
	@DisplayName("Echo activateAs Trinity applies bodyForm buff on boss body")
	void echoTrinityAppliesBodyForm() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		ClericArmor armor = new ClericArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		Trinity trinity = new Trinity();
		trinity.imbueBodyForm(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing());

		boolean ok = trinity.activateAs(UseContext.echo(boss), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(BodyForm.BodyFormBuff.class)).isNotNull();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
	}

	@Test
	@DisplayName("activateAs refuses when ClassArmor charge is too low")
	void activateAsRefusesLowCharge() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		HuntressArmor armor = new HuntressArmor();
		armor.charge = 0;
		NaturesPower ability = new NaturesPower();

		Assertions.assertThat(ability.activateAs(UseContext.hero(hero), armor, hero.pos))
				.isFalse();
	}

	private static Hero huntressHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.HUNTRESS.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static int emptyAdjacent(int from) {
		for (int i : PathFinder.NEIGHBOURS8) {
			int cell = from + i;
			if (cell >= 0 && cell < Dungeon.level.length()
					&& Dungeon.level.map[cell] == Terrain.EMPTY
					&& Dungeon.hero.pos != cell
					&& com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell) == null) {
				return cell;
			}
		}
		return -1;
	}
}
