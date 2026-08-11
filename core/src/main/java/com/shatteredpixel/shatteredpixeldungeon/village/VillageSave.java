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

package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.IOException;

/**
 * Where the player was standing in the village, and who they looked like.
 *
 * <p>Its own file, outside {@code GamesInProgress} and outside every play-mode
 * folder — the village belongs to the player, not to a run, and survives every
 * run's death. Deliberately tiny: the maps are hand-authored and rebuilt from
 * code, so only the position and appearance need storing.
 */
public final class VillageSave {

	private static final String FILE = "village.dat";

	private static final String AREA = "area";
	private static final String CELL = "cell";
	private static final String LOOK = "look";

	private static VillageMap.Area area = VillageMap.Area.VILLAGE;
	private static int cell = -1;
	private static HeroClass look = HeroClass.WARRIOR;

	private VillageSave() {
	}

	public static VillageMap.Area area() {
		return area;
	}

	public static int cell() {
		return cell;
	}

	public static HeroClass look() {
		return look;
	}

	public static void setLook(HeroClass heroClass) {
		if (heroClass != null) {
			look = heroClass;
		}
	}

	/** Records where the avatar is, without touching the disk. */
	public static void setPosition(VillageMap.Area newArea, int newCell) {
		area = newArea == null ? VillageMap.Area.VILLAGE : newArea;
		cell = newCell;
	}

	/** Forgets everything, so the next entry starts at the village arrival point. */
	public static void reset() {
		area = VillageMap.Area.VILLAGE;
		cell = -1;
		look = HeroClass.WARRIOR;
	}

	public static boolean exists() {
		return FileUtils.fileExists(FILE);
	}

	public static void load() {
		if (!exists()) {
			reset();
			return;
		}
		try {
			Bundle bundle = FileUtils.bundleFromFile(FILE);
			area = readArea(bundle.getString(AREA));
			cell = bundle.getInt(CELL);
			look = readLook(bundle.getString(LOOK));
		} catch (IOException e) {
			// a village we cannot read is a village we start fresh, never a crash
			reset();
		}
	}

	public static void save() {
		Bundle bundle = new Bundle();
		bundle.put(AREA, area.name());
		bundle.put(CELL, cell);
		bundle.put(LOOK, look.name());
		try {
			FileUtils.bundleToFile(FILE, bundle);
		} catch (IOException e) {
			// losing the village position is not worth interrupting play over
			ShatteredPixelDungeon.reportException(e);
		}
	}

	private static VillageMap.Area readArea(String name) {
		try {
			return VillageMap.Area.valueOf(name);
		} catch (IllegalArgumentException | NullPointerException e) {
			return VillageMap.Area.VILLAGE;
		}
	}

	private static HeroClass readLook(String name) {
		try {
			return HeroClass.valueOf(name);
		} catch (IllegalArgumentException | NullPointerException e) {
			return HeroClass.WARRIOR;
		}
	}
}
