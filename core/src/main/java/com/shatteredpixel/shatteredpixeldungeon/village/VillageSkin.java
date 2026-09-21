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

import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

/**
 * A patch of the village re-skinned from some other region's tile sheet.
 *
 * <p>A level draws its terrain from one sheet — {@code tilesTex()} — so the
 * village's ground, paths and woods would all have to be the city's if terrain
 * were the only mechanism. The altar already answers that with a tilemap per
 * quarter; this is the same trick for everything outside it, and one class
 * rather than four because the only thing that differs between them is the
 * sheet, the tile and which cells are covered.
 *
 * <p>Which cells are covered is handed in as a mask rather than read back off
 * {@code Dungeon.level}: the level that built this is the level it belongs to,
 * and reaching for the global would quietly re-skin whatever happened to be
 * loaded instead.
 *
 * <p>The alt tile is picked from the cell's own coordinates, not at random. The
 * engine's own {@code getVisualWithAlts} needs {@code setupVariance()}, which
 * runs in {@code GameScene.create()} and so is not available to the tests or to
 * the map renderer.
 */
public class VillageSkin extends CustomTilemap implements AltarOverlay {

	private String sheet;
	private int tile;
	private int altTile;
	private boolean[] covers;

	/** For bundle restore, which fills everything in immediately afterwards. */
	public VillageSkin() {
	}

	/**
	 * @param covers one flag per cell of the rect, row-major from its top-left
	 */
	public VillageSkin(String sheet, int tile, int altTile,
			int left, int top, int width, int height, boolean[] covers) {
		if (covers.length != width * height) {
			throw new IllegalArgumentException(
					"mask is " + covers.length + " cells for a " + width + "x" + height + " rect");
		}
		this.sheet = sheet;
		this.tile = tile;
		this.altTile = altTile;
		this.covers = covers;
		texture = sheet;
		setRect(left, top, width, height);
	}

	@Override
	public String textureName() {
		return sheet;
	}

	/** Pure — no GL. See {@link AltarQuadrant#tileData()}. */
	@Override
	public int[] tileData() {
		int[] data = new int[tileW * tileH];
		for (int i = 0; i < data.length; i++) {
			if (!covers[i]) {
				data[i] = -1;
			} else {
				int x = tileX + (i % tileW);
				int y = tileY + (i / tileW);
				data[i] = isAlt(x, y) ? altTile : tile;
			}
		}
		return data;
	}

	/**
	 * Scattered rather than striped: a plain {@code (x + y) % 2} lays the two
	 * tiles out as a checkerboard, which reads as a pattern instead of as
	 * variation.
	 */
	static boolean isAlt(int x, int y) {
		return (x * 31 + y * 17) % 3 == 0;
	}

	@Override
	public Tilemap create() {
		Tilemap v = super.create();
		v.map(tileData(), tileW);
		return v;
	}

	private static final String SHEET = "sheet";
	private static final String TILE = "tile";
	private static final String ALT_TILE = "altTile";
	private static final String COVERS = "covers";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(SHEET, sheet);
		bundle.put(TILE, tile);
		bundle.put(ALT_TILE, altTile);
		bundle.put(COVERS, covers);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		sheet = bundle.getString(SHEET);
		tile = bundle.getInt(TILE);
		altTile = bundle.getInt(ALT_TILE);
		covers = bundle.getBooleanArray(COVERS);
		texture = sheet;
	}
}
