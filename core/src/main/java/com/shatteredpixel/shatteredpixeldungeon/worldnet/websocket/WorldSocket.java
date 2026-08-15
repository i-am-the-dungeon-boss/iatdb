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

/**
 * A text-only WebSocket, reduced to what the world channel actually needs.
 *
 * <p>Exists as an interface for two reasons. It lets
 * {@link WebSocketWorldNetEngine} be unit-tested without a network, which is the
 * only practical way to cover reconnects and backoff. And it isolates the one
 * genuinely risky piece of this feature — a hand-rolled RFC 6455 client, needed
 * because RoboVM cannot AOT-compile any of the usual WebSocket libraries — so
 * that a platform which turns out to need a different implementation can supply
 * one without the engine noticing.
 *
 * <p>{@link #open} is asynchronous and {@link #send} must not block the caller;
 * implementations own whatever threads they need. Callbacks therefore arrive off
 * the render thread, and the engine queues them rather than acting on them
 * directly.
 */
public interface WorldSocket {

	/** Callbacks arrive on an implementation-owned thread, never the render thread. */
	interface Listener {

		void onOpen();

		void onText(String text);

		/** Terminal: the socket is finished, whether it closed cleanly or failed. */
		void onClosed(String reason);
	}

	void open(Listener listener);

	/** Queues a text frame. Silently dropped if the socket is not open. */
	void send(String text);

	void close();

	/**
	 * Builds a socket for the configured backend.
	 *
	 * <p>A factory rather than a constructor call so the engine holds no opinion
	 * about URLs, headers or credentials — a reconnect simply asks for another
	 * one.
	 */
	interface Factory {
		WorldSocket create();
	}
}
