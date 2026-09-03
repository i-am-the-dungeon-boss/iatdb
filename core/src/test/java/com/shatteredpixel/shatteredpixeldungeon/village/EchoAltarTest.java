package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The altar's shape and the tiles laid over it.
 *
 * <p>No test here calls {@code create()}. It resolves the texture through the
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
		Assertions.assertThat(EchoAltar.quadrantOf(EchoAltar.CENTRE_X - 3, EchoAltar.CENTRE_Y - 3))
				.isEqualTo(EchoAltar.SEWERS);
		Assertions.assertThat(EchoAltar.quadrantOf(EchoAltar.CENTRE_X + 3, EchoAltar.CENTRE_Y - 3))
				.isEqualTo(EchoAltar.PRISON);
		Assertions.assertThat(EchoAltar.quadrantOf(EchoAltar.CENTRE_X + 3, EchoAltar.CENTRE_Y + 3))
				.isEqualTo(EchoAltar.CAVES);
		Assertions.assertThat(EchoAltar.quadrantOf(EchoAltar.CENTRE_X - 3, EchoAltar.CENTRE_Y + 3))
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
				boolean mine = EchoAltar.quadrantOf(x, y) == region;
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
			int expected = EchoAltar.quadrantOf(x, y) == EchoAltar.NONE ? 0 : 1;
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
				boolean sameTile = AltarQuadrant.floorFor(regions[i])
						== AltarQuadrant.floorFor(regions[j]);
				Assertions.assertThat(sameSheet && sameTile)
						.as("regions %d and %d would look identical", regions[i], regions[j])
						.isFalse();
			}
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
	@DisplayName("The dais covers the raised centre and the four steps up to it")
	void theDaisCoversTheCentreAndItsTreads() {
		AltarDais dais = new AltarDais();
		int[] data = dais.tileData();

		Assertions.assertThat(data).hasSize(EchoAltar.DAIS_SPAN * EchoAltar.DAIS_SPAN);

		int platform = 0;
		int treads = 0;
		for (int i = 0; i < data.length; i++) {
			int x = EchoAltar.DAIS_LEFT + (i % EchoAltar.DAIS_SPAN);
			int y = EchoAltar.DAIS_TOP + (i / EchoAltar.DAIS_SPAN);
			if (EchoAltar.inDais(x, y)) {
				Assertions.assertThat(data[i]).isEqualTo(DungeonTileSheet.FLOOR_SP);
				platform++;
			} else if (EchoAltar.isTread(x, y)) {
				Assertions.assertThat(data[i]).isEqualTo(DungeonTileSheet.FLOOR_SP);
				treads++;
			} else {
				Assertions.assertThat(data[i])
						.as("(%d,%d) is neither platform nor tread", x, y)
						.isEqualTo(SKIP);
			}
		}

		Assertions.assertThat(platform).as("a five by five platform").isEqualTo(25);
		Assertions.assertThat(treads).as("one step per approach").isEqualTo(4);
	}

	@Test
	@DisplayName("Every step tread sits on the walkway, so the altar is climbed from all four sides")
	void everyTreadSitsOnAnArmOfTheWalkway() {
		int found = 0;
		for (int y = EchoAltar.DISC_TOP; y < EchoAltar.DISC_TOP + EchoAltar.DISC_SPAN; y++) {
			for (int x = EchoAltar.DISC_LEFT; x < EchoAltar.DISC_LEFT + EchoAltar.DISC_SPAN; x++) {
				if (!EchoAltar.isTread(x, y)) {
					continue;
				}
				found++;
				Assertions.assertThat(EchoAltar.onCross(x, y))
						.as("tread (%d,%d) should sit on the walkway", x, y)
						.isTrue();
			}
		}
		Assertions.assertThat(found).isEqualTo(4);
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
