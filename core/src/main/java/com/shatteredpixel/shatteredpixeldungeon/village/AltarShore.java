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

import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

/**
 * The worked edge where a basin meets the paving around it.
 *
 * <p>The village lays its basins in a plaza floored with {@code EMPTY_SP}, and
 * {@code EMPTY_SP} is not in {@link DungeonTileSheet#waterStitcheable} — so the
 * terrain layer stitches every basin cell against nothing, lands on the bare
 * {@code WATER} index, and {@code DungeonTerrainTilemap.needsRender} then skips
 * it outright. The result is water with no shoreline at all: a square hole cut
 * in the floor. The four-bit stitch is therefore worked out here from the
 * basin's own shape rather than from terrain.
 *
 * <p>Drawn from the region's own sheet and laid over {@link AltarPool}, so the
 * shore matches the water it holds — the sewers' brick lip, the caves' raw rock
 * — instead of borrowing the city's.
 */
public class AltarShore extends CustomTilemap implements AltarOverlay {

	private int region = EchoAltar.NONE;

	/** For bundle restore, which fills the region in immediately afterwards. */
	public AltarShore() {
	}

	public AltarShore(int region) {
		region(region);
	}

	public void region(int region) {
		this.region = region;
		texture = AltarQuadrant.textureFor(region);
		setRect(EchoAltar.basinLeft(region), EchoAltar.basinTop(region),
				EchoAltar.basinWidth(region), EchoAltar.basinHeight(region));
	}

	public int region() {
		return region;
	}

	@Override
	public String textureName() {
		return AltarQuadrant.textureFor(region);
	}

	/** Pure — no GL. See {@link AltarQuadrant#tileData()}. */
	@Override
	public int[] tileData() {
		int[] data = new int[tileW * tileH];
		for (int i = 0; i < data.length; i++) {
			int x = tileX + (i % tileW);
			int y = tileY + (i / tileW);
			if (!EchoAltar.inBasin(x, y)) {
				data[i] = -1;
			} else {
				data[i] = DungeonTileSheet.stitchWaterTile(
						neighbour(x, y - 1), neighbour(x + 1, y),
						neighbour(x, y + 1), neighbour(x - 1, y));
			}
		}
		return data;
	}

	/**
	 * What the stitch should treat a neighbour as. Anything outside the basin is
	 * the plaza's floor, which the shore has to be drawn against whatever the
	 * terrain there happens to be paved with.
	 */
	private static int neighbour(int x, int y) {
		return EchoAltar.inBasin(x, y) ? Terrain.WATER : Terrain.EMPTY;
	}

	@Override
	public Tilemap create() {
		Tilemap v = super.create();
		v.map(tileData(), tileW);
		return v;
	}

	private static final String REGION = "region";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(REGION, region);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		region(bundle.getInt(REGION));
	}
}
