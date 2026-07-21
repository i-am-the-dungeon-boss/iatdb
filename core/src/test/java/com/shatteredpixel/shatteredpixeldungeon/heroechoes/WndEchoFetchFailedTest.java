package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupFailureKind;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEchoFetchFailed;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ExtendWith(GdxTestExtension.class)
class WndEchoFetchFailedTest {

	@Test
	@DisplayName("dialog message includes server HTTP failure hint")
	void dialogMessageIncludesServerHint() {
		EchoLookupOutcome failed = EchoLookupOutcome.error(EchoLookupFailureKind.SERVER, 503);
		String message = WndEchoFetchFailed.buildMessage(failed.failureHint());

		Assertions.assertThat(message).contains("HTTP 503");
		Assertions.assertThat(message).doesNotContainIgnoringCase("solo");
	}

	@Test
	@DisplayName("dialog message includes network failure hint")
	void dialogMessageIncludesNetworkHint() {
		EchoLookupOutcome failed = EchoLookupOutcome.error(EchoLookupFailureKind.NETWORK);
		String message = WndEchoFetchFailed.buildMessage(failed.failureHint());

		Assertions.assertThat(message).containsIgnoringCase("network");
	}

	@Test
	@DisplayName("dialog offers retry and abort, not continue solo")
	void dialogOffersRetryAndAbortNotContinueSolo() throws IOException {
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndEchoFetchFailed.java");
		Assertions.assertThat(source).contains("onAbort");
		Assertions.assertThat(source).doesNotContain("onContinueSolo");
		Assertions.assertThat(source).contains("\"abort\"");
		Assertions.assertThat(source).doesNotContain("continue_solo");
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
