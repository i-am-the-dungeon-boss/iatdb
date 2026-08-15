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

package com.shatteredpixel.shatteredpixeldungeon.worldnet;

/**
 * Whether this client may speak on the world channel, and why not.
 *
 * <p>The moderation mute: an admin sets {@code muted_until} on the player's
 * account and the server refuses everything they say until it passes. <b>Not
 * {@link MuteList}</b>, which is this player choosing not to hear specific
 * others and never leaves the device. One stops you talking, the other stops
 * you listening.
 *
 * <p>One question with two answers behind it, which is the whole reason this is
 * a type rather than a pair of fields on the engine:
 *
 * <ul>
 * <li>a <b>deadline</b>, handed over at connect from the authentication
 * response. It needs no refreshing — the client can see for itself when it
 * passes, so a mute lifts with no frame, no request and no reconnect.</li>
 * <li>a <b>refusal</b>, when the server rejects a send as muted. A moderator
 * muted this player after the deadline was handed over, so there is no new
 * deadline to be had; it holds until the next {@link #reset(long)}, which the
 * next connect performs with a fresh deadline from authentication.</li>
 * </ul>
 *
 * <p>Never latched into a flag: {@link #isActive()} re-reads the clock on every
 * call, so nothing has to notice the moment a mute expires.
 */
public final class ServerMute {

	private long until;
	private boolean refused;

	/** Adopts the deadline authentication gave us, discarding anything older. */
	public void reset(long mutedUntil) {
		this.until = Math.max(mutedUntil, 0L);
		this.refused = false;
	}

	/** The server turned a send away as muted; hold until the next {@link #reset(long)}. */
	public void refusedByServer() {
		this.refused = true;
	}

	public boolean isActive() {
		return refused || until > System.currentTimeMillis();
	}
}
