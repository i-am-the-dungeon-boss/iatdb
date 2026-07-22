package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class WndGameDebugToolsTest {

	@AfterEach
	void cleanup() {
		DebugSettings.resetForTests();
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("pause-menu echo debug tools are hidden in release builds")
	void echoDebugToolsHiddenInReleaseBuilds() {
		DebugSettings.setDebugBuildOverride(false);
		Assertions.assertThat(WndGame.showsEchoDebugTools()).isFalse();
	}

	@Test
	@DisplayName("pause-menu echo debug tools are available in debug builds")
	void echoDebugToolsAvailableInDebugBuilds() {
		DebugSettings.setDebugBuildOverride(true);
		Assertions.assertThat(WndGame.showsEchoDebugTools()).isTrue();
	}

	@Test
	@DisplayName("pause menu includes stop-echo-hunting action in debug builds")
	void pauseMenuIncludesStopEchoHuntingInDebugBuilds() throws Exception {
		String source = java.nio.file.Files.readString(
				findSource("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndGame.java"));
		Assertions.assertThat(source).contains("stop_echo_hunting");
		Assertions.assertThat(source).contains("EchoBoss.stopAllHunting");
		Assertions.assertThat(source).contains("showsEchoDebugTools()");
	}

	@Test
	@DisplayName("pause menu includes restock-ground-items action in debug builds")
	void pauseMenuIncludesRestockGroundItemsInDebugBuilds() throws Exception {
		String source = java.nio.file.Files.readString(
				findSource("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndGame.java"));
		Assertions.assertThat(source).contains("restock_ground_items");
		Assertions.assertThat(source).contains("DebugArenaItems.restockGround");
	}

	@Test
	@DisplayName("pause menu includes give-echo-arsenal action in debug builds")
	void pauseMenuIncludesGiveEchoArsenalInDebugBuilds() throws Exception {
		String source = java.nio.file.Files.readString(
				findSource("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndGame.java"));
		Assertions.assertThat(source).contains("give_echo_arsenal");
		Assertions.assertThat(source).contains("DebugEchoArsenal.grantAndCycleAll");
	}

	@Test
	@DisplayName("settings debug checkboxes follow debug build flag")
	void settingsDebugCheckboxesFollowDebugBuildFlag() {
		DebugSettings.setDebugBuildOverride(false);
		Assertions.assertThat(DebugSettings.isDebugBuild()).isFalse();

		DebugSettings.setDebugBuildOverride(true);
		Assertions.assertThat(DebugSettings.isDebugBuild()).isTrue();
	}

	private static java.nio.file.Path findSource(String relativePath) throws java.io.IOException {
		java.nio.file.Path dir = java.nio.file.Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			java.nio.file.Path candidate = dir.resolve(relativePath);
			if (java.nio.file.Files.isRegularFile(candidate)) {
				return candidate;
			}
			dir = dir.getParent();
		}
		throw new AssertionError("Could not find " + relativePath);
	}
}
