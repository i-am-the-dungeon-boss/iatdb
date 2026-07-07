package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoLeaderboardEntry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class WndLeaderboardTest {

	@Test
	@DisplayName("cancelled fetch lifecycle is inactive")
	void cancelledFetchLifecycleIsInactive() {
		WndLeaderboard.OnlineFetchLifecycle lifecycle = new WndLeaderboard.OnlineFetchLifecycle();
		lifecycle.cancel();

		Assertions.assertThat(lifecycle.isActive()).isFalse();
	}

	@Test
	@DisplayName("applyOnlineLeaderboard returns before touching UI when lifecycle cancelled")
	void applyOnlineLeaderboardReturnsEarlyWhenCancelled() {
		WndLeaderboard.OnlineFetchLifecycle lifecycle = new WndLeaderboard.OnlineFetchLifecycle();
		lifecycle.cancel();

		EchoLeaderboardEntry entry = new EchoLeaderboardEntry(
				1, "echo-1", true, 5, "WARRIOR", 100, 20, 50, 1f
		);

		Assertions.assertThatCode(() ->
				WndLeaderboard.applyOnlineLeaderboard(lifecycle, null, 5, List.of(entry), null)
		).doesNotThrowAnyException();
	}
}
