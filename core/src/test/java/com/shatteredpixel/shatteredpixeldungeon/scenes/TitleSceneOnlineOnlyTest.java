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
	@DisplayName("title scene offers village, solo and ranked, all gated on backend online and the auth gate")
	void titleSceneOffersVillageGatedOnOnline() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/TitleScene.java");

		// Village leads, with the two direct-to-run shortcuts beside it.
		Assertions.assertThat(source).contains("btnVillage");
		Assertions.assertThat(source).contains("btnSolo");
		Assertions.assertThat(source).contains("btnRanked");
		Assertions.assertThat(source).contains("btnVillage.enable(alpha != 0 && online)");
		Assertions.assertThat(source).contains("btnSolo.enable(alpha != 0 && online)");
		Assertions.assertThat(source).contains("btnRanked.enable(alpha != 0 && online)");

		// the shortcuts commit through the same entry point the dungeon mouth uses,
		// so a run starts identically however it was reached
		Assertions.assertThat(source).contains("VillageGateway::enterVillage");
		Assertions.assertThat(source).contains("VillageGateway.beginRun(mode)");

		// but the game still requires a reachable backend and an identified player
		Assertions.assertThat(source).contains("EchoBackendProbe.isOnlineReady()");
		Assertions.assertThat(source).contains("showOfflineConnectionDialog()");
		Assertions.assertThat(source).contains("EchoPlayerAuthGate.ensureReadyThen");
	}

	@Test
	@DisplayName("village is the first button in the play row")
	void villageIsLaidOutFirst() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/TitleScene.java");
		String playRow = source.substring(
				source.indexOf("private void layoutPlayModeButtons("),
				source.indexOf("private void enterVillage("));

		// Village anchors the block: it is placed first, at `left`, and in portrait
		// the shortcut row is offset from its bottom rather than sharing its row.
		Assertions.assertThat(playRow).contains("btnVillage.setRect(left, top,");
		// the same gap the rest of the menu uses, so every row is evenly spaced
		Assertions.assertThat(playRow).contains("btnVillage.bottom() + gap");
		Assertions.assertThat(playRow).doesNotContain("PLAY_ROW_GAP");
		Assertions.assertThat(playRow).doesNotContain("btnRanked.setRect(left,");
		Assertions.assertThat(playRow.indexOf("btnVillage.setRect"))
				.isLessThan(playRow.indexOf("btnSolo.setRect"));
		Assertions.assertThat(playRow.indexOf("btnSolo.setRect"))
				.isLessThan(playRow.indexOf("btnRanked.setRect"));
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
