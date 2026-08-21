package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfAntiMagic;

/**
 * Echo scroll read — fork-owned dispatcher. Hero keeps upstream {@link Scroll#doRead()}.
 */
public final class EchoScrollAdapter {

	private EchoScrollAdapter() {
	}

	public static boolean read(EchoBoss boss, Scroll scroll) {
		if (boss == null || scroll == null) {
			return false;
		}
		EchoActionContext ctx;
		try {
			ctx = EchoActionContext.of(boss);
		} catch (IllegalArgumentException e) {
			return false;
		}
		if (ctx.body.buff(MagicImmune.class) != null) {
			return false;
		}
		if (ctx.body.buff(Blindness.class) != null) {
			return false;
		}
		if (ctx.stats().buff(UnstableSpellbook.bookRecharge.class) != null
				&& ctx.stats().buff(UnstableSpellbook.bookRecharge.class).isCursed()
				&& !(scroll instanceof ScrollOfRemoveCurse || scroll instanceof ScrollOfAntiMagic)) {
			return false;
		}
		scroll.setCurrent(ctx.stats());
		return EchoScrollHandlers.read(ctx, scroll);
	}
}
