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
import java.awt.image.BufferedImage;
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

	private VillageMapRenderer(Level level, BufferedImage sheet, BufferedImage water) {
		this.map = level.map;
		this.width = level.width();
		this.height = level.height();
		this.sheet = sheet;
		this.water = water;
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
		BufferedImage image = new VillageMapRenderer(level, sheet, water).render();
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
		System.out.println("village rendered to " + out.getCanonicalPath()
				+ " (" + image.getWidth() + "x" + image.getHeight()
				+ ", seed " + options.seed + ")");

		// the headless libGDX application keeps a non-daemon loop thread alive,
		// so the process has to be told to end once the image is written
		System.exit(0);
	}

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
		for (int pos = 0; pos < map.length; pos++) {
			draw(g, pos, raisedTerrainVisual(pos));
		}
		for (int pos = 0; pos < map.length; pos++) {
			draw(g, pos, wallVisual(pos));
		}
		g.dispose();
		return image;
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
