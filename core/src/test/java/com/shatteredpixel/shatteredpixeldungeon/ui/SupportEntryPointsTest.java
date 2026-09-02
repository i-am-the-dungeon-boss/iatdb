package com.shatteredpixel.shatteredpixeldungeon.ui;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Support screen is reachable on desktop, Android and iOS because it always
 * offers the Ko-fi link; only the Play tip buttons stay behind billing.
 */
class SupportEntryPointsTest {

	@Test
	@DisplayName("title screen shows the support button on every platform")
	void titleSceneShowsSupportEverywhere() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/TitleScene.java");
		Assertions.assertThat(source).contains("SupportPrompts.supportSceneEnabled()");
		Assertions.assertThat(source).doesNotContain("SupportPrompts.playBillingEnabled()");
	}

	@Test
	@DisplayName("victory window offers support on every platform")
	void victoryWindowShowsSupportEverywhere() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndVictoryCongrats.java");
		Assertions.assertThat(source).contains("SupportPrompts.supportSceneEnabled()");
		Assertions.assertThat(source).doesNotContain("SupportPrompts.playBillingEnabled()");
	}

	@Test
	@DisplayName("supporter scene keeps Play tip buttons behind billing availability")
	void supporterSceneGatesTipButtons() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/SupporterScene.java");
		Assertions.assertThat(source).contains("SupportPrompts.playBillingEnabled()");
		Assertions.assertThat(source).contains("ProjectLinks.KOFI_URL");
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
