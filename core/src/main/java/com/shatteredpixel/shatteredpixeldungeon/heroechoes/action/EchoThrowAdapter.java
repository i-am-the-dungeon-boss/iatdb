package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.AiItemActions;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.ItemEchoBridge;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.BombEchoBridge;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBowEchoBridge;
import com.watabou.utils.Callback;

/**
 * Echo throwable execute — fork-owned. Does not open CellSelector, does not
 * touch
 * player QuickSlot / Improvised Projectiles / Catalog riders.
 */
public final class EchoThrowAdapter {

	private EchoThrowAdapter() {
	}

	public static boolean throwItem(EchoBoss boss, Item item, int dst) {
		if (boss == null || item == null || Dungeon.level == null || dst < 0) {
			return false;
		}
		EchoActionContext ctx = EchoActionContext.of(boss);

		if (item instanceof SpiritBow.SpiritArrow) {
			return SpiritBowEchoBridge.throwArrow(boss, (SpiritBow.SpiritArrow) item, dst);
		}

		if (item instanceof Bomb) {
			BombEchoBridge.markEchoLightThrow((Bomb) item);
		}

		ctx.busy();

		final int cell = item.throwPos(ctx.body.pos, dst);
		final float delay = item.castDelay(ctx.kit, cell);
		final Bomb bomb = item instanceof Bomb ? (Bomb) item : null;

		Callback onArrive = () -> {
			// Re-apply throw intent here: MissileSprite can delay this callback, and
			// static flags (e.g. Bomb.lightingFuse) must not be assumed to still hold.
			if (bomb != null) {
				BombEchoBridge.reassertLightFuse(bomb);
			}
			EchoKitBorrow.run(ctx, () -> {
				AiItemActions.withUser(ctx.kit, item, () -> {
					Item thrown = item.detach(ctx.kit.belongings.backpack);
					if (thrown != null) {
						if (bomb != null) {
							BombEchoBridge.markDetachedIgnite(bomb, thrown);
						}
						AiItemActions.onThrow(thrown, cell);
					}
				});
				ctx.complete(delay);
			});
		};

		ItemEchoBridge.castVisual(item, ctx.body.sprite, ctx.body.pos, dst, onArrive);
		return true;
	}
}
