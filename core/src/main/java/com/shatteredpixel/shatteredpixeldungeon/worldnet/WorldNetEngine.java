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

import java.util.List;

/**
 * The whole contract a world-channel transport must satisfy.
 *
 * <p>
 * This interface is the swap point. The shipped implementation is a raw
 * WebSocket, hand-rolled because desktop, Android <em>and</em> RoboVM have to
 * agree. Another engine implements this same
 * interface and nothing above it changes.
 *
 * <p>
 * <b>Chat and presence must stay separable.</b> The socket engine happens to
 * carry both over one connection, but that is an implementation detail and must
 * never leak above this interface: an engine that sourced the two differently
 * can only be dropped in if callers treat {@link Listener#onChatMessages} and
 * {@link Listener#onPresenceSnapshot} as independent streams.
 *
 * <p>
 * Callbacks fire from {@link #tick(float)}, which callers invoke on the
 * render thread. Implementations therefore never need
 * {@code Game.runOnRenderThread} — they queue work and hand it over on tick.
 */
public interface WorldNetEngine {

	void connect(WorldIdentity identity, Listener listener);

	void disconnect();

	boolean isConnected();

	/**
	 * @param local the village avatar, or null while in a run (chat only, no roster
	 *              entry)
	 */
	void setPresence(WorldPresence local);

	void sendChat(String text);

	void report(String messageId, String reason);

	/**
	 * Hints that the player is actively reading or writing chat.
	 *
	 * <p>
	 * Part of the contract rather than one engine's private knob: it is the
	 * game telling the transport how much the player cares right now. The socket
	 * engine, already immediate, spends it on keeping the connection warm; an
	 * engine that had to ask for updates would spend it on a tighter interval.
	 */
	void setChatFocused(boolean focused);

	/**
	 * Drives the engine. Called once per frame on the render thread; a
	 * push-based engine may do nothing but drain its queue here.
	 *
	 * @param elapsed seconds since the previous tick
	 */
	void tick(float elapsed);

	/** Short identifier for logs and the debug overlay. */
	String name();

	/**
	 * A nested interface rather than {@code java.util.function} — see the
	 * forbidden-API table in {@code .cursor/rules/cross-platform.mdc}; RoboVM's
	 * {@code robovm-rt} does not carry those types.
	 */
	interface Listener {

		/** New lines only — already de-duplicated against what was delivered before. */
		void onChatMessages(List<WorldChatMessage> messages);

		/** The full village roster excluding the local player, not a delta. */
		void onPresenceSnapshot(List<WorldPresence> others);

		void onStatus(Status status, String detail);

		/**
		 * The answer to {@link WorldNetEngine#isServerMuted()} changed.
		 *
		 * <p>
		 * Separate from {@link #onStatus} because a mute is not a connection
		 * state: a muted player is connected, sees chat and is seen in the village,
		 * and is only barred from speaking. Folding the two together made
		 * {@code MUTED} and {@code CONNECTED} mutually exclusive and left callers
		 * reading a link state to answer a permission question.
		 */
		void onServerMute(boolean muted);

		/**
		 * The game build the server expects, as stated when the connection opened.
		 *
		 * <p>
		 * Reported as a fact rather than a verdict: the transport has no opinion
		 * on what should happen to a client that does not match, and a server too
		 * old to send a version says nothing at all rather than claiming one.
		 */
		void onServerVersion(String versionName);
	}

	/**
	 * Whether the server will refuse anything this client says right now.
	 *
	 * <p>
	 * Answered locally: the deadline arrives on {@link WorldIdentity} from the
	 * authentication response, so this costs nothing and needs no polling.
	 */
	boolean isServerMuted();

	/**
	 * Replaces the deadline given at connect with a freshly read one.
	 *
	 * <p>
	 * The deadline reaching zero does not mean the mute is over — an admin may
	 * have extended it — so it is re-read at that moment and handed back here
	 * rather than waiting for the next connect. Also clears a refusal the server
	 * gave without a deadline, since this value supersedes it.
	 */
	void applyServerMute(long mutedUntil);

	enum Status {
		CONNECTING,
		CONNECTED,
		/** Reachable but failing — backing off, or a send was rejected. */
		DEGRADED,
		DISCONNECTED
	}
}
