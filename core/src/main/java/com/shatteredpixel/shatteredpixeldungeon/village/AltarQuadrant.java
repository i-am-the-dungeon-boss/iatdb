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
 * One quarter of the echo altar, floored with its own region's ground.
 *
 * <p>The echo standing here is the reigning player-echo for that stretch of the
 * dungeon, and nothing about the figure says which stretch that is — the ground
 * under it carries the whole of that meaning, which is why each quarter is
 * textured from its own region sheet rather than recoloured.
 *
 * <p>All four quarters are the same class holding a region, not four
 * near-identical classes: the only thing that varies is the texture and which
 * cells the quarter owns, and both fall out of the region.
 */
public class AltarQuadrant extends CustomTilemap implements AltarOverlay {

	private int region = EchoAltar.NONE;

	/** For bundle restore, which fills the region in immediately afterwards. */
	public AltarQuadrant() {
	}

	public AltarQuadrant(int region) {
		region(region);
	}

	public void region(int region) {
		this.region = region;
		texture = textureFor(region);
		setRect(EchoAltar.DISC_LEFT, EchoAltar.DISC_TOP,
				EchoAltar.DISC_SPAN, EchoAltar.DISC_SPAN);
	}

	public int region() {
		return region;
	}

	/**
	 * The tile each region floors its quarter with.
	 *
	 * <p>Not simply {@code FLOOR} everywhere. Four dungeon floors laid side by
	 * side have to be told apart at a glance, and the plain floors of the sewers
	 * and the prison are within a few values of the same grey while the caves'
	 * is so dark it reads as a hole in the plaza. So each quarter takes whichever
	 * of its own sheet's ground tiles carries the most colour — still that
	 * region's own art, just the part of it that says which region this is.
	 */
	public static int floorFor(int region) {
		switch (region) {
			case EchoAltar.SEWERS:
				// the sewers' worked floor: warm brown against the prison's grey
				return DungeonTileSheet.FLOOR_SP;
			case EchoAltar.PRISON:
				return DungeonTileSheet.FLOOR;
			case EchoAltar.CAVES:
				// the caves' plain floor is nearly black; its worked one is not
				return DungeonTileSheet.FLOOR_SP;
			case EchoAltar.CITY:
				// the city's special floor is the red carpet the walkway is made
				// of, so this quarter has to stay on the plain blue-grey stone
				return DungeonTileSheet.FLOOR;
			default:
				throw new IllegalArgumentException("no altar region floor for " + region);
		}
	}

	/**
	 * The sheet a region floors its quarter from. Unknown regions throw rather
	 * than falling back to a default sheet: a quarter drawn from the wrong region
	 * is exactly the bug this class exists to prevent, and it would be silent.
	 */
	public static String textureFor(int region) {
		switch (region) {
			case EchoAltar.SEWERS:
				return Assets.Environment.TILES_SEWERS;
			case EchoAltar.PRISON:
				return Assets.Environment.TILES_PRISON;
			case EchoAltar.CAVES:
				return Assets.Environment.TILES_CAVES;
			case EchoAltar.CITY:
				return Assets.Environment.TILES_CITY;
			default:
				throw new IllegalArgumentException("no altar region texture for " + region);
		}
	}

	@Override
	public String textureName() {
		return textureFor(region);
	}

	/**
	 * Pure — no GL, no {@code Dungeon.level}. The tests assert on this, because
	 * {@link #create()} resolves the texture through the texture cache and cannot
	 * run headlessly; do not inline this back into {@code create()}.
	 */
	@Override
	public int[] tileData() {
		int[] data = new int[tileW * tileH];
		for (int i = 0; i < data.length; i++) {
			int x = tileX + (i % tileW);
			int y = tileY + (i / tileW);
			if (EchoAltar.quadrantOf(x, y) != region || EchoAltar.inBasin(x, y)) {
				// -1 is the engine's skip index (Tilemap.needsRender), which is
				// what lets a rectangle paint a quarter-disc. The basin is left
				// out too: flooring it would bury the water under stone
				data[i] = -1;
			} else {
				// no alt-tile variance: FLOOR_ALT_1 and FLOOR_ALT_2 are pixel for
				// pixel the plain floor on every region sheet, so varying between
				// them buys nothing but a scene-time dependency the tests cannot
				// satisfy
				data[i] = floorFor(region);
			}
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
		// re-derives the texture too: a restored quarter without one would draw
		// nothing at all rather than fail
		region(bundle.getInt(REGION));
	}
}
