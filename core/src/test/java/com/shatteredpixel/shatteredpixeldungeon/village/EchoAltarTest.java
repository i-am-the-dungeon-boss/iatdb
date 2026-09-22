package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The altar's shape and the tiles laid over it.
 *
 * <p>
 * No test here calls {@code create()}. It resolves the texture through the
 * texture cache and so needs a GL context the headless harness does not have,
 * which is the whole reason every altar tilemap keeps its tile maths in a pure
 * {@code tileData()} — assert on that instead.
 */
class EchoAltarTest {

	private static final int SKIP = -1;

	@Test
	@DisplayName("Every cell of the disc is either a quarter, the walkway or the raised centre")
	void everyDiscCellHasExactlyOneRole() {
		for (int y = EchoAltar.DISC_TOP; y < EchoAltar.DISC_TOP + EchoAltar.DISC_SPAN; y++) {
			for (int x = EchoAltar.DISC_LEFT; x < EchoAltar.DISC_LEFT + EchoAltar.DISC_SPAN; x++) {
				if (!EchoAltar.inDisc(x, y)) {
					continue;
				}
				int roles = 0;
				if (EchoAltar.inDais(x, y)) {
					roles++;
				}
				if (EchoAltar.onCross(x, y)) {
					roles++;
				}
				if (EchoAltar.quadrantOf(x, y) != EchoAltar.NONE) {
					roles++;
				}
				Assertions.assertThat(roles)
						.as("cell (%d,%d) should have exactly one role on the altar", x, y)
						.isEqualTo(1);
			}
		}
	}

	@Test
	@DisplayName("The quarters run sewers, prison, caves, city clockwise from the north-west")
	void quartersRunClockwiseFromTheNorthWest() {
		// two out and two along: clear of the three by three dais, clear of
		// the walkway on both axes, and inside a disc that reaches four
		Assertions.assertThat(EchoAltar.quadrantOf(EchoAltar.CENTRE_X - 2, EchoAltar.CENTRE_Y - 2))
				.isEqualTo(EchoAltar.SEWERS);
		Assertions.assertThat(EchoAltar.quadrantOf(EchoAltar.CENTRE_X + 2, EchoAltar.CENTRE_Y - 2))
				.isEqualTo(EchoAltar.PRISON);
		Assertions.assertThat(EchoAltar.quadrantOf(EchoAltar.CENTRE_X + 2, EchoAltar.CENTRE_Y + 2))
				.isEqualTo(EchoAltar.CAVES);
		Assertions.assertThat(EchoAltar.quadrantOf(EchoAltar.CENTRE_X - 2, EchoAltar.CENTRE_Y + 2))
				.isEqualTo(EchoAltar.CITY);
	}

	@Test
	@DisplayName("The disc's bounding square really does contain the whole disc")
	void theBoundingSquareFitsTheDisc() {
		// the tilemaps are rectangles, so a disc reaching past its own bound
		// would silently lose its outermost cells rather than fail
		for (int y = 0; y < 33; y++) {
			for (int x = 0; x < 33; x++) {
				if (!EchoAltar.inDisc(x, y)) {
					continue;
				}
				Assertions.assertThat(x)
						.isBetween(EchoAltar.DISC_LEFT,
								EchoAltar.DISC_LEFT + EchoAltar.DISC_SPAN - 1);
				Assertions.assertThat(y)
						.isBetween(EchoAltar.DISC_TOP,
								EchoAltar.DISC_TOP + EchoAltar.DISC_SPAN - 1);
			}
		}
	}

