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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Villager;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.shatteredpixel.shatteredpixeldungeon.village.AltarDais;
import com.shatteredpixel.shatteredpixeldungeon.village.AltarPool;
import com.shatteredpixel.shatteredpixeldungeon.village.AltarQuadrant;
import com.shatteredpixel.shatteredpixeldungeon.village.AltarShore;
import com.shatteredpixel.shatteredpixeldungeon.village.AltarThrone;
import com.shatteredpixel.shatteredpixeldungeon.village.AltarThroneShadow;
import com.shatteredpixel.shatteredpixeldungeon.village.EchoAltar;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageDock;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageSkin;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageShore;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndDungeonMode;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * The ground level: where the player lands before descending.
 *
 * <p>Holds the village and the dungeon entrance. Walking into the dungeon
 * entrance does not descend directly — it opens {@link WndDungeonMode}, which
 * is where a run commits to solo or ranked play. The player's house is built
 * and routed (branch 1) but has no door out here yet.
 *
 * <p>This level is peaceful by construction: no spawns, no respawner, no
 * hunger (see {@code Hunger.act}), and the whole map starts revealed.
 */
public class VillageLevel extends Level {

	public static final int SIZE = 33;

	/**
	 * Village occupies depth 0 on the main branch. Branch 1 is the player's
	 * house, which still builds and still routes — it simply has no door out
	 * here yet, so nothing in the village leads to it.
	 */
	public static final int VILLAGE_DEPTH = 0;
	public static final int HOUSE_BRANCH = 1;

	/** Column the paved path runs down, and the row the dungeon stair sits on. */
	private static final int PATH_X = 16;
	private static final int GATE_Y = 4;

	/** The dock runs south from the sand on these two columns, east of centre. */
	public static final int DOCK_X = 21;
	public static final int DOCK_W = 2;

	/**
	 * The dock starts a row above the beach, at the tavern's south door, so it
	 * is walked onto off the tavern's own porch rather than off open sand — the
	 * whole reason the tavern was sited here.
	 */
	public static final int DOCK_TOP = SIZE - 7;
	public static final int DOCK_H = 4;

	/** The well, on the north-west green. */
	private static final int WELL_X = 7;
	private static final int WELL_Y = 13;

	/** The row the east and west tracks run along, level with the altar. */
	public static final int TRACK_Y = 16;

	/**
	 * Where the east road is closed off. The paving carries on past this cell to
	 * the map's edge but nobody ever walks it: a road that stops exactly where
	 * the player is stopped reads as the world ending, while one that keeps going
	 * past a barricade reads as there being somewhere else to go.
	 */
	public static final int EAST_BARRICADE_X = SIZE - 4;

