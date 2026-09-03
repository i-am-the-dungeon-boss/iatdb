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

package com.shatteredpixel.shatteredpixeldungeon.tools;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Villager;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.village.AltarOverlay;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.village.AltarOverlay;
import com.shatteredpixel.shatteredpixeldungeon.village.EchoAltar;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageFigurePlacement;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Renders {@link VillageLevel} to a PNG offline, using the game's real city
 * tileset and the same tile-stitching rules the running game uses, so a layout
 * change can be eyeballed without launching the desktop client.
 *
 * <p>The three visual layers of {@code GameScene} are reproduced here —
 * terrain, raised terrain and walls — because the real tilemaps are
 * {@code Tilemap}s and need a GL context. Only the visual selection is copied;
 * the tile indices themselves come from {@link DungeonTileSheet}.
 *
 * <p>Run it via {@code ./gradlew :core:renderVillage}.
 */
public final class VillageMapRenderer {

	private static final int TILE = 16;
	private static final int SHEET_COLUMNS = 16;
	private static final String ASSETS_DIR = "iatdb.assetsDir";
	private static final String ROOT_DIR = "iatdb.rootDir";

	private final int[] map;
	private final int width;
	private final int height;
	private final BufferedImage sheet;
	private final BufferedImage water;
	private final Level level;
	private final File assets;

	private VillageMapRenderer(Level level, BufferedImage sheet, BufferedImage water,
			File assets) {
		this.map = level.map;
		this.width = level.width();
		this.height = level.height();
		this.sheet = sheet;
		this.water = water;
		this.level = level;
		this.assets = assets;
	}

	public static void main(String[] args) throws IOException {
		Options options = Options.parse(args);

		GdxTestExtension.initIfNeeded();
		Dungeon.depth = VillageLevel.VILLAGE_DEPTH;
		Dungeon.branch = 0;
		Random.pushGenerator(options.seed);

		VillageLevel level = new VillageLevel();
		level.create();
		Dungeon.level = level;
		// GameScene.create does this before building any tilemap: it seeds the
		// per-cell roll that picks between a tile's alt visuals
		DungeonTileSheet.setupVariance(level.map.length, options.seed);

		File assets = dirProperty(ASSETS_DIR);
		BufferedImage sheet = ImageIO.read(new File(assets, level.tilesTex()));
		BufferedImage water = ImageIO.read(new File(assets, level.waterTex()));
		BufferedImage image = new VillageMapRenderer(level, sheet, water, assets).render();
		if (options.markers) {
			drawMarkers(image, level);
		}
		image = scale(image, options.scale);

		Random.popGenerator();

		File out = options.out != null ? options.out
				: new File(dirProperty(ROOT_DIR), "docs/village/village-map.png");
		if (out.getParentFile() != null) {
			out.getParentFile().mkdirs();
		}
		ImageIO.write(image, "png", out);

		File chart = new File(out.getParentFile(), "village-map.txt");
		writeChart(level, chart);
		System.out.println("village charted to " + chart.getCanonicalPath());

		System.out.println("village rendered to " + out.getCanonicalPath()
				+ " (" + image.getWidth() + "x" + image.getHeight()
				+ ", seed " + options.seed + ")");

		// the headless libGDX application keeps a non-daemon loop thread alive,
		// so the process has to be told to end once the image is written
		System.exit(0);
	}

	/**
	 * A one-character-per-cell chart of the village, for marking up by hand.
	 *
	 * <p>The picture is the better likeness but it cannot be edited, and a chart
	 * typed out by hand drifts from the code the moment either moves. This is
	 * read off the built level, so it is always what the game actually makes.
	 *
	 * <p>Where a cell's terrain does not tell the whole story the chart shows the
	 * thing that does: every cell of the altar is the same paving, so it is the
	 * quarter, the basin or the dais that gets the character, and the five spots
	 * the echo bosses stand on are numbered over the top of all of it.
	 */
	private static void writeChart(VillageLevel level, File out) throws IOException {
		int w = level.width();
		StringBuilder chart = new StringBuilder();
		chart.append(CHART_LEGEND);

		chart.append("     ");
		for (int x = 0; x < w; x++) {
			chart.append(x % 10);
		}
		chart.append('\n');

		int[] posts = VillageFigurePlacement.depthPosts(level);
		int[] mentions = VillageFigurePlacement.mentionPosts(level);

		for (int y = 0; y < level.height(); y++) {
			chart.append(String.format("%3d  ", y));
			for (int x = 0; x < w; x++) {
				chart.append(chartChar(level, posts, mentions, x, y));
			}
			chart.append('\n');
		}
		try (Writer writer = new OutputStreamWriter(
				new FileOutputStream(out), StandardCharsets.UTF_8)) {
			writer.write(chart.toString());
		}
	}

