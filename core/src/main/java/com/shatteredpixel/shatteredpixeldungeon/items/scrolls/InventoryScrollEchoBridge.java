package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoActionContext;
import com.shatteredpixel.shatteredpixeldungeon.items.AiItemActions;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

/**
 * Echo inventory-scroll auto-selection — same-package access to protected hooks.
 *
 * Echo: auto-pick first usable kit item and apply without bag UI.
 * Hero: opens {@link #doRead()} selector.
 */
public final class InventoryScrollEchoBridge {

	private InventoryScrollEchoBridge() {
	}

	/**
	 * Echo: auto-pick first usable kit item and apply without bag UI.
	 * Hero: opens {@link #doRead()} selector.
	 */
	public static boolean read(InventoryScroll scroll, EchoActionContext ctx) {
		Item pick = null;
		for (Item item : ctx.kit.belongings) {
			if (item != scroll && scroll.usableOnItem(item)) {
				pick = item;
				break;
			}
		}
		if (pick == null) {
			return false;
		}
		InventoryScroll working = scroll;
		if (!scroll.anonymous) {
			Item detached = scroll.detach(ctx.kit.belongings.backpack);
			if (detached instanceof InventoryScroll) {
				working = (InventoryScroll) detached;
			}
		}
		final InventoryScroll applied = working;
		final Item target = pick;
		AiItemActions.withUser(ctx.kit, applied, () -> applied.onItemSelected(target));
		return true;
	}

	/**
	 * Echo: auto-upgrade first upgradable kit item (no {@link com.shatteredpixel.shatteredpixeldungeon.windows.WndUpgrade}).
	 * Hero: opens the upgrade confirmation window via {@link ScrollOfUpgrade#doRead()}.
	 */
	public static boolean readUpgrade(ScrollOfUpgrade scroll, EchoActionContext ctx) {
		Item pick = null;
		for (Item item : ctx.kit.belongings) {
			if (item != scroll && scroll.usableOnItem(item)) {
				pick = item;
				break;
			}
		}
		if (pick == null) {
			return false;
		}
		ScrollOfUpgrade working = scroll;
		if (!scroll.anonymous) {
			Item detached = scroll.detach(ctx.kit.belongings.backpack);
			if (detached instanceof ScrollOfUpgrade) {
				working = (ScrollOfUpgrade) detached;
			}
		}
		final ScrollOfUpgrade applied = working;
		final Item target = pick;
		AiItemActions.withUser(ctx.kit, applied, () -> applied.upgradeItem(target));
		return true;
	}
}
