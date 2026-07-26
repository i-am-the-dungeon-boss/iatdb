package com.shatteredpixel.shatteredpixeldungeon.services.updates;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(GdxTestExtension.class)
class UpdatesImmediateCheckTest {

	@AfterEach
	void clearUpdates() {
		Updates.service = null;
		Updates.clearUpdate();
	}

	@Test
	@DisplayName("immediate check returns update data when service reports available")
	void immediateCheckReturnsUpdateWhenAvailable() {
		AvailableUpdateData remote = new AvailableUpdateData();
		remote.versionName = "9.9.9";
		Updates.service = immediateService(remote);

		AvailableUpdateData result = Updates.checkForUpdateImmediate();

		Assertions.assertThat(result).isSameAs(remote);
		Assertions.assertThat(Updates.updateAvailable()).isTrue();
		Assertions.assertThat(Updates.updateData().versionName).isEqualTo("9.9.9");
	}

	@Test
	@DisplayName("immediate check returns null when no update found")
	void immediateCheckReturnsNullWhenCurrent() {
		Updates.service = immediateService(null);

		Assertions.assertThat(Updates.checkForUpdateImmediate()).isNull();
		Assertions.assertThat(Updates.updateAvailable()).isFalse();
	}

	@Test
	@DisplayName("immediate check returns null when connection fails")
	void immediateCheckReturnsNullOnConnectionFailure() {
		Updates.service = failingService();

		Assertions.assertThat(Updates.checkForUpdateImmediate()).isNull();
		Assertions.assertThat(Updates.updateAvailable()).isFalse();
	}

	@Test
	@DisplayName("immediate check ignores recent lastCheck delay")
	void immediateCheckIgnoresCheckDelay() {
		AtomicInteger checks = new AtomicInteger();
		AvailableUpdateData remote = new AvailableUpdateData();
		remote.versionName = "9.9.9";
		Updates.service = countingService(checks, remote);

		Updates.checkForUpdate();
		Assertions.assertThat(checks.get()).isEqualTo(1);

		AvailableUpdateData result = Updates.checkForUpdateImmediate();

		Assertions.assertThat(checks.get()).isEqualTo(2);
		Assertions.assertThat(result).isSameAs(remote);
	}

	@Test
	@DisplayName("immediate check returns null when update prompts unsupported")
	void immediateCheckSkippedWithoutService() {
		Updates.service = null;

		Assertions.assertThat(Updates.checkForUpdateImmediate()).isNull();
	}

	private static UpdateService immediateService(AvailableUpdateData update) {
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
				if (update != null) {
					callback.onUpdateAvailable(update);
				} else {
					callback.onNoUpdateFound();
				}
			}

			@Override
			public void initializeUpdate(AvailableUpdateData data) {
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

	private static UpdateService failingService() {
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
				callback.onConnectionFailed();
			}

			@Override
			public void initializeUpdate(AvailableUpdateData data) {
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

	private static UpdateService countingService(AtomicInteger checks, AvailableUpdateData update) {
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
				callback.onUpdateAvailable(update);
			}

			@Override
			public void initializeUpdate(AvailableUpdateData data) {
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