	private static char chartChar(VillageLevel level, int[] posts, int[] mentions,
			int x, int y) {
		int cell = level.cell(x, y);

		for (int i = 0; i < posts.length; i++) {
			if (posts[i] == cell) {
				return (char) ('1' + i);
			}
		}
		for (int i = 0; i < mentions.length; i++) {
			if (mentions[i] == cell) {
				return 'm';
			}
		}
		for (Mob mob : level.mobs) {
			if (mob.pos == cell) {
				return 'V';
			}
		}

		if (EchoAltar.isThroneSeat(x, y)) {
			return '@';
		}
		if (EchoAltar.inBasin(x, y)) {
			return basinChar(EchoAltar.quadrantOf(x, y));
		}
		if (EchoAltar.inDais(x, y)) {
			return 'H';
		}
		if (EchoAltar.isTread(x, y)) {
			return 'h';
		}
		int quarter = EchoAltar.quadrantOf(x, y);
		if (quarter != EchoAltar.NONE) {
			return quarterChar(quarter);
		}
		if (EchoAltar.onCross(x, y)) {
			return '*';
		}

		switch (level.map[cell]) {
			case Terrain.WALL:           return '#';
			case Terrain.WALL_DECO:      return 'D';
			case Terrain.HIGH_GRASS:     return 'T';
			case Terrain.FURROWED_GRASS: return 't';
			case Terrain.GRASS:          return ',';
			case Terrain.EMPTY:          return '.';
			case Terrain.EMPTY_SP:       return '=';
			case Terrain.WATER:          return '~';
			case Terrain.DOOR:           return '+';
			case Terrain.EXIT:           return '>';
			case Terrain.WELL:           return 'o';
			case Terrain.STATUE:         return 'I';
			case Terrain.EMBERS:         return 'e';
			case Terrain.BARRICADE:      return 'X';
			case Terrain.CUSTOM_DECO:    return '@';
			default:                     return '?';
		}
	}

	private static char quarterChar(int region) {
		switch (region) {
			case EchoAltar.SEWERS: return 's';
			case EchoAltar.PRISON: return 'p';
			case EchoAltar.CAVES:  return 'c';
			case EchoAltar.CITY:   return 'y';
			default: throw new IllegalArgumentException("no chart character for " + region);
		}
	}

	private static char basinChar(int region) {
		return Character.toUpperCase(quarterChar(region));
	}

	private static final String CHART_LEGEND = ""
			+ "Village map - generated from the built level, not hand-typed.\n"
			+ "Regenerate with:  ./gradlew :core:renderVillage\n"
			+ "To propose a change, edit this file and hand it back.\n"
			+ "\n"
			+ "  #  wall             T  forest (high grass)  ,  grass\n"
			+ "  .  sand             ~  water                =  paving / carpet\n"
			+ "  +  door             >  dungeon stair        X  barricade\n"
			+ "  o  well             I  statue               e  forge embers\n"
			+ "  D  wall decoration  V  villager\n"
			+ "\n"
			+ "the altar - all one paving underneath, the letter is what is drawn on it\n"
			+ "  *  walkway          H  raised dais          h  step tread\n"
			+ "  @  throne seat, solid - nobody stands on it\n"
			+ "  s  sewers quarter   p  prison   c  caves    y  city\n"
			+ "  S  sewers basin     P  prison   C  caves    Y  city   - each its own water\n"
			+ "\n"
			+ "  1..5  where the echo bosses stand, shallowest first\n"
			+ "  m     where an honorable mention stands\n"
			+ "\n";

	/**
	 * A directory handed over by the {@code renderVillage} Gradle task. The tool
	 * runs from a scratch working directory — {@link GdxTestExtension} aims game
	 * file I/O there, and it must not be a source directory — so the asset tree
	 * and the repo root have to be passed in rather than derived from the cwd.
	 */
	private static File dirProperty(String key) {
		String path = System.getProperty(key);
		if (path == null || path.isEmpty()) {
			throw new IllegalStateException("-D" + key + " is not set;"
					+ " run this tool via ./gradlew :core:renderVillage");
		}
		File dir = new File(path);
		if (!dir.isDirectory()) {
			throw new IllegalStateException("-D" + key + " is not a directory: " + path);
		}
		return dir;
	}

