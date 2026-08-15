/*
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

package com.shatteredpixel.shatteredpixeldungeon.worldnet.ui;

import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNet;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageSession;
import com.watabou.utils.Strings;

/**
 * Decides whether the local hero wears a {@link NameTag}, and what it reads.
 *
 * <p>Remote players are labelled from their roster entry; the local player has
 * no roster entry of their own, so the label comes from the account name
 * instead. It is village-only for the same reason the ghosts are: in a run the
 * hero is the only player around and a floating name is just clutter.
 */
public final class LocalPlayerTag {

	private LocalPlayerTag() {
	}

	/** The tag text for the current player, or {@code null} when none should show. */
	public static String label() {
		// Through WorldNet rather than the Echo session directly, so the UI has
		// no opinion about where identity comes from.
		return label(VillageSession.inVillage(), WorldNet.localName());
	}

	/** Pure form of {@link #label()}, split out so the rule can be tested. */
	public static String label(boolean inVillage, String username) {
		if (!inVillage || Strings.isBlank(username)) {
			return null;
		}
		return username.trim();
	}
}
