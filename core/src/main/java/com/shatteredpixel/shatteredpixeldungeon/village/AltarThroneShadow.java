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

/**
 * The shadow the throne's back casts, one cell above the seat.
 *
 * <p>Separate from {@link AltarThrone} because it belongs on the wall layer:
 * the ground layer draws under everything standing on it, and a shadow that an
 * echo walks over stops reading as a shadow. This is the same split
 * {@code CityBossLevel} makes between its ground and wall visuals.
 */
public class AltarThroneShadow extends CustomTilemap implements AltarOverlay {

	/** Row 13, column 5 of the 8-wide boss sheet. */
	private static final int SHADOW = 13 * 8 + 5;

	{
		texture = Assets.Environment.CITY_BOSS;
	}

	public AltarThroneShadow() {
		setRect(EchoAltar.THRONE_SEAT_X, EchoAltar.THRONE_SEAT_Y - 1, 1, 1);
	}

	@Override
	public String textureName() {
		return Assets.Environment.CITY_BOSS;
	}

	/** Pure — no GL. See {@link AltarQuadrant#tileData()}. */
	@Override
	public int[] tileData() {
		return new int[] { SHADOW };
	}

	@Override
	public Tilemap create() {
		Tilemap v = super.create();
		v.map(tileData(), tileW);
		return v;
	}
}
