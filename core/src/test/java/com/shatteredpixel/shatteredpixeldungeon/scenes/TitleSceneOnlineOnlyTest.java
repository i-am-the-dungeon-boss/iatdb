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
	@DisplayName("title scene offers the village, gated on backend online and the auth gate")
	void titleSceneOffersVillageGatedOnOnline() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/TitleScene.java");

		// Play mode is now chosen at the dungeon mouth, not here.
		Assertions.assertThat(source).contains("btnVillage");
		Assertions.assertThat(source).contains("btnVillage.enable(alpha != 0 && online)");
		Assertions.assertThat(source).contains("VillageGateway::enterVillage");
		Assertions.assertThat(source).doesNotContain("btnRanked");
		Assertions.assertThat(source).doesNotContain("btnSolo");

		// but the game still requires a reachable backend and an identified player
		Assertions.assertThat(source).contains("EchoBackendProbe.isOnlineReady()");
		Assertions.assertThat(source).contains("showOfflineConnectionDialog()");
		Assertions.assertThat(source).contains("EchoPlayerAuthGate.ensureReadyThen");
	}

	@Test
	@DisplayName("the solo/ranked choice lives in the dungeon-mouth prompt")
	void modeChoiceMovedToTheDungeonMouth() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndDungeonMode.java");

		Assertions.assertThat(source).contains("EchoPlayMode.SOLO");
		Assertions.assertThat(source).contains("EchoPlayMode.RANKED");
		Assertions.assertThat(source).contains("EchoPlayerAuthGate.ensureReadyThen");
		Assertions.assertThat(source).contains("VillageGateway.beginRun");
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
