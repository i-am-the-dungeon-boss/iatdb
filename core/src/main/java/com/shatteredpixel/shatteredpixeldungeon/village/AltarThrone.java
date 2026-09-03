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
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Tilemap;

/**
 * The throne at the middle of the altar, borrowed from the dwarf king's arena.
 *
 * <p>{@code CityBossLevel} draws the same chair, but it finds it by sniffing at
 * neighbouring terrain — correct only inside an arena laid out exactly its way,
 * and it assumes a data index that equals the level cell index. Neither holds
 * here, so the nine tiles are written out instead.
 *
 * <p>The seat is left walkable on purpose: the deepest echo's post is the chair
 * itself, so the reigning Halls champion is found sitting in it.
 */
public class AltarThrone extends CustomTilemap implements AltarOverlay {

	/**
	 * custom_tiles/city_boss.png is <b>8</b> columns wide, not the 16 of the
	 * region sheets. Rows 13-15, columns 1-3.
	 */
	private static final int[] THRONE = {
			13 * 8 + 1, 13 * 8 + 2, 13 * 8 + 3,
			14 * 8 + 1, 14 * 8 + 2, 14 * 8 + 3,
			15 * 8 + 1, 15 * 8 + 2, 15 * 8 + 3
	};

	{
		texture = Assets.Environment.CITY_BOSS;
	}

	public AltarThrone() {
		// the chair is three wide and three tall with the seat at its middle
		setRect(EchoAltar.THRONE_SEAT_X - 1, EchoAltar.THRONE_SEAT_Y - 1, 3, 3);
	}

	@Override
	public String textureName() {
		return Assets.Environment.CITY_BOSS;
	}

	/** Pure — no GL. See {@link AltarQuadrant#tileData()}. */
	@Override
	public int[] tileData() {
		return THRONE.clone();
	}

	@Override
	public Tilemap create() {
		Tilemap v = super.create();
		v.map(tileData(), tileW);
		return v;
	}

	@Override
	public String name(int tileX, int tileY) {
		if (isSeat(tileX, tileY)) {
			return Messages.get(this, "name");
		}
		return super.name(tileX, tileY);
	}

	@Override
	public String desc(int tileX, int tileY) {
		if (isSeat(tileX, tileY)) {
			return Messages.get(this, "desc");
		}
		return super.desc(tileX, tileY);
	}

	private boolean isSeat(int withinX, int withinY) {
		return EchoAltar.isThroneSeat(this.tileX + withinX, this.tileY + withinY);
	}
}
