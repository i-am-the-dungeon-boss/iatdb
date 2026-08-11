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

import com.watabou.noosa.Tilemap;
import com.watabou.noosa.TextureFilm;

/**
 * Draws a {@link VillageMap}.
 *
 * <p>Flat tiles only. The dungeon's tilemaps autotile walls into raised
 * three-dimensional visuals, but that logic reads the global dungeon level, so
 * the village draws its own tiles straight from the sheet instead. The village
 * reads as a flat top-down settlement rather than a dungeon interior.
 */
public class VillageTilemap extends Tilemap {

	public static final int SIZE = 16;

	public VillageTilemap(String tex, VillageMap map) {
		super(tex, new TextureFilm(tex, SIZE, SIZE));
		map(map.visuals(), map.width);
	}

	/** Top-left corner of a cell, in world coordinates. */
	public static float cellX(VillageMap map, int cell) {
		return map.x(cell) * SIZE;
	}

	public static float cellY(VillageMap map, int cell) {
		return map.y(cell) * SIZE;
	}

	/** The cell under a world-space point, or -1 when outside the map. */
	public static int cellAt(VillageMap map, float worldX, float worldY) {
		int x = (int) Math.floor(worldX / SIZE);
		int y = (int) Math.floor(worldY / SIZE);
		if (x < 0 || y < 0 || x >= map.width || y >= map.height) {
			return -1;
		}
		return map.cell(x, y);
	}
}
