package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClericArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.DuelistArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.HuntressArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MageArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.RogueArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Canvas-aligned Echo armor-ability kinds not already covered by
 * ArmorAbilityActivateAsTest.
 */
@ExtendWith(GdxTestExtension.class)
class EchoBossArsenalArmorAbilityKindsTest {

	private static Fight fight() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 1);
		return new Fight(player, boss);
	}

	private static final class Fight {
		final Hero player;
		final EchoBoss boss;

		Fight(Hero player, EchoBoss boss) {
			this.player = player;
			this.boss = boss;
		}

		UseContext echo() {
			return UseContext.echo(boss);
		}
	}

	@Test
	@DisplayName("Echo DeathMark activateAs marks the player from boss body aim")
	void deathMarkMarksPlayer() {
		Fight f = fight();
		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new DeathMark().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.player.buff(DeathMark.DeathMarkTracker.class)).isNotNull();
		Assertions.assertThat(armor.charge).isLessThan(100);
	}

	@Test
	@DisplayName("Echo DeathMark activateAs clears busy so the boss turn can resume")
	void deathMarkClearsBusy() {
		Fight f = fight();
		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new DeathMark().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.boss.isBusy())
				.as("DeathMark must spendAfterThrow for Echo — kit.next leaves the boss stuck")
				.isFalse();
	}

	@Test
	@DisplayName("Echo ElementalStrike activateAs damages the Hero from boss body aim")
	void elementalStrikeDamagesHero() {
		Fight f = fight();
		DuelistArmor armor = new DuelistArmor();
		armor.charge = 100;
		f.boss.getEchoHero().belongings.weapon = new WornShortsword();
		EchoTestSupport.attachInstantProjectileParent(f.boss);
		int hpBefore = f.player.HP;

		boolean ok = new ElementalStrike().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo WildMagic activateAs fires frost wand VFX without NPE when kit is headless")
	void wildMagicFrostWandDoesNotNpeWhenKitHeadless() {
		Hero player = new Hero();
		Dungeon.hero = player;
		HeroClass.MAGE.initHero(player);
		player.lvl = 6;
		player.HP = player.HT = 40;

		WandOfFrost seed = new WandOfFrost();
		seed.curCharges = 5;
		seed.collect(player.belongings.backpack);

		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, frostWandPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(boss);

		Assertions.assertThat(boss.getEchoHero().sprite).isNull();
		WandOfFrost wand = boss.getEchoHero().belongings.getItem(WandOfFrost.class);
		Assertions.assertThat(wand).isNotNull();
		wand.cursed = false;
		wand.curCharges = 5;

		MageArmor armor = new MageArmor();
		armor.charge = 100;

		Assertions.assertThatCode(() -> new WildMagic().activateAs(UseContext.echo(boss), armor, player.pos))
				.doesNotThrowAnyException();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(fx.magicMissileRecycles).isGreaterThan(0);
	}

	@Test
	@DisplayName("Echo WildMagic activateAs fires disintegration DeathRay without NPE when kit is headless")
	void wildMagicDisintegrationDoesNotNpeWhenKitHeadless() {
		Hero player = new Hero();
		Dungeon.hero = player;
		HeroClass.MAGE.initHero(player);
		player.lvl = 6;
		player.HP = player.HT = 40;

		WandOfDisintegration seed = new WandOfDisintegration();
		seed.curCharges = 5;
		seed.collect(player.belongings.backpack);

		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, disintegrationWandPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		Assertions.assertThat(boss.getEchoHero().sprite).isNull();
		WandOfDisintegration wand = boss.getEchoHero().belongings.getItem(WandOfDisintegration.class);
		Assertions.assertThat(wand).isNotNull();
		wand.cursed = false;
		wand.curCharges = 5;

		MageArmor armor = new MageArmor();
		armor.charge = 100;
		int hpBefore = player.HP;

		Assertions.assertThatCode(() -> new WildMagic().activateAs(UseContext.echo(boss), armor, player.pos))
				.doesNotThrowAnyException();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo ShadowClone activateAs spawns a ShadowAlly beside the boss body")
	void shadowCloneSpawnsAllyBesideBoss() {
		Fight f = fight();
		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new ShadowClone().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(f.boss.isBusy()).isFalse();
		ShadowClone.ShadowAlly ally = findMob(ShadowClone.ShadowAlly.class);
		Assertions.assertThat(ally).isNotNull();
		Assertions.assertThat(Dungeon.level.distance(f.boss.pos, ally.pos)).isEqualTo(1);
	}

	@Test
	@DisplayName("Echo SpiritHawk activateAs spawns a HawkAlly beside the boss body")
	void spiritHawkSpawnsAllyBesideBoss() {
		Fight f = fight();
		HuntressArmor armor = new HuntressArmor();
		armor.charge = 100;

		boolean ok = new SpiritHawk().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(f.boss.isBusy()).isFalse();
		SpiritHawk.HawkAlly hawk = findMob(SpiritHawk.HawkAlly.class);
		Assertions.assertThat(hawk).isNotNull();
		Assertions.assertThat(Dungeon.level.distance(f.boss.pos, hawk.pos)).isEqualTo(1);
	}

	@Test
	@DisplayName("Echo AscendedForm activateAs applies AscendBuff on the boss body")
	void ascendedFormAppliesAscendBuffOnBoss() {
		Fight f = fight();
		ClericArmor armor = new ClericArmor();
		armor.charge = 100;

		boolean ok = new AscendedForm().activateAs(f.echo(), armor, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(f.boss.isBusy()).isFalse();
		Assertions.assertThat(f.boss.buff(AscendedForm.AscendBuff.class)).isNotNull();
		Assertions.assertThat(f.boss.getEchoHero().buff(AscendedForm.AscendBuff.class)).isNull();
	}

	@Test
	@DisplayName("Echo Ratmogrify activateAs transforms an enemy into a TransmogRat")
	void ratmogrifyTransformsEnemyBesideBoss() {
		Fight f = fight();
		int cell = emptyAdjacent(f.boss.pos);
		Assertions.assertThat(cell).isGreaterThanOrEqualTo(0);

		Snake snake = new Snake();
		snake.pos = cell;
		EchoTestSupport.linkStubSprite(snake);
		Dungeon.level.mobs.add(snake);
		Actor.add(snake);

		RogueArmor armor = new RogueArmor();
		armor.charge = 100;

		boolean ok = new Ratmogrify().activateAs(f.echo(), armor, cell);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(100);
		Assertions.assertThat(f.boss.isBusy()).isFalse();
		Assertions.assertThat(findMob(Snake.class)).isNull();
		Ratmogrify.TransmogRat rat = findMob(Ratmogrify.TransmogRat.class);
		Assertions.assertThat(rat).isNotNull();
		Assertions.assertThat(rat.pos).isEqualTo(cell);
	}

	private static <T extends Mob> T findMob(Class<T> type) {
		for (Mob m : Dungeon.level.mobs) {
			if (type.isInstance(m)) {
				return type.cast(m);
			}
		}
		return null;
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

	private static EchoPolicy frostWandPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("WandOfFrost")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy disintegrationWandPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("WandOfDisintegration")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}
}
