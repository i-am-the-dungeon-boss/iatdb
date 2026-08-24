package com.shatteredpixel.shatteredpixeldungeon.items.weapon;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoActionContext;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoKitBorrow;
import com.shatteredpixel.shatteredpixeldungeon.items.AiItemActions;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.ItemEchoBridge;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Callback;

/**
 * Same-package Echo SpiritBow shot — Hero keeps {@link SpiritBow.SpiritArrow#cast}.
 * Hit via kit; borrow body sprite/pos when body ≠ kit (Echo hit VFX).
 * Hero-only sniper SPEED flurry (unchanged behavior) stays on the Hero cast path.
 */
public final class SpiritBowEchoBridge {

	private SpiritBowEchoBridge() {
	}

	/** Hit via kit; borrow body sprite/pos when body ≠ kit (Echo hit VFX). */
	public static boolean throwArrow(EchoBoss boss, SpiritBow.SpiritArrow arrow, int dst) {
		if (boss == null || arrow == null || Dungeon.level == null || dst < 0) {
			return false;
		}
		EchoActionContext ctx = EchoActionContext.of(boss);
		final int cell = arrow.throwPos(ctx.body.pos, dst);
		arrow.markTargetCell(cell);

		Char found = Actor.findChar(cell);
		Hero enemy = Dungeon.hero;
		if (found == null && enemy != null && cell == enemy.pos) {
			found = enemy;
		}
		if (found == null || found == ctx.stats() || found == ctx.body) {
			return false;
		}

		ctx.busy();

		final float delay = arrow.castDelay(ctx.stats(), cell);
		final Char target = found;
		Callback onArrive = () -> {
			EchoKitBorrow.run(ctx, () -> {
				AiItemActions.withUser(ctx.stats(), arrow, () -> {
					Item i = arrow.detach(ctx.gear().backpack);
					if (i != null) {
						AiItemActions.onThrow(i, cell);
					}
				});
			});
			// Throwing is a reveal, and it is the body that loosed the arrow —
			// the phantom kit stands nowhere, so dispelling it exposes nobody.
			Invisibility.dispelOnThrow(ctx.body);
			ctx.complete(delay);
		};

		CharSprite sprite = ctx.body.sprite;
		if (sprite != null && sprite.parent != null
				&& (sprite.visible
						|| (target != null && target.sprite != null && target.sprite.visible)
						|| (Dungeon.level != null && cell >= 0
								&& cell < Dungeon.level.heroFOV.length
								&& Dungeon.level.heroFOV[cell]))) {
			ItemEchoBridge.castVisual(arrow, sprite, ctx.body.pos, dst, onArrive);
		} else {
			onArrive.call();
		}
		return true;
	}
}
