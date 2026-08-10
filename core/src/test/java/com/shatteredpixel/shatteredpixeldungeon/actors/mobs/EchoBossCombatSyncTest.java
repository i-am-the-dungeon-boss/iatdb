package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Body buffs that kit combat rolls must see (Barkskin, Earthroot), mirroring
 * combatSpeed(alsoMoveBuffs).
 */
@ExtendWith(GdxTestExtension.class)
class EchoBossCombatSyncTest {

	private Hero hero;
	private EchoBoss boss;
	private Hero kit;

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
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		kit = boss.getEchoHero();
		// Isolate barkskin / earthroot from gear DR and armor glyphs
		kit.belongings.armor = null;
		kit.belongings.weapon = null;
	}

	@Test
	@DisplayName("body Barkskin is included in EchoBoss drRoll")
	void bodyBarkskinIncludedInDrRoll() {
		Assertions.assertThat(Barkskin.currentLevel(kit)).isZero();
		Buff.append(boss, Barkskin.class).set(12, 100);
		Assertions.assertThat(Barkskin.currentLevel(boss)).isEqualTo(12);
		Assertions.assertThat(Barkskin.currentLevel(kit)).isZero();

		Assertions.assertThat(kit.drRoll())
				.as("kit alone has no barkskin")
				.isZero();

		int sum = 0;
		for (int i = 0; i < 40; i++) {
			int roll = boss.drRoll();
			Assertions.assertThat(roll).isBetween(0, 12);
			sum += roll;
		}
		Assertions.assertThat(sum)
				.as("body barkskin must contribute to EchoBoss.drRoll")
				.isGreaterThan(0);
	}

	@Test
	@DisplayName("body Earthroot absorbs via EchoBoss defenseProc")
	void bodyEarthrootAbsorbsViaDefenseProc() {
		Assertions.assertThat(kit.buff(Earthroot.Armor.class)).isNull();
		Earthroot.Armor root = Buff.affect(boss, Earthroot.Armor.class);
		root.level(500);

		int damage = 40;
		int block = (Dungeon.scalingDepth() + 5) / 2;
		int expected = damage - Math.min(damage, block);

		Assertions.assertThat(boss.defenseProc(hero, damage)).isEqualTo(expected);
		Assertions.assertThat(kit.buff(Earthroot.Armor.class)).isNull();
	}
}
