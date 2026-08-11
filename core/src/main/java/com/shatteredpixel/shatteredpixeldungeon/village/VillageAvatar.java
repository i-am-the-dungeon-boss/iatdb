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

package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;

import java.util.ArrayList;
import java.util.List;

/**
 * The player's figure in the village.
 *
 * <p>Not a {@code Hero} and not a {@code Char}: it has no HP, no inventory, no
 * turn scheduling and no combat. It is a walking portrait that remembers which
 * cell it stands on. Everything the player earns lives in a run, and runs start
 * at the dungeon entrance.
 */
public class VillageAvatar extends MovieClip {

	private static final int FRAME_WIDTH = 12;
	private static final int FRAME_HEIGHT = 15;
	private static final int RUN_FRAMERATE = 20;

	/** Cells per second while walking. */
	private static final float SPEED = 5f;

	private final VillageMap map;
	private final List<Integer> route = new ArrayList<>();

	private Animation idle;
	private Animation run;

	private int cell;
	private float stepProgress;
	private int stepFrom;

	public VillageAvatar(VillageMap map, HeroClass look, int startCell) {
		super();
		this.map = map;
		this.cell = startCell;
		this.stepFrom = startCell;

		texture(look.spritesheet());
		TextureFilm film = new TextureFilm(texture, FRAME_WIDTH, FRAME_HEIGHT);

		idle = new Animation(1, true);
		idle.frames(film, 0, 0, 0, 1, 0, 0, 1, 1);

		run = new Animation(RUN_FRAMERATE, true);
		run.frames(film, 2, 3, 4, 5, 6, 7);

		play(idle);
		snapToCell();
	}

	public int cell() {
		return cell;
	}

	public boolean walking() {
		return !route.isEmpty();
	}

	/** Sends the avatar walking to a cell; does nothing if there is no route. */
	public void walkTo(int destination) {
		List<Integer> found = map.route(cell, destination);
		route.clear();
		if (found.isEmpty()) {
			return;
		}
		route.addAll(found);
		stepFrom = cell;
		stepProgress = 0;
		play(run);
	}

	public void stop() {
		route.clear();
		stepProgress = 0;
		stepFrom = cell;
		snapToCell();
		play(idle);
	}

	@Override
	public void update() {
		super.update();
		if (route.isEmpty()) {
			return;
		}

		stepProgress += com.watabou.noosa.Game.elapsed * SPEED;
		while (stepProgress >= 1f && !route.isEmpty()) {
			stepProgress -= 1f;
			stepFrom = cell = route.remove(0);
		}

		if (route.isEmpty()) {
			stop();
			return;
		}

		int next = route.get(0);
		if (map.x(next) != map.x(stepFrom)) {
			flipHorizontal = map.x(next) < map.x(stepFrom);
		}
		interpolateTowards(next);
	}

	/** Places the sprite between its current cell and the next, mid-step. */
	private void interpolateTowards(int next) {
		float fromX = VillageTilemap.cellX(map, stepFrom);
		float fromY = VillageTilemap.cellY(map, stepFrom);
		float toX = VillageTilemap.cellX(map, next);
		float toY = VillageTilemap.cellY(map, next);
		x = centreX(fromX + (toX - fromX) * stepProgress);
		y = footY(fromY + (toY - fromY) * stepProgress);
	}

	private void snapToCell() {
		x = centreX(VillageTilemap.cellX(map, cell));
		y = footY(VillageTilemap.cellY(map, cell));
	}

	private float centreX(float tileX) {
		return tileX + (VillageTilemap.SIZE - FRAME_WIDTH) / 2f;
	}

	private float footY(float tileY) {
		return tileY + VillageTilemap.SIZE - FRAME_HEIGHT;
	}
}
