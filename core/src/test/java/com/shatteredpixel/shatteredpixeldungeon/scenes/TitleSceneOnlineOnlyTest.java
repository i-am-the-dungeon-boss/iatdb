package com.shatteredpixel.shatteredpixeldungeon.scenes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class TitleSceneOnlineOnlyTest {

	@Test
	@DisplayName("title scene offers solo and ranked, both gated on backend online")
	void titleSceneOffersSoloAndRankedGatedOnOnline() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/TitleScene.java");

		Assertions.assertThat(source).contains("EchoPlayMode.RANKED");
		Assertions.assertThat(source).contains("EchoPlayMode.SOLO");
		Assertions.assertThat(source).contains("btnSolo");
		Assertions.assertThat(source).contains("btnRanked.enable(alpha != 0 && online)");
		Assertions.assertThat(source).contains("btnSolo.enable(alpha != 0 && online)");
		Assertions.assertThat(source).contains("EchoPlayerAuthGate.ensureReadyThen");
		Assertions.assertThat(source).doesNotContain("mode == EchoPlayMode.RANKED");
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