	@Test
	@DisplayName("Each quarter draws its own cells and nobody else's")
	void eachQuarterPaintsOnlyItsOwnCells() {
		int[] regions = {
				EchoAltar.SEWERS, EchoAltar.PRISON, EchoAltar.CAVES, EchoAltar.CITY
		};

		for (int region : regions) {
			AltarQuadrant quadrant = new AltarQuadrant(region);
			int[] data = quadrant.tileData();

			Assertions.assertThat(data).hasSize(EchoAltar.DISC_SPAN * EchoAltar.DISC_SPAN);

			for (int i = 0; i < data.length; i++) {
				int x = EchoAltar.DISC_LEFT + (i % EchoAltar.DISC_SPAN);
				int y = EchoAltar.DISC_TOP + (i / EchoAltar.DISC_SPAN);
				boolean mine = EchoAltar.quadrantOf(x, y) == region && !EchoAltar.inBasin(x, y);
				if (mine) {
					Assertions.assertThat(data[i])
							.as("region %d should floor (%d,%d)", region, x, y)
							.isEqualTo(AltarQuadrant.floorFor(region));
				} else {
					Assertions.assertThat(data[i])
							.as("region %d should leave (%d,%d) alone", region, x, y)
							.isEqualTo(SKIP);
				}
			}
		}
	}

	@Test
	@DisplayName("The four quarters together cover every quarter cell exactly once")
	void theQuartersTileTheDiscWithoutOverlapping() {
		int span = EchoAltar.DISC_SPAN;
		int[] painters = new int[span * span];

		int[] regions = {
				EchoAltar.SEWERS, EchoAltar.PRISON, EchoAltar.CAVES, EchoAltar.CITY
		};
		for (int region : regions) {
			int[] data = new AltarQuadrant(region).tileData();
			for (int i = 0; i < data.length; i++) {
				if (data[i] != SKIP) {
					painters[i]++;
				}
			}
		}

		for (int i = 0; i < painters.length; i++) {
			int x = EchoAltar.DISC_LEFT + (i % span);
			int y = EchoAltar.DISC_TOP + (i / span);
			int expected = EchoAltar.quadrantOf(x, y) == EchoAltar.NONE
					|| EchoAltar.inBasin(x, y) ? 0 : 1;
			Assertions.assertThat(painters[i])
					.as("(%d,%d) should be drawn by %d quarter(s)", x, y, expected)
					.isEqualTo(expected);
		}
	}

	@Test
	@DisplayName("Each quarter is floored from its own region's sheet")
	void eachQuarterUsesItsOwnRegionSheet() {
		Assertions.assertThat(AltarQuadrant.textureFor(EchoAltar.SEWERS))
				.isEqualTo(Assets.Environment.TILES_SEWERS);
		Assertions.assertThat(AltarQuadrant.textureFor(EchoAltar.PRISON))
				.isEqualTo(Assets.Environment.TILES_PRISON);
		Assertions.assertThat(AltarQuadrant.textureFor(EchoAltar.CAVES))
				.isEqualTo(Assets.Environment.TILES_CAVES);
		Assertions.assertThat(AltarQuadrant.textureFor(EchoAltar.CITY))
				.isEqualTo(Assets.Environment.TILES_CITY);
	}

	/**
	 * Two quarters wearing the same tile would leave the player unable to tell
	 * which region an echo holds, which is the one thing the altar's ground is
	 * there to say. The sheets differ, so this is not about the index alone -
	 * but two quarters on visually similar sheets sharing an index is how that
	 * would happen.
	 */
	@Test
	@DisplayName("No two quarters are floored the same way")
	void everyQuarterIsTellableFromTheOthers() {
		int[] regions = {
				EchoAltar.SEWERS, EchoAltar.PRISON, EchoAltar.CAVES, EchoAltar.CITY
		};
		for (int i = 0; i < regions.length; i++) {
			for (int j = i + 1; j < regions.length; j++) {
				boolean sameSheet = AltarQuadrant.textureFor(regions[i])
						.equals(AltarQuadrant.textureFor(regions[j]));
				boolean sameTile = AltarQuadrant.floorFor(regions[i]) == AltarQuadrant.floorFor(regions[j]);
				Assertions.assertThat(sameSheet && sameTile)
						.as("regions %d and %d would look identical", regions[i], regions[j])
						.isFalse();
			}
		}
	}

