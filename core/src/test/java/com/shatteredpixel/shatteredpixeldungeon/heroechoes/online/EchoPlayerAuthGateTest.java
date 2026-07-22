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
	@DisplayName("auth prompt includes optional email and never asks for password")
	void authPromptIncludesOptionalEmailWithoutPassword() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoPlayerAuthGate.java");

		Assertions.assertThat(source).contains("auth_email_hint");
		Assertions.assertThat(source).contains("onSelect(boolean positive, String text, String email)");
		Assertions.assertThat(source).doesNotContain("promptForPassword");
		Assertions.assertThat(source).doesNotContain("setCredentials");
		Assertions.assertThat(source).doesNotContain("auth_password");
	}

	@Test
	@DisplayName("accepts emails with local part, at-sign, and dotted domain")
	void looksLikeEmailAcceptsBasicAddresses() {
		Assertions.assertThat(EchoPlayerAuthGate.looksLikeEmail("a@b.co")).isTrue();
		Assertions.assertThat(EchoPlayerAuthGate.looksLikeEmail("hero@example.com")).isTrue();
	}

	@Test
	@DisplayName("rejects blank or malformed emails")
	void looksLikeEmailRejectsInvalid() {
		Assertions.assertThat(EchoPlayerAuthGate.looksLikeEmail("")).isFalse();
		Assertions.assertThat(EchoPlayerAuthGate.looksLikeEmail("not-an-email")).isFalse();
		Assertions.assertThat(EchoPlayerAuthGate.looksLikeEmail("@missing.local")).isFalse();
		Assertions.assertThat(EchoPlayerAuthGate.looksLikeEmail("no@tld")).isFalse();
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
