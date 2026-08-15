package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

import org.assertj.core.api.Assertions;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TitleVillageIconTest {

	@Test
	@DisplayName("village icon is its own asset, not the scrolling arch texture")
	void villageIconUsesItsOwnAsset() {
		Assertions.assertThat(TitleVillageIcon.asset()).isEqualTo(Assets.Interfaces.VILLAGE);
		Assertions.assertThat(TitleVillageIcon.asset()).isNotEqualTo(Assets.Interfaces.ARCS_FG);
	}

	@Test
	@DisplayName("the village asset is the city wall tile the village is built from, framed")
	void villageAssetIsTheCityWallTile() throws IOException {
		BufferedImage village = ImageIO.read(asset("core/src/main/assets/interfaces/village.png"));
		BufferedImage city = ImageIO.read(asset("core/src/main/assets/environment/tiles_city.png"));

		Assertions.assertThat(village.getWidth()).isEqualTo(16);
		Assertions.assertThat(village.getHeight()).isEqualTo(16);

		// FLAT_WALL is tile (1,4) of a 16-wide sheet of 16px tiles, so its top-left
		// pixel is (0, 48). VillageLevel.tilesTex() is this same city sheet, so the
		// button shows exactly the stone the village walls are drawn in.
		for (int y = 1; y < 15; y++) {
			for (int x = 1; x < 15; x++) {
				Assertions.assertThat(village.getRGB(x, y))
						.as("pixel %d,%d", x, y)
						.isEqualTo(city.getRGB(x, 48 + y));
			}
		}

		// framed like the other title icons: an opaque outline ring, open corners
		int outline = village.getRGB(1, 0);
		Assertions.assertThat(outline >>> 24).isEqualTo(255);
		for (int i = 1; i < 15; i++) {
			Assertions.assertThat(village.getRGB(i, 0)).isEqualTo(outline);
			Assertions.assertThat(village.getRGB(i, 15)).isEqualTo(outline);
			Assertions.assertThat(village.getRGB(0, i)).isEqualTo(outline);
			Assertions.assertThat(village.getRGB(15, i)).isEqualTo(outline);
		}
		Assertions.assertThat(village.getRGB(0, 0) >>> 24).isZero();
		Assertions.assertThat(village.getRGB(15, 15) >>> 24).isZero();
	}

	private static File asset(String relativePath) {
		Path dir = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			Path candidate = dir.resolve(relativePath);
			if (Files.isRegularFile(candidate)) {
				return candidate.toFile();
			}
			dir = dir.getParent();
		}
		throw new AssertionError("Could not find " + relativePath);
	}

	@Test
	@DisplayName("a texture already at icon size is left alone")
	void iconSizedTextureIsNotScaled() {
		Assertions.assertThat(TitleVillageIcon.scaleFor(16f)).isEqualTo(1f);
	}

	@Test
	@DisplayName("an empty texture never yields a divide-by-zero scale")
	void emptyTextureFallsBackToOne() {
		Assertions.assertThat(TitleVillageIcon.scaleFor(0f)).isEqualTo(1f);
	}
}
