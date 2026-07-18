package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;

/**
 * Title-screen backend health probe failed. User may Retry or Dismiss.
 */
public class WndEchoConnectionFailed extends WndOptions {

	public interface Listener {
		void onRetry();

		void onDismiss();
	}

	private final Listener listener;

	public WndEchoConnectionFailed(String message, Listener listener) {
		super(
				Icons.get(Icons.WARNING),
				Messages.get(WndEchoConnectionFailed.class, "title"),
				message,
				Messages.get(WndEchoConnectionFailed.class, "retry"),
				Messages.get(WndEchoConnectionFailed.class, "dismiss"));
		this.listener = listener;
	}

	@Override
	protected void onSelect(int index) {
		notifySelect(listener, index);
	}

	static void notifySelect(Listener listener, int index) {
		if (index == 0) {
			listener.onRetry();
		} else {
			listener.onDismiss();
		}
	}
}
