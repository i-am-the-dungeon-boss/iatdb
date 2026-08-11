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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A village map: plain tile data with no dungeon, level, hero or save-slot
 * involvement. Hand-authored and deterministic, so every build produces the
 * same map and nothing about it needs storing.
 */
public final class VillageMap {

	/** Which map this is. */
	public enum Area {
		VILLAGE,
		HOUSE
	}

	public static final int VILLAGE_SIZE = 33;
	public static final int HOUSE_WIDTH = 13;
	public static final int HOUSE_HEIGHT = 12;

	private static final int HOUSE_LEFT = 4;
	private static final int HOUSE_TOP = 5;
	private static final int HOUSE_W = 9;
	private static final int HOUSE_H = 6;

	public final Area area;
	public final int width;
	public final int height;
	public final int[] tiles;

	/** Where the avatar stands when arriving in this area. */
	private int arrival;
	/** The door leading to the other area, or -1 if there is none. */
	private int door = -1;
	/** The stair into the dungeon, or -1 if there is none. */
	private int dungeonEntrance = -1;

	private VillageMap(Area area, int width, int height) {
		this.area = area;
		this.width = width;
		this.height = height;
		this.tiles = new int[width * height];
	}

	public static VillageMap create(Area area) {
		return area == Area.HOUSE ? buildHouse() : buildVillage();
	}

	public int length() {
		return tiles.length;
	}

	public int cell(int x, int y) {
		return y * width + x;
	}

	public int x(int cell) {
		return cell % width;
	}

	public int y(int cell) {
		return cell / width;
	}

	public boolean inside(int cell) {
		return cell >= 0 && cell < tiles.length;
	}

	public boolean passable(int cell) {
		return inside(cell) && VillageTerrain.passable(tiles[cell]);
	}

	public int arrival() {
		return arrival;
	}

	public int door() {
		return door;
	}

	public int dungeonEntrance() {
		return dungeonEntrance;
	}

	/** Tile visuals for the whole map, in tilemap order. */
	public int[] visuals() {
		int[] visuals = new int[tiles.length];
		for (int i = 0; i < tiles.length; i++) {
			visuals[i] = VillageTerrain.visual(tiles[i]);
		}
		return visuals;
	}

	// ---------------------------------------------------------------- layouts

	private static VillageMap buildVillage() {
		VillageMap map = new VillageMap(Area.VILLAGE, VILLAGE_SIZE, VILLAGE_SIZE);
		Arrays.fill(map.tiles, VillageTerrain.WALL);

		// open ground, leaving the outermost ring as wall so the map is closed
		map.fill(1, 1, VILLAGE_SIZE - 2, VILLAGE_SIZE - 2, VillageTerrain.GROUND);

		// shore along the top, with a paved bank below it
		map.fill(1, 1, VILLAGE_SIZE - 2, 3, VillageTerrain.WATER);
		map.fill(1, 4, VILLAGE_SIZE - 2, 1, VillageTerrain.PATH);

		// the player's house, with its door in the south wall
		map.fill(HOUSE_LEFT, HOUSE_TOP, HOUSE_W, HOUSE_H, VillageTerrain.WALL);
		map.fill(HOUSE_LEFT + 1, HOUSE_TOP + 1, HOUSE_W - 2, HOUSE_H - 2, VillageTerrain.PATH);
		int pathX = HOUSE_LEFT + HOUSE_W / 2;
		map.door = map.cell(pathX, HOUSE_TOP + HOUSE_H - 1);
		map.tiles[map.door] = VillageTerrain.DOOR;

		// market stall
		map.fill(20, 6, 8, 5, VillageTerrain.WALL);
		map.fill(21, 7, 6, 3, VillageTerrain.PATH);
		map.tiles[map.cell(24, 10)] = VillageTerrain.DOOR;
		map.tiles[map.cell(21, 8)] = VillageTerrain.WALL_DECO;

		// village green
		map.tiles[map.cell(16, 14)] = VillageTerrain.WELL;
		map.tiles[map.cell(11, 18)] = VillageTerrain.FIREPIT;
		map.tiles[map.cell(15, 27)] = VillageTerrain.STATUE;
		map.tiles[map.cell(19, 27)] = VillageTerrain.STATUE;

		// paved path from the front door down to the dungeon
		map.fill(pathX, HOUSE_TOP + HOUSE_H, 1, VILLAGE_SIZE - HOUSE_TOP - HOUSE_H - 3,
				VillageTerrain.PATH);
		map.fill(pathX - 1, 24, 3, 4, VillageTerrain.PATH);

		// tall grass, kept off the path
		for (int y = 12; y < VILLAGE_SIZE - 2; y += 3) {
			for (int x = 2; x < VILLAGE_SIZE - 2; x += 4) {
				int cell = map.cell(x, y);
				if (map.tiles[cell] == VillageTerrain.GROUND && Math.abs(x - pathX) > 2) {
					map.tiles[cell] = VillageTerrain.TALL_GRASS;
				}
			}
		}

		map.dungeonEntrance = map.cell(pathX, VILLAGE_SIZE - 4);
		map.tiles[map.dungeonEntrance] = VillageTerrain.DUNGEON_ENTRANCE;

		// arrive just outside the front door
		map.arrival = map.door + map.width;
		map.tiles[map.arrival] = VillageTerrain.PATH;
		return map;
	}

