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
	@DisplayName("creating an account does not show the privacy policy")
	void accountCreationOmitsPrivacyPolicy() throws IOException {
		String live = liveSource(readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoPlayerAuthGate.java"));

		Assertions.assertThat(live).doesNotContain("auth_terms");
		Assertions.assertThat(live).doesNotContain("privacy_continue");
	}

	@Test
	@DisplayName("username prompt has no copy or paste buttons")
	void usernamePromptHasNoClipboardButtons() throws IOException {
		String gate = liveSource(readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoPlayerAuthGate.java"));
		String input = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndTextInput.java");

		Assertions.assertThat(gate).contains("fieldError,\n\t\t\t\tfalse)");
		Assertions.assertThat(input).contains("if (clipboard)");
	}

	@Test
	@DisplayName("creating an account asks for a username and not an email")
	void accountCreationDoesNotAskForEmail() throws IOException {
		String raw = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoPlayerAuthGate.java");
		String live = liveSource(raw);

		Assertions.assertThat(live).doesNotContain("auth_email_hint");
		Assertions.assertThat(live).doesNotContain("looksLikeEmail");
		Assertions.assertThat(live).doesNotContain("setCredentials");
		Assertions.assertThat(raw).contains("auth_email_hint");
		Assertions.assertThat(raw).contains("looksLikeEmail");
	}

	@Test
	@DisplayName("text input can show a red field error under the box")
	void textInputSupportsFieldError() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndTextInput.java");

		Assertions.assertThat(source).contains("errorMessage");
		Assertions.assertThat(source).contains("CharSprite.NEGATIVE");
	}

	@Test
	@DisplayName("text input supports an optional secondary field under the primary box")
	void textInputSupportsSecondaryField() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndTextInput.java");

		Assertions.assertThat(source).contains("secondaryBox");
		Assertions.assertThat(source).contains("onSelect(boolean positive, String text, String secondary)");
	}

	@Test
	@DisplayName("TextInput claimKeyboardFocus clears the previous owner's stage focus")
	void textInputClaimKeyboardFocusClearsPreviousOwner() throws IOException {
		String source = readSource("SPD-classes/src/main/java/com/watabou/noosa/TextInput.java");

		Assertions.assertThat(source).contains("claimKeyboardFocus");
		Assertions.assertThat(source).contains("setKeyboardFocus(null)");
		Assertions.assertThat(source).contains("FocusListener");
	}

	@Test
	@DisplayName("auth secondary field does not keep keyboard focus when the dialog opens")
	void secondaryFieldDoesNotStealInitialFocus() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndTextInput.java");

		Assertions.assertThat(source).contains("textBox.claimKeyboardFocus()");
	}

	/** Drops full-line // and # comments so paused email text can stay in the file. */
	private static String liveSource(String source) {
		StringBuilder out = new StringBuilder();
		for (String line : source.split("\n", -1)) {
			String trimmed = line.trim();
			if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
				continue;
			}
			out.append(line).append('\n');
		}
		return out.toString();
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
