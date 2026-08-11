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

import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;

/**
 * The village's own terrain vocabulary.
 *
 * <p>Deliberately independent of {@code levels.Terrain}: the village is not a
 * dungeon floor and does not want the dungeon's terrain semantics (traps,
 * secrets, chasms, locked doors). Only the tile *visual* indices are borrowed
 * from {@link DungeonTileSheet}, which are plain constants describing where art
 * sits on a tileset — they carry no run state.
 */
public final class VillageTerrain {

	public static final int GROUND = 0;
	public static final int PATH = 1;
	public static final int TALL_GRASS = 2;
	public static final int WATER = 3;
	public static final int WALL = 4;
	public static final int WALL_DECO = 5;
	public static final int DOOR = 6;
	public static final int WELL = 7;
	public static final int FIREPIT = 8;
	public static final int STATUE = 9;
	public static final int BOOKSHELF = 10;
	public static final int ALCHEMY = 11;
	public static final int CHEST = 12;
	/** The stair down into the dungeon. Standing on it raises the mode prompt. */
	public static final int DUNGEON_ENTRANCE = 13;

	private static final int COUNT = 14;

	private static final boolean[] PASSABLE = new boolean[COUNT];
	private static final int[] VISUAL = new int[COUNT];

	static {
		passable(GROUND, DungeonTileSheet.GRASS);
		passable(PATH, DungeonTileSheet.FLOOR_SP);
		passable(TALL_GRASS, DungeonTileSheet.FLAT_HIGH_GRASS);
		passable(WATER, DungeonTileSheet.WATER);
		passable(DOOR, DungeonTileSheet.FLAT_DOOR);
		passable(FIREPIT, DungeonTileSheet.EMBERS);
		passable(DUNGEON_ENTRANCE, DungeonTileSheet.EXIT);

		solid(WALL, DungeonTileSheet.FLAT_WALL);
		solid(WALL_DECO, DungeonTileSheet.FLAT_WALL_DECO);
		solid(WELL, DungeonTileSheet.WELL);
		solid(STATUE, DungeonTileSheet.FLAT_STATUE);
		solid(BOOKSHELF, DungeonTileSheet.FLAT_BOOKSHELF);
		solid(ALCHEMY, DungeonTileSheet.FLAT_ALCHEMY_POT);
		solid(CHEST, DungeonTileSheet.PEDESTAL);
	}

	private VillageTerrain() {
	}

	private static void passable(int terrain, int visual) {
		PASSABLE[terrain] = true;
		VISUAL[terrain] = visual;
	}

	private static void solid(int terrain, int visual) {
		PASSABLE[terrain] = false;
		VISUAL[terrain] = visual;
	}

	public static boolean passable(int terrain) {
		return terrain >= 0 && terrain < COUNT && PASSABLE[terrain];
	}

	/** Index of this terrain's art on the tileset. */
	public static int visual(int terrain) {
		if (terrain < 0 || terrain >= COUNT) {
			return DungeonTileSheet.FLAT_WALL;
		}
		return VISUAL[terrain];
	}

	public static int count() {
		return COUNT;
	}
}
