package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BattleAxe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Cudgel;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gauntlet;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HandAxe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarScythe;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Duelist adapter coverage for weapons wired after the initial Scimitar/Rapier
 * migration.
 */
@ExtendWith(GdxTestExtension.class)
class EchoDuelistAdapterMissingWeaponsTest {

	@Test
	@DisplayName("Echo abilityAs BattleAxe heavy blow damages the player from the boss body")
	void echoBattleAxeHeavyBlowHitsFromBossBody() {
		assertHeavyBlowHits(new BattleAxe());
	}

	@Test
	@DisplayName("Echo abilityAs Cudgel heavy blow damages the player from the boss body")
	void echoCudgelHeavyBlowHitsFromBossBody() {
		assertHeavyBlowHits(new Cudgel());
	}

	@Test
	@DisplayName("Echo abilityAs HandAxe heavy blow damages the player from the boss body")
	void echoHandAxeHeavyBlowHitsFromBossBody() {
		assertHeavyBlowHits(new HandAxe());
	}

	@Test
	@DisplayName("Echo abilityAs Gloves combo strike damages the player from the boss body")
	void echoGlovesComboStrikeHitsFromBossBody() {
		assertComboStrikeHits(new Gloves());
	}

	@Test
	@DisplayName("Echo abilityAs Gauntlet combo strike damages the player from the boss body")
	void echoGauntletComboStrikeHitsFromBossBody() {
		assertComboStrikeHits(new Gauntlet());
	}

	@Test
	@DisplayName("Echo abilityAs Greatshield guard buffs the boss body")
	void echoGreatshieldGuardBuffsBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Greatshield shield = new Greatshield();
		equipWeapon(kit, shield, 5);

		boolean ok = EchoDuelistAdapter.useAbility(boss, shield, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(RoundShield.GuardTracker.class)).isNotNull();
		Assertions.assertThat(kit.buff(RoundShield.GuardTracker.class)).isNull();
	}

	@Test
	@DisplayName("Echo abilityAs Katana lunge moves boss body and damages the player")
	void echoKatanaLungeHitsFromBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Katana katana = new Katana();
		equipWeapon(kit, katana, 5);

		boss.fieldOfView = new boolean[Dungeon.level.length()];
		java.util.Arrays.fill(boss.fieldOfView, true);
		player.invisible = 1;
		int hpBefore = player.HP;
		int bossBefore = boss.pos;

		boolean ok = EchoDuelistAdapter.useAbility(boss, katana, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.pos).isNotEqualTo(bossBefore);
		Assertions.assertThat(Dungeon.level.distance(boss.pos, player.pos))
				.isLessThanOrEqualTo(katana.reachFactor(kit));
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
		Assertions.assertThat(kit.pos).isNotEqualTo(boss.pos);
	}

	@Test
	@DisplayName("Echo abilityAs WarScythe harvest applies Bleeding on the player")
	void echoWarScytheHarvestBleedsPlayer() {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		WarScythe scythe = new WarScythe();
		equipWeapon(kit, scythe, 5);
		placeBossAdjacentTo(boss, player);

		boolean ok = EchoDuelistAdapter.useAbility(boss, scythe, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.buff(Bleeding.class)).isNotNull();
	}

	private static void assertHeavyBlowHits(MeleeWeapon weapon) {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		MeleeWeapon equipped = equipWeapon(kit, weapon, 5);
		placeBossAdjacentTo(boss, player);
		player.invisible = 1;

		int hpBefore = player.HP;
		boolean ok = EchoDuelistAdapter.useAbility(boss, equipped, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	private static void assertComboStrikeHits(MeleeWeapon weapon) {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				template, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		MeleeWeapon equipped = equipWeapon(kit, weapon, 5);
		placeBossAdjacentTo(boss, player);

		int hpBefore = player.HP;
		boolean ok = EchoDuelistAdapter.useAbility(boss, equipped, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
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
}
