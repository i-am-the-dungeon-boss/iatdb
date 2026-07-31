package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.services.updates.AvailableUpdateData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class InterlevelEchoPrefetchUpdateGateTest {

	@Test
	@DisplayName("blocks echo prefetch when an update is required")
	void blocksWhenUpdateRequired() {
		AvailableUpdateData update = new AvailableUpdateData();
		update.versionName = "9.9.9";

		Assertions.assertThat(InterlevelScene.shouldBlockEchoPrefetchForUpdate(update)).isTrue();
	}

	@Test
	@DisplayName("does not block echo prefetch when game version is current")
	void allowsWhenNoUpdate() {
		Assertions.assertThat(InterlevelScene.shouldBlockEchoPrefetchForUpdate(null)).isFalse();
	}

	@Test
	@DisplayName("echo prefetch checks game version and shows unpassable update prompt")
	void prefetchWiresForcedUpdatePrompt() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/InterlevelScene.java");

		Assertions.assertThat(source).contains("checkForUpdateImmediate");
		Assertions.assertThat(source).contains("shouldBlockEchoPrefetchForUpdate");
		Assertions.assertThat(source).contains("WndUpdateAvailable");
		Assertions.assertThat(source).contains("promptForcedUpdate");
	}

	@Test
	@DisplayName("generated boss floors still prefetch before loadLevel")
	void generatedBossFloorsStillPrefetch() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/InterlevelScene.java");

		Assertions.assertThat(source).contains("loadLevelWithEchoPrefetch");
		Assertions.assertThat(source).contains(
				"level = loadLevelWithEchoPrefetch(Dungeon.depth, Dungeon.branch)");
		Assertions.assertThat(source).contains(
				"if (!level.locked) {\n\t\t\t\t\tprefetchEchoBossIfNeeded");
	}

	private static String readSource(String relativePath) throws IOException {
		Path dir = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			Path candidate = dir.resolve(relativePath);
			if (Files.isRegularFile(candidate)) {
				return Files.readString(candidate, StandardCharsets.UTF_8);
			}
			dir = dir.getParent();
		}
		throw new AssertionError("Could not find " + relativePath);
	}
}
