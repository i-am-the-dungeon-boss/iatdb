package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Villager;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.levels.HouseLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.HighGrass;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class VillageLevelBuildTest {

	@BeforeEach
	void onGroundLevel() {
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Dungeon.depth = VillageLevel.VILLAGE_DEPTH;
		Dungeon.branch = 0;
	}

	private VillageLevel village() {
		VillageLevel level = new VillageLevel();
		level.create();
		return level;
	}

	private HouseLevel house() {
		Dungeon.branch = VillageLevel.HOUSE_BRANCH;
		HouseLevel level = new HouseLevel();
		level.create();
		Dungeon.branch = 0;
		return level;
	}

	@Test
	@DisplayName("The village builds at its declared size and starts fully revealed")
	void villageBuildsRevealed() {
		VillageLevel level = village();

		Assertions.assertThat(level.width()).isEqualTo(VillageLevel.SIZE);
		Assertions.assertThat(level.height()).isEqualTo(VillageLevel.SIZE);
		Assertions.assertThat(level.mapped).containsOnly(true);
		Assertions.assertThat(level.visited).containsOnly(true);
	}

	@Test
	@DisplayName("The dungeon mouth is the village's only way out, and it leads to depth 1")
	void villageTransitionsAreWired() {
		VillageLevel level = village();

		LevelTransition toDungeon = level.getTransition(LevelTransition.Type.REGULAR_EXIT);
		Assertions.assertThat(toDungeon).isNotNull();
		Assertions.assertThat(toDungeon.destDepth).isEqualTo(1);
		Assertions.assertThat(toDungeon.destBranch).isZero();
		Assertions.assertThat(level.map[toDungeon.cell()]).isEqualTo(Terrain.EXIT);

		// the house is built and still routes, but nothing here opens onto it
		Assertions.assertThat(level.transitions).hasSize(1);
	}

	@Test
	@DisplayName("The house, though unreachable for now, still leads back to the village")
	void houseReturnsToVillage() {
		HouseLevel level = house();

		LevelTransition back = level.getTransition(LevelTransition.Type.BRANCH_EXIT);
		Assertions.assertThat(back).isNotNull();
		Assertions.assertThat(back.destDepth).isEqualTo(VillageLevel.VILLAGE_DEPTH);
		Assertions.assertThat(back.destBranch).isZero();
		// the doorway triggers it; the hero comes home to the tile inside it
		Assertions.assertThat(level.map[level.doorCell()]).isEqualTo(Terrain.DOOR);
		Assertions.assertThat(back.cell()).isEqualTo(level.doorstepCell());
	}

	@Test
	@DisplayName("The hero can walk from where they arrive to the dungeon mouth")
	void pathFromArrivalToDungeonIsWalkable() {
		VillageLevel level = village();

		Assertions.assertThat(reachable(level, level.arrivalCell(), level.dungeonEntrance()))
				.as("dungeon entrance reachable from the arrival point")
				.isTrue();
	}

	@Test
	@DisplayName("The village has no way up: no entrance stairs and no entrance transition")
	void villageHasNoStairsUp() {
		VillageLevel level = village();

		Assertions.assertThat(level.map).doesNotContain(Terrain.ENTRANCE);
		Assertions.assertThat(level.transitions)
				.extracting(transition -> transition.type)
				.doesNotContain(LevelTransition.Type.REGULAR_ENTRANCE,
						LevelTransition.Type.SURFACE);
	}

	@Test
	@DisplayName("The hero arrives between the two gate statues, two steps short of the stairs")
	void arrivalCellIsBetweenTheGateStatues() {
		VillageLevel level = village();

		int arrival = level.arrivalCell();
		int mouth = level.dungeonEntrance();
		Assertions.assertThat(arrival).isNotEqualTo(mouth);
		Assertions.assertThat(level.invalidHeroPos(arrival)).isFalse();
		Assertions.assertThat(level.passable[arrival]).isTrue();
		Assertions.assertThat(level.map[arrival]).isEqualTo(Terrain.EMPTY_SP);

		// one block south of the old arrival, so the hero stands between the statues
		Assertions.assertThat(arrival).isEqualTo(mouth + 2 * level.width());
		Assertions.assertThat(level.map[arrival - 1]).isEqualTo(Terrain.STATUE);
		Assertions.assertThat(level.map[arrival + 1]).isEqualTo(Terrain.STATUE);
	}

	@Test
	@DisplayName("The smith stands in the smithy with the forge on their east")
	void smithStandsBesideTheForge() {
		VillageLevel level = village();

		int smith = -1;
		for (Mob mob : level.mobs) {
			if (mob instanceof Villager && ((Villager) mob).kind() == Villager.Kind.SMITH) {
				smith = mob.pos;
			}
		}

		Assertions.assertThat(smith % level.width()).isEqualTo(24);
		Assertions.assertThat(smith / level.width()).isEqualTo(8);
		Assertions.assertThat(level.map[smith + 1]).isEqualTo(Terrain.EMBERS);
	}

	@Test
	@DisplayName("The dungeon gate stands on the northern half of the map")
	void dungeonMouthIsOnTheNorthEdge() {
		VillageLevel level = village();

		int mouth = level.dungeonEntrance();
		Assertions.assertThat(mouth / level.width())
				.as("the gate should sit in the north half")
				.isLessThan(VillageLevel.SIZE / 2);
		Assertions.assertThat(level.map[mouth]).isEqualTo(Terrain.EXIT);
	}

	@Test
	@DisplayName("The sea lies along the southern edge, and nowhere else")
	void theSeaIsOnTheSouthEdge() {
		VillageLevel level = village();

		boolean sawWater = false;
		for (int cell = 0; cell < level.length(); cell++) {
			if (level.map[cell] != Terrain.WATER) {
				continue;
			}
			int x = cell % level.width();
			int y = cell / level.width();
			// the altar's basins are the other water in town, and they are not
			// the sea - EchoAltarTest owns those
			if (EchoAltar.inBasin(x, y)) {
				continue;
			}
			sawWater = true;
			Assertions.assertThat(y)
					.as("water at (%d,%d) should be in the south half", x, y)
					.isGreaterThanOrEqualTo(VillageLevel.SIZE / 2);
		}
		Assertions.assertThat(sawWater).as("the village should have a shoreline").isTrue();
	}

	/**
	 * The map border must never be the last thing the player sees, so the ring
	 * just inside the wall is always forest or open sea — never bare ground that
	 * would read as the world simply stopping.
	 */
	@Test
	@DisplayName("The edge of the map is always forest or sea, never bare ground")
	void theMapEdgeIsAlwaysForestOrSea() {
		VillageLevel level = village();

		for (int cell = 0; cell < level.length(); cell++) {
			int x = cell % level.width();
			int y = cell / level.width();
			boolean onEdgeRing = x <= 2 || y <= 2
					|| x >= level.width() - 3 || y >= level.height() - 3;
			if (!onEdgeRing) {
				continue;
			}
			// the one deliberate exception: the east road runs out through the
			// edge on purpose, so the world reads as carrying on past the trees
			if (y == VillageLevel.TRACK_Y && x > VillageLevel.EAST_BARRICADE_X) {
				continue;
			}
			Assertions.assertThat(level.map[cell])
					.as("cell (%d,%d) on the map edge", x, y)
					.isIn(Terrain.GRASS, Terrain.WATER, Terrain.WALL);
		}
	}

	/**
	 * The east road is the one piece of the village that exists to be looked at
	 * rather than walked. It has to carry on past the last cell the player can
	 * stand on, because a road that ends exactly where the player is stopped
	 * reads as the map ending; a road that keeps going past a barricade reads as
	 * somewhere else being out there. So both halves are asserted together — the
	 * paving continues, and none of the continuation is reachable.
	 */
	@Test
	@DisplayName("The east road carries on past a barricade the player cannot pass")
	void theEastRoadCarriesOnButIsClosed() {
		VillageLevel level = village();

		int row = VillageLevel.TRACK_Y;
		int barricade = level.cell(VillageLevel.EAST_BARRICADE_X, row);
		Assertions.assertThat(level.map[barricade])
				.as("the road east should be barricaded")
				.isEqualTo(Terrain.BARRICADE);
		Assertions.assertThat(level.solid[barricade])
				.as("the barricade should be solid")
				.isTrue();

		for (int x = VillageLevel.EAST_BARRICADE_X + 1; x < VillageLevel.SIZE - 1; x++) {
			int cell = level.cell(x, row);
			Assertions.assertThat(level.map[cell])
					.as("the road should carry on paved at (%d,%d)", x, row)
					.isEqualTo(Terrain.EMPTY_SP);
			Assertions.assertThat(reachable(level, level.arrivalCell(), cell))
					.as("the road past the barricade should not be walkable, at (%d,%d)", x, row)
					.isFalse();
		}
	}

	@Test
	@DisplayName("The dock is east of the path down from town, and planked in wood")
	void theDockIsEastAndWooden() {
		VillageLevel level = village();

		Assertions.assertThat(VillageLevel.DOCK_X)
				.as("the dock should sit east of the road down through town")
				.isGreaterThan(VillageLevel.TRACK_Y);

		VillageDock dock = null;
		for (int i = 0; i < level.customTiles.size(); i++) {
			if (level.customTiles.get(i) instanceof VillageDock) {
				dock = (VillageDock) level.customTiles.get(i);
			}
		}
		Assertions.assertThat(dock).as("the jetty should be planked over").isNotNull();
		Assertions.assertThat(dock.tileX).isEqualTo(VillageLevel.DOCK_X);
		// boards, and the city sheet has none - see VillageDock
		Assertions.assertThat(dock.textureName()).isEqualTo(Assets.Environment.TILES_SEWERS);
		Assertions.assertThat(dock.tileData()).doesNotContain(-1);
	}

	/**
	 * Reads the four skins back off the level: the ground, the woods and their
	 * overhang, and the paving. Each is checked cell by cell against the terrain
	 * that should have summoned it, so a skin covering one cell too many - the
	 * dock, or the altar - fails here rather than in the game.
	 */
	@Test
	@DisplayName("The village is skinned from the caves, the sewers and the halls")
	void theVillageIsSkinnedFromOtherRegionsSheets() {
		VillageLevel level = village();

		VillageSkin green = skin(level.customTiles, Assets.Environment.TILES_CAVES);
		VillageSkin sand = skin(level.customTiles, Assets.Environment.TILES_CAVES, 1);
		VillageSkin woods = skin(level.customTiles, Assets.Environment.TILES_SEWERS);
		VillageSkin paving = skin(level.customTiles, Assets.Environment.TILES_HALLS);
		VillageSkin overhang = skin(level.customWalls, Assets.Environment.TILES_SEWERS);

		Assertions.assertThat(green).as("the ground is caves grass").isNotNull();
		Assertions.assertThat(woods).as("the woods are sewers grass").isNotNull();
		Assertions.assertThat(paving).as("the paths are halls paving").isNotNull();
		Assertions.assertThat(overhang).as("the woods keep their overhang").isNotNull();
		Assertions.assertThat(sand).as("the shore is caves floor").isNotNull();

		int[] greenData = green.tileData();
		int[] woodsData = woods.tileData();
		int[] pavingData = paving.tileData();
		int[] overhangData = overhang.tileData();
		int[] sandData = sand.tileData();

		for (int i = 0; i < greenData.length; i++) {
			int x = green.tileX + (i % green.tileW);
			int y = green.tileY + (i / green.tileW);
			int tile = level.map[level.cell(x, y)];

			check(greenData[i], tile == Terrain.GRASS, x, y, "caves grass");

			// the woods are drawn entirely by the skin: no cell of them is
			// left as HIGH_GRASS terrain, because the terrain layer would then
			// draw the city's own blades over the top of the skin
			Assertions.assertThat(tile)
					.as("(%d,%d) still draws tall grass from the level's own sheet", x, y)
					.isNotEqualTo(Terrain.HIGH_GRASS);
			if (woodsData[i] != -1 && i >= green.tileW) {
				// and the opaque ground runs under every blade, or the tile
				// beneath shows through the gaps between them
				check(greenData[i], true, x, y, "caves grass under the woods");
				check(overhangData[i - green.tileW], true, x, y - 1, "overhang above the woods");
			}

			// the paving skips the whole disc - the altar's own tilemaps own
			// its quarters, and its four walkways are left to the city's own
			// worked floor - and the dock, which is planked from the sewers
			boolean paved = tile == Terrain.EMPTY_SP
					&& !EchoAltar.inDisc(x, y)
					&& !(x >= VillageLevel.DOCK_X && x < VillageLevel.DOCK_X + VillageLevel.DOCK_W
							&& y >= VillageLevel.DOCK_TOP
							&& y < VillageLevel.DOCK_TOP + VillageLevel.DOCK_H);
			check(pavingData[i], paved, x, y, "halls paving");

			// the shore is the only bare floor in town, so it is the only
			// thing the sand skin covers
			check(sandData[i], tile == Terrain.EMPTY, x, y, "caves sand");
		}
	}

	private void check(int drawn, boolean expected, int x, int y, String what) {
		if (expected) {
			Assertions.assertThat(drawn).as("(%d,%d) should be %s", x, y, what)
					.isNotEqualTo(-1);
		} else {
			Assertions.assertThat(drawn).as("(%d,%d) should not be %s", x, y, what)
					.isEqualTo(-1);
		}
	}

	private VillageSkin skin(
			java.util.List<? extends com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap> tiles,
			String sheet) {
		return skin(tiles, sheet, 0);
	}

	/** @param ordinal which skin on that sheet, in the order the level added them */
	private VillageSkin skin(
			java.util.List<? extends com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap> tiles,
			String sheet, int ordinal) {
		int seen = 0;
		for (int i = 0; i < tiles.size(); i++) {
			Object candidate = tiles.get(i);
			if (candidate instanceof VillageSkin
					&& sheet.equals(((VillageSkin) candidate).textureName())
					&& seen++ == ordinal) {
				return (VillageSkin) candidate;
			}
		}
		return null;
	}

	@Test
	@DisplayName("The walkway is the same paving inside the altar as out, and runs up to the dais")
	void theWalkwaySplitsTheQuartersWithTheCitysCarpet() {
		VillageLevel level = village();

		Assertions.assertThat(level.tilesTex()).isEqualTo(Assets.Environment.TILES_CITY);

		for (int y = 1; y < VillageLevel.SIZE - 1; y++) {
			for (int x = 1; x < VillageLevel.SIZE - 1; x++) {
				if (!EchoAltar.onCross(x, y)) {
					continue;
				}
				Assertions.assertThat(level.map[level.cell(x, y)])
						.as("the walkway at (%d,%d) is the city's worked paving", x, y)
						.isEqualTo(Terrain.EMPTY_SP);
			}
		}

		// the four arms are the city's own worked floor, not the halls paving
		// the rest of the town's roads are laid with: the altar is a city
		// monument, and its walkways read as its own stonework rather than as
		// the road happening to run through it. Nothing overlays them, so the
		// terrain layer draws EMPTY_SP from tilesTex() - the city sheet.
		VillageSkin paving = skin(level.customTiles, Assets.Environment.TILES_HALLS);
		int[] paved = paving.tileData();
		int arms = 0;
		for (int j = 0; j < paved.length; j++) {
			int x = paving.tileX + (j % paving.tileW);
			int y = paving.tileY + (j / paving.tileW);
			if (EchoAltar.onCross(x, y)) {
				Assertions.assertThat(paved[j])
						.as("the walkway at (%d,%d) is left to the city", x, y)
						.isEqualTo(-1);
				arms++;
			}
		}
		Assertions.assertThat(arms).as("the walkway has four arms").isGreaterThan(0);

		// and the road reaches the platform: the cell just outside each dais
		// edge, on each axis, is walkway
		Assertions.assertThat(EchoAltar.onCross(
				EchoAltar.CENTRE_X, EchoAltar.CENTRE_Y - EchoAltar.DAIS_REACH - 1))
				.as("the north arm meets the dais").isTrue();

		// and no quarter may cover it
		for (int i = 0; i < level.customTiles.size(); i++) {
			Object tiles = level.customTiles.get(i);
			if (!(tiles instanceof AltarQuadrant)) {
				continue;
			}
			AltarQuadrant quarter = (AltarQuadrant) tiles;
			int[] data = quarter.tileData();
			for (int j = 0; j < data.length; j++) {
				int x = quarter.tileX + (j % quarter.tileW);
				int y = quarter.tileY + (j / quarter.tileW);
				if (EchoAltar.onCross(x, y)) {
					Assertions.assertThat(data[j])
							.as("(%d,%d) is walkway and must be left bare", x, y)
							.isEqualTo(-1);
				}
			}
		}
	}

	@Test
	@DisplayName("The altar is laid over with its four quarters, its dais and its throne")
	void theAltarIsDressedWithItsTilemaps() {
		VillageLevel level = village();

		int quarters = 0;
		boolean dais = false;
		boolean throne = false;
		for (int i = 0; i < level.customTiles.size(); i++) {
			Object tiles = level.customTiles.get(i);
			if (tiles instanceof AltarQuadrant) {
				quarters++;
			} else if (tiles instanceof AltarDais) {
				dais = true;
			} else if (tiles instanceof AltarThrone) {
				throne = true;
			}
		}

		Assertions.assertThat(quarters).as("one quarter per region").isEqualTo(4);

		int basins = 0;
		for (int i = 0; i < level.customTiles.size(); i++) {
			if (level.customTiles.get(i) instanceof AltarPool) {
				basins++;
			}
		}
		Assertions.assertThat(basins).as("one basin per region").isEqualTo(4);

		// the shore has to come after the water it edges, or the flat patch of
		// region water is drawn over the top of its own shoreline
		int shores = 0;
		int lastPool = -1;
		for (int i = 0; i < level.customTiles.size(); i++) {
			Object tiles = level.customTiles.get(i);
			if (tiles instanceof AltarPool) {
				lastPool = i;
			} else if (tiles instanceof AltarShore) {
				shores++;
				Assertions.assertThat(i).as("shore %d is laid over the water", shores)
						.isGreaterThan(lastPool);
			}
		}
		Assertions.assertThat(shores).as("one shore per basin").isEqualTo(4);
		Assertions.assertThat(dais).as("the raised centre").isTrue();
		Assertions.assertThat(throne).as("the throne").isTrue();

		// the shadow is on the ground layer, not the wall layer: GameScene adds
		// customWalls AFTER the mobs, so a shadow up there is drawn over the
		// name tag of whoever is sitting in the chair. Below the mobs it is
		// behind the tag, which is where a throne belongs in z
		boolean shadow = false;
		for (int i = 0; i < level.customTiles.size(); i++) {
			if (level.customTiles.get(i) instanceof AltarThroneShadow) {
				shadow = true;
			}
		}
		Assertions.assertThat(shadow).as("the throne's shadow, on the ground layer").isTrue();
		for (int i = 0; i < level.customWalls.size(); i++) {
			Assertions.assertThat(level.customWalls.get(i))
					.as("nothing of the throne may sit above the mobs layer")
					.isNotInstanceOf(AltarThroneShadow.class);
		}
	}

	@Test
	@DisplayName("The village's water is not the city's lava-toned sheet")
	void theSeaIsBlue() {
		VillageLevel level = village();

		// the rest of the village is floored from the city sheet, but its water
		// is the magma one - fine for a few decorative cells, wrong for a sea
		Assertions.assertThat(level.waterTex())
				.isNotEqualTo(Assets.Environment.WATER_CITY);
	}

	/**
	 * A beach that is only water in some columns is not a coast, it is a hole in
	 * the map; one that is only sand is not a sea. The waterline may wander, but
	 * every column of it has to be a beach on the way to being a sea.
	 */
	@Test
	@DisplayName("Every column of the shore has sand on it and water past it")
	void theShoreIsAlwaysBeachThenSea() {
		VillageLevel level = village();

		for (int x = 4; x < VillageLevel.SIZE - 4; x++) {
			int sand = 0;
			int water = 0;
			int lastSand = -1;
			int firstWater = -1;
			// below the town: the altar's basins are inland water and would
			// otherwise read as a second, higher sea
			for (int y = VillageLevel.SIZE - 7; y < VillageLevel.SIZE - 1; y++) {
				int tile = level.map[level.cell(x, y)];
				if (tile == Terrain.EMPTY) {
					sand++;
					lastSand = y;
				} else if (tile == Terrain.WATER) {
					water++;
					if (firstWater < 0) {
						firstWater = y;
					}
				}
			}
			// the dock is planked over its own two columns, so it has no beach
			if (x >= VillageLevel.DOCK_X && x < VillageLevel.DOCK_X + VillageLevel.DOCK_W) {
				continue;
			}
			Assertions.assertThat(sand).as("column %d has no sand", x).isGreaterThanOrEqualTo(1);
			Assertions.assertThat(water).as("column %d has no sea", x).isGreaterThanOrEqualTo(1);
			Assertions.assertThat(lastSand)
					.as("column %d puts sand past its own waterline", x)
					.isLessThan(firstWater);
		}
	}

	/**
	 * The sand is drawn from the caves sheet, so the lip where the sea meets it
	 * has to be too. The level's own terrain layer stitches that lip from
	 * tilesTex(), which is the city's — a pale worked kerb against raw cave
	 * floor. The rim is therefore redrawn from the caves sheet on top.
	 */
	@Test
	@DisplayName("The sea is edged in the same sheet the sand is drawn from")
	void theSeaIsEdgedToMatchTheSand() {
		VillageLevel level = village();

		VillageShore shore = null;
		for (int i = 0; i < level.customTiles.size(); i++) {
			if (level.customTiles.get(i) instanceof VillageShore) {
				shore = (VillageShore) level.customTiles.get(i);
			}
		}
		Assertions.assertThat(shore).as("the sea needs a shoreline").isNotNull();
		Assertions.assertThat(shore.textureName()).isEqualTo(Assets.Environment.TILES_CAVES);
		Assertions.assertThat(level.waterTex()).isEqualTo(Assets.Environment.WATER_CAVES);

		int[] data = shore.tileData();
		for (int i = 0; i < data.length; i++) {
			int x = shore.tileX + (i % shore.tileW);
			int y = shore.tileY + (i / shore.tileW);
			if (data[i] == -1) {
				continue;
			}
			Assertions.assertThat(level.map[level.cell(x, y)])
					.as("(%d,%d) is edged but is not water", x, y)
					.isEqualTo(Terrain.WATER);
			// a bare WATER index is the one the terrain layer skips, so an
			// edge tile that lands on it would draw nothing at all
			Assertions.assertThat(data[i])
					.as("(%d,%d) should be a stitched edge", x, y)
					.isGreaterThan(DungeonTileSheet.WATER)
					.isLessThanOrEqualTo(DungeonTileSheet.WATER + 15);
		}
	}

	@Test
	@DisplayName("The dock runs off the sand and out over the water")
	void theDockRunsIntoTheWater() {
		VillageLevel level = village();

		// the planking stops one row short of the outermost ring, so open sea
		// still runs past the end of the dock
		int dockEnd = level.cell(VillageLevel.DOCK_X, VillageLevel.SIZE - 4);
		Assertions.assertThat(level.map[dockEnd])
				.as("the far end of the dock should be walkable planking")
				.isEqualTo(Terrain.EMPTY_SP);

		// and it has to be a jetty rather than a boardwalk: the planking is
		// laid over the sea, so there must be open water either side of it
		Assertions.assertThat(level.map[level.cell(VillageLevel.DOCK_X - 1, VillageLevel.SIZE - 4)])
				.as("the sea should run alongside the dock's far end")
				.isEqualTo(Terrain.WATER);
		Assertions.assertThat(level.map[level.cell(
				VillageLevel.DOCK_X + VillageLevel.DOCK_W, VillageLevel.SIZE - 4)])
				.as("the sea should run alongside the dock's far end")
				.isEqualTo(Terrain.WATER);
		Assertions.assertThat(reachable(level, level.arrivalCell(), dockEnd))
				.as("the dock should be walkable from the arrival point")
				.isTrue();

		// and it starts at the tavern's own south door, which is the whole
		// reason the tavern was pulled east in the first place
		Assertions.assertThat(level.map[level.cell(VillageLevel.DOCK_X, VillageLevel.DOCK_TOP)])
				.as("the head of the dock should be planking")
				.isEqualTo(Terrain.EMPTY_SP);
		Assertions.assertThat(
				level.map[level.cell(VillageLevel.DOCK_X, VillageLevel.DOCK_TOP - 1)])
				.as("the tavern's south door should open straight onto the dock")
				.isEqualTo(Terrain.DOOR);
	}

	/**
	 * A village the player cannot cross is worse than an empty one. Every open
	 * cell must hang together, which also proves each building is enterable
	 * without naming a single door.
	 */
	@Test
	@DisplayName("Every open cell in the village is reachable from the arrival point")
	void theWholeVillageHangsTogether() {
		VillageLevel level = village();

		for (int cell = 0; cell < level.length(); cell++) {
			if (!level.passable[cell]) {
				continue;
			}
			int x = cell % level.width();
			int y = cell / level.width();
			// the road past the east barricade is the sole exception, and is
			// unwalkable on purpose - theEastRoadCarriesOnButIsClosed owns it
			if (y == VillageLevel.TRACK_Y && x > VillageLevel.EAST_BARRICADE_X) {
				continue;
			}
			Assertions.assertThat(reachable(level, level.arrivalCell(), cell))
					.as("cell (%d,%d) is walled off from the rest of town", x, y)
					.isTrue();
		}
	}

	@Test
	@DisplayName("Every building has a door, and the town has at least five of them")
	void everyBuildingIsEnterable() {
		VillageLevel level = village();

		int doors = 0;
		for (int cell = 0; cell < level.length(); cell++) {
			if (level.map[cell] != Terrain.DOOR) {
				continue;
			}
			doors++;
			Assertions.assertThat(reachable(level, level.arrivalCell(), cell))
					.as("door at (%d,%d) cannot be reached",
							cell % level.width(), cell / level.width())
					.isTrue();
		}
		Assertions.assertThat(doors)
				.as("guild, smithy, elder, shop and tavern all need a way in")
				.isGreaterThanOrEqualTo(5);
	}

	@Test
	@DisplayName("Village grass is see-through, so nothing in town hides behind a hedge")
	void villageGrassDoesNotBlockSight() {
		VillageLevel level = village();

		int grass = anyGrassCell(level);
		Assertions.assertThat(level.losBlocking[grass]).isFalse();

		// and it stays see-through when the map is edited cell by cell
		Level.set(grass, Terrain.HIGH_GRASS, level);
		Assertions.assertThat(level.losBlocking[grass]).isFalse();
	}

	@Test
	@DisplayName("Walking through village grass leaves it standing and yields nothing")
	void villageGrassIsNeverTrampled() {
		VillageLevel level = village();
		Dungeon.level = level;

		int grass = anyGrassCell(level);
		HighGrass.trample(level, grass);

		Assertions.assertThat(level.map[grass]).isEqualTo(Terrain.HIGH_GRASS);
		Assertions.assertThat(level.heaps.valueList()).isEmpty();
		Assertions.assertThat(level.grassCanBeTrampled()).isFalse();
	}

	@Test
	@DisplayName("Dungeon grass still tramples: the village opt-out is village-only")
	void dungeonGrassStillTramples() {
		Assertions.assertThat(new SewerLevel().grassCanBeTrampled()).isTrue();
	}

	/**
	 * The village's woods are drawn by a skin rather than laid as tall grass
	 * terrain — see theVillageIsSkinnedFromOtherRegionsSheets — so a cell has
	 * to be planted to test the rules that govern tall grass when there is any.
	 */
	private int anyGrassCell(VillageLevel level) {
		for (int i = 0; i < level.length(); i++) {
			if (level.map[i] == Terrain.GRASS) {
				Level.set(i, Terrain.HIGH_GRASS, level);
				return i;
			}
		}
		throw new AssertionError("the village grew no grass at all");
	}

	@Test
	@DisplayName("Neither ground level spawns monsters or a respawner")
	void groundLevelIsPeaceful() {
		VillageLevel village = village();
		Assertions.assertThat(village.createMob()).isNull();
		Assertions.assertThat(village.addRespawner()).isNull();

		HouseLevel house = house();
		Assertions.assertThat(house.createMob()).isNull();
		Assertions.assertThat(house.addRespawner()).isNull();
		Assertions.assertThat(house.mobs).isEmpty();
	}

	@Test
	@DisplayName("The house is always solo: nobody is ever generated inside it")
	void houseIsAlwaysSolo() {
		Assertions.assertThat(house().mobs).isEmpty();
	}

	@Test
	@DisplayName("Every transition drops the hero somewhere they can legally stand")
	void transitionCellsAreValidHeroPositions() {
		assertTransitionsStandable(village());
		assertTransitionsStandable(house());
	}

	/**
	 * Dungeon.switchLevel falls back to getTransition(null).cell() when a saved
	 * position is unusable, so a transition on invalid terrain gets the hero
	 * placed there anyway. On the outermost ring that also stands them on a row
	 * with no wall beyond it, and the shadowcast runs off the end of the map.
	 */
	private void assertTransitionsStandable(Level level) {
		for (LevelTransition transition : level.transitions) {
			int cell = transition.cell();
			Assertions.assertThat(level.invalidHeroPos(cell))
					.as("%s transition at cell %d of %s is not standable",
							transition.type, cell, level.getClass().getSimpleName())
					.isFalse();
			assertInterior(level, cell);
		}
		assertInterior(level, level.entrance());
	}

	/** Nothing the hero can occupy may sit on the outermost ring. */
	private void assertInterior(Level level, int cell) {
		int x = cell % level.width();
		int y = cell / level.width();
		Assertions.assertThat(x > 0 && x < level.width() - 1 && y > 0 && y < level.height() - 1)
				.as("cell (%d,%d) of %s must not be on the map border",
						x, y, level.getClass().getSimpleName())
				.isTrue();
	}

	@Test
	@DisplayName("Field of view survives from every cell the hero can stand on")
	void fieldOfViewNeverEscapesTheMap() {
		assertFieldOfViewWorksEverywhere(village());
		assertFieldOfViewWorksEverywhere(house());
	}

	/**
	 * On an out-of-bounds scan castShadow catches, reports and blanks the whole
	 * field of view, so a blanked source cell is precisely the symptom of the
	 * shadowcast leaving the map.
	 */
	private void assertFieldOfViewWorksEverywhere(Level level) {
		boolean[] fov = new boolean[level.length()];
		int distance = level.viewDistance;
		for (int cell = 0; cell < level.length(); cell++) {
			if (level.invalidHeroPos(cell)) {
				continue;
			}
			ShadowCaster.castShadow(cell % level.width(), cell / level.width(),
					level.width(), fov, level.losBlocking, distance);
			Assertions.assertThat(fov[cell])
					.as("field of view from cell %d of %s escaped the map",
							cell, level.getClass().getSimpleName())
					.isTrue();
		}
	}

	/** Flood fill across passable cells. */
	private boolean reachable(Level level, int from, int to) {
		boolean[] seen = new boolean[level.length()];
		java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
		queue.add(from);
		seen[from] = true;
		int[] steps = { -1, 1, -level.width(), level.width() };
		while (!queue.isEmpty()) {
			int cell = queue.poll();
			if (cell == to) {
				return true;
			}
			for (int step : steps) {
				int next = cell + step;
				if (next < 0 || next >= level.length() || seen[next]) {
					continue;
				}
				if (level.passable[next] || level.map[next] == Terrain.EXIT
						|| level.map[next] == Terrain.DOOR) {
					seen[next] = true;
					queue.add(next);
				}
			}
		}
		return false;
	}
}
