package com.shatteredpixel.shatteredpixeldungeon.windows;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Walking up the stairs on depth 1 ends the trip into the dungeon, so it asks
 * first rather than acting on a single step in the wrong direction.
 */
class WndLeaveDungeonTest {

	@Test
	@DisplayName("only the leave option walks home; anything else stays in the dungeon")
	void onlyLeaveWalksHome() {
		Assertions.assertThat(WndLeaveDungeon.leavesOn(WndLeaveDungeon.LEAVE)).isTrue();
		Assertions.assertThat(WndLeaveDungeon.leavesOn(WndLeaveDungeon.STAY)).isFalse();
		// WndOptions reports a dismissed window as an out-of-range index
		Assertions.assertThat(WndLeaveDungeon.leavesOn(-1)).isFalse();
		Assertions.assertThat(WndLeaveDungeon.leavesOn(99)).isFalse();
	}

	@Test
	@DisplayName("the depth-1 surface stair asks before walking home")
	void sewerStairAsksFirst() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/SewerLevel.java");
		String surfaceBranch = source.substring(
				source.indexOf("if (transition.type == LevelTransition.Type.SURFACE)"),
				source.indexOf("Statistics.ascended = true"));

		// the prompt replaces the immediate walk home, and the transition is
		// refused so the hero stays put until the player answers
		Assertions.assertThat(surfaceBranch).contains("new WndLeaveDungeon()");
		Assertions.assertThat(surfaceBranch).doesNotContain("VillageGateway.returnToVillage()");
		Assertions.assertThat(surfaceBranch).contains("return false;");
	}

	private static String readSource(String relativePath) throws IOException {
		Path dir = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			Path candidate = dir.resolve(relativePath);
			if (Files.isRegularFile(candidate)) {
				return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
			}
			dir = dir.getParent();
		}
		throw new AssertionError("Could not find " + relativePath);
	}
}
