package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import java.io.IOException;

/**
 * Thrown when the player aborts an echo fetch after auto-retries and the
 * Retry/Abort dialog.
 * Interlevel loading should persist the run and return to the title screen.
 */
public final class EchoFetchAbortedException extends IOException {

	public EchoFetchAbortedException() {
		super("echo fetch aborted");
	}
}
