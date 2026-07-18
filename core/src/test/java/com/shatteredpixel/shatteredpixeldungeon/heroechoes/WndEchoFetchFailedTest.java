package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupFailureKind;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEchoFetchFailed;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class WndEchoFetchFailedTest {

	@Test
	@DisplayName("dialog message includes server HTTP failure hint")
	void dialogMessageIncludesServerHint() {
		EchoLookupOutcome failed = EchoLookupOutcome.error(EchoLookupFailureKind.SERVER, 503);
		String message = WndEchoFetchFailed.buildMessage(failed.failureHint());

		Assertions.assertThat(message).contains("HTTP 503");
		Assertions.assertThat(message).containsIgnoringCase("solo");
	}

	@Test
	@DisplayName("dialog message includes network failure hint")
	void dialogMessageIncludesNetworkHint() {
		EchoLookupOutcome failed = EchoLookupOutcome.error(EchoLookupFailureKind.NETWORK);
		String message = WndEchoFetchFailed.buildMessage(failed.failureHint());

		Assertions.assertThat(message).containsIgnoringCase("network");
	}
}
