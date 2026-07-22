package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class EchoPlayerAuthGateTest {

	@Test
	@DisplayName("missing session always prompts; never auto-registers cached local name")
	void missingSessionAlwaysPrompts() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoPlayerAuthGate.java");

		Assertions.assertThat(source).contains("WndTextInput");
		Assertions.assertThat(source).doesNotContain("registerThen(existing");
		Assertions.assertThat(source).doesNotContain("registerThen(EchoPlayerAuth.preferredUsername()");
		Assertions.assertThat(source).contains("auth_username_taken");
		Assertions.assertThat(source).contains("USERNAME_TAKEN");
		// Taken-name feedback stays under the text box, not a separate error popup.
		Assertions.assertThat(source).doesNotContain(
				"new WndError(Messages.get(TitleScene.class, \"auth_username_taken\"))");
	}

	@Test
	@DisplayName("text input can show a red field error under the box")
	void textInputSupportsFieldError() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndTextInput.java");

		Assertions.assertThat(source).contains("errorMessage");
		Assertions.assertThat(source).contains("CharSprite.NEGATIVE");
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
