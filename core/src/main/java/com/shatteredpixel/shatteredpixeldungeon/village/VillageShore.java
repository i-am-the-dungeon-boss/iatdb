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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

/**
 * The lip where the sea meets the beach.
 *
 * <p>The level draws its terrain from {@code tilesTex()}, which is the city's,
 * so the waterline is stitched as a pale worked kerb. The beach it runs along
 * is caves floor and the sea is caves water, and a city kerb between the two
 * reads as a harbour wall dropped into a stretch of open coast. The four-bit
 * stitch is therefore recomputed here and redrawn from the caves sheet.
 *
 * <p>Which cells are sea is handed in as a mask rather than read back off
 * {@code Dungeon.level}, for the reason given on {@link VillageSkin}. The altar
 * basins are left out of it: they are inland water with their own
 * {@link AltarShore}, each edged in its own region's stone.
 */
public class VillageShore extends CustomTilemap implements AltarOverlay {

	private boolean[] sea;

	/** For bundle restore, which fills the mask in immediately afterwards. */
	public VillageShore() {
	}

	/**
	 * @param sea one flag per cell of the rect, row-major from its top-left
	 */
	public VillageShore(int left, int top, int width, int height, boolean[] sea) {
		if (sea.length != width * height) {
			throw new IllegalArgumentException(
					"mask is " + sea.length + " cells for a " + width + "x" + height + " rect");
		}
		this.sea = sea;
		texture = Assets.Environment.TILES_CAVES;
		setRect(left, top, width, height);
	}

	@Override
	public String textureName() {
		return Assets.Environment.TILES_CAVES;
	}

	/** Pure — no GL. See {@link AltarQuadrant#tileData()}. */
	@Override
	public int[] tileData() {
		int[] data = new int[tileW * tileH];
		for (int i = 0; i < data.length; i++) {
			int x = i % tileW;
			int y = i / tileW;
			if (!sea[i]) {
				data[i] = -1;
				continue;
			}
			int stitched = DungeonTileSheet.stitchWaterTile(
					land(x, y - 1), land(x + 1, y), land(x, y + 1), land(x - 1, y));
			// open water stitches to the bare WATER index, which the terrain
			// layer skips - so there is nothing to redraw there either
			data[i] = stitched == DungeonTileSheet.WATER ? -1 : stitched;
		}
		return data;
	}

	/**
	 * What the stitch should see in a neighbouring cell. Off the rect counts as
	 * sea, so the edge of the overlay is not mistaken for a shoreline of its own.
	 */
	private int land(int x, int y) {
		if (x < 0 || y < 0 || x >= tileW || y >= tileH) {
			return com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WATER;
		}
		return sea[y * tileW + x]
				? com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WATER
				: com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY;
	}

	@Override
	public Tilemap create() {
		Tilemap v = super.create();
		v.map(tileData(), tileW);
		return v;
	}

	private static final String SEA = "sea";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(SEA, sea);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		sea = bundle.getBooleanArray(SEA);
		texture = Assets.Environment.TILES_CAVES;
	}
}
