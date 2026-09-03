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

/**
 * A piece of the altar's art, readable without a GL context.
 *
 * <p>{@code CustomTilemap} only exposes its tiles by building a live
 * {@code Tilemap} against the texture cache, which needs a graphics context.
 * The altar is drawn from five different sheets and is the part of the village
 * most likely to be laid out wrongly, so its tiles have to be checkable — by
 * the tests, and by the map renderer that draws the design doc's picture.
 * Everything on the altar answers those two questions off a plain array.
 */
public interface AltarOverlay {

	/** The tile index per cell of this piece's rect, or -1 where it draws nothing. */
	int[] tileData();

	/** The sheet those indices are read from. */
	String textureName();
}
