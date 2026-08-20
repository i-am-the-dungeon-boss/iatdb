package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TimeStasis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoHardStunTest {

	private Hero hero;
	private EchoBoss boss;

	@BeforeEach
	void setUp() {
		EchoTestSupport.resetWorkflowState();
		hero = EchoTestSupport.warriorHero();
		boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
	}

	@Test
	@DisplayName("10-turn Paralysis caps to 3 on and 3 immune")
	void tenTurnParalysisCapsToThreeOnAndThreeImmune() {
		Paralysis stun = Buff.affect(hero, Paralysis.class, 10f);

		Assertions.assertThat(stun.cooldown()).isEqualTo(3f);
		stun.detach();
		Paralysis.Immunity immunity = hero.buff(Paralysis.Immunity.class);
		Assertions.assertThat(immunity).isNotNull();
		Assertions.assertThat(immunity.cooldown()).isEqualTo(3f);
	}

	@Test
	@DisplayName("1-turn Paralysis lands 1 on and 1 immune")
	void oneTurnParalysisLandsOneOnAndOneImmune() {
		Paralysis stun = Buff.affect(hero, Paralysis.class, 1f);

		Assertions.assertThat(stun.cooldown()).isEqualTo(1f);
		stun.detach();
		Paralysis.Immunity immunity = hero.buff(Paralysis.Immunity.class);
		Assertions.assertThat(immunity).isNotNull();
		Assertions.assertThat(immunity.cooldown()).isEqualTo(1f);
	}

	@Test
	@DisplayName("Paralysis.Immunity blocks Frost, Magical Sleep and both stasis classes")
	void immunityBlocksFrostMagicalSleepAndStasis() {
		Buff.affect(hero, Paralysis.Immunity.class, 3f);
		hero.HP = hero.HT - 5;

		Assertions.assertThat(Buff.affect(hero, Frost.class, 2f).target).isNull();
		Assertions.assertThat(hero.buff(Frost.class)).isNull();
		Assertions.assertThat(Buff.affect(hero, MagicalSleep.class).target).isNull();
		Assertions.assertThat(hero.buff(MagicalSleep.class)).isNull();
		TimeStasis blocked = Buff.affect(hero, TimeStasis.class, 10f);
		Assertions.assertThat(blocked.target).isNotSameAs(hero);
		Assertions.assertThat(hero.buff(TimeStasis.class)).isNull();

		TimekeepersHourglass glass = new TimekeepersHourglass();
		TimekeepersHourglass.timeStasis stasis = glass.new timeStasis();
		Assertions.assertThat(stasis.attachTo(hero)).isFalse();
		Assertions.assertThat(hero.buff(TimekeepersHourglass.timeStasis.class)).isNull();
	}

	@Test
	@DisplayName("Frost detach grants paralysis immunity for landed turns")
	void frostDetachGrantsSharedImmunity() {
		Frost frost = Buff.affect(hero, Frost.class, 2f);

		Assertions.assertThat(frost.cooldown()).isEqualTo(2f);
		frost.detach();
		Paralysis.Immunity immunity = hero.buff(Paralysis.Immunity.class);
		Assertions.assertThat(immunity).isNotNull();
		Assertions.assertThat(immunity.cooldown()).isEqualTo(2f);
	}

	@Test
	@DisplayName("Time Stasis 100 caps to 3 and does not grant immunity")
	void timeStasisCapsWithoutGrantingImmunity() {
		TimeStasis stasis = Buff.affect(hero, TimeStasis.class, 100f);

		Assertions.assertThat(stasis.cooldown()).isEqualTo(3f);
		stasis.detach();
		Assertions.assertThat(hero.buff(Paralysis.Immunity.class)).isNull();
	}

	@Test
	@DisplayName("hourglass time stasis is capped to 3 and does not grant immunity")
	void hourglassStasisCapsWithoutGrantingImmunity() {
		TimekeepersHourglass glass = new TimekeepersHourglass();
		TimekeepersHourglass.timeStasis stasis = glass.new timeStasis();

		Assertions.assertThat(stasis.attachTo(hero)).isTrue();
		Assertions.assertThat(stasis.cooldown()).isEqualTo(3f);
		stasis.detach();
		Assertions.assertThat(hero.buff(Paralysis.Immunity.class)).isNull();
	}

	@Test
	@DisplayName("EchoBoss 10-turn Paralysis caps to 3 on and 3 immune")
	void echoBossParalysisCapsAndGrantsImmunity() {
		Paralysis stun = Buff.affect(boss, Paralysis.class, 10f);

		Assertions.assertThat(stun.cooldown()).isEqualTo(3f);
		stun.detach();
		Paralysis.Immunity immunity = boss.buff(Paralysis.Immunity.class);
		Assertions.assertThat(immunity).isNotNull();
		Assertions.assertThat(immunity.cooldown()).isEqualTo(3f);
	}

	@Test
	@DisplayName("rats keep uncapped Paralysis and gain no immunity")
	void ratParalysisUnchanged() {
		Rat rat = new Rat();
		rat.pos = hero.pos + Dungeon.level.width();
		EchoTestSupport.linkStubSprite(rat);
		com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(rat);
		Dungeon.level.mobs.add(rat);

		Paralysis stun = Buff.affect(rat, Paralysis.class, 10f);
		Assertions.assertThat(stun.cooldown()).isEqualTo(10f);
		stun.detach();
		Assertions.assertThat(rat.buff(Paralysis.Immunity.class)).isNull();
	}

	@Test
	@DisplayName("already stunned combatant rejects a second hard stun")
	void alreadyStunnedRejectsSecondHardStun() {
		Buff.affect(hero, Paralysis.class, 3f);

		Assertions.assertThat(Buff.affect(hero, Frost.class, 2f).target).isNull();
		Assertions.assertThat(hero.buff(Frost.class)).isNull();
		Assertions.assertThat(Buff.affect(hero, MagicalSleep.class).target).isNull();
		Assertions.assertThat(hero.buff(MagicalSleep.class)).isNull();
		TimeStasis stasis = Buff.affect(hero, TimeStasis.class, 3f);
		Assertions.assertThat(stasis.target).isNotSameAs(hero);
		Assertions.assertThat(hero.buff(TimeStasis.class)).isNull();
	}

	@Test
	@DisplayName("Hero outside the echo fight gains no paralysis immunity")
	void heroOutsideFightGainsNoImmunity() {
		Dungeon.resetEchoStateForTests();
		Assertions.assertThat(Dungeon.isEchoBossActive()).isFalse();

		Buff.affect(hero, Paralysis.class, 1f).detach();

		Assertions.assertThat(hero.buff(Paralysis.Immunity.class)).isNull();
	}

	@Test
	@DisplayName("first defenseSkill while stunned is 0 then later hits are half")
	void firstStunnedHitIsZeroThenHalf() {
		int full = hero.defenseSkill(boss);
		hero.paralysed = 1;

		Assertions.assertThat(hero.defenseSkill(boss)).isEqualTo(0);
		Assertions.assertThat(hero.defenseSkill(boss))
				.isEqualTo(Math.max(1, Math.round(full / 2f)));
	}

	@Test
	@DisplayName("a new stun restores the guaranteed first hit")
	void newStunRestoresGuaranteedHit() {
		Paralysis first = Buff.affect(hero, Paralysis.class, 1f);
		Assertions.assertThat(hero.defenseSkill(boss)).isEqualTo(0);
		Assertions.assertThat(hero.defenseSkill(boss)).isGreaterThan(0);
		first.detach();
		Buff.detach(hero, Paralysis.Immunity.class);

		Buff.affect(hero, Paralysis.class, 1f);
		Assertions.assertThat(hero.defenseSkill(boss)).isEqualTo(0);
	}

	@Test
	@DisplayName("unseen adjacent door halves evasion and stacks with stun half")
	void unseenAdjacentDoorHalvesAndStacksWithStun() {
		int full = hero.defenseSkill(boss);
		hero.fieldOfView = new boolean[Dungeon.level.length()];
		java.util.Arrays.fill(hero.fieldOfView, true);
		hero.fieldOfView[boss.pos] = false;
		Dungeon.level.map[boss.pos] = Terrain.DOOR;
		Dungeon.level.buildFlagMaps();

		Assertions.assertThat(hero.defenseSkill(boss))
				.isEqualTo(Math.max(1, Math.round(full / 2f)));

		hero.paralysed = 1;
		Assertions.assertThat(hero.defenseSkill(boss)).isEqualTo(0);
		int stacked = hero.defenseSkill(boss);
		Assertions.assertThat(stacked).isEqualTo(Math.max(1, Math.round(full / 4f)));
	}

	@Test
	@DisplayName("visible or missing door does not halve evasion")
	void visibleOrMissingDoorDoesNotHalve() {
		int full = hero.defenseSkill(boss);
		hero.fieldOfView = new boolean[Dungeon.level.length()];
		java.util.Arrays.fill(hero.fieldOfView, true);

		Assertions.assertThat(hero.defenseSkill(boss)).isEqualTo(full);

		Dungeon.level.map[boss.pos] = Terrain.DOOR;
		Dungeon.level.buildFlagMaps();
		Assertions.assertThat(hero.defenseSkill(boss)).isEqualTo(full);
	}

	@Test
	@DisplayName("Illuminated plus Cleric is a guaranteed hit")
	void illuminatedClericIsGuaranteedHit() {
		Hero cleric = new Hero();
		HeroClass.CLERIC.initHero(cleric);
		cleric.pos = boss.pos;
		Buff.affect(hero, GuidingLight.Illuminated.class);

		Assertions.assertThat(hero.defenseSkill(cleric)).isEqualTo(0);
	}

	@Test
	@DisplayName("Parry still grants infinite evasion while stunned")
	void parryStillInfiniteWhileStunned() {
		Buff.affect(hero, Combo.ParryTracker.class, 1f);
		hero.paralysed = 1;

		Assertions.assertThat(hero.defenseSkill(boss)).isEqualTo(Char.INFINITE_EVASION);
	}

	@Test
	@DisplayName("Focus still grants infinite evasion via Char.hit while stunned")
	void focusStillInfiniteViaCharHit() {
		Buff.affect(hero, MonkEnergy.MonkAbility.Focus.FocusBuff.class);
		hero.paralysed = 1;

		Assertions.assertThat(Char.hit(boss, hero, false)).isFalse();
	}

	@Test
	@DisplayName("MagicalSleep on EchoBoss is not capped and does not grant immunity")
	void magicalSleepOnEchoBossIsNotCapped() {
		MagicalSleep sleep = Buff.affect(boss, MagicalSleep.class);

		Assertions.assertThat(sleep).isNotNull();
		Assertions.assertThat(sleep.cooldown()).isNotEqualTo(3f);
		sleep.act();
		Assertions.assertThat(boss.buff(MagicalSleep.class)).isSameAs(sleep);
		sleep.detach();
		Assertions.assertThat(boss.buff(Paralysis.Immunity.class)).isNull();
	}
}
