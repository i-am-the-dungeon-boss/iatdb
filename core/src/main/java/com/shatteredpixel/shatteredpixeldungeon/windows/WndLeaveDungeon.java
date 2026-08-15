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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageGateway;

/**
 * The prompt at the top of the first floor: the stair out of the dungeon.
 *
 * <p>The counterpart to {@link WndDungeonMode} at the other end. Leaving is not
 * destructive — the run is saved where it stands and can be resumed — but it is
 * a whole trip ended by one step in the wrong direction, so it asks first.
 */
public class WndLeaveDungeon extends WndOptions {

	public static final int LEAVE = 0;
	public static final int STAY = 1;

	public WndLeaveDungeon() {
		super(Messages.get(WndLeaveDungeon.class, "title"),
				Messages.get(WndLeaveDungeon.class, "body"),
				Messages.get(WndLeaveDungeon.class, "leave"),
				Messages.get(WndLeaveDungeon.class, "stay"));
	}

	/**
	 * Pure mapping from option index to the decision. A dismissed window reports
	 * an index outside the options, which counts as staying.
	 */
	public static boolean leavesOn(int index) {
		return index == LEAVE;
	}

	@Override
	protected void onSelect(int index) {
		if (!leavesOn(index)) {
			return;
		}
		Level.beforeTransition();
		VillageGateway.returnToVillage();
	}
}