	private static VillageMap buildHouse() {
		VillageMap map = new VillageMap(Area.HOUSE, HOUSE_WIDTH, HOUSE_HEIGHT);
		Arrays.fill(map.tiles, VillageTerrain.WALL);

		// the door row must not be the outermost row, or the map is not closed
		int doorRow = HOUSE_HEIGHT - 2;
		map.fill(1, 1, HOUSE_WIDTH - 2, doorRow - 1, VillageTerrain.PATH);

		map.tiles[map.cell(2, 1)] = VillageTerrain.FIREPIT;
		map.tiles[map.cell(4, 1)] = VillageTerrain.BOOKSHELF;
		map.tiles[map.cell(HOUSE_WIDTH - 3, 1)] = VillageTerrain.ALCHEMY;
		map.tiles[map.cell(2, 3)] = VillageTerrain.CHEST;

		map.door = map.cell(HOUSE_WIDTH / 2, doorRow);
		map.tiles[map.door] = VillageTerrain.DOOR;
		map.arrival = map.door - map.width;
		return map;
	}

	private void fill(int left, int top, int w, int h, int terrain) {
		for (int y = top; y < top + h; y++) {
			for (int x = left; x < left + w; x++) {
				tiles[cell(x, y)] = terrain;
			}
		}
	}

	// ---------------------------------------------------------------- pathing

	/**
	 * Shortest step-by-step route between two cells, or an empty list when there
	 * is none. Plain breadth-first search over passable cells — the village has
	 * no flying, no doors that lock, and nothing that changes passability.
	 */
	public List<Integer> route(int from, int to) {
		List<Integer> path = new ArrayList<>();
		if (!passable(from) || !passable(to)) {
			return path;
		}
		int[] cameFrom = new int[tiles.length];
		Arrays.fill(cameFrom, -1);
		cameFrom[from] = from;

		ArrayDeque<Integer> queue = new ArrayDeque<>();
		queue.add(from);
		boolean found = from == to;
		while (!queue.isEmpty() && !found) {
			int cell = queue.poll();
			for (int next : neighbours(cell)) {
				if (cameFrom[next] != -1 || !passable(next)) {
					continue;
				}
				cameFrom[next] = cell;
				if (next == to) {
					found = true;
					break;
				}
				queue.add(next);
			}
		}
		if (!found) {
			return path;
		}
		for (int cell = to; cell != from; cell = cameFrom[cell]) {
			path.add(0, cell);
		}
		return path;
	}

	/** Orthogonal neighbours that stay on the map, without wrapping rows. */
	public int[] neighbours(int cell) {
		int x = x(cell);
		int y = y(cell);
		int count = 0;
		int[] found = new int[4];
		if (x > 0) {
			found[count++] = cell - 1;
		}
		if (x < width - 1) {
			found[count++] = cell + 1;
		}
		if (y > 0) {
			found[count++] = cell - width;
		}
		if (y < height - 1) {
			found[count++] = cell + width;
		}
		return Arrays.copyOf(found, count);
	}
}
