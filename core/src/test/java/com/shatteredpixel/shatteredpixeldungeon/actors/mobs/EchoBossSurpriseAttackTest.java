package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greataxe;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Simulates Mob.surprisedBy / Char.hit surprise paths for Hero ↔ EchoBoss.
 * Documents current behavior (including EchoBoss defenseSkill override).
 */
@ExtendWith(GdxTestExtension.class)
class EchoBossSurpriseAttackTest {

	private Hero hero;
	private EchoBoss boss;

	@BeforeEach
	void setUp() {
		EchoTestSupport.resetWorkflowState();
		hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.ROGUE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 40;
		boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		// Adjacent for melee / attack simulations
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		prepareSeenInFov();
	}

	@Test
	@DisplayName("EchoBoss allocates fieldOfView when hunting")
	void echoBossAllocatesFieldOfViewWhenHunting() {
		boss.fieldOfView = null;

		boss.state = boss.HUNTING;
		boss.aggro(hero);
		boss.act();

		Assertions.assertThat(boss.fieldOfView).isNotNull();
		Assertions.assertThat(boss.fieldOfView).hasSize(Dungeon.level.length());
		Assertions.assertThat(boss.fieldOfView[hero.pos])
				.as("open 7x7 fight fixture should see adjacent hero")
				.isTrue();
	}

	@Test
	@DisplayName("visible hero in FOV with enemySeen is not a surprise")
	void visibleHeroInFovWithEnemySeenIsNotSurprise() {
		Assertions.assertThat(boss.surprisedBy(hero)).isFalse();
		Assertions.assertThat(boss.defenseSkill(hero)).isGreaterThan(0);
	}

	@Test
	@DisplayName("invisible hero surprises EchoBoss")
	void invisibleHeroSurprisesEchoBoss() {
		hero.invisible = 1;

		Assertions.assertThat(boss.surprisedBy(hero)).isTrue();
		Assertions.assertThat(hero.canSurpriseAttack()).isTrue();
	}

	@Test
	@DisplayName("hero out of EchoBoss FOV surprises even when visible")
	void heroOutOfFovSurprisesEchoBoss() {
		boss.fieldOfView[hero.pos] = false;
		boss.enemySeen = true;
		hero.invisible = 0;

		Assertions.assertThat(boss.surprisedBy(hero)).isTrue();
	}

	@Test
	@DisplayName("unseen visible hero in FOV surprises EchoBoss")
	void unseenVisibleHeroInFovSurprisesEchoBoss() {
		boss.enemySeen = false;
		hero.invisible = 0;
		Assertions.assertThat(boss.fieldOfView[hero.pos]).isTrue();

		Assertions.assertThat(boss.surprisedBy(hero)).isTrue();
	}

	@Test
	@DisplayName("null FOV does not surprise via FOV clause when seen and visible")
	void nullFovDoesNotSurpriseWhenSeenAndVisible() {
		boss.fieldOfView = null;
		boss.enemySeen = true;
		hero.invisible = 0;

		Assertions.assertThat(boss.surprisedBy(hero)).isFalse();
	}

	@Test
	@DisplayName("flail cannot surprise EchoBoss")
	void flailCannotSurpriseEchoBoss() {
		Flail flail = new Flail();
		flail.identify();
		hero.belongings.weapon = flail;
		hero.STR = Math.max(hero.STR(), flail.STRReq());
		hero.invisible = 1;

		Assertions.assertThat(hero.canSurpriseAttack()).isFalse();
		Assertions.assertThat(boss.surprisedBy(hero)).isFalse();
		Assertions.assertThat(boss.surprisedBy(hero, false))
				.as("non-attacking surprise check ignores weapon gate")
				.isTrue();
	}

	@Test
	@DisplayName("under-STR weapon cannot surprise EchoBoss")
	void underStrWeaponCannotSurpriseEchoBoss() {
		Greataxe axe = new Greataxe();
		axe.identify();
		hero.belongings.weapon = axe;
		hero.STR = axe.STRReq() - 1;
		hero.invisible = 1;

		Assertions.assertThat(hero.canSurpriseAttack()).isFalse();
		Assertions.assertThat(boss.surprisedBy(hero)).isFalse();
	}

