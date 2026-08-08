package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;

/**
 * Hero armor-ability activation mirroring upstream {@link ArmorAbility#use} without
 * {@link com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector}.
 */
public final class ArmorAbilityTestSupport {

	private ArmorAbilityTestSupport() {
	}

	public static boolean activateHero(Hero hero, ClassArmor armor, ArmorAbility ability, Integer target) {
		if (hero == null || armor == null || ability == null) {
			return false;
		}
		if (ability.targetingPrompt() != null && target == null) {
			return false;
		}
		if (armor.charge < ability.chargeUse(hero)) {
			return false;
		}
		int cell = target != null ? target : hero.pos;
		hero.busy();
		ability.activate(armor, hero, cell);
		return true;
	}
}
