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

	/** Column the paved path runs down, and the row it starts on. */
	private static final int PATH_X = 8;
	private static final int PATH_TOP = 6;

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
		return Assets.Environment.WATER_CITY;
	}

	@Override
	protected boolean build() {
		setSize(SIZE, SIZE);
		Arrays.fill(map, Terrain.WALL);

		// the open ground, ringed by wall so the level stays closed
		Painter.fill(this, 1, 1, SIZE - 2, SIZE - 2, Terrain.GRASS);

		buildShore();
		buildVillage();
		buildPathAndEntrances();

		feeling = Feeling.NONE;
		return true;
	}

	/** Water along the top edge, with a walkable bank below it. */
	private void buildShore() {
		Painter.fill(this, 1, 1, SIZE - 2, 3, Terrain.WATER);
		Painter.fill(this, 1, 4, SIZE - 2, 1, Terrain.EMPTY_SP);
	}

	/** Shop, smithy, well and greenery — dressing plus the shopkeeper's stall. */
	private void buildVillage() {
		// market stall, east side
		Painter.fill(this, 20, 6, 8, 5, Terrain.WALL);
		Painter.fill(this, 21, 7, 6, 3, Terrain.EMPTY_SP);
		map[cell(24, 10)] = Terrain.DOOR;
		map[cell(21, 8)] = Terrain.WALL_DECO;

		// well on the green
		map[cell(16, 14)] = Terrain.WELL;

		// a firepit the villagers gather round
		map[cell(11, 18)] = Terrain.EMBERS;
		map[cell(10, 18)] = Terrain.EMPTY_SP;
		map[cell(12, 18)] = Terrain.EMPTY_SP;

		// statue flanking the dungeon mouth
		map[cell(15, 27)] = Terrain.STATUE;
		map[cell(19, 27)] = Terrain.STATUE;

		// tall grass patches, kept clear of the path
		for (int i = 0; i < 40; i++) {
			int x = Random.IntRange(2, SIZE - 3);
			int y = Random.IntRange(12, SIZE - 3);
			int cell = cell(x, y);
			if (map[cell] == Terrain.GRASS && Math.abs(x - pathX()) > 2) {
				map[cell] = Terrain.HIGH_GRASS;
			}
		}
	}

	/**
	 * The paved path down to the dungeon mouth, the dungeon transition itself,
	 * and the arrival point.
	 */
	private void buildPathAndEntrances() {
		int px = pathX();
		Painter.fill(this, px, PATH_TOP, 1, SIZE - PATH_TOP - 3, Terrain.EMPTY_SP);
		Painter.fill(this, px - 1, 24, 3, 4, Terrain.EMPTY_SP);

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

	public int cell(int x, int y) {
		return y * width() + x;
	}

	/** Column the paved path runs down. */
	static int pathX() {
		return PATH_X;
	}

	/**
	 * Where the hero stands on arriving in town: one step north of the dungeon
	 * mouth, facing it. The village has no entrance transition to be placed at,
	 * so every arrival names this cell explicitly — and it is never the mouth
	 * itself, which would leave the player standing on a transition.
	 */
	public int arrivalCell() {
		return dungeonEntrance() - width();
	}

	public int dungeonEntrance() {
		return cell(pathX(), SIZE - 4);
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
		addVillager(Villager.Kind.KEEPER, cell(24, 9));
		addVillager(Villager.Kind.SMITH, cell(11, 19));
		addVillager(Villager.Kind.SAGE, cell(17, 15));
		addVillager(Villager.Kind.ELDER, cell(pathX() + 2, 25));
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
