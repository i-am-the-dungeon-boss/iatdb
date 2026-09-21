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

package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/**
 * The start-here shout over the village dungeon stairs.
 *
 * <p>
 * Same rising, fading text as a figure's title: the stairs are scenery, so
 * the words come from here and {@link VillageFigureTitleCrier} does the
 * shouting.
 */
public final class VillageStairsGuide {

	/** Warm gold, distinct from the icy remote-player names. */
	private static final int COLOR = 0xFFE680;

	private VillageStairsGuide() {
	}

	/** The cell the shout is anchored to: the dungeon mouth itself. */
	public static int cell(VillageLevel level) {
		return level.dungeonEntrance();
	}

	public static VillageTitle title() {
		return new VillageTitle(Messages.get(VillageLevel.class, "start_here"), COLOR, null);
	}
}
