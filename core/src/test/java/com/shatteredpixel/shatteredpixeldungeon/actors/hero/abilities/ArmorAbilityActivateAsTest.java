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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpectralBlades;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

@ExtendWith(GdxTestExtension.class)
class ArmorAbilityActivateAsTest {

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
	
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
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
	
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
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
	
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
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
	
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
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
	
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
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
	
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
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
	
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
	}

	@Test
	@DisplayName("Echo Challenge activateAs clears busy so the boss turn can resume")
	void echoChallengeClearsBusy() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		Dungeon.level.heroFOV = boss.fieldOfView;

		DuelistArmor armor = new DuelistArmor();
		armor.charge = 100;

		boolean ok = new Challenge().activateAs(UseContext.echo(boss), armor, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.buff(Challenge.DuelParticipant.class)).isNotNull();
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
	}

	@Test
	@DisplayName("Echo SmokeBomb activateAs clears busy so the boss turn can resume")
	void echoSmokeBombClearsBusy() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		int dest = emptyAdjacent(boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);

		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new SmokeBomb().activateAs(UseContext.echo(boss), armor, dest);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
	}

	@Test
	@DisplayName("Echo SmokeBomb shadow-step clears busy so the boss turn can resume")
	void echoSmokeBombShadowStepClearsBusy() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		grantTalent(kit, Talent.SHADOW_STEP, 1);
		kit.invisible = 1;

		int dest = emptyAdjacent(boss.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);

		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new SmokeBomb().activateAs(UseContext.echo(boss), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.pos).isEqualTo(dest);
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
	}

	@Test
	@DisplayName("Echo WildMagic with Conserved Magic free finish clears busy")
	void echoWildMagicConservedMagicClearsBusy() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		Hero kit = boss.getEchoHero();
		grantTalent(kit, Talent.CONSERVED_MAGIC, 4);
		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.cursed = false;
		wand.curCharges = 5;
		wand.collect(kit.belongings.backpack);

		MageArmor armor = new MageArmor();
		armor.charge = 100;

		boolean ok = new WildMagic().activateAs(UseContext.echo(boss), armor, player.pos);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
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
		// Strip player DR so a guaranteed hit always reduces HP (cloth DR can absorb
		// low rolls).
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
	
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
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
		Buff.affect(hero, Invisibility.class, Invisibility.DURATION);
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new Shockwave().activateAs(UseContext.hero(hero), armor, boss.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(hero.buff(Invisibility.class)).isNull();
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
		Buff.affect(boss, Invisibility.class, Invisibility.DURATION);
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100;

		boolean ok = new Shockwave().activateAs(UseContext.echo(boss), armor, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(Invisibility.class)).isNull();
	
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
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
	
		EchoBossTurnAssert.assertCanTakeNextTurn(boss);
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

	private static void grantTalent(Hero kit, Talent talent, int points) {
		while (kit.talents.size() < 4) {
			kit.talents.add(new java.util.LinkedHashMap<>());
		}
		kit.talents.get(3).put(talent, points);
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
