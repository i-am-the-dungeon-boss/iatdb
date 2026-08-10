/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
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

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.audio.Music;

import java.util.Arrays;

/**
 * The player's house: the interior behind the village's front door.
 *
 * <p>This room is <em>always solo</em>. It is the one place on the ground level
 * that stays single-player, and it is deliberately kept that way so it remains
 * a private space if the village itself ever becomes shared.
 */
public class HouseLevel extends Level {

	private static final int WIDTH = 13;
	private static final int HEIGHT = 11;

	{
		color1 = 0x6a563c;
		color2 = 0x9b7d51;
		viewDistance = WIDTH;
	}

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.play(Assets.Music.CITY_2, true);
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_CITY;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_CITY;
	}

	@Override
	protected boolean build() {
		setSize(WIDTH, HEIGHT);
		Arrays.fill(map, Terrain.WALL);
		Painter.fill(this, 1, 1, WIDTH - 2, HEIGHT - 2, Terrain.EMPTY_SP);

		// hearth and furnishings along the back wall
		map[cell(2, 1)] = Terrain.EMBERS;
		map[cell(4, 1)] = Terrain.BOOKSHELF;
		map[cell(WIDTH - 3, 1)] = Terrain.ALCHEMY;
		map[cell(2, 3)] = Terrain.PEDESTAL;
		map[cell(WIDTH - 3, 3)] = Terrain.STATUE_SP;

		int door = doorCell();
		map[door] = Terrain.DOOR;
		transitions.add(new LevelTransition(this, door, LevelTransition.Type.BRANCH_EXIT,
				VillageLevel.VILLAGE_DEPTH, 0, LevelTransition.Type.BRANCH_ENTRANCE));

		feeling = Feeling.NONE;
		viewDistance = WIDTH;
		return true;
	}

	public int cell(int x, int y) {
		return y * width() + x;
	}

	public int doorCell() {
		return cell(WIDTH / 2, HEIGHT - 1);
	}

	@Override
	public void create() {
		super.create();
		Arrays.fill(visited, true);
		Arrays.fill(mapped, true);
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	protected void createMobs() {
		// always solo — nobody else is ever in here
	}

	@Override
	protected void createItems() {
		// nothing is handed out at home
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	public int randomRespawnCell(Char ch) {
		return doorCell() - width();
	}

	@Override
	public String tileName(int tile) {
		if (tile == Terrain.DOOR) {
			return Messages.get(HouseLevel.class, "door_name");
		}
		return super.tileName(tile);
	}

	@Override
	public String tileDesc(int tile) {
		switch (tile) {
			case Terrain.DOOR:
				return Messages.get(HouseLevel.class, "door_desc");
			case Terrain.EMBERS:
				return Messages.get(HouseLevel.class, "hearth_desc");
			case Terrain.PEDESTAL:
				return Messages.get(HouseLevel.class, "chest_desc");
			default:
				return super.tileDesc(tile);
		}
	}
}
