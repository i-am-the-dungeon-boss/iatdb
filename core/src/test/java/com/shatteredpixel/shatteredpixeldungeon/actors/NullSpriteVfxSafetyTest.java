package com.shatteredpixel.shatteredpixeldungeon.actors;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WellFed;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Potential;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Reproduces production NPEs when combat VFX runs on a char with null sprite
 * (echo kit / headless actors). Gameplay must still apply.
 */
@ExtendWith(GdxTestExtension.class)
class NullSpriteVfxSafetyTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("WellFed heal tick skips status VFX when target sprite is null")
	void wellFedHealTickSkipsStatusWhenSpriteNull() {
		Hero hero = EchoTestSupport.warriorHero();
		hero.sprite = null;
		hero.HP = 10;
		hero.HT = 30;

		WellFed buff = Buff.affect(hero, WellFed.class);
		Bundle state = new Bundle();
		state.put("left", 19);
		buff.restoreFromBundle(state);

		Assertions.assertThatCode(buff::act).doesNotThrowAnyException();
		Assertions.assertThat(hero.HP).isEqualTo(11);
	}

	@Test
	@DisplayName("Blocking proc applies shield when attacker sprite is null")
	void blockingProcAppliesShieldWhenAttackerSpriteNull() {
		Hero attacker = EchoTestSupport.warriorHero();
		Hero defender = EchoTestSupport.warriorHero();
		attacker.sprite = null;
		WornShortsword weapon = new WornShortsword();
		Blocking enchant = new Blocking();

		boolean shielded = false;
		for (int i = 0; i < 200; i++) {
			enchant.proc(weapon, attacker, defender, 8);
			if (attacker.buff(Blocking.BlockBuff.class) != null) {
				shielded = true;
				break;
			}
		}

		Assertions.assertThat(shielded).isTrue();
	}

	@Test
	@DisplayName("Potential proc charges wands when defender sprite is null")
	void potentialProcChargesWhenDefenderSpriteNull() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		hero.sprite = null;
		Hero attacker = EchoTestSupport.warriorHero();
		ClothArmor armor = new ClothArmor();
		Potential glyph = new Potential();

		Assertions.assertThat(hero.belongings.charge(0f))
				.as("mage fixture must own a wand charger so Potential reaches VFX")
				.isGreaterThan(0);

		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 200; i++) {
				glyph.proc(armor, attacker, hero, 5);
			}
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Bleeding tick damages when target sprite is null")
	void bleedingTickDamagesWhenSpriteNull() {
		Hero hero = EchoTestSupport.warriorHero();
		hero.sprite = null;
		hero.HP = hero.HT = 200;
		Bleeding bleeding = Buff.affect(hero, Bleeding.class);
		// High enough that Math.round(NormalFloat(level/2, level)) cannot be 0.
		bleeding.set(100f);

		Assertions.assertThatCode(bleeding::act).doesNotThrowAnyException();
		Assertions.assertThat(hero.HP).isLessThan(200);
	}

	@Test
	@DisplayName("Shocking proc applies arc damage when sprites are null")
	void shockingProcAppliesDamageWhenSpritesNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.HP = boss.HT = 500;

		Hero attacker = boss.getEchoHero();
		Assertions.assertThat(attacker.sprite).isNull();
		player.sprite = null;

		WornShortsword weapon = new WornShortsword();
		Shocking enchant = new Shocking();

		Assertions.assertThatCode(() -> {
			for (int i = 0; i < 200; i++) {
				enchant.proc(weapon, attacker, player, 12);
			}
		}).doesNotThrowAnyException();
	}
}
