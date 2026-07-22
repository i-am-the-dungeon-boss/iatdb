package com.shatteredpixel.shatteredpixeldungeon.scenes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class TitleSceneDebugModeTest {

	@Test
	@DisplayName("title scene offers debug mode gated on debug builds, without online gate")
	void titleSceneOffersDebugModeWithoutOnlineGate() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/TitleScene.java");

		Assertions.assertThat(source).contains("EchoPlayMode.DEBUG");
		Assertions.assertThat(source).contains("btnDebug");
		Assertions.assertThat(source).contains("DebugSettings.isDebugBuild()");
		Assertions.assertThat(source).contains("beginDebugRun");
		Assertions.assertThat(source).doesNotContain("beginDebugRun(EchoPlayMode");
		Assertions.assertThat(countOccurrences(source, "DebugSettings.isDebugBuild()")).isGreaterThanOrEqualTo(2);
	}

	private static int countOccurrences(String haystack, String needle) {
		int count = 0;
		int from = 0;
		while ((from = haystack.indexOf(needle, from)) >= 0) {
			count++;
			from += needle.length();
		}
		return count;
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
