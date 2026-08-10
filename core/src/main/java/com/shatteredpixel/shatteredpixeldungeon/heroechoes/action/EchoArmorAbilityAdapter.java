package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;

/**
 * Echo ClassArmor charge skill — fork-owned dispatcher. Hero keeps upstream
 * {@link ArmorAbility#use(ClassArmor, com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero)}.
 * <p>
 * Soft refuses (self-aim, empty cell, missing staff) cancel busy and still
 * return {@code true} (turn can resume). Hard refuses
 * (null target, low charge, Trinity without body form) return {@code false}
 * before busy.
 */
public final class EchoArmorAbilityAdapter {

	private EchoArmorAbilityAdapter() {
	}

	public static boolean activate(EchoBoss boss, ClassArmor armor, ArmorAbility ability, Integer target) {
		if (boss == null || armor == null || ability == null) {
			return false;
		}
		EchoActionContext ctx;
		try {
			ctx = EchoActionContext.of(boss);
		} catch (IllegalArgumentException e) {
			return false;
		}
		if (ability.targetingPrompt() != null && target == null) {
			return false;
		}
		if (ability instanceof Trinity) {
			Trinity trinity = (Trinity) ability;
			if (trinity.bodyFormForEcho() == null) {
				return false;
			}
		}
		if (armor.charge < ability.chargeUse(ctx.kit)) {
			return false;
		}
		ctx.busy();
		boolean ok = EchoArmorHandlers.activate(ctx, armor, ability, target);
		if (!ok) {
			ctx.cancel();
		}
		return true;
	}
}
