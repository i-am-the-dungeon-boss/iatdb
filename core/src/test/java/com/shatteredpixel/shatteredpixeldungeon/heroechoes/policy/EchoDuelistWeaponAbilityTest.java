package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * Duelist weapon abilities must actually fire: one weapon from each of the
 * backend's four buckets (lunge / reach / buff / strike) reads as ready,
 * executes, and spends charge — and every "not actually usable" case falls
 * through to plain melee rather than wasting the turn.
 */
@ExtendWith(GdxTestExtension.class)
class EchoDuelistWeaponAbilityTest {

	@Test
	@DisplayName("lunge bucket: an equipped, charged Rapier fires WEAPON_ABILITY and spends charge")
	void lungeWeaponFires() {
		assertAbilityFires(new Rapier(), 2);
	}

	@Test
	@DisplayName("reach bucket: an equipped, charged Spear fires WEAPON_ABILITY and spends charge")
	void reachWeaponFires() {
		assertAbilityFires(new Spear(), 2);
	}

	@Test
	@DisplayName("buff bucket: an equipped, charged Greatshield fires WEAPON_ABILITY and spends charge")
	void buffWeaponFires() {
		assertAbilityFires(new Greatshield(), 1);
	}

	@Test
	@DisplayName("strike bucket: an equipped, charged Sword fires WEAPON_ABILITY and spends charge")
	void strikeWeaponFires() {
		assertAbilityFires(new Sword(), 1);
	}

	@Test
	@DisplayName("an unequipped weapon is not a ready WEAPON_ABILITY")
	void unequippedWeaponIsNotReady() {
		Fixture f = fixture(new Sword(), 10, false, true, 1);

		Assertions.assertThat(status(f).isRoleReady(EchoPolicyHazards.WEAPON_ABILITY)).isFalse();
	}

	@Test
	@DisplayName("a weapon the echo cannot lift is not a ready WEAPON_ABILITY")
	void underStrengthWeaponIsNotReady() {
		Fixture f = fixture(new Greatshield(), 10, true, true, 1);
		f.kit.STR = 1;

		Assertions.assertThat(status(f).isRoleReady(EchoPolicyHazards.WEAPON_ABILITY)).isFalse();
	}

	@Test
	@DisplayName("an uncharged weapon is not a ready WEAPON_ABILITY")
	void unchargedWeaponIsNotReady() {
		Fixture f = fixture(new Rapier(), 0, true, true, 2);

		Assertions.assertThat(status(f).isRoleReady(EchoPolicyHazards.WEAPON_ABILITY)).isFalse();
	}

	@Test
	@DisplayName("sensing readiness never attaches a Charger as a side effect")
	void sensingDoesNotAttachACharger() {
		Fixture f = fixture(new Rapier(), 10, true, true, 2);
		f.kit.buff(MeleeWeapon.Charger.class).detach();

		EchoPolicyStatus status = status(f);

		Assertions.assertThat(f.kit.buff(MeleeWeapon.Charger.class)).isNull();
		Assertions.assertThat(status.isRoleReady(EchoPolicyHazards.WEAPON_ABILITY)).isFalse();
	}

	@Test
	@DisplayName("out of LOS a targeted ability falls through to melee instead of wasting the turn")
	void outOfLosFallsThroughToMelee() {
		Fixture f = fixture(new Rapier(), 10, true, false, 2);

		boolean spent = EchoRoleExecutor.execute(f.boss, f.policy, status(f),
				new EchoPlan(EchoPolicyHazards.WEAPON_ABILITY, "reactions", null));

		Assertions.assertThat(spent).isFalse();
	}

	private static void assertAbilityFires(MeleeWeapon weapon, int distance) {
		Fixture f = fixture(weapon, 10, true, true, distance);
		EchoPolicyStatus status = status(f);
		Assertions.assertThat(status.isRoleReady(EchoPolicyHazards.WEAPON_ABILITY)).isTrue();
		float chargeBefore = charge(f.kit);

		boolean spent = EchoRoleExecutor.execute(f.boss, f.policy, status,
				new EchoPlan(EchoPolicyHazards.WEAPON_ABILITY, "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(charge(f.kit)).isLessThan(chargeBefore);
	}

	private static float charge(Hero kit) {
		MeleeWeapon.Charger charger = kit.buff(MeleeWeapon.Charger.class);
		return charger == null ? 0f : charger.charges + charger.partialCharge;
	}

	private static EchoPolicyStatus status(Fixture f) {
		return EchoPolicyStatusBuilder.build(f.boss, f.policy);
	}

	private static final class Fixture {
		EchoBoss boss;
		Hero kit;
		EchoPolicy policy;
	}

	private static Fixture fixture(
			MeleeWeapon weapon, int charges, boolean equipped, boolean inLos, int distance) {
		Hero player = EchoTestSupport.warriorHero();
		Hero template = duelistHero();
		EchoPolicy policy = EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put(EchoPolicyHazards.WEAPON_ABILITY,
						EchoTestSupport.capability(weapon.getClass().getSimpleName()))
				.put(EchoPolicyHazards.MELEE, EchoTestSupport.capability("*melee")));
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(template, policy, 5);
		EchoTestSupport.installEchoBossLevel(player, boss, distance);
		EchoTestSupport.linkStubSprite(boss);

		Hero kit = boss.getEchoHero();
		weapon.identify();
		if (equipped) {
			kit.belongings.weapon = weapon;
			weapon.activate(kit);
		} else {
			weapon.collect(kit.belongings.backpack);
		}
		kit.STR = Math.max(kit.STR(), weapon.STRReq());
		MeleeWeapon.Charger charger = Buff.affect(kit, MeleeWeapon.Charger.class);
		charger.charges = charges;
		charger.partialCharge = 0;

		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, inLos);
		player.invisible = inLos ? 0 : 1;

		Fixture f = new Fixture();
		f.boss = boss;
		f.kit = kit;
		f.policy = policy;
		return f;
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
}
