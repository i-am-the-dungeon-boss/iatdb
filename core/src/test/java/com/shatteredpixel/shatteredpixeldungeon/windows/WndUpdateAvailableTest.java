package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.AvailableUpdateData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(GdxTestExtension.class)
class WndUpdateAvailableTest {

	@Test
	@DisplayName("update option invokes onUpdate")
	void updateOptionInvokesOnUpdate() {
		AtomicInteger updates = new AtomicInteger();
		WndUpdateAvailable.notifySelect(() -> updates.incrementAndGet(), 0);

		Assertions.assertThat(updates.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("non-update option does not invoke onUpdate")
	void nonUpdateOptionDoesNotInvokeOnUpdate() {
		AtomicInteger updates = new AtomicInteger();
		WndUpdateAvailable.notifySelect(() -> updates.incrementAndGet(), 1);

		Assertions.assertThat(updates.get()).isZero();
	}

	@Test
	@DisplayName("back press does not dismiss update prompt")
	void backPressDoesNotDismiss() {
		Assertions.assertThat(WndUpdateAvailable.allowsBackDismiss()).isFalse();
	}

	@Test
	@DisplayName("update button does not close the forced update window")
	void updateButtonDoesNotCloseWindow() {
		Assertions.assertThat(WndUpdateAvailable.allowsHide()).isFalse();
	}

	@Test
	@DisplayName("body shows installed and required version numbers")
	void bodyShowsInstalledAndRequiredVersions() {
		AvailableUpdateData update = new AvailableUpdateData();
		update.versionName = "0.0.2";

		String body = WndUpdateAvailable.bodyFor(update, "0.0.1");

		Assertions.assertThat(body).contains("0.0.1");
		Assertions.assertThat(body).contains("0.0.2");
	}
}
