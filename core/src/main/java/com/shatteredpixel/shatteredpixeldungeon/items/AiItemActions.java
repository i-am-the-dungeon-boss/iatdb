package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;

/**
 * Package bridge so AI can set {@link Item#curUser} / {@link Item#curItem}
 * for zap/throw side effects without CellSelector.
 */
public final class AiItemActions {

	private AiItemActions() {
	}

	public static void withUser(Hero user, Item item, Runnable action) {
		Hero previousUser = Item.curUser;
		Item previousItem = Item.curItem;
		Item.curUser = user;
		Item.curItem = item;
		try {
			action.run();
		} finally {
			Item.curUser = previousUser;
			Item.curItem = previousItem;
		}
	}

	public static void withUser(Hero user, Runnable action) {
		withUser(user, Item.curItem, action);
	}
}
