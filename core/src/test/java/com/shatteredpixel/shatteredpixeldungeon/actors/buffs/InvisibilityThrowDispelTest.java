package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoThrowAdapter;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Throwing is a reveal: hurling anything gives away the thrower's position,
 * whether or not the throw connects. Covers every throw entry point for both
 * the Hero and an EchoBoss, including the spirit bow.
 */
@ExtendWith(GdxTestExtension.class)
class InvisibilityThrowDispelTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("Hero throwing a missile weapon dispels invisibility even when it misses")
	void heroThrownMissileDispelsInvisibilityOnMiss() {
		Hero hero = rogueHero();
		EchoBoss boss = bossFor(hero);

		Buff.affect(hero, Invisibility.class, Invisibility.DURATION);
		// Infinite evasion → guaranteed dodge (miss)
		Buff.affect(boss, MonkEnergy.MonkAbility.Focus.FocusBuff.class);

		ThrowingKnife knives = new ThrowingKnife();
		knives.identify();
		knives.quantity(3);
		Assertions.assertThat(knives.collect(hero.belongings.backpack)).isTrue();

		knives.cast(hero, boss.pos);

		Assertions.assertThat(hero.buff(Invisibility.class))
				.as("throwing is a reveal, hit or miss")
				.isNull();
		Assertions.assertThat(hero.invisible).isEqualTo(0);
	}

	@Test
	@DisplayName("Hero throwing a non-weapon item at an empty cell dispels invisibility")
	void heroThrownBombDispelsInvisibility() {
		Hero hero = rogueHero();
		EchoBoss boss = bossFor(hero);

		Buff.affect(hero, Invisibility.class, Invisibility.DURATION);

		Bomb bomb = new Bomb();
		Assertions.assertThat(bomb.collect(hero.belongings.backpack)).isTrue();

		int emptyCell = hero.pos + 1;
		Assertions.assertThat(emptyCell).isNotEqualTo(boss.pos);
		bomb.cast(hero, emptyCell);

		Assertions.assertThat(hero.buff(Invisibility.class))
				.as("a thrown bomb reveals the thrower even with nothing to hit")
				.isNull();
		Assertions.assertThat(hero.invisible).isEqualTo(0);
	}

	@Test
	@DisplayName("Hero spirit bow shot dispels invisibility even at an empty cell")
	void heroSpiritBowShotDispelsInvisibility() {
		Hero hero = huntressHero();
		EchoBoss boss = bossFor(hero);

		Buff.affect(hero, Invisibility.class, Invisibility.DURATION);

		SpiritBow bow = new SpiritBow();
		Assertions.assertThat(bow.collect(hero.belongings.backpack)).isTrue();

		int emptyCell = hero.pos + 1;
		Assertions.assertThat(emptyCell).isNotEqualTo(boss.pos);
		bow.knockArrow().cast(hero, emptyCell);

		Assertions.assertThat(hero.buff(Invisibility.class))
				.as("loosing a spirit arrow reveals the archer")
				.isNull();
		Assertions.assertThat(hero.invisible).isEqualTo(0);
	}

	@Test
	@DisplayName("EchoBoss throwing a missile weapon dispels the boss body's invisibility")
	void echoThrownMissileDispelsBossInvisibility() {
		Hero hero = rogueHero();
		ThrowingKnife seed = new ThrowingKnife();
		seed.identify();
		seed.quantity(3);
		seed.collect(hero.belongings.backpack);
		EchoBoss boss = bossFor(hero);

		Buff.affect(boss, Invisibility.class, Invisibility.DURATION);
		Assertions.assertThat(boss.invisible).isGreaterThan(0);
		// Infinite evasion → the hero dodges, so only the throw itself can reveal
		Buff.affect(hero, MonkEnergy.MonkAbility.Focus.FocusBuff.class);

		ThrowingKnife kitKnives = boss.getEchoHero().belongings.getItem(ThrowingKnife.class);
		Assertions.assertThat(kitKnives).isNotNull();

		Assertions.assertThat(EchoThrowAdapter.throwItem(boss, kitKnives, hero.pos)).isTrue();

		Assertions.assertThat(boss.buff(Invisibility.class))
				.as("the Echo body throws, so the body is revealed — not the phantom kit")
				.isNull();
		Assertions.assertThat(boss.invisible).isEqualTo(0);
	}

	@Test
	@DisplayName("EchoBoss throwing a non-weapon item dispels the boss body's invisibility")
	void echoThrownBombDispelsBossInvisibility() {
		Hero hero = rogueHero();
		new Bomb().collect(hero.belongings.backpack);
		EchoBoss boss = bossFor(hero);

		Buff.affect(boss, Invisibility.class, Invisibility.DURATION);

		Bomb kitBomb = boss.getEchoHero().belongings.getItem(Bomb.class);
		Assertions.assertThat(kitBomb).isNotNull();

		Assertions.assertThat(EchoThrowAdapter.throwItem(boss, kitBomb, hero.pos)).isTrue();

		Assertions.assertThat(boss.buff(Invisibility.class)).isNull();
		Assertions.assertThat(boss.invisible).isEqualTo(0);
	}

	@Test
	@DisplayName("EchoBoss spirit bow shot dispels the boss body's invisibility")
	void echoSpiritBowShotDispelsBossInvisibility() {
		Hero hero = huntressHero();
		EchoBoss boss = bossFor(hero);

		Buff.affect(boss, Invisibility.class, Invisibility.DURATION);
		Buff.affect(hero, MonkEnergy.MonkAbility.Focus.FocusBuff.class);

		SpiritBow bow = new SpiritBow();
		Assertions.assertThat(bow.collect(boss.getEchoHero().belongings.backpack)).isTrue();

		Assertions.assertThat(EchoThrowAdapter.throwItem(boss, bow.knockArrow(), hero.pos)).isTrue();

		Assertions.assertThat(boss.buff(Invisibility.class))
				.as("the spirit bow reveals the Echo body like any other throw")
				.isNull();
		Assertions.assertThat(boss.invisible).isEqualTo(0);
	}

	@Test
	@DisplayName("EchoBoss throw leaves the target's invisibility alone")
	void echoThrowKeepsTargetInvisibility() {
		Hero hero = rogueHero();
		ThrowingKnife seed = new ThrowingKnife();
		seed.identify();
		seed.quantity(3);
		seed.collect(hero.belongings.backpack);
		EchoBoss boss = bossFor(hero);

		Buff.affect(hero, Invisibility.class, Invisibility.DURATION);
		Buff.affect(hero, MonkEnergy.MonkAbility.Focus.FocusBuff.class);

		ThrowingKnife kitKnives = boss.getEchoHero().belongings.getItem(ThrowingKnife.class);
		Assertions.assertThat(kitKnives).isNotNull();

		Assertions.assertThat(EchoThrowAdapter.throwItem(boss, kitKnives, hero.pos)).isTrue();

		Assertions.assertThat(hero.buff(Invisibility.class))
				.as("being missed by someone else's throw is not a reveal")
				.isNotNull();
		Assertions.assertThat(hero.invisible).isGreaterThan(0);
	}

	private static EchoBoss bossFor(Hero hero) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		return boss;
	}

	private static Hero rogueHero() {
		return heroOfClass(HeroClass.ROGUE);
	}

	private static Hero huntressHero() {
		return heroOfClass(HeroClass.HUNTRESS);
	}

	private static Hero heroOfClass(HeroClass cls) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		cls.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}
}
