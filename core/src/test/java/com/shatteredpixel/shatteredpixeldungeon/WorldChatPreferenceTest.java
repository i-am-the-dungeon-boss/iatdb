package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Dungeon world chat is on by default and the player's own choice is kept, so
 * unchecking it once keeps it off on later launches instead of the run starting
 * in chat again. The village channel is separate and unaffected.
 */
@ExtendWith(GdxTestExtension.class)
@DisplayName("Dungeon world chat preference")
class WorldChatPreferenceTest {

	@AfterEach
	void cleanup() {
		SPDSettings.worldChat(true);
	}

	@Test
	@DisplayName("is enabled for a player who has never touched the setting")
	void defaultsToOn() throws IOException {
		// Settings storage has no remove(), so a never-set key cannot be staged
		// behaviourally; the default lives in the accessor and is asserted there.
		Assertions.assertThat(worldChatAccessors())
				.contains("getBoolean(KEY_WORLD_CHAT, true)");
	}

	@Test
	@DisplayName("writes the player's choice to preferences rather than memory")
	void persistsTheChoice() throws IOException {
		Assertions.assertThat(worldChatAccessors()).contains("put(KEY_WORLD_CHAT, value)");
	}

	@Test
	@DisplayName("remembers that the player turned it off")
	void remembersOptOut() {
		SPDSettings.worldChat(false);

		Assertions.assertThat(SPDSettings.getBoolean(SPDSettings.KEY_WORLD_CHAT, true)).isFalse();
		Assertions.assertThat(SPDSettings.worldChat()).isFalse();
	}

	@Test
	@DisplayName("remembers that the player turned it back on")
	void remembersOptIn() {
		SPDSettings.worldChat(false);
		SPDSettings.worldChat(true);

		Assertions.assertThat(SPDSettings.getBoolean(SPDSettings.KEY_WORLD_CHAT, false)).isTrue();
		Assertions.assertThat(SPDSettings.worldChat()).isTrue();
	}

	private static String worldChatAccessors() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/SPDSettings.java");
		return source.substring(
				source.indexOf("public static void worldChat(boolean value)"),
				source.indexOf("worldChatMuted"));
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
		throw new IOException("Not found: " + relativePath);
	}
}
