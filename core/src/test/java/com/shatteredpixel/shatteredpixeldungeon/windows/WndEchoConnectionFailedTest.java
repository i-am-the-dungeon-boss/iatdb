package com.shatteredpixel.shatteredpixeldungeon.windows;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

class WndEchoConnectionFailedTest {

	@Test
	@DisplayName("retry option invokes onRetry")
	void retryOptionInvokesOnRetry() {
		AtomicInteger retries = new AtomicInteger();
		AtomicInteger dismisses = new AtomicInteger();
		WndEchoConnectionFailed.Listener listener = new WndEchoConnectionFailed.Listener() {
			@Override
			public void onRetry() {
				retries.incrementAndGet();
			}

			@Override
			public void onDismiss() {
				dismisses.incrementAndGet();
			}
		};

		WndEchoConnectionFailed.notifySelect(listener, 0);

		Assertions.assertThat(retries.get()).isEqualTo(1);
		Assertions.assertThat(dismisses.get()).isZero();
	}

	@Test
	@DisplayName("dismiss option invokes onDismiss")
	void dismissOptionInvokesOnDismiss() {
		AtomicInteger retries = new AtomicInteger();
		AtomicInteger dismisses = new AtomicInteger();
		WndEchoConnectionFailed.Listener listener = new WndEchoConnectionFailed.Listener() {
			@Override
			public void onRetry() {
				retries.incrementAndGet();
			}

			@Override
			public void onDismiss() {
				dismisses.incrementAndGet();
			}
		};

		WndEchoConnectionFailed.notifySelect(listener, 1);

		Assertions.assertThat(dismisses.get()).isEqualTo(1);
		Assertions.assertThat(retries.get()).isZero();
	}
}
