package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;

/**
 * Ranked echo prefetch failed after auto-retries. User must Retry or Continue
 * solo.
 */
public class WndEchoFetchFailed extends WndOptions {

	public interface Listener {
		void onRetry();

		void onContinueSolo();
	}

	private final Listener listener;

	public WndEchoFetchFailed(Listener listener) {
		this(listener, "");
	}

	public WndEchoFetchFailed(Listener listener, String failureHint) {
		super(
				Icons.get(Icons.WARNING),
				Messages.get(WndEchoFetchFailed.class, "title"),
				buildMessage(failureHint),
				Messages.get(WndEchoFetchFailed.class, "retry"),
				Messages.get(WndEchoFetchFailed.class, "continue_solo"));
		this.listener = listener;
	}

	public static String buildMessage(String failureHint) {
		if (failureHint == null || failureHint.isBlank()) {
			return Messages.get(WndEchoFetchFailed.class, "message");
		}
		return Messages.get(WndEchoFetchFailed.class, "message_with_reason", failureHint);
	}

	@Override
	protected void onSelect(int index) {
		if (index == 0) {
			listener.onRetry();
		} else {
			listener.onContinueSolo();
		}
	}

	@Override
	public void onBackPressed() {
		// Require an explicit choice.
	}
}
