/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.heroechoes.inspect;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;

/**
 * A throwaway copy of somebody else's item, made to be looked at.
 *
 * <p>Two things are wrong with showing a foreign item directly. Its description
 * is written against {@code Dungeon.hero} — so a duelist reading a mage echo's
 * staff is shown duelist ability text — and it reads as the guesswork its owner
 * died with rather than as the known kit an echo is laid out as.
 *
 * <p>Both are fixed on a copy rather than on the item, because the item may be a
 * live {@link com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss}'s
 * fighting kit. A preview stamps the copy with the hero it belongs to, so
 * {@link Item#owner()} answers with that hero instead of the player, and
 * identifies the copy for display. The original is never touched.
 */
public final class ItemPreview {

	private ItemPreview() {
	}

	/**
	 * A copy of {@code item} that describes itself as {@code owner}'s and reads
	 * as identified. Null when the item cannot be copied, which is the caller's
	 * cue to show nothing — a preview that fell back to the original would be
	 * the leak this class exists to close.
	 */
	public static Item of(Item item, Hero owner) {
		if (item == null || owner == null) {
			throw new IllegalArgumentException("a preview needs an item and the hero it belongs to");
		}
		Item preview = item.duplicate();
		if (preview == null) {
			return null;
		}
		stamp(preview, owner);
		identifyForDisplay(preview);
		return preview;
	}

	/**
	 * A staff carries a wand whose stats the staff's own description quotes, and
	 * a copy's wand is a copy too — so it needs the same owner, or that one
	 * paragraph would still be written against the player.
	 */
	private static void stamp(Item preview, Hero owner) {
		preview.previewOwner(owner);
		if (preview instanceof MagesStaff && ((MagesStaff) preview).wand() != null) {
			((MagesStaff) preview).wand().previewOwner(owner);
		}
	}

	/**
	 * An echo is a dead hero laid out for inspection, so what it carried reads as
	 * known rather than as the guesswork its owner died with.
	 *
	 * <p>{@code identify(false)} deliberately skips the by-hero half of
	 * identification — no catalog entry, no discovered-item statistic. Rings are
	 * the one worn slot whose identity lives in a table shared with the player
	 * rather than on the item, so they are anonymized instead: the base game's
	 * own way to show a true name without touching that table.
	 */
	private static void identifyForDisplay(Item preview) {
		if (preview instanceof Ring) {
			((Ring) preview).anonymize();
		}
		preview.identify(false);
		if (preview instanceof MagesStaff && ((MagesStaff) preview).wand() != null) {
			((MagesStaff) preview).wand().identify(false);
		}
	}
}