	/**
	 * The four quarters are told apart by their sheet, not by the slot on it. Every
	 * one of them takes its region's plain floor, and what separates them is the
	 * walkway: the city sheet's worked floor, which is the red carpet the disc's
	 * own {@code EMPTY_SP} paving already renders as, drawn by no overlay at all.
	 */
	@Test
	@DisplayName("Every quarter is floored with its own sheet's plain floor")
	void everyQuarterIsFlooredWithItsSheetsPlainFloor() {
		int[] regions = {
				EchoAltar.SEWERS, EchoAltar.PRISON, EchoAltar.CAVES, EchoAltar.CITY
		};
		for (int region : regions) {
			Assertions.assertThat(AltarQuadrant.floorFor(region))
					.as("region %d takes the plain floor", region)
					.isEqualTo(DungeonTileSheet.FLOOR);
		}
	}

	/**
	 * The city quarter is the awkward one: the village's own paving is the city
	 * sheet's special floor, so a city quarter laid in it would be invisible
	 * against the walkway running through the middle of it.
	 */
	@Test
	@DisplayName("The city quarter is not floored with the walkway's own carpet")
	void theCityQuarterDoesNotVanishIntoTheWalkway() {
		Assertions.assertThat(AltarQuadrant.floorFor(EchoAltar.CITY))
				.isNotEqualTo(DungeonTileSheet.FLOOR_SP);
	}

