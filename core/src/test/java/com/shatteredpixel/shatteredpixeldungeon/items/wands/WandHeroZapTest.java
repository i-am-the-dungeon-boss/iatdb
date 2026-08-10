package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Phase 1 characterization: Hero {@link Wand#zap(Hero, int)} after CellSelector
 * resolves a cell.
 */
@ExtendWith(GdxTestExtension.class)
class WandHeroZapTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("Hero zap consumes the upstream charge count")
	void heroZapConsumesUpstreamChargeCount() {
		Hero hero = mageHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		Wand wand = staffWand(hero);
		wand.curCharges = 3;
		int chargesBefore = wand.curCharges;

		boolean ok = wand.zap(hero, target.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(wand.curCharges).isEqualTo(chargesBefore - 1);
	}

	@Test
	@DisplayName("Hero zap applies identification and talent riders")
	void heroZapAppliesIdentificationAndTalentRiders() {
		Hero hero = mageHero();
		while (hero.pointsInTalent(Talent.SCHOLARS_INTUITION) < 2) {
			hero.upgradeTalent(Talent.SCHOLARS_INTUITION);
		}
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		Wand wand = staffWand(hero);
		wand.curCharges = 3;
		wand.levelKnown = false;
		wand.cursedKnown = false;

		boolean ok = wand.zap(hero, target.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(wand.isIdentified()).isTrue();
		Assertions.assertThat(Catalog.isSeen(wand.getClass())).isTrue();
	}

	@Test
	@DisplayName("Hero zap spends TIME_TO_ZAP exactly once after the callback")
	void heroZapSpendsTimeToZapExactlyOnceAfterCallback() {
		Hero hero = mageHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		Wand wand = staffWand(hero);
		wand.curCharges = 3;
		float before = hero.cooldown();

		boolean ok = wand.zap(hero, target.pos);

		Assertions.assertThat(ok).isTrue();
		// Wand.TIME_TO_ZAP is 1f on master and main.
		Assertions.assertThat(hero.cooldown()).isEqualTo(before + 1f);
	}

	private static Hero mageHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static Wand staffWand(Hero hero) {
		MagesStaff staff = hero.belongings.getItem(MagesStaff.class);
		Assertions.assertThat(staff).isNotNull();
		Wand wand = staff.wand();
		Assertions.assertThat(wand).isNotNull();
		return wand;
	}
}
