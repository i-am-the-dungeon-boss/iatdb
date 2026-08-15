/*
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.SentryCrashReporting;

/**
 * Re-reads {@code muted_until} at the moment the stored one runs out.
 *
 * <p>The deadline held on the device is only ever a snapshot of what the server
 * said when this client last authenticated. Reaching it does not prove the mute
 * is over: an admin may have extended it since. So the deadline passing is the
 * one moment worth spending a request on — the client asks {@code /v1/auth/me}
 * once, and adopts whatever comes back.
 *
 * <p>That is the whole refresh schedule. There is no interval and no polling:
 * between authenticating and the deadline there is nothing a request could tell
 * this client that it does not already know.
 *
 * <p>The mute is not held past its deadline while the answer is outstanding.
 * The player gets their composer back immediately and the check runs behind
 * them, because the server refuses a muted send anyway — at worst one message is
 * typed and turned away, which is a far better trade than gagging someone whose
 * mute really did lapse and who happens to be offline.
 */
public final class ServerMuteRefresh {

	/** Long enough that a player with no connection is not retrying constantly. */
	private static final long RETRY_AFTER_MS = 60_000L;

	/** The deadline the server has already been asked about; never asked twice. */
	private static long answered;
	/** The deadline the outstanding attempt is about, so the answer latches the right one. */
	private static long asking;
	/** Earliest next attempt — set when one is claimed, so it doubles as an in-flight guard. */
	private static long nextAttempt;

	private ServerMuteRefresh() {
	}

	/**
	 * Called every frame from {@code WorldNet.tick}; almost always does nothing.
	 *
	 * <p>The work happens on its own thread: this runs on the render thread, and
	 * the refresh writes only to {@link EchoPlayerSession}, which is synchronized
	 * and is re-read by the world channel on a later frame. Nothing has to be
	 * handed back across the thread boundary.
	 */
	public static void verifyIfLapsed() {
		if (!EchoOnlineSettings.isConfigured() || !claim(System.currentTimeMillis())) {
			return;
		}
		new Thread(() -> refresh(EchoClient.createDefault()), "echo-mute-refresh").start();
	}

	/**
	 * Whether a check is due, taking it if so.
	 *
	 * <p>Claiming and deciding are one step on purpose: the caller runs once per
	 * frame, so a decision that did not also stake its claim would start a fresh
	 * thread every frame until the first one answered.
	 */
	static synchronized boolean claim(long now) {
		if (now < nextAttempt) {
			return false;
		}
		long stored = EchoPlayerSession.mutedUntil();
		if (stored <= 0L || stored > now || stored == answered) {
			return false;
		}
		if (!EchoPlayerSession.hasSession()) {
			return false;
		}
		asking = stored;
		nextAttempt = now + RETRY_AFTER_MS;
		return true;
	}

	/**
	 * Asks the server what the mute really is now. Synchronous — call off the
	 * render thread.
	 *
	 * <p>A failure is left unanswered rather than treated as "no mute": the
	 * throttle from {@link #claim(long)} then allows another attempt later. An
	 * unreachable server must not be able to lift a mute.
	 */
	static void refresh(EchoClient client) {
		if (client == null || !EchoPlayerSession.hasSession()) {
			return;
		}
		try {
			// fetchMe stores whatever muted_until comes back, including its absence,
			// so adopting the answer needs nothing more than the call itself.
			if (client.fetchMe()) {
				answer();
			}
		} catch (Exception failed) {
			SentryCrashReporting.report(failed);
		}
	}

	/**
	 * Latches the deadline that was asked about, not the one that came back — an
	 * extension is a new deadline, and it has to be checked in its turn when it
	 * too runs out.
	 */
	private static synchronized void answer() {
		answered = asking;
		nextAttempt = 0L;
	}

	/** Test hook: forget which deadline has been asked about. */
	public static synchronized void resetForTests() {
		answered = 0L;
		asking = 0L;
		nextAttempt = 0L;
	}
}
