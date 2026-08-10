package com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Phase 1 characterization: Hero cleric spell master entry points.
 */
@ExtendWith(GdxTestExtension.class)
class ClericSpellHeroCastTest {

	@Test
	@DisplayName("Hero targeted cleric spell keeps the master CellSelector flow")
	void heroTargetedClericSpellKeepsMasterCellSelectorFlow() {
		Assertions.assertThat(Sunray.INSTANCE)
				.as("targeted spells must subclass TargetedClericSpell (selectCell in onCast)")
				.isInstanceOf(TargetedClericSpell.class);
		Assertions.assertThat(HolyWeapon.INSTANCE)
				.as("self spells must not open CellSelector")
				.isNotInstanceOf(TargetedClericSpell.class);
	}

	@Test
	@DisplayName("Hero self cleric spell charges the Tome and spends hero time once")
	void heroSelfClericSpellChargesTomeAndSpendsHeroTimeOnce() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.CLERIC.initHero(hero);
		hero.lvl = 6;
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		HolyTome tome = (HolyTome) hero.belongings.artifact;
		tome.directCharge(5f);
		String chargeBefore = tome.status();
		float before = hero.cooldown();
		hero.sprite.visible = false;

		HolyWeapon.INSTANCE.onCast(tome, hero);

		Assertions.assertThat(hero.buff(HolyWeapon.HolyWepBuff.class)).isNotNull();
		Assertions.assertThat(tome.status()).isNotEqualTo(chargeBefore);
		Assertions.assertThat(hero.cooldown()).isGreaterThanOrEqualTo(before);
	}
}
