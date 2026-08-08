package com.shatteredpixel.shatteredpixeldungeon.items.stones;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Enchanting;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoActionContext;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;

/**
 * Same-package bridge so Echo inventory-stone adapters can auto-select kit items
 * and apply stone effects without Hero bag UI.
 *
 * Self-activate. Hero opens bag UI; Echo auto-picks the first usable kit item.
 */
public final class InventoryStoneEchoBridge {

	private InventoryStoneEchoBridge() {
	}

	/** First backpack/equip item this stone can affect. */
	public static Item firstUsable(InventoryStone stone, Hero kit) {
		for (Item item : kit.belongings) {
			if (item != stone && stone.usableOnItem(item)) {
				return item;
			}
		}
		return null;
	}

	/**
	 * Apply stone to a chosen item. Default bridges to
	 * {@link #onItemSelected(Item)}
	 * with {@code curUser}/{@code curItem} set for legacy callers.
	 */
	public static boolean applyEcho(EchoActionContext ctx, InventoryStone stone, Item item) {
		if (stone instanceof StoneOfEnchantment) {
			return applyEnchantment(ctx, (StoneOfEnchantment) stone, item);
		}
		if (stone instanceof StoneOfAugmentation) {
			return applyAugmentation(ctx, (StoneOfAugmentation) stone, item);
		}
		if (stone instanceof StoneOfDetectMagic) {
			return applyDetectMagic(ctx, (StoneOfDetectMagic) stone, item);
		}
		if (stone instanceof StoneOfIntuition) {
			return applyIntuition(ctx, (StoneOfIntuition) stone, item);
		}
		return false;
	}

	private static boolean applyEnchantment(EchoActionContext ctx, StoneOfEnchantment stone, Item item) {
		if (!stone.anonymous) {
			stone.detach(ctx.kit.belongings.backpack);
			Catalog.countUse(stone.getClass());
			Talent.onRunestoneUsed(ctx.kit, ctx.body.pos, stone.getClass());
		}

		if (item instanceof Weapon) {
			((Weapon) item).enchant();
		} else {
			((Armor) item).inscribe();
		}

		if (ctx.canWorldFx()) {
			ctx.body.sprite.emitter().start(Speck.factory(Speck.LIGHT), 0.1f, 5);
			Enchanting.show(ctx.kit, item);
		}
		return true;
	}

	private static boolean applyAugmentation(EchoActionContext ctx, StoneOfAugmentation stone, Item item) {
		// Echo: no WndAugment — default SPEED / EVASION
		if (item instanceof Weapon) {
			((Weapon) item).augment = Weapon.Augment.SPEED;
		} else if (item instanceof Armor) {
			((Armor) item).augment = Armor.Augment.EVASION;
		} else {
			return false;
		}
		if (!stone.anonymous) {
			stone.detach(ctx.kit.belongings.backpack);
			Catalog.countUse(stone.getClass());
			Talent.onRunestoneUsed(ctx.kit, ctx.body.pos, stone.getClass());
		}
		return true;
	}

	private static boolean applyDetectMagic(EchoActionContext ctx, StoneOfDetectMagic stone, Item item) {
		item.cursedKnown = true;
		if (!stone.anonymous) {
			stone.detach(ctx.kit.belongings.backpack);
			Catalog.countUse(stone.getClass());
			Talent.onRunestoneUsed(ctx.kit, ctx.body.pos, stone.getClass());
		}
		return true;
	}

	private static boolean applyIntuition(EchoActionContext ctx, StoneOfIntuition stone, Item item) {
		// Echo: skip guess UI — identify the unknown item
		item.identify();
		if (!stone.anonymous) {
			stone.detach(ctx.kit.belongings.backpack);
			Catalog.countUse(stone.getClass());
			Talent.onRunestoneUsed(ctx.kit, ctx.body.pos, stone.getClass());
		}
		return true;
	}
}