	@Test
	@DisplayName("A quarter with no region refuses to draw rather than picking a sheet")
	void anUnsetQuarterRefusesToDraw() {
		Assertions.assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
			@Override
			public void call() {
				AltarQuadrant.textureFor(EchoAltar.NONE);
			}
		}).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Every quarter has a basin, sitting on that quarter and nowhere near the throne")
	void everyQuarterHoldsOneBasin() {
		int[] regions = {
				EchoAltar.SEWERS, EchoAltar.PRISON, EchoAltar.CAVES, EchoAltar.CITY
		};
		int[] shapes = new int[regions.length];

		for (int r = 0; r < regions.length; r++) {
			int region = regions[r];
			int left = EchoAltar.basinLeft(region);
			int top = EchoAltar.basinTop(region);
			int found = 0;
			for (int y = top; y < top + EchoAltar.basinHeight(region); y++) {
				for (int x = left; x < left + EchoAltar.basinWidth(region); x++) {
					if (!EchoAltar.inBasin(x, y)) {
						continue;
					}
					found++;
					Assertions.assertThat(EchoAltar.quadrantOf(x, y))
							.as("the basin at (%d,%d) should sit on its own quarter", x, y)
							.isEqualTo(region);
				}
			}
			Assertions.assertThat(found)
					.as("every cell of region %d's basin is inside its own box", region)
					.isEqualTo(EchoAltar.basinSize(region));
			Assertions.assertThat(found).as("region %d holds water at all", region)
					.isGreaterThan(0);
			shapes[r] = found;
		}

		// four identical squares told the player nothing about the four places
		// they stand for, so no two quarters may pool the same amount of water
		Assertions.assertThat(shapes).doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("No basin swallows the spot an echo stands on")
	void noBasinTakesAPost() {
		for (int i = 0; i < EchoAltar.POST_X.length; i++) {
			Assertions.assertThat(EchoAltar.inBasin(EchoAltar.POST_X[i], EchoAltar.POST_Y[i]))
					.as("post %d should stay dry", i)
					.isFalse();
		}
	}

	@Test
	@DisplayName("Every basin is edged with its own region's shoreline, not cut square")
	void everyBasinIsEdgedWithItsOwnRegionsShore() {
		int[] regions = {
				EchoAltar.SEWERS, EchoAltar.PRISON, EchoAltar.CAVES, EchoAltar.CITY };

		for (int r = 0; r < regions.length; r++) {
			AltarShore shore = new AltarShore(regions[r]);

			Assertions.assertThat(shore.textureName())
					.as("the shore is drawn from the region's own sheet")
					.isEqualTo(AltarQuadrant.textureFor(regions[r]));
			Assertions.assertThat(shore.tileW).isEqualTo(EchoAltar.basinWidth(regions[r]));
			Assertions.assertThat(shore.tileH).isEqualTo(EchoAltar.basinHeight(regions[r]));

			// +1 land above, +2 land right, +4 land below, +8 land left. No basin
			// cell may land on the bare WATER index: that is the one the terrain
			// layer skips outright, and it is exactly the square hole this fixes.
			int[] data = shore.tileData();
			int water = 0;
			for (int i = 0; i < data.length; i++) {
				if (data[i] == -1) {
					continue;
				}
				water++;
				Assertions.assertThat(data[i])
						.as("region %d, slot %d is a stitched shore", regions[r], i)
						.isGreaterThan(DungeonTileSheet.WATER)
						.isLessThanOrEqualTo(DungeonTileSheet.WATER + 15);
			}
			Assertions.assertThat(water).isEqualTo(EchoAltar.basinSize(regions[r]));
		}
	}

	@Test
	@DisplayName("The deepest echo sits in the throne, at the middle of the altar")
	void theDeepestEchoSitsInTheThrone() {
		int deepest = EchoAltar.POST_X.length - 1;

		Assertions.assertThat(EchoAltar.POST_X[deepest]).isEqualTo(EchoAltar.THRONE_SEAT_X);
		Assertions.assertThat(EchoAltar.POST_Y[deepest]).isEqualTo(EchoAltar.THRONE_SEAT_Y);
		Assertions.assertThat(EchoAltar.isThroneSeat(EchoAltar.CENTRE_X, EchoAltar.CENTRE_Y))
				.as("the chair stands on the centre of the disc")
				.isTrue();
	}

	@Test
	@DisplayName("Each basin holds its own region's water, and no two hold the same")
	void eachBasinHoldsItsOwnRegionsWater() {
		Assertions.assertThat(AltarPool.waterFor(EchoAltar.SEWERS))
				.isEqualTo(Assets.Environment.WATER_SEWERS);
		Assertions.assertThat(AltarPool.waterFor(EchoAltar.PRISON))
				.isEqualTo(Assets.Environment.WATER_PRISON);
		Assertions.assertThat(AltarPool.waterFor(EchoAltar.CAVES))
				.isEqualTo(Assets.Environment.WATER_CAVES);
		Assertions.assertThat(AltarPool.waterFor(EchoAltar.CITY))
				.isEqualTo(Assets.Environment.WATER_CITY);

		int[] regions = {
				EchoAltar.SEWERS, EchoAltar.PRISON, EchoAltar.CAVES, EchoAltar.CITY
		};
		for (int i = 0; i < regions.length; i++) {
			AltarPool pool = new AltarPool(regions[i]);
			Assertions.assertThat(pool.tileX).isEqualTo(EchoAltar.basinLeft(regions[i]));
			Assertions.assertThat(pool.tileY).isEqualTo(EchoAltar.basinTop(regions[i]));
			// the water images are 32x32, so only four tiles exist to index
			int[] data = pool.tileData();
			int filled = 0;
			for (int j = 0; j < data.length; j++) {
				if (data[j] == SKIP) {
					continue;
				}
				filled++;
				Assertions.assertThat(data[j]).isBetween(0, 3);
			}
			Assertions.assertThat(filled)
					.as("region %d fills exactly its own basin", regions[i])
					.isEqualTo(EchoAltar.basinSize(regions[i]));
			for (int j = i + 1; j < regions.length; j++) {
				Assertions.assertThat(AltarPool.waterFor(regions[i]))
						.isNotEqualTo(AltarPool.waterFor(regions[j]));
			}
		}
	}

	@Test
	@DisplayName("A quarter leaves its basin unfloored rather than paving over the water")
	void quartersLeaveTheirBasinsOpen() {
		int region = EchoAltar.SEWERS;
		int[] data = new AltarQuadrant(region).tileData();
		for (int i = 0; i < data.length; i++) {
			int x = EchoAltar.DISC_LEFT + (i % EchoAltar.DISC_SPAN);
			int y = EchoAltar.DISC_TOP + (i / EchoAltar.DISC_SPAN);
			if (EchoAltar.inBasin(x, y)) {
				Assertions.assertThat(data[i])
						.as("(%d,%d) is basin and should be left to the water", x, y)
						.isEqualTo(SKIP);
			}
		}
	}

	@Test
	@DisplayName("The dais is a three by three platform of the halls' plain floor")
	void theDaisCoversTheCentre() {
		AltarDais dais = new AltarDais();
		int[] data = dais.tileData();

		Assertions.assertThat(data).hasSize(EchoAltar.DAIS_SPAN * EchoAltar.DAIS_SPAN);
		Assertions.assertThat(EchoAltar.DAIS_SPAN).as("three cells to a side").isEqualTo(3);

		int platform = 0;
		for (int i = 0; i < data.length; i++) {
			int x = EchoAltar.DAIS_LEFT + (i % EchoAltar.DAIS_SPAN);
			int y = EchoAltar.DAIS_TOP + (i / EchoAltar.DAIS_SPAN);
			Assertions.assertThat(EchoAltar.inDais(x, y))
					.as("(%d,%d) is inside the dais rect, so it is dais", x, y)
					.isTrue();
			Assertions.assertThat(data[i]).isEqualTo(DungeonTileSheet.FLOOR);
			platform++;
		}

		Assertions.assertThat(platform).as("a three by three platform").isEqualTo(9);
	}

	@Test
	@DisplayName("The dais sits inside the disc, so the quarters still ring it")
	void theDaisFitsInsideTheDisc() {
		for (int y = EchoAltar.DAIS_TOP; y < EchoAltar.DAIS_TOP + EchoAltar.DAIS_SPAN; y++) {
			for (int x = EchoAltar.DAIS_LEFT; x < EchoAltar.DAIS_LEFT + EchoAltar.DAIS_SPAN; x++) {
				Assertions.assertThat(EchoAltar.inDisc(x, y))
						.as("dais cell (%d,%d) is on the disc", x, y)
						.isTrue();
			}
		}
	}

	/**
	 * Written out as literal indices rather than computed. The boss sheet is
	 * eight columns wide where every region sheet is sixteen, so a copy-paste
	 * from the region tilemaps would land on entirely different art — and it
	 * would still render, just wrongly. This fails loudly instead.
	 */
	@Test
	@DisplayName("The throne is drawn from the eight-column boss sheet")
	void theThroneUsesTheEightColumnBossSheet() {
		AltarThrone throne = new AltarThrone();

		Assertions.assertThat(throne.tileW).isEqualTo(3);
		Assertions.assertThat(throne.tileH).isEqualTo(3);
		Assertions.assertThat(throne.tileData())
				.containsExactly(105, 106, 107, 113, 114, 115, 121, 122, 123);
	}

	@Test
	@DisplayName("The throne is placed so its seat lands on the seat cell")
	void theThroneIsCentredOnItsSeat() {
		AltarThrone throne = new AltarThrone();

		// the middle of the nine tiles is the chair itself
		Assertions.assertThat(throne.tileX + 1).isEqualTo(EchoAltar.THRONE_SEAT_X);
		Assertions.assertThat(throne.tileY + 1).isEqualTo(EchoAltar.THRONE_SEAT_Y);
	}

	@Test
	@DisplayName("The throne's shadow is a single tile above the seat")
	void theThroneCastsItsShadowAboveTheSeat() {
		AltarThroneShadow shadow = new AltarThroneShadow();

		Assertions.assertThat(shadow.tileData()).containsExactly(109);
		Assertions.assertThat(shadow.tileX).isEqualTo(EchoAltar.THRONE_SEAT_X);
		Assertions.assertThat(shadow.tileY).isEqualTo(EchoAltar.THRONE_SEAT_Y - 1);
	}
}
