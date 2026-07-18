package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.services.updates.AvailableUpdateData;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.UpdateService;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.Updates;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TitleFeedButtonsTest {

	@AfterEach
	void clearUpdates() {
		Updates.service = null;
		Updates.clearUpdate();
	}

	@Test
	@DisplayName("title News and Changes buttons hidden when updates unsupported")
	void hiddenWithoutUpdateService() {
		Updates.service = null;
		Assertions.assertThat(TitleFeedButtons.visible()).isFalse();
	}

	@Test
	@DisplayName("title News and Changes buttons hidden even when update prompts supported")
	void hiddenWithUpdatePrompts() {
		Updates.service = new UpdateService() {
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
		Assertions.assertThat(TitleFeedButtons.visible()).isFalse();
	}
}