	{
		color1 = 0x48763c;
		color2 = 0x59994a;
		// slightly further than the dungeon's 8: the village is a safe place and
		// should read as open, without revealing the whole map at once
		viewDistance = 12;
	}

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.play(Assets.Music.CITY_1, true);
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_CITY;
	}

	@Override
	public String waterTex() {
		// deliberately not the city's water, which the rest of the village's
		// tileset comes from: that sheet is the lava-toned one, and a strip of
		// it along the south edge reads as magma rather than as the sea. The
		// caves' water is the one that sits closest to the sand it washes over
		return Assets.Environment.WATER_CAVES;
	}

	@Override
	protected boolean build() {
		setSize(SIZE, SIZE);
		Arrays.fill(map, Terrain.WALL);

		// the open ground, ringed by wall so the level stays closed
		Painter.fill(this, 1, 1, SIZE - 2, SIZE - 2, Terrain.GRASS);

		buildForestBelt();
		buildShore();
		buildGatehouse();
		buildBuildings();
		buildAltar();
		buildPathAndEntrances();
		scatterGrass();
		skinTheVillage();

		feeling = Feeling.NONE;
		return true;
	}

	/**
	 * Trees banked against the north, east and west edges.
	 *
	 * <p>The village has to read as a place in a world rather than a square of
	 * ground with nothing past it, so the map never ends in bare grass: the woods
	 * close it on three sides and the sea on the fourth, and the camera is held
	 * inside that ring so the player never scrolls out to the border.
	 */
	private void buildForestBelt() {
		Painter.fill(this, 1, 1, SIZE - 2, 3, Terrain.HIGH_GRASS);
		Painter.fill(this, 1, 1, 3, SIZE - 2, Terrain.HIGH_GRASS);
		Painter.fill(this, SIZE - 4, 1, 3, SIZE - 2, Terrain.HIGH_GRASS);
	}

	/**
	 * How far the sea bites into the beach, column by column, repeating across
	 * the map. A ruled waterline reads as a wall the map was cut off at; a few
	 * cells of give makes it read as a coast.
	 */
	private static final int[] SHORE_BITE = { 0, 1, 1, 2, 1, 0, 0, 1, 2, 2, 1, 0, 1 };

	/**
	 * The first row of beach. Fixed, so that however far the sea comes in there
	 * is always at least one row of sand above it and at least one row of water
	 * below: a column that is all sand is not a sea, and one that is all water
	 * is a hole in the map rather than a coast.
	 */
	private static final int SHORE_TOP = SIZE - 6;

	/** Sand along the south edge, open sea past it, and a dock out into it. */
	private void buildShore() {
		for (int x = 1; x < SIZE - 1; x++) {
			// the sea comes all the way in under the dock, so the jetty has
			// water to stand out over rather than three rows of beach
			boolean underDock = x >= DOCK_X - 1 && x < DOCK_X + DOCK_W + 1;
			int waterline = SHORE_TOP + 1
					+ (underDock ? 0 : SHORE_BITE[x % SHORE_BITE.length]);
			Painter.fill(this, x, waterline, 1, SIZE - 1 - waterline, Terrain.WATER);
			if (x <= 3 || x >= SIZE - 4) {
				// the belt keeps the outer columns: the beach must not be the
				// last thing on the map edge, or the world reads as cut off
				continue;
			}
			// bare floor, not the paving used everywhere else: the shore should
			// read as ground the town stops at, rather than as more of its carpet
			Painter.fill(this, x, SHORE_TOP, 1, waterline - SHORE_TOP, Terrain.EMPTY);
		}
		// from the sand out over the sea, stopping a row short of the outermost
		// ring so open water still runs past its end and the planking never
		// reads as the map running out
		Painter.fill(this, DOCK_X, DOCK_TOP, DOCK_W, DOCK_H, Terrain.EMPTY_SP);
		customTiles.add(new VillageDock(DOCK_X, DOCK_TOP, DOCK_W, DOCK_H));
	}

	/**
	 * The dungeon mouth, at the top of the map. Deliberately not a building: a
	 * walled gatehouse with its own door puts two thresholds between the town
	 * and the one thing the town exists to point at, and the second of them
	 * carries no meaning.
	 */
	private void buildGatehouse() {
		Painter.fill(this, pathX() - 1, GATE_Y - 1, 3, 3, Terrain.EMPTY_SP);
		// gateposts: either side of the path where it leaves the mouth, so they
		// are passed between on the way in rather than looked at from a forecourt
		map[cell(pathX() - 1, GATE_Y + 2)] = Terrain.STATUE;
		map[cell(pathX() + 1, GATE_Y + 2)] = Terrain.STATUE;
	}

	/** Guild and smithy to the north, elder and shop to the south, tavern on the sand. */
	private void buildBuildings() {
		// guild: the crier shouts titles from its steps
		Painter.fill(this, 5, 6, 7, 5, Terrain.WALL);
		Painter.fill(this, 6, 7, 5, 3, Terrain.EMPTY_SP);
		map[cell(8, 10)] = Terrain.DOOR;
		map[cell(7, 6)] = Terrain.WALL_DECO;

		// smithy, the forge on the smith's east
		Painter.fill(this, 21, 6, 7, 5, Terrain.WALL);
		Painter.fill(this, 22, 7, 5, 3, Terrain.EMPTY_SP);
		map[cell(24, 10)] = Terrain.DOOR;
		map[cell(25, 8)] = Terrain.EMBERS;

		// elder's cottage
		Painter.fill(this, 4, 18, 6, 5, Terrain.WALL);
		Painter.fill(this, 5, 19, 4, 3, Terrain.EMPTY_SP);
		map[cell(6, 22)] = Terrain.DOOR;

		// shop
		Painter.fill(this, 23, 18, 6, 5, Terrain.WALL);
		Painter.fill(this, 24, 19, 4, 3, Terrain.EMPTY_SP);
		// the shop faces the road, not the beach: its door is on the north wall
		map[cell(26, 18)] = Terrain.DOOR;
		map[cell(25, 22)] = Terrain.WALL_DECO;

		// tavern, facing the water and pulled east so the dock runs off its
		// porch rather than off open beach
		// clear of column 16, which the road down through town runs along
		Painter.fill(this, 17, 22, 8, 4, Terrain.WALL);
		Painter.fill(this, 18, 23, 6, 2, Terrain.EMPTY_SP);
		map[cell(20, 22)] = Terrain.DOOR;
		map[cell(21, 25)] = Terrain.DOOR;

		// the well, moved off the middle of town to make room for the altar
		map[cell(WELL_X, WELL_Y)] = Terrain.WELL;
	}

	/**
	 * The echo altar: one paved disc, quartered by its walkway, with the throne
	 * seated at the centre.
	 *
	 * <p>Terrain only. What makes each quarter read as its own region is a set of
	 * {@code CustomTilemap}s laid over this paving, so the ground here is the
	 * same walkable paving throughout and the shape lives in {@link EchoAltar}.
	 */
	private void buildAltar() {
		for (int y = 1; y < SIZE - 1; y++) {
			for (int x = 1; x < SIZE - 1; x++) {
				if (EchoAltar.inDisc(x, y)) {
					map[cell(x, y)] = Terrain.EMPTY_SP;
				}
			}
		}
		// a basin in each quarter, holding that region's own water. Real water
		// terrain, so it behaves like water; the region's colour comes from the
		// AltarPool laid over it, since a level only has one water texture
		int[] regions = { EchoAltar.SEWERS, EchoAltar.PRISON, EchoAltar.CAVES, EchoAltar.CITY };
		for (int y = 1; y < SIZE - 1; y++) {
			for (int x = 1; x < SIZE - 1; x++) {
				if (EchoAltar.inBasin(x, y)) {
					map[cell(x, y)] = Terrain.WATER;
				}
			}
		}

		// order is draw order: the quarters floor the disc, the basins fill the
		// holes left in them, each basin is then edged with its own region's
		// shoreline, the dais covers the middle and the throne tops it
		for (int i = 0; i < regions.length; i++) {
			customTiles.add(new AltarQuadrant(regions[i]));
		}
		for (int i = 0; i < regions.length; i++) {
			customTiles.add(new AltarPool(regions[i]));
		}
		for (int i = 0; i < regions.length; i++) {
			customTiles.add(new AltarShore(regions[i]));
		}
		customTiles.add(new AltarDais());
		customTiles.add(new AltarThrone());
		customTiles.add(new AltarThroneShadow());
	}

	/**
	 * The paving joining the gate to the altar and on to the tavern, the two
	 * tracks trailing off into the woods, the dungeon transition and the arrival
	 * point.
	 */
	private void buildPathAndEntrances() {
		int px = pathX();
		// the spine: the mouth down to the altar, and the altar down to the sand
		pave(px, GATE_Y + 2, px, 11);
		pave(px, 21, px, 26);

		// two avenues, one to each side of the altar, so every door in town is
		// stepped onto off paving rather than off open grass
		pave(8, 11, 24, 11);
		pave(6, 26, 21, 26);

		buildWellCourt();

		// and the short runs that take each avenue up to a door
		pave(6, 23, 6, 26);
		pave(26, 16, 26, 17);
		pave(px, 21, 20, 21);

		// the east road runs the whole way out through the trees and off the
		// map, and is barricaded where it leaves town — so the paving the
		// player can see carries on past the last cell they can reach.
		//
		// It runs through a stone cut rather than open woods, because the belt is
		// walkable high grass: a barricade alone would only be walked around.
		Painter.fill(this, 21, TRACK_Y, SIZE - 22, 1, Terrain.EMPTY_SP);
		Painter.fill(this, EAST_BARRICADE_X, TRACK_Y - 1, SIZE - 1 - EAST_BARRICADE_X, 1,
				Terrain.WALL);
		Painter.fill(this, EAST_BARRICADE_X, TRACK_Y + 1, SIZE - 1 - EAST_BARRICADE_X, 1,
				Terrain.WALL);
		map[cell(EAST_BARRICADE_X, TRACK_Y)] = Terrain.BARRICADE;

		// where the hero arrives in the village: on the paving between the two
		// gate statues, two steps short of the mouth. No staircase and no
		// transition — there is nothing above the village, so the town has no
		// way up.
		map[arrivalCell()] = Terrain.EMPTY_SP;

		// the way into the dungeon — descends to depth 1, but only after the
		// solo/ranked prompt is answered (see activateTransition)
		int mouth = dungeonEntrance();
		map[mouth] = Terrain.EXIT;
		transitions.add(new LevelTransition(this, mouth, LevelTransition.Type.REGULAR_EXIT,
				1, 0, LevelTransition.Type.REGULAR_ENTRANCE));

		// the house is not wired up yet: HouseLevel still builds and branch 1
		// still routes to it, but nothing in the village opens onto it
	}

	/** Tall grass over the open green, wherever the town has not already built. */
	/**
	 * Re-skins everything outside the altar from other regions' sheets.
	 *
	 * <p>Runs last, once every cell has settled, because each skin is driven by
	 * the terrain that ended up there rather than by the rects that laid it. The
	 * altar is left alone: its own tilemaps already own the disc, and the walkway
	 * quartering it is deliberately bare so the city's carpet shows through.
	 *
	 * <p>None of these overlap, so their order among themselves does not matter.
	 * The dock is the one cell-level exception and is cut out of the paving by
	 * hand, since it is planked from the sewers sheet and was added earlier.
	 */
	private void skinTheVillage() {
		int left = 1;
		int top = 1;
		int width = SIZE - 2;
		int height = SIZE - 2;

		boolean[] paving = new boolean[width * height];
		boolean[] green = new boolean[width * height];
		boolean[] woods = new boolean[width * height];
		boolean[] overhang = new boolean[width * height];
		boolean[] sand = new boolean[width * height];
		boolean[] sea = new boolean[width * height];

		for (int i = 0; i < paving.length; i++) {
			int x = left + (i % width);
			int y = top + (i / width);
			int tile = map[cell(x, y)];

			// the whole disc belongs to the altar: its quarters to their own
			// tilemaps, and its four walkways to the city's worked floor,
			// which the terrain layer already draws from tilesTex(). The
			// monument is the city's, so its stonework is too - the halls
			// paving stops where the town's roads reach it
			paving[i] = tile == Terrain.EMPTY_SP
					&& !EchoAltar.inDisc(x, y)
					&& !onDock(x, y);
			// the ground goes under the woods as well as under the open green:
			// tall grass is drawn with gaps between its blades, so without an
			// opaque tile beneath it the city's own grass shows through and the
			// belt reads as a blend of two regions rather than as one
			green[i] = tile == Terrain.GRASS || tile == Terrain.HIGH_GRASS;
			woods[i] = tile == Terrain.HIGH_GRASS;
			// the overhang is drawn on the cell above the grass, by the wall
			// layer - see DungeonWallsTilemap.getTileVisual
			overhang[i] = y + 1 < SIZE && map[cell(x, y + 1)] == Terrain.HIGH_GRASS;
			// the shore is the only bare floor the village lays - see
			// buildShore, which uses it precisely so the sand is separable
			// from the paving everywhere else
			sand[i] = tile == Terrain.EMPTY;
			// the altar's basins are inland water and keep their own shores,
			// each edged in its quarter's own stone
			sea[i] = tile == Terrain.WATER && !EchoAltar.inDisc(x, y);
		}

		customTiles.add(new VillageSkin(Assets.Environment.TILES_CAVES,
				DungeonTileSheet.GRASS, DungeonTileSheet.GRASS_ALT,
				left, top, width, height, green));
		customTiles.add(new VillageSkin(Assets.Environment.TILES_CAVES,
				DungeonTileSheet.FLOOR_ALT_2, DungeonTileSheet.FLOOR_ALT_2,
				left, top, width, height, sand));
		customTiles.add(new VillageSkin(Assets.Environment.TILES_SEWERS,
				DungeonTileSheet.RAISED_HIGH_GRASS, DungeonTileSheet.RAISED_HIGH_GRASS_ALT,
				left, top, width, height, woods));
		customTiles.add(new VillageSkin(Assets.Environment.TILES_HALLS,
				DungeonTileSheet.FLOOR_SP, DungeonTileSheet.FLOOR_SP,
				left, top, width, height, paving));
		customWalls.add(new VillageSkin(Assets.Environment.TILES_SEWERS,
				DungeonTileSheet.HIGH_GRASS_OVERHANG, DungeonTileSheet.HIGH_GRASS_OVERHANG_ALT,
				left, top, width, height, overhang));
		// last of the ground layer: the lip has to sit over the city kerb the
		// terrain layer already stitched, not under it
		customTiles.add(new VillageShore(left, top, width, height, sea));

		// and now the terrain under the woods is dropped to plain grass.
		//
		// The tall grass the terrain layer draws comes from tilesTex(), which is
		// the city's white-flowered blades, and GameScene adds raisedTerrain
		// AFTER customTiles — so no skin can ever cover it. The woods are drawn
		// entirely by the two skins above instead, and the terrain beneath them
		// has to stop drawing blades of its own or the two show through each
		// other. It costs nothing here: village grass neither blocks sight
		// (buildFlagMaps) nor can be trampled (grassCanBeTrampled).
		for (int i = 0; i < woods.length; i++) {
			if (woods[i]) {
				map[cell(left + (i % width), top + (i / width))] = Terrain.GRASS;
			}
		}
	}

	private boolean onDock(int x, int y) {
		return x >= DOCK_X && x < DOCK_X + DOCK_W
				&& y >= DOCK_TOP && y < DOCK_TOP + DOCK_H;
	}

	private void scatterGrass() {
		for (int i = 0; i < 40; i++) {
			int x = Random.IntRange(4, SIZE - 5);
			int y = Random.IntRange(6, SIZE - 7);
			int cell = cell(x, y);
			// plain grass is the only thing left open; paving, walls, sand and
			// the altar have all claimed their cells by now
			if (map[cell] == Terrain.GRASS) {
				map[cell] = Terrain.HIGH_GRASS;
			}
		}
	}

	/**
	 * How far a path is allowed to wander off its own line, indexed by how far
	 * along it has got. Derived from the path rather than drawn at random so the
	 * town lays out the same way every time and the tests can assert against it.
	 */
	private static final int[] WANDER = { 0, 0, 1, 0, -1, 0, 0, 1, 0, -1 };

	/**
	 * Paves a wandering path between two cells.
	 *
	 * <p>Straight rules of paving read as surveying. A village path is a line
	 * worn by people walking roughly the same way twice, so it drifts a cell
	 * either side of true — but never at its two ends, which have to meet the
	 * door or the junction they were aimed at.
	 *
	 * <p>Anything already built wins: a drift that would land on a wall, a door
	 * or the water is dropped back onto the straight line rather than paved
	 * over, so a path can never open a hole in somebody's house.
	 */
	private void pave(int fromX, int fromY, int toX, int toY) {
		int span = Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY));
		int px = fromX;
		int py = fromY;
		paveCell(px, py);
		for (int step = 1; step <= span; step++) {
			int x = fromX + (toX - fromX) * step / span;
			int y = fromY + (toY - fromY) * step / span;
			if (step > 1 && step < span) {
				// held for three steps at a time. A drift that changed every
				// step would zig-zag, and the bridging between one zig and the
				// next would fill the whole band in as a slab of paving
				int drift = WANDER[(step / 3 + fromX + fromY) % WANDER.length];
				if (Math.abs(toX - fromX) >= Math.abs(toY - fromY)) {
					y = clear(x, y + drift) ? y + drift : y;
				} else {
					x = clear(x + drift, y) ? x + drift : x;
				}
			}
			// bridge, so a drift never leaves the path joined only corner to corner
			while (px != x) {
				px += Integer.signum(x - px);
				paveCell(px, py);
			}
			while (py != y) {
				py += Integer.signum(y - py);
				paveCell(px, py);
			}
		}
	}

	/**
	 * The court around the well, where the honorable mentions gather.
	 *
	 * <p>They stood on open grass before, which read as a crowd loitering in a
	 * field rather than as a gathering with somewhere to gather. The court is
	 * the well's five-by-five square, which is exactly the reach of the ring
	 * {@link VillageFigurePlacement} arranges them in, so every mention has
	 * paving underfoot without the court being sized by hand to match.
	 *
	 * <p>Its two upper corners are dropped to take the square edge off, and the
	 * north side is run up to the avenue on row 11 so the court is reached by
	 * road like every other place in town.
	 */
	private void buildWellCourt() {
		for (int y = WELL_Y - 2; y <= WELL_Y + 2; y++) {
			for (int x = WELL_X - 2; x <= WELL_X + 2; x++) {
				boolean corner = y == WELL_Y - 2 && Math.abs(x - WELL_X) == 2;
				if (!corner) {
					// paveCell leaves the well itself alone
					paveCell(x, y);
				}
			}
		}
		pave(WELL_X, WELL_Y - 2, WELL_X, 11);
	}

	private void paveCell(int x, int y) {
		if (clear(x, y)) {
			map[cell(x, y)] = Terrain.EMPTY_SP;
		}
	}

	/** Whether a path may take this cell, or something already built owns it. */
	private boolean clear(int x, int y) {
		if (x < 1 || y < 1 || x >= SIZE - 1 || y >= SIZE - 1) {
			return false;
		}
		int tile = map[cell(x, y)];
		return tile != Terrain.WALL && tile != Terrain.DOOR && tile != Terrain.WATER
				&& tile != Terrain.STATUE && tile != Terrain.WELL;
	}

	public int cell(int x, int y) {
		return y * width() + x;
	}

	/** Column the paved path runs down. */
	static int pathX() {
		return PATH_X;
	}

	/**
	 * Where the hero stands on arriving in town: between the two gate statues,
	 * two steps south of the dungeon mouth. The gate is at the top of the map,
	 * so the town lies below the arrival point. The village has no entrance
	 * transition to be placed at, so every arrival names this cell explicitly —
	 * and it is never the mouth itself, which would leave the player standing on
	 * a transition.
	 */
	public int arrivalCell() {
		return dungeonEntrance() + 2 * width();
	}

	public int dungeonEntrance() {
		return cell(pathX(), GATE_Y);
	}

	@Override
	public void create() {
		super.create();
		Arrays.fill(visited, true);
		Arrays.fill(mapped, true);
	}

	/**
	 * Village grass is the ordinary tall grass, kept as scenery: walking through
	 * it neither flattens it nor shakes seeds out of it, the way the huntress
	 * leaves grass standing.
	 */
	@Override
	public boolean grassCanBeTrampled() {
		return false;
	}

	/**
	 * The village reads as open ground, so its grass is see-through. It is the
	 * same terrain the dungeon uses — only the sight line is given back, since
	 * there is nothing in town worth hiding behind a hedge.
	 */
	@Override
	public void buildFlagMaps() {
		super.buildFlagMaps();
		for (int i = 0; i < length(); i++) {
			if (isGrass(map[i])) {
				losBlocking[i] = false;
			}
		}
	}

	@Override
	public void updateCellFlags(int cell) {
		super.updateCellFlags(cell);
		if (isGrass(map[cell])) {
			losBlocking[cell] = false;
		}
	}

	private static boolean isGrass(int terrain) {
		return terrain == Terrain.HIGH_GRASS || terrain == Terrain.FURROWED_GRASS;
	}

	/** The dungeon mouth asks for a play mode instead of descending. */
	@Override
	public boolean activateTransition(Hero hero, LevelTransition transition) {
		if (transition.type == LevelTransition.Type.REGULAR_EXIT) {
			Game.runOnRenderThread(() -> GameScene.show(new WndDungeonMode()));
			return false;
		}
		return super.activateTransition(hero, transition);
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	protected void createMobs() {
		// flavour only: the stall keeper carries no stock. With no gold and
		// nothing that survives the descent, a real trade window is a dead end.
		// each keeps their own house. The elder is the exception: a ghost has
		// no house to keep, and standing out in the open is the whole of what
		// makes them read as one
		addVillager(Villager.Kind.SAGE, cell(8, 8));
		addVillager(Villager.Kind.SMITH, cell(24, 8));
		addVillager(Villager.Kind.KEEPER, cell(26, 20));
		addVillager(Villager.Kind.ELDER, cell(6, 24));
	}

	private void addVillager(Villager.Kind kind, int pos) {
		Villager villager = new Villager();
		villager.setKind(kind);
		villager.pos = pos;
		mobs.add(villager);
	}

	@Override
	protected void createItems() {
		// the village hands out nothing; supplies come from the shopkeeper
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	public int randomRespawnCell(Char ch) {
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8) {
			int cell = entrance() + i;
			if (passable[cell] && Actor.findChar(cell) == null) {
				candidates.add(cell);
			}
		}
		if (candidates.isEmpty()) {
			return entrance();
		}
		return Random.element(candidates);
	}

	@Override
	public String tileName(int tile) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(VillageLevel.class, "water_name");
			case Terrain.EXIT:
				return Messages.get(VillageLevel.class, "exit_name");
			case Terrain.DOOR:
				return Messages.get(VillageLevel.class, "door_name");
			default:
				return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc(int tile) {
		switch (tile) {
			case Terrain.EXIT:
				return Messages.get(VillageLevel.class, "exit_desc");
			case Terrain.EMBERS:
				return Messages.get(VillageLevel.class, "embers_desc");
			case Terrain.STATUE:
				return Messages.get(VillageLevel.class, "statue_desc");
			default:
				return super.tileDesc(tile);
		}
	}

	/** True when the given depth/branch pair addresses the village itself. */
	public static boolean isVillage(int depth, int branch) {
		return depth == VILLAGE_DEPTH && branch == 0;
	}

	/** True when the depth addresses anywhere on the ground level. */
	public static boolean isGroundLevel(int depth) {
		return depth == VILLAGE_DEPTH;
	}
}
