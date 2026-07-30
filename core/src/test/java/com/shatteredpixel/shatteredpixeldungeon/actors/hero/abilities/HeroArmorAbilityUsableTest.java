package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpectralBlades;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake;
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
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

/**
 * Happy-path: every {@link ArmorAbility} is usable by the Hero via
 * {@link ArmorAbility#activateAs(UseContext, ClassArmor, Integer)}.
 *
 * <p>Enemy-targeting abilities are checked against both a normal mob and
 * {@link EchoBoss}. Self / summon / UI abilities keep a single fixture.
 */
@ExtendWith(GdxTestExtension.class)
class HeroArmorAbilityUsableTest {

	enum EnemyKind {
		ECHO_BOSS,
		NORMAL_MOB
	}

	static Stream<Arguments> enemyTargets() {
		return Stream.of(
				Arguments.of("EchoBoss", EnemyKind.ECHO_BOSS),
				Arguments.of("normal mob", EnemyKind.NORMAL_MOB));
	}

	// --- Warrior ---

	@Test
	@DisplayName("Hero Endure activateAs applies EndureTracker and spends charge")
	void heroEndure() {
		Fight f = fight(HeroClass.WARRIOR);
		WarriorArmor armor = charged(new WarriorArmor());

		boolean ok = new Endure().activateAs(f.heroCtx(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.hero.buff(Endure.EndureTracker.class)).isNotNull();
		Assertions.assertThat(armor.charge).isEqualTo(50f);
	}

	@ParameterizedTest(name = "Hero Shockwave activateAs damages {0} and spends charge")
	@MethodSource("enemyTargets")
	void heroShockwave(String label, EnemyKind kind) {
		Fight f = fight(HeroClass.WARRIOR, 2, kind);
		WarriorArmor armor = charged(new WarriorArmor());
		int hpBefore = f.enemy.HP;
		f.enemy.invisible = 1;

		boolean ok = new Shockwave().activateAs(f.heroCtx(), armor, f.enemy.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.enemy.HP).isLessThan(hpBefore);
		Assertions.assertThat(
				f.enemy.buff(Paralysis.class) != null || f.enemy.buff(Cripple.class) != null)
				.isTrue();
		Assertions.assertThat(armor.charge).isEqualTo(65f);
	}

	@Test
	@DisplayName("Hero HeroicLeap activateAs moves the hero and spends charge")
	void heroHeroicLeap() {
		Fight f = fight(HeroClass.WARRIOR);
		EchoTestSupport.attachInstantProjectileParent(f.hero);
		int dest = emptyAdjacent(f.hero.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		WarriorArmor armor = charged(new WarriorArmor());

		boolean ok = new HeroicLeap().activateAs(f.heroCtx(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.hero.pos).isEqualTo(dest);
		Assertions.assertThat(armor.charge).isEqualTo(65f);
	}

	// --- Mage ---

	@ParameterizedTest(name = "Hero ElementalBlast activateAs damages {0} with imbued staff")
	@MethodSource("enemyTargets")
	void heroElementalBlast(String label, EnemyKind kind) {
		Fight f = fight(HeroClass.MAGE, 1, kind);
		f.hero.belongings.weapon = new MagesStaff(new WandOfMagicMissile());
		MageArmor armor = charged(new MageArmor());
		EchoTestSupport.attachInstantProjectileParent(f.hero);
		int hpBefore = f.enemy.HP;

		boolean ok = new ElementalBlast().activateAs(f.heroCtx(), armor, f.enemy.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.enemy.HP).isLessThan(hpBefore);
		Assertions.assertThat(armor.charge).isEqualTo(65f);
	}

	@ParameterizedTest(name = "Hero WildMagic activateAs damages {0} from backpack wands")
	@MethodSource("enemyTargets")
	void heroWildMagic(String label, EnemyKind kind) {
		Fight f = fight(HeroClass.MAGE, 2, kind);
		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.cursed = false;
		wand.curCharges = 5;
		wand.collect(f.hero.belongings.backpack);
		grantTalent(f.hero, Talent.CONSERVED_MAGIC, 4);
		MageArmor armor = charged(new MageArmor());
		EchoTestSupport.attachInstantProjectileParent(f.hero);
		int hpBefore = f.enemy.HP;

		boolean ok = new WildMagic().activateAs(f.heroCtx(), armor, f.enemy.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.enemy.HP).isLessThan(hpBefore);
		Assertions.assertThat(armor.charge).isEqualTo(75f);
	}

	@Test
	@DisplayName("Hero WarpBeacon activateAs places WarpBeaconTracker on the hero")
	void heroWarpBeacon() {
		Fight f = fight(HeroClass.MAGE, 2);
		int beacon = f.hero.pos;
		Dungeon.level.visited[beacon] = true;
		Dungeon.level.mapped[beacon] = true;
		MageArmor armor = charged(new MageArmor());

		boolean ok = new WarpBeacon().activateAs(f.heroCtx(), armor, beacon);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.hero.buff(WarpBeacon.WarpBeaconTracker.class)).isNotNull();
	}

	// --- Rogue ---

	@Test
	@DisplayName("Hero SmokeBomb activateAs teleports the hero and spends charge")
	void heroSmokeBomb() {
		Fight f = fight(HeroClass.ROGUE);
		int dest = emptyAdjacent(f.hero.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		RogueArmor armor = charged(new RogueArmor());

		boolean ok = new SmokeBomb().activateAs(f.heroCtx(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.hero.pos).isEqualTo(dest);
		Assertions.assertThat(armor.charge).isEqualTo(50f);
	}

	@ParameterizedTest(name = "Hero DeathMark activateAs marks {0} and spends charge")
	@MethodSource("enemyTargets")
	void heroDeathMark(String label, EnemyKind kind) {
		Fight f = fight(HeroClass.ROGUE, 2, kind);
		fillHeroFov();
		RogueArmor armor = charged(new RogueArmor());

		boolean ok = new DeathMark().activateAs(f.heroCtx(), armor, f.enemy.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.enemy.buff(DeathMark.DeathMarkTracker.class)).isNotNull();
		Assertions.assertThat(armor.charge).isEqualTo(75f);
	}

	@Test
	@DisplayName("Hero ShadowClone activateAs spawns a ShadowAlly beside the hero")
	void heroShadowClone() {
		Fight f = fight(HeroClass.ROGUE);
		RogueArmor armor = charged(new RogueArmor());

		boolean ok = new ShadowClone().activateAs(f.heroCtx(), armor, null);

		Assertions.assertThat(ok).isTrue();
		ShadowClone.ShadowAlly ally = findMob(ShadowClone.ShadowAlly.class);
		Assertions.assertThat(ally).isNotNull();
		Assertions.assertThat(Dungeon.level.distance(f.hero.pos, ally.pos)).isEqualTo(1);
		Assertions.assertThat(armor.charge).isEqualTo(65f);
	}

	// --- Huntress ---

	@ParameterizedTest(name = "Hero SpectralBlades activateAs damages {0} and spends charge")
	@MethodSource("enemyTargets")
	void heroSpectralBlades(String label, EnemyKind kind) {
		Fight f = fight(HeroClass.HUNTRESS, 2, kind);
		fillHeroFov();
		f.hero.fieldOfView = Dungeon.level.heroFOV;
		HuntressArmor armor = charged(new HuntressArmor());
		EchoTestSupport.attachInstantProjectileParent(f.hero);
		// Guarantee surprise-hit accuracy (SpiritBow STR gate + dodge RNG).
		f.hero.STR = 20;
		f.hero.invisible = 1;
		int hpBefore = f.enemy.HP;

		boolean ok = new SpectralBlades().activateAs(f.heroCtx(), armor, f.enemy.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.enemy.HP).isLessThan(hpBefore);
		Assertions.assertThat(armor.charge).isEqualTo(75f);
	}

	@Test
	@DisplayName("Hero NaturesPower activateAs applies tracker and spends charge")
	void heroNaturesPower() {
		Fight f = fight(HeroClass.HUNTRESS);
		HuntressArmor armor = charged(new HuntressArmor());

		boolean ok = new NaturesPower().activateAs(f.heroCtx(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.hero.buff(NaturesPower.naturesPowerTracker.class)).isNotNull();
		Assertions.assertThat(armor.charge).isEqualTo(65f);
	}

	@Test
	@DisplayName("Hero SpiritHawk activateAs spawns a HawkAlly beside the hero")
	void heroSpiritHawk() {
		Fight f = fight(HeroClass.HUNTRESS);
		HuntressArmor armor = charged(new HuntressArmor());

		boolean ok = new SpiritHawk().activateAs(f.heroCtx(), armor, null);

		Assertions.assertThat(ok).isTrue();
		SpiritHawk.HawkAlly hawk = findMob(SpiritHawk.HawkAlly.class);
		Assertions.assertThat(hawk).isNotNull();
		Assertions.assertThat(Dungeon.level.distance(f.hero.pos, hawk.pos)).isEqualTo(1);
		Assertions.assertThat(armor.charge).isEqualTo(65f);
	}

	// --- Duelist ---

	@ParameterizedTest(name = "Hero Challenge activateAs applies DuelParticipant on hero and {0}")
	@MethodSource("enemyTargets")
	void heroChallenge(String label, EnemyKind kind) {
		Fight f = fight(HeroClass.DUELIST, 2, kind);
		fillHeroFov();
		DuelistArmor armor = charged(new DuelistArmor());

		boolean ok = new Challenge().activateAs(f.heroCtx(), armor, f.enemy.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.hero.buff(Challenge.DuelParticipant.class)).isNotNull();
		Assertions.assertThat(f.enemy.buff(Challenge.DuelParticipant.class)).isNotNull();
		Assertions.assertThat(armor.charge).isEqualTo(65f);
	}

	@ParameterizedTest(name = "Hero ElementalStrike activateAs damages {0} and spends charge")
	@MethodSource("enemyTargets")
	void heroElementalStrike(String label, EnemyKind kind) {
		Fight f = fight(HeroClass.DUELIST, 2, kind);
		f.hero.belongings.weapon = new WornShortsword();
		DuelistArmor armor = charged(new DuelistArmor());
		EchoTestSupport.attachInstantProjectileParent(f.hero);
		f.enemy.invisible = 1;
		int hpBefore = f.enemy.HP;

		boolean ok = new ElementalStrike().activateAs(f.heroCtx(), armor, f.enemy.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.enemy.HP).isLessThan(hpBefore);
		Assertions.assertThat(armor.charge).isEqualTo(75f);
	}

	@Test
	@DisplayName("Hero Feint activateAs moves the hero and leaves AfterImage")
	void heroFeint() {
		Fight f = fight(HeroClass.DUELIST, 2);
		int dest = emptyAdjacent(f.hero.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		int start = f.hero.pos;
		DuelistArmor armor = charged(new DuelistArmor());

		boolean ok = new Feint().activateAs(f.heroCtx(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.hero.pos).isEqualTo(dest);
		Feint.AfterImage image = findMob(Feint.AfterImage.class);
		Assertions.assertThat(image).isNotNull();
		Assertions.assertThat(image.pos).isEqualTo(start);
		Assertions.assertThat(armor.charge).isEqualTo(50f);
	}

	// --- Cleric ---

	@Test
	@DisplayName("Hero AscendedForm activateAs applies AscendBuff and spends charge")
	void heroAscendedForm() {
		Fight f = fight(HeroClass.CLERIC);
		ClericArmor armor = charged(new ClericArmor());

		boolean ok = new AscendedForm().activateAs(f.heroCtx(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.hero.buff(AscendedForm.AscendBuff.class)).isNotNull();
		Assertions.assertThat(armor.charge).isEqualTo(50f);
	}

	@Test
	@DisplayName("Hero Trinity activateAs without imbue returns true (warns; UI needs imbue)")
	void heroTrinityWithoutImbue() {
		Fight f = fight(HeroClass.CLERIC);
		ClericArmor armor = charged(new ClericArmor());

		boolean ok = new Trinity().activateAs(f.heroCtx(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isEqualTo(100f);
	}

	@Test
	@DisplayName("Hero Trinity imbueBodyForm is accepted for later UI activation")
	void heroTrinityImbueAccepted() {
		Fight f = fight(HeroClass.CLERIC);
		Trinity trinity = new Trinity();
		trinity.imbueBodyForm(new Blazing());

		Assertions.assertThat(trinity.chargeUse(f.hero)).isGreaterThan(0f);
	}

	@Test
	@DisplayName("Hero PowerOfMany activateAs summons LightAlly on empty FOV cell")
	void heroPowerOfMany() {
		Fight f = fight(HeroClass.CLERIC);
		int dest = emptyAdjacent(f.hero.pos);
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		Assertions.assertThat(Dungeon.level.heroFOV[dest]).isTrue();
		ClericArmor armor = charged(new ClericArmor());

		boolean ok = new PowerOfMany().activateAs(f.heroCtx(), armor, dest);

		Assertions.assertThat(ok).isTrue();
		PowerOfMany.LightAlly ally = findMob(PowerOfMany.LightAlly.class);
		Assertions.assertThat(ally).isNotNull();
		Assertions.assertThat(ally.pos).isEqualTo(dest);
		Assertions.assertThat(ally.buff(PowerOfMany.PowerBuff.class)).isNotNull();
		Assertions.assertThat(ally.buff(Barrier.class)).isNotNull();
		Assertions.assertThat(armor.charge).isEqualTo(65f);
	}

	// --- Shared / Ratmogrify ---

	@Test
	@DisplayName("Hero Ratmogrify activateAs transforms a normal mob and spends charge")
	void heroRatmogrifyTransformsNormalMob() {
		Fight f = fight(HeroClass.ROGUE);
		int cell = emptyAdjacent(f.hero.pos);
		Assertions.assertThat(cell).isGreaterThanOrEqualTo(0);
		Snake snake = placeSnake(cell);
		fillHeroFov();
		RogueArmor armor = charged(new RogueArmor());

		boolean ok = new Ratmogrify().activateAs(f.heroCtx(), armor, cell);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(findMob(Snake.class)).isNull();
		Assertions.assertThat(findMob(Ratmogrify.TransmogRat.class)).isNotNull();
		Assertions.assertThat(armor.charge).isEqualTo(50f);
		Assertions.assertThat(Actor.findChar(cell)).isInstanceOf(Ratmogrify.TransmogRat.class);
		// Original fight enemy (EchoBoss) must remain untouched.
		Assertions.assertThat(f.enemy.isAlive()).isTrue();
		Assertions.assertThat(f.enemy).isInstanceOf(EchoBoss.class);
	}

	@Test
	@DisplayName("Hero Ratmogrify activateAs refuses EchoBoss without spending charge")
	void heroRatmogrifyRefusesEchoBoss() {
		Fight f = fight(HeroClass.ROGUE, 2, EnemyKind.ECHO_BOSS);
		fillHeroFov();
		RogueArmor armor = charged(new RogueArmor());
		int hpBefore = f.enemy.HP;

		boolean ok = new Ratmogrify().activateAs(f.heroCtx(), armor, f.enemy.pos);

		Assertions.assertThat(ok).isTrue(); // charge gate passed; activate refuses transform
		Assertions.assertThat(armor.charge).isEqualTo(100f);
		Assertions.assertThat(f.enemy).isInstanceOf(EchoBoss.class);
		Assertions.assertThat(f.enemy.HP).isEqualTo(hpBefore);
		Assertions.assertThat(findMob(Ratmogrify.TransmogRat.class)).isNull();
	}

	@Test
	@DisplayName("HeroClass armorAbilities covers every class ability exercised above")
	void heroClassArmorAbilitiesAreCovered() {
		Assertions.assertThat(abilityTypes(HeroClass.WARRIOR))
				.containsExactly(HeroicLeap.class, Shockwave.class, Endure.class);
		Assertions.assertThat(abilityTypes(HeroClass.MAGE))
				.containsExactly(ElementalBlast.class, WildMagic.class, WarpBeacon.class);
		Assertions.assertThat(abilityTypes(HeroClass.ROGUE))
				.containsExactly(SmokeBomb.class, DeathMark.class, ShadowClone.class);
		Assertions.assertThat(abilityTypes(HeroClass.HUNTRESS))
				.containsExactly(SpectralBlades.class, NaturesPower.class, SpiritHawk.class);
		Assertions.assertThat(abilityTypes(HeroClass.DUELIST))
				.containsExactly(Challenge.class, ElementalStrike.class, Feint.class);
		Assertions.assertThat(abilityTypes(HeroClass.CLERIC))
				.containsExactly(AscendedForm.class, Trinity.class, PowerOfMany.class);
	}

	private static Class<?>[] abilityTypes(HeroClass heroClass) {
		ArmorAbility[] abilities = heroClass.armorAbilities();
		Class<?>[] types = new Class<?>[abilities.length];
		for (int i = 0; i < abilities.length; i++) {
			types[i] = abilities[i].getClass();
		}
		return types;
	}

	private static Fight fight(HeroClass heroClass) {
		return fight(heroClass, 1, EnemyKind.ECHO_BOSS);
	}

	private static Fight fight(HeroClass heroClass, int enemyOffset) {
		return fight(heroClass, enemyOffset, EnemyKind.ECHO_BOSS);
	}

	private static Fight fight(HeroClass heroClass, int enemyOffset, EnemyKind kind) {
		Hero hero = heroOf(heroClass);
		Mob enemy = createEnemy(hero, kind);
		EchoTestSupport.installEchoBossLevel(hero, enemy, enemyOffset);
		return new Fight(hero, enemy);
	}

	private static Mob createEnemy(Hero hero, EnemyKind kind) {
		Mob enemy;
		if (kind == EnemyKind.ECHO_BOSS) {
			enemy = EchoTestSupport.createBossWithPolicy(
					hero, EchoTestSupport.healCapabilityPolicy(), 5);
		} else {
			Snake snake = new Snake();
			snake.HP = snake.HT = 40;
			enemy = snake;
		}
		// Usability tests assert damage landed — don't let dodge RNG flake the suite.
		enemy.defenseSkill = 0;
		return enemy;
	}

	private static Snake placeSnake(int cell) {
		Snake snake = new Snake();
		snake.HP = snake.HT = 40;
		snake.pos = cell;
		EchoTestSupport.linkStubSprite(snake);
		Dungeon.level.mobs.add(snake);
		Actor.add(snake);
		return snake;
	}

	private static Hero heroOf(HeroClass heroClass) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		heroClass.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 40;
		return hero;
	}

	private static <A extends ClassArmor> A charged(A armor) {
		armor.charge = 100;
		return armor;
	}

	private static void fillHeroFov() {
		Dungeon.level.heroFOV = new boolean[Dungeon.level.length()];
		Arrays.fill(Dungeon.level.heroFOV, true);
	}

	private static void grantTalent(Hero hero, Talent talent, int points) {
		while (hero.talents.size() < 4) {
			hero.talents.add(new LinkedHashMap<>());
		}
		hero.talents.get(3).put(talent, points);
	}

	private static int emptyAdjacent(int from) {
		for (int i : PathFinder.NEIGHBOURS8) {
			int cell = from + i;
			if (cell >= 0 && cell < Dungeon.level.length()
					&& Dungeon.level.map[cell] == Terrain.EMPTY
					&& Dungeon.hero.pos != cell
					&& Actor.findChar(cell) == null) {
				return cell;
			}
		}
		return -1;
	}

	private static <T extends Mob> T findMob(Class<T> type) {
		for (Mob m : Dungeon.level.mobs) {
			if (type.isInstance(m)) {
				return type.cast(m);
			}
		}
		return null;
	}

	private static final class Fight {
		final Hero hero;
		final Char enemy;

		Fight(Hero hero, Char enemy) {
			this.hero = hero;
			this.enemy = enemy;
		}

		UseContext heroCtx() {
			return UseContext.hero(hero);
		}
	}
}
