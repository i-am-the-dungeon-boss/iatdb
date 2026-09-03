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
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

/**
 * The basin in one quarter of the altar, holding that region's own water.
 *
 * <p>A level has exactly one water texture — {@code GameScene} builds a single
 * scrolling block from {@code Level.waterTex()} and every water cell on the map
 * shows through it. So four different waters cannot be terrain; each basin is
 * instead laid over its water cells as a flat patch of that region's water
 * image, which is what makes the sewers' green and the halls' red sit in the
 * same plaza.
 *
 * <p>The trade is that these basins do not ripple and do not take the sheet's
 * shoreline stitching, since the overlay draws above both. Worked stone basins
 * with a hard edge is the reading that costs nothing to be honest about.
 */
public class AltarPool extends CustomTilemap implements AltarOverlay {

	/** The water images are 32x32 — two tiles by two, repeating. */
	private static final int TEXTURE_TILES = 2;

	private int region = EchoAltar.NONE;

	/** For bundle restore, which fills the region in immediately afterwards. */
	public AltarPool() {
	}

	public AltarPool(int region) {
		region(region);
	}

	public void region(int region) {
		this.region = region;
		texture = waterFor(region);
		setRect(EchoAltar.basinLeft(region), EchoAltar.basinTop(region),
				EchoAltar.BASIN_SPAN, EchoAltar.BASIN_SPAN);
	}

	public int region() {
		return region;
	}

	/**
	 * The water a region fills its basin from. Unknown regions throw rather than
	 * falling back: a basin quietly showing the wrong region's water says the
	 * wrong thing about the echo standing beside it, and says it silently.
	 */
	public static String waterFor(int region) {
		switch (region) {
			case EchoAltar.SEWERS:
				return Assets.Environment.WATER_SEWERS;
			case EchoAltar.PRISON:
				return Assets.Environment.WATER_PRISON;
			case EchoAltar.CAVES:
				return Assets.Environment.WATER_CAVES;
			case EchoAltar.CITY:
				return Assets.Environment.WATER_CITY;
			default:
				throw new IllegalArgumentException("no altar basin water for " + region);
		}
	}

	@Override
	public String textureName() {
		return waterFor(region);
	}

	/** Pure — no GL. See {@link AltarQuadrant#tileData()}. */
	@Override
	public int[] tileData() {
		int[] data = new int[tileW * tileH];
		for (int i = 0; i < data.length; i++) {
			int x = tileX + (i % tileW);
			int y = tileY + (i / tileW);
			// indexed by absolute position, so the water's pattern runs on across
			// the basin instead of restarting in each cell
			data[i] = (y % TEXTURE_TILES) * TEXTURE_TILES + (x % TEXTURE_TILES);
		}
		return data;
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
