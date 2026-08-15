package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Dungeon world chat is opt-in per session.
 *
 * <p>It is deliberately not a persisted preference: joining a public channel is
 * a choice worth making knowingly, and a flag saved once would keep a player in
 * chat on every future launch without ever asking again. The village channel is
 * separate and unaffected.
 */
@ExtendWith(GdxTestExtension.class)
@DisplayName("Dungeon world chat session scope")
class WorldChatSessionScopeTest {

	@AfterEach
	void cleanup() {
		SPDSettings.worldChat(false);
	}

	@Test
	@DisplayName("stays off even if an older build left the flag enabled on disk")
	void ignoresAPersistedValue() {
		// Poisoning storage first is what makes this meaningful: asserting the
		// default alone would pass simply because some earlier test happened to
		// leave `false` behind.
		SPDSettings.put(SPDSettings.KEY_WORLD_CHAT, true);

		Assertions.assertThat(SPDSettings.worldChat()).isFalse();
	}

	@Test
	@DisplayName("can be turned on and off again within a session")
	void togglesWithinTheSession() {
		SPDSettings.worldChat(true);
		Assertions.assertThat(SPDSettings.worldChat()).isTrue();

		SPDSettings.worldChat(false);
		Assertions.assertThat(SPDSettings.worldChat()).isFalse();
	}

	@Test
	@DisplayName("is held in memory rather than written to preferences")
	void isNeverPersisted() throws IOException {
		// A behavioural test cannot see the difference — the class would have to
		// be reloaded to simulate a relaunch. What actually guarantees the reset
		// is that the value never reaches storage, so that is what is asserted.
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/SPDSettings.java");
		String accessors = source.substring(
				source.indexOf("public static void worldChat(boolean value)"),
				source.indexOf("worldChatMuted"));

		Assertions.assertThat(accessors).doesNotContain("put(");
		Assertions.assertThat(accessors).doesNotContain("getBoolean(");
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
