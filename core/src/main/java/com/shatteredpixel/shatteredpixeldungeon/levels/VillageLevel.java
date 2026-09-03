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
import com.shatteredpixel.shatteredpixeldungeon.village.AltarDais;
import com.shatteredpixel.shatteredpixeldungeon.village.AltarPool;
import com.shatteredpixel.shatteredpixeldungeon.village.AltarQuadrant;
import com.shatteredpixel.shatteredpixeldungeon.village.AltarThrone;
import com.shatteredpixel.shatteredpixeldungeon.village.AltarThroneShadow;
import com.shatteredpixel.shatteredpixeldungeon.village.EchoAltar;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageDock;
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
		// tileset comes from: that sheet is the lava-toned one, and a three-row
		// strip of it along the south edge reads as magma rather than as the sea
		return Assets.Environment.WATER_PRISON;
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

	/** Sand along the south edge, open sea past it, and a dock out into it. */
	private void buildShore() {
		Painter.fill(this, 1, SIZE - 4, SIZE - 2, 3, Terrain.WATER);
		// bare floor, not the paving used everywhere else: the shore should read
		// as ground the town stops at, rather than as more of its carpet
		Painter.fill(this, 4, SIZE - 6, SIZE - 8, 2, Terrain.EMPTY);
		// the dock stops a row short of the water's edge, so the outermost ring
		// stays open sea and the planking never reads as the map running out
		Painter.fill(this, DOCK_X, SIZE - 6, DOCK_W, 3, Terrain.EMPTY_SP);
		customTiles.add(new VillageDock(DOCK_X, SIZE - 6, DOCK_W, 3));
	}

	/** The gatehouse over the dungeon stair, at the top of the map. */
	private void buildGatehouse() {
		Painter.fill(this, 13, 2, 7, 5, Terrain.WALL);
		Painter.fill(this, 14, 3, 5, 3, Terrain.EMPTY_SP);
		// the way out of the gatehouse, into town
		map[cell(pathX(), 6)] = Terrain.EMPTY_SP;

		// statues flanking the gate
		map[cell(12, 4)] = Terrain.STATUE;
		map[cell(20, 4)] = Terrain.STATUE;
	}

	/** Guild and smithy to the north, elder and shop to the south, tavern on the sand. */
	private void buildBuildings() {
		// guild: the crier shouts titles from its steps
		Painter.fill(this, 5, 6, 7, 5, Terrain.WALL);
		Painter.fill(this, 6, 7, 5, 3, Terrain.EMPTY_SP);
		map[cell(8, 10)] = Terrain.DOOR;
		map[cell(7, 6)] = Terrain.WALL_DECO;

		// smithy, its forge banked up against the north wall
		Painter.fill(this, 21, 6, 7, 5, Terrain.WALL);
		Painter.fill(this, 22, 7, 5, 3, Terrain.EMPTY_SP);
		map[cell(24, 10)] = Terrain.DOOR;
		map[cell(24, 8)] = Terrain.EMBERS;

		// elder's cottage
		Painter.fill(this, 4, 18, 6, 5, Terrain.WALL);
		Painter.fill(this, 5, 19, 4, 3, Terrain.EMPTY_SP);
		map[cell(6, 22)] = Terrain.DOOR;

		// shop
		Painter.fill(this, 23, 18, 6, 5, Terrain.WALL);
		Painter.fill(this, 24, 19, 4, 3, Terrain.EMPTY_SP);
		map[cell(26, 22)] = Terrain.DOOR;
		map[cell(25, 18)] = Terrain.WALL_DECO;

		// tavern, facing the water, with a door onto the sand
		Painter.fill(this, 12, 23, 7, 4, Terrain.WALL);
		Painter.fill(this, 13, 24, 5, 2, Terrain.EMPTY_SP);
		map[cell(16, 23)] = Terrain.DOOR;
		map[cell(15, 26)] = Terrain.DOOR;

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
		// holes left in them, the dais covers the middle, the throne tops it
		for (int i = 0; i < regions.length; i++) {
			customTiles.add(new AltarQuadrant(regions[i]));
		}
		for (int i = 0; i < regions.length; i++) {
			customTiles.add(new AltarPool(regions[i]));
		}
		customTiles.add(new AltarDais());
		customTiles.add(new AltarThrone());
		customWalls.add(new AltarThroneShadow());
	}

	/**
	 * The paving joining the gate to the altar and on to the tavern, the two
	 * tracks trailing off into the woods, the dungeon transition and the arrival
	 * point.
	 */
	private void buildPathAndEntrances() {
		int px = pathX();
		// gate down to the altar, and altar down to the tavern door
		Painter.fill(this, px, 7, 1, 3, Terrain.EMPTY_SP);
		Painter.fill(this, px, 20, 1, 3, Terrain.EMPTY_SP);
		// the west track carries one column into the belt and stops there, the
		// woods closing over it
		Painter.fill(this, 3, TRACK_Y, 7, 1, Terrain.EMPTY_SP);

		// the east road instead runs the whole way out through the trees and off
		// the map, and is barricaded where it leaves town — so the paving the
		// player can see carries on past the last cell they can reach.
		//
		// It runs through a stone cut rather than open woods, because the belt is
		// walkable high grass: a barricade alone would only be walked around.
		Painter.fill(this, 23, TRACK_Y, SIZE - 24, 1, Terrain.EMPTY_SP);
		Painter.fill(this, EAST_BARRICADE_X, TRACK_Y - 1, SIZE - 1 - EAST_BARRICADE_X, 1,
				Terrain.WALL);
		Painter.fill(this, EAST_BARRICADE_X, TRACK_Y + 1, SIZE - 1 - EAST_BARRICADE_X, 1,
				Terrain.WALL);
		map[cell(EAST_BARRICADE_X, TRACK_Y)] = Terrain.BARRICADE;

		// where the hero arrives in the village: on the paving in front of the
		// dungeon mouth, a step short of it. No staircase and no transition —
		// there is nothing above the village, so the town has no way up.
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

	public int cell(int x, int y) {
		return y * width() + x;
	}

	/** Column the paved path runs down. */
	static int pathX() {
		return PATH_X;
	}

	/**
	 * Where the hero stands on arriving in town: one step south of the dungeon
	 * mouth, facing it. The gate is at the top of the map, so the town lies below
	 * the arrival point. The village has no entrance transition to be placed at,
	 * so every arrival names this cell explicitly — and it is never the mouth
	 * itself, which would leave the player standing on a transition.
	 */
	public int arrivalCell() {
		return dungeonEntrance() + width();
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
		// each stands on their own doorstep, where they can actually be seen
		addVillager(Villager.Kind.SAGE, cell(8, 11));
		addVillager(Villager.Kind.SMITH, cell(24, 11));
		addVillager(Villager.Kind.ELDER, cell(6, 23));
		addVillager(Villager.Kind.KEEPER, cell(26, 23));
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