	@Test
	@DisplayName("EchoBoss defenseSkill stays kit evasion even when surprised")
	void echoBossDefenseSkillIgnoresSurpriseZeroing() {
		int seenEvasion = boss.defenseSkill(hero);
		Assertions.assertThat(seenEvasion).isGreaterThan(0);

		hero.invisible = 1;
		Assertions.assertThat(boss.surprisedBy(hero)).isTrue();

		// EchoBoss overrides Mob.defenseSkill → does not return 0 on surprise
		Assertions.assertThat(boss.defenseSkill(hero)).isEqualTo(seenEvasion);

		Rat rat = new Rat();
		rat.pos = boss.pos;
		rat.fieldOfView = boss.fieldOfView.clone();
		rat.enemySeen = true;
		Assertions.assertThat(rat.surprisedBy(hero)).isTrue();
		Assertions.assertThat(rat.defenseSkill(hero)).isEqualTo(0);
	}

	@Test
	@DisplayName("invisible hero always hits EchoBoss via Char.hit")
	void invisibleHeroAlwaysHitsEchoBoss() {
		hero.invisible = 1;

		Assertions.assertThat(Char.hit(hero, boss, false)).isTrue();
	}

	@Test
	@DisplayName("Focus on EchoBoss still dodges invisible hero")
	void focusOnEchoBossBeatsInvisibleHeroAccuracy() {
		hero.invisible = 1;
		Buff.affect(boss, MonkEnergy.MonkAbility.Focus.FocusBuff.class);

		Assertions.assertThat(Char.hit(hero, boss, false)).isFalse();
	}

	@Test
	@DisplayName("invisible EchoBoss always hits hero via Char.hit but does not surprise")
	void invisibleEchoBossHitsHeroWithoutSurprise() {
		boss.invisible = 1;
		Assertions.assertThat(hero.defenseSkill(boss)).isGreaterThan(0);

		Assertions.assertThat(Char.hit(boss, hero, false)).isTrue();
		Assertions.assertThat(hero).isNotInstanceOf(Mob.class);
	}

	@Test
	@DisplayName("echo kit cannot surprise via Mob.surprisedBy")
	void echoKitCannotSurpriseViaSurprisedBy() {
		Hero kit = boss.getEchoHero();
		kit.invisible = 1;
		kit.pos = boss.pos;

		Rat rat = new Rat();
		rat.pos = hero.pos;
		rat.fieldOfView = new boolean[Dungeon.level.length()];
		rat.enemySeen = false;

		Assertions.assertThat(rat.surprisedBy(kit))
				.as("surprisedBy requires attacker == Dungeon.hero")
				.isFalse();
		Assertions.assertThat(rat.surprisedBy(hero)).isTrue();
	}

	@Test
	@DisplayName("dagger surprise gate is open when invisible hero targets EchoBoss")
	void daggerSurpriseGateOpensForInvisibleHeroVsEchoBoss() {
		Dagger dagger = new Dagger();
		dagger.identify();
		hero.belongings.weapon = dagger;
		hero.STR = Math.max(hero.STR(), dagger.STRReq());
		hero.invisible = 1;

		Assertions.assertThat(boss.surprisedBy(hero)).isTrue();
		Assertions.assertThat(hero.canSurpriseAttack()).isTrue();

		boss.HP = boss.HT = 5000;
		int before = boss.HP;
		Assertions.assertThat(hero.attack(boss)).isTrue();
		Assertions.assertThat(boss.HP).isLessThan(before);
	}

	private void prepareSeenInFov() {
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Dungeon.level.updateFieldOfView(boss, boss.fieldOfView);
		boss.enemySeen = true;
		boss.enemy = hero;
		hero.invisible = 0;
	}
}
