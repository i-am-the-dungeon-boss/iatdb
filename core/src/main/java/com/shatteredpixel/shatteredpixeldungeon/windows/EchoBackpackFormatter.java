package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** Formats backpack contents for echo detail views. */
public final class EchoBackpackFormatter {

	private EchoBackpackFormatter() {}

	public static void appendTo(StringBuilder sb, Bag backpack) {
		appendLine(sb, "backpack_header");
		if (backpack == null || backpack.items.isEmpty()) {
			appendRawLine(sb, Messages.get(WndEchoes.class, "backpack_empty"));
			return;
		}
		appendBagContents(sb, backpack, "  ");
	}

	private static void appendBagContents(StringBuilder sb, Bag bag, String indent) {
		for (Item item : bag.items) {
			appendRawLine(sb, indent + formatItem(item));
			if (item instanceof Bag) {
				Bag nested = (Bag) item;
				if (!nested.items.isEmpty()) {
					appendBagContents(sb, nested, indent + "  ");
				}
			}
		}
	}

	static String formatItem(Item item) {
		if (item.stackable && item.quantity() > 1) {
			return Messages.get(WndEchoes.class, "backpack_item_qty", item.name(), item.quantity());
		}
		return item.title();
	}

	private static void appendRawLine(StringBuilder sb, String line) {
		if (sb.length() > 0) sb.append('\n');
		sb.append(line);
	}

	private static void appendLine(StringBuilder sb, String key, Object... args) {
		if (sb.length() > 0) sb.append('\n');
		sb.append(Messages.get(WndEchoes.class, key, args));
	}
}
