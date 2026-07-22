package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.AvailableUpdateData;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.UpdateService;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.Updates;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(GdxTestExtension.class)
class TitleUpdateNudgeTest {

	@AfterEach
	void clearUpdates() {
		Updates.service = null;
		Updates.clearUpdate();
	}

	@Test
	@DisplayName("shows update nudge when update available and not yet shown")
	void showsWhenAvailableAndNotShown() {
		Assertions.assertThat(TitleScene.shouldShowUpdateNudge(true, false)).isTrue();
	}

	@Test
	@DisplayName("does not show update nudge when already shown")
	void hidesWhenAlreadyShown() {
		Assertions.assertThat(TitleScene.shouldShowUpdateNudge(true, true)).isFalse();
	}

	@Test
	@DisplayName("does not show update nudge when no update available")
	void hidesWhenNoUpdate() {
		Assertions.assertThat(TitleScene.shouldShowUpdateNudge(false, false)).isFalse();
	}

	@Test
	@DisplayName("requests update check when updates enabled")
	void requestsCheckWhenEnabled() {
		AtomicInteger checks = new AtomicInteger();
		Updates.service = trackingService(checks);

		TitleScene.requestUpdateCheckIfEnabled(true);

		Assertions.assertThat(checks.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("skips update check when updates disabled")
	void skipsCheckWhenDisabled() {
		AtomicInteger checks = new AtomicInteger();
		Updates.service = trackingService(checks);

		TitleScene.requestUpdateCheckIfEnabled(false);

		Assertions.assertThat(checks.get()).isZero();
	}

	private static UpdateService trackingService(AtomicInteger checks) {
		return new UpdateService() {
			@Override
			public boolean supportsUpdatePrompts() {
				return true;
			}

			@Override
			public boolean supportsBetaChannel() {
				return false;
			}

			@Override
			public void checkForUpdate(boolean useMetered, boolean includeBetas, UpdateResultCallback callback) {
				checks.incrementAndGet();
				callback.onNoUpdateFound();
			}

			@Override
			public void initializeUpdate(AvailableUpdateData update) {
			}

			@Override
			public boolean supportsReviews() {
				return false;
			}

			@Override
			public void initializeReview(ReviewResultCallback callback) {
				callback.onComplete();
			}

			@Override
			public void openReviewURI() {
			}
		};
	}
}
