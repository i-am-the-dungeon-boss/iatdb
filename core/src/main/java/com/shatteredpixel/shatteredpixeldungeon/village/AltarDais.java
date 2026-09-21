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
 * The raised centre of the altar, floored from the halls.
 *
 * <p>The deepest echo the site knows sits here, so the ground under it is the
 * deepest ground in the game — the same reasoning as the four quarters, applied
 * to the middle. Nine cells to a side, and nothing but platform: the walkway
 * runs up to its edge in the same paving it carries everywhere else in town,
 * which reads as one road arriving rather than as a stepped approach.
 */
public class AltarDais extends CustomTilemap implements AltarOverlay {

	{
		texture = Assets.Environment.TILES_HALLS;
	}

	public AltarDais() {
		setRect(EchoAltar.DAIS_LEFT, EchoAltar.DAIS_TOP,
				EchoAltar.DAIS_SPAN, EchoAltar.DAIS_SPAN);
	}

	@Override
	public String textureName() {
		return Assets.Environment.TILES_HALLS;
	}

	/** Pure — no GL. See {@link AltarQuadrant#tileData()}. */
	@Override
	public int[] tileData() {
		int[] data = new int[tileW * tileH];
		for (int i = 0; i < data.length; i++) {
			int x = tileX + (i % tileW);
			int y = tileY + (i / tileW);
			// the halls' plain floor, as every quarter takes its own sheet's
			// plain floor - the centre is the halls quarter and has no reason
			// to be laid differently from the other four
			data[i] = EchoAltar.inDais(x, y) ? DungeonTileSheet.FLOOR : -1;
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
