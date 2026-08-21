package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;

/**
 * Echo Holy Tome cast — fork-owned. Validates equip/charge on the kit and
 * MagicImmune on the boss body, then delegates to {@link EchoClericHandlers}.
 * Char-safe targeted effect for Echo (cell already chosen).
 */
public final class EchoClericAdapter {

	private EchoClericAdapter() {
	}

	public static boolean cast(EchoBoss boss, HolyTome tome, ClericSpell spell, Integer target) {
		if (boss == null || tome == null || spell == null) {
			return false;
		}
		EchoActionContext ctx;
		try {
			ctx = EchoActionContext.of(boss);
		} catch (IllegalArgumentException e) {
			return false;
		}
		if (ctx.body.buff(MagicImmune.class) != null) {
			return EchoActionSupport.refuse(ctx);
		}
		if (!tome.isEquipped(ctx.stats()) && !ctx.stats().hasTalent(Talent.LIGHT_READING)) {
			return EchoActionSupport.refuse(ctx);
		}
		if (tome.cursed) {
			return EchoActionSupport.refuse(ctx);
		}
		if (!tome.canCast(ctx.stats(), spell)) {
			return EchoActionSupport.refuse(ctx);
		}
		if (spell.targetingFlags() != -1 && (target == null || target < 0)) {
			return EchoActionSupport.refuse(ctx);
		}
		if (!EchoClericHandlers.cast(ctx, tome, spell, target)) {
			return EchoActionSupport.refuse(ctx);
		}
		return true;
	}
}
