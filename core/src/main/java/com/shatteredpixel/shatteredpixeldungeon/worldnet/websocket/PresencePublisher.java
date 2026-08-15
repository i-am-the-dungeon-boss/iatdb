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

package com.shatteredpixel.shatteredpixeldungeon.worldnet.websocket;

import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldPresence;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.wire.WorldFrameCodec;

import org.json.JSONException;

/**
 * What the server has been told about this player's avatar, and what it is still
 * owed.
 *
 * <p>The village is updated every frame the hero moves, and almost none of those
 * updates are news: the server's roster is the set of open sockets, so an avatar
 * that has not moved needs saying exactly once. This is the filter that turns a
 * per-frame call into the handful of frames actually worth sending.
 *
 * <p>Knows nothing about sockets. It answers "what should go out now", the
 * engine puts it on the wire, and a socket that has just been replaced simply
 * calls {@link #forget()} — a fresh connection has never heard any of it.
 */
final class PresencePublisher {

	private WorldPresence local;
	private WorldPresence published;
	private boolean pendingLeave;

	/**
	 * Records where the avatar is, or that there is no longer one.
	 *
	 * <p>The transition matters, not the state: the server needs telling once that
	 * the avatar has gone, and a run must not repeat that on every frame.
	 */
	void set(WorldPresence next) {
		if (next == null && local != null) {
			pendingLeave = true;
			published = null;
		}
		local = next;
	}

	/**
	 * A fresh socket knows nothing about us.
	 *
	 * <p>The roster on the other side is the set of live connections, so an avatar
	 * that is not restated on this one simply does not exist in the village.
	 */
	void forget() {
		published = null;
	}

	/** Wipes everything: there is no session left for any of it to mean anything in. */
	void clear() {
		local = null;
		published = null;
		pendingLeave = false;
	}

	/**
	 * Whether the server still believes in an avatar that ought to be retracted
	 * before the socket goes.
	 */
	boolean owesGoodbye() {
		return published != null || pendingLeave;
	}

	/**
	 * The frame to send now, or null when the server is already up to date.
	 *
	 * <p>A pending leave goes first and alone: once the avatar is gone there is no
	 * position left worth stating, and the next real position will come on the
	 * following tick anyway.
	 */
	String nextFrame() throws JSONException {
		if (pendingLeave) {
			String frame = WorldFrameCodec.encodeLeave();
			pendingLeave = false;
			return frame;
		}
		if (local == null || samePlace(published, local)) {
			return null;
		}
		String frame = WorldFrameCodec.encodePresence(local);
		published = local;
		return frame;
	}

	private static boolean samePlace(WorldPresence published, WorldPresence current) {
		return published != null
				&& published.cell == current.cell
				&& published.facing == current.facing
				&& published.heroClass.equals(current.heroClass);
	}
}