	private BufferedImage render() {
		BufferedImage image = new BufferedImage(width * TILE, height * TILE,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		for (int pos = 0; pos < map.length; pos++) {
			drawWaterBed(g, pos);
		}
		for (int pos = 0; pos < map.length; pos++) {
			draw(g, pos, terrainVisual(pos));
		}
		drawOverlays(g, level.customTiles);
		for (int pos = 0; pos < map.length; pos++) {
			draw(g, pos, raisedTerrainVisual(pos));
		}
		for (int pos = 0; pos < map.length; pos++) {
			draw(g, pos, wallVisual(pos));
		}
		drawOverlays(g, level.customWalls);
		g.dispose();
		return image;
	}

	/**
	 * The altar is not terrain — it is a stack of {@code CustomTilemap}s, each
	 * read off a different region's sheet, laid over paving that is the same
	 * everywhere. Without this the doc's picture shows a plain red disc and the
	 * one part of the village most likely to be laid out wrongly is the one part
	 * nobody can look at.
	 */
	private void drawOverlays(Graphics2D g, java.util.List<? extends CustomTilemap> tilemaps) {
		for (int i = 0; i < tilemaps.size(); i++) {
			CustomTilemap tiles = tilemaps.get(i);
			if (!(tiles instanceof AltarOverlay)) {
				continue;
			}
			AltarOverlay overlay = (AltarOverlay) tiles;
			BufferedImage texture = overlaySheet(overlay.textureName());
			int columns = texture.getWidth() / TILE;
			int[] data = overlay.tileData();
			for (int j = 0; j < data.length; j++) {
				if (data[j] < 0) {
					continue;
				}
				int sx = (data[j] % columns) * TILE;
				int sy = (data[j] / columns) * TILE;
				if (sy + TILE > texture.getHeight()) {
					continue;
				}
				int dx = (tiles.tileX + (j % tiles.tileW)) * TILE;
				int dy = (tiles.tileY + (j / tiles.tileW)) * TILE;
				g.drawImage(texture, dx, dy, dx + TILE, dy + TILE,
						sx, sy, sx + TILE, sy + TILE, null);
			}
		}
	}

	private final java.util.HashMap<String, BufferedImage> sheets = new java.util.HashMap<>();

	private BufferedImage overlaySheet(String name) {
		BufferedImage cached = sheets.get(name);
		if (cached != null) {
			return cached;
		}
		try {
			BufferedImage loaded = ImageIO.read(new File(assets, name));
			if (loaded == null) {
				throw new IllegalStateException("not an image: " + name);
			}
			sheets.put(name, loaded);
			return loaded;
		} catch (IOException e) {
			throw new IllegalStateException("could not read altar sheet " + name, e);
		}
	}

	/**
	 * In game the water texture is a scrolling block drawn behind the tilemap,
	 * and the sheet's water tiles are only the translucent edges — so without a
	 * bed underneath them the river renders as a hole.
	 */
	private void drawWaterBed(Graphics2D g, int pos) {
		if (map[pos] != Terrain.WATER) {
			return;
		}
		int dx = (pos % width) * TILE;
		int dy = (pos / width) * TILE;
		int sx = dx % water.getWidth();
		int sy = dy % water.getHeight();
		g.drawImage(water, dx, dy, dx + TILE, dy + TILE, sx, sy, sx + TILE, sy + TILE, null);
	}

	private void draw(Graphics2D g, int pos, int visual) {
		if (visual < 0) {
			return;
		}
		int sx = (visual % SHEET_COLUMNS) * TILE;
		int sy = (visual / SHEET_COLUMNS) * TILE;
		if (sy + TILE > sheet.getHeight()) {
			return;
		}
		int dx = (pos % width) * TILE;
		int dy = (pos / width) * TILE;
		g.drawImage(sheet, dx, dy, dx + TILE, dy + TILE, sx, sy, sx + TILE, sy + TILE, null);
	}

	/** Mirrors {@code DungeonTerrainTilemap.getTileVisual(pos, tile, false)}. */
	private int terrainVisual(int pos) {
		int tile = map[pos];
		int visual = DungeonTileSheet.directVisuals.get(tile, -1);
		if (visual != -1) {
			return DungeonTileSheet.getVisualWithAlts(visual, pos);
		}

		if (tile == Terrain.WATER) {
			return DungeonTileSheet.stitchWaterTile(
					map[pos + PathFinder.CIRCLE4[0]],
					map[pos + PathFinder.CIRCLE4[1]],
					map[pos + PathFinder.CIRCLE4[2]],
					map[pos + PathFinder.CIRCLE4[3]]);
		} else if (tile == Terrain.CHASM) {
			return DungeonTileSheet.stitchChasmTile(pos > width ? map[pos - width] : -1);
		}

		if (DungeonTileSheet.doorTile(tile)) {
			return DungeonTileSheet.getRaisedDoorTile(tile, map[pos - width]);
		} else if (DungeonTileSheet.wallStitcheable(tile)) {
			return DungeonTileSheet.getRaisedWallTile(tile, pos,
					right(pos), below(pos), left(pos));
		} else if (tile == Terrain.STATUE) {
			return DungeonTileSheet.RAISED_STATUE;
		} else if (tile == Terrain.STATUE_SP) {
			return DungeonTileSheet.RAISED_STATUE_SP;
		} else if (tile == Terrain.REGION_DECO) {
			return DungeonTileSheet.RAISED_REGION_DECO;
		} else if (tile == Terrain.REGION_DECO_ALT) {
			return DungeonTileSheet.RAISED_REGION_DECO_ALT;
		} else if (tile == Terrain.ALCHEMY) {
			return DungeonTileSheet.RAISED_ALCHEMY_POT;
		} else if (tile == Terrain.BARRICADE) {
			return DungeonTileSheet.RAISED_BARRICADE;
		} else if (tile == Terrain.HIGH_GRASS) {
			return DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_HIGH_GRASS, pos);
		} else if (tile == Terrain.FURROWED_GRASS) {
			return DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.RAISED_FURROWED_GRASS, pos);
		}
		return DungeonTileSheet.NULL_TILE;
	}

	/** Mirrors {@code RaisedTerrainTilemap.getTileVisual}. */
	private int raisedTerrainVisual(int pos) {
		if (map[pos] == Terrain.HIGH_GRASS) {
			return DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.HIGH_GRASS_UNDERHANG, pos);
		} else if (map[pos] == Terrain.FURROWED_GRASS) {
			return DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.FURROWED_UNDERHANG, pos);
		}
		return -1;
	}

	/** Mirrors {@code DungeonWallsTilemap.getTileVisual}. */
	private int wallVisual(int pos) {
		int tile = map[pos];
		int belowTile = below(pos);

		if (DungeonTileSheet.wallStitcheable(tile)) {
			if (pos + width < map.length && !DungeonTileSheet.wallStitcheable(belowTile)) {
				if (belowTile == Terrain.DOOR) {
					return DungeonTileSheet.DOOR_SIDEWAYS;
				} else if (belowTile == Terrain.LOCKED_DOOR || belowTile == Terrain.HERO_LKD_DR) {
					return DungeonTileSheet.DOOR_SIDEWAYS_LOCKED;
				} else if (belowTile == Terrain.CRYSTAL_DOOR) {
					return DungeonTileSheet.DOOR_SIDEWAYS_CRYSTAL;
				} else if (belowTile == Terrain.OPEN_DOOR) {
					return DungeonTileSheet.NULL_TILE;
				}
			} else {
				return DungeonTileSheet.stitchInternalWallTile(tile,
						right(pos), rightBelow(pos), belowTile, leftBelow(pos), left(pos));
			}
		}

		if (tile == Terrain.LOCKED_EXIT || tile == Terrain.UNLOCKED_EXIT) {
			return DungeonTileSheet.EXIT_UNDERHANG;
		} else if (pos + width < map.length && DungeonTileSheet.wallStitcheable(belowTile)) {
			return DungeonTileSheet.stitchWallOverhangTile(tile,
					rightBelow(pos), belowTile, leftBelow(pos));
		} else if (insideMap(pos) && (belowTile == Terrain.DOOR
				|| belowTile == Terrain.LOCKED_DOOR || belowTile == Terrain.HERO_LKD_DR)) {
			return DungeonTileSheet.DOOR_OVERHANG;
		} else if (insideMap(pos) && belowTile == Terrain.OPEN_DOOR) {
			return DungeonTileSheet.DOOR_OVERHANG_OPEN;
		} else if (insideMap(pos) && belowTile == Terrain.CRYSTAL_DOOR) {
			return DungeonTileSheet.DOOR_OVERHANG_CRYSTAL;
		} else if (belowTile == Terrain.STATUE) {
			return DungeonTileSheet.STATUE_OVERHANG;
		} else if (belowTile == Terrain.STATUE_SP) {
			return DungeonTileSheet.STATUE_SP_OVERHANG;
		} else if (belowTile == Terrain.REGION_DECO) {
			return DungeonTileSheet.REGION_DECO_OVERHANG;
		} else if (belowTile == Terrain.REGION_DECO_ALT) {
			return DungeonTileSheet.REGION_DECO_ALT_OVERHANG;
		} else if (belowTile == Terrain.ALCHEMY) {
			return DungeonTileSheet.ALCHEMY_POT_OVERHANG;
		} else if (belowTile == Terrain.BARRICADE) {
			return DungeonTileSheet.BARRICADE_OVERHANG;
		} else if (belowTile == Terrain.HIGH_GRASS) {
			return DungeonTileSheet.getVisualWithAlts(
					DungeonTileSheet.HIGH_GRASS_OVERHANG, pos + width);
		} else if (belowTile == Terrain.FURROWED_GRASS) {
			return DungeonTileSheet.getVisualWithAlts(
					DungeonTileSheet.FURROWED_OVERHANG, pos + width);
		}
		return -1;
	}

	private boolean insideMap(int pos) {
		int x = pos % width;
		int y = pos / width;
		return x > 0 && x < width - 1 && y > 0 && y < height - 1;
	}

	private int right(int pos) {
		return (pos + 1) % width != 0 ? map[pos + 1] : -1;
	}

	private int left(int pos) {
		return pos % width != 0 ? map[pos - 1] : -1;
	}

	private int below(int pos) {
		return pos + width < map.length ? map[pos + width] : -1;
	}

	private int rightBelow(int pos) {
		return (pos + 1) % width != 0 && pos + width < map.length ? map[pos + 1 + width] : -1;
	}

	private int leftBelow(int pos) {
		return pos % width != 0 && pos + width < map.length ? map[pos - 1 + width] : -1;
	}

	/**
	 * Villagers and the arrival cell, which are actors rather than terrain and so
	 * have no tile of their own. Labels rather than sprites: the point is to see
	 * where they stand relative to the layout.
	 */
	private static void drawMarkers(BufferedImage image, VillageLevel level) {
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));

		for (Mob mob : level.mobs) {
			// two letters: SMITH and SAGE would otherwise both read as "S"
			String label = mob instanceof Villager
					? ((Villager) mob).kind().name().substring(0, 2)
					: "M";
			marker(g, level, mob.pos, new Color(0xFF, 0xD8, 0x4A), label);
		}
		marker(g, level, level.arrivalCell(), new Color(0x6C, 0xC6, 0xFF), "@");
		marker(g, level, level.dungeonEntrance(), new Color(0xFF, 0x6B, 0x6B), "V");
		g.dispose();
	}

	private static void marker(Graphics2D g, Level level, int cell, Color color, String label) {
		int x = (cell % level.width()) * TILE;
		int y = (cell / level.width()) * TILE;
		g.setStroke(new BasicStroke(1f));
		g.setColor(new Color(0, 0, 0, 140));
		g.fillRect(x + 1, y + 1, TILE - 2, TILE - 2);
		g.setColor(color);
		g.drawRect(x + 1, y + 1, TILE - 3, TILE - 3);
		g.drawString(label, x + 3, y + 11);
	}

	private static BufferedImage scale(BufferedImage image, int scale) {
		if (scale <= 1) {
			return image;
		}
		BufferedImage scaled = new BufferedImage(image.getWidth() * scale,
				image.getHeight() * scale, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaled.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(image, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
		g.dispose();
		return scaled;
	}

	/** {@code --out <path> --scale <n> --seed <n> --no-markers}. */
	private static final class Options {

		/** Null until {@code --out} is given: the default needs {@code ROOT_DIR}. */
		private File out;
		private int scale = 3;
		private long seed = 1;
		private boolean markers = true;

		static Options parse(String[] args) {
			Options options = new Options();
			for (int i = 0; i < args.length; i++) {
				switch (args[i]) {
					case "--out":
						options.out = new File(args[++i]).getAbsoluteFile();
						break;
					case "--scale":
						options.scale = Integer.parseInt(args[++i]);
						break;
					case "--seed":
						options.seed = Long.parseLong(args[++i]);
						break;
					case "--no-markers":
						options.markers = false;
						break;
					default:
						throw new IllegalArgumentException("unknown option: " + args[i]);
				}
			}
			return options;
		}
	}
}
