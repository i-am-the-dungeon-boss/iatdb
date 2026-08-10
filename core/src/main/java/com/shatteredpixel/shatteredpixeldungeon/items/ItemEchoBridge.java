package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.watabou.utils.Callback;

/**
 * Same-package throw VFX helper for Echo adapters / headless tests. Hero
 * {@link Item#cast} keeps its own upstream missile path.
 */
public final class ItemEchoBridge {

	private ItemEchoBridge() {
	}

	/**
	 * Throw VFX only: zap + sound + {@link MissileSprite} of this item.
	 * No inventory detach and no actor time spend — callers own those.
	 * If {@code from} is off-stage, invokes {@code onArrive} immediately.
	 *
	 * @return collision cell of the throw
	 */
	public static int castVisual(Item item, CharSprite from, int fromPos, int dst, Callback onArrive) {
		final int cell = item.throwPos(fromPos, dst);
		if (from == null || from.parent == null) {
			if (onArrive != null) {
				onArrive.call();
			}
			return cell;
		}

		from.zap(cell);
		item.throwSound();

		Char atCell = Actor.findChar(cell);
		if (atCell != null && atCell.sprite != null) {
			((MissileSprite) from.parent.recycle(MissileSprite.class))
					.reset(from, atCell.sprite, item, onArrive);
		} else {
			((MissileSprite) from.parent.recycle(MissileSprite.class))
					.reset(from, cell, item, onArrive);
		}
		return cell;
	}
}
