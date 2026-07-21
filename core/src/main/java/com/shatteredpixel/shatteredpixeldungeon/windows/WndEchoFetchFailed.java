package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;

/**
 * Echo prefetch failed after auto-retries. User must Retry or Abort.
 */
public class WndEchoFetchFailed extends WndOptions {

	public interface Listener {
		void onRetry();

		void onAbort();
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
				Messages.get(WndEchoFetchFailed.class, "abort"));
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
			listener.onAbort();
		}
	}

	@Override
	public void onBackPressed() {
		// Require an explicit choice.
	}
}
