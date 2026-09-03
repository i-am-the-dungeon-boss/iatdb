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

/**
 * Planking over the jetty running out into the sea.
 *
 * <p>The village is floored from the city sheet, which has no wood on it at
 * all — its worked floor is the red carpet the walkway is made of, so a dock
 * laid in the level's own tiles reads as a rug thrown onto the water. The
 * sewers' worked floor is boards, and boards are what a dock is, so the jetty
 * borrows that one tile and nothing else from that sheet.
 */
public class VillageDock extends CustomTilemap implements AltarOverlay {

	{
		texture = Assets.Environment.TILES_SEWERS;
	}

	public VillageDock(int left, int top, int width, int height) {
		setRect(left, top, width, height);
	}

	@Override
	public String textureName() {
		return Assets.Environment.TILES_SEWERS;
	}

	/** Pure — no GL. See {@link AltarQuadrant#tileData()}. */
	@Override
	public int[] tileData() {
		int[] data = new int[tileW * tileH];
		for (int i = 0; i < data.length; i++) {
			data[i] = DungeonTileSheet.FLOOR_SP;
		}
		return data;
	}

	@Override
	public Tilemap create() {
		Tilemap v = super.create();
		v.map(tileData(), tileW);
		return v;
	}
}
