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

package com.shatteredpixel.shatteredpixeldungeon.worldnet.wire;

import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldChatMessage;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldPresence;

import java.util.Collections;
import java.util.List;

/**
 * One decoded server-to-client frame from {@code GET /v1/world/socket}.
 *
 * <p>A single type with a discriminator rather than a class per frame: the
 * variants are tiny, the engine switches over them in one place, and this keeps
 * the socket transport from spraying half a dozen near-empty classes across the
 * package.
 *
 * <p>Unrecognised frames decode to {@link Kind#UNKNOWN} rather than raising.
 * The server may learn to send something this build has never heard of, and a
 * client from an older release must ignore it and carry on rather than drop the
 * connection.
 */
public final class WorldFrame {

	/**
	 * No mute frame: {@code muted_until} reaches this client on the authentication
	 * response and is held in the session until it lapses, so the socket never
	 * carries mute state. A send the server refuses arrives as {@link Kind#ERROR}
	 * with the {@code muted} code.
	 */
	public enum Kind {
		HELLO,
		CHAT,
		ROSTER,
		ERROR,
		PONG,
		UNKNOWN
	}

	public final Kind kind;
	public final List<WorldChatMessage> messages;
	public final List<WorldPresence> occupants;
	/** One of the server's error codes, or empty. Never shown to the player as-is. */
	public final String errorCode;
	public final String detail;
	/**
	 * On {@link Kind#HELLO}, this client's own server-side id; empty elsewhere.
	 *
	 * <p>The only way the client learns it. The roster is broadcast whole to every
	 * connection on an instance so the server serialises it once, which leaves
	 * dropping one's own entry to the client — and that needs a name for oneself.
	 */
	public final String playerId;
	/**
	 * On {@link Kind#HELLO}, the game build this server expects; empty elsewhere,
	 * and empty from a server too old to send it.
	 *
	 * <p>Every reconnect lands on the newest deployment, so this is how a client
	 * finds out within minutes of a release that it is out of date — without
	 * waiting for its next visit to the title screen.
	 */
	public final String versionName;

	private WorldFrame(
			Kind kind,
			List<WorldChatMessage> messages,
			List<WorldPresence> occupants,
			String errorCode,
			String detail,
			String playerId,
			String versionName) {
		this.kind = kind;
		this.messages = messages;
		this.occupants = occupants;
		this.errorCode = errorCode;
		this.detail = detail;
		this.playerId = playerId;
		this.versionName = versionName;
	}

	static WorldFrame hello(String playerId, String versionName) {
		return new WorldFrame(
				Kind.HELLO,
				Collections.emptyList(),
				Collections.emptyList(),
				"",
				"",
				playerId != null ? playerId : "",
				versionName != null ? versionName : "");
	}

	static WorldFrame pong() {
		return simple(Kind.PONG);
	}

	static WorldFrame unknown() {
		return simple(Kind.UNKNOWN);
	}

	static WorldFrame chat(List<WorldChatMessage> messages) {
		return new WorldFrame(Kind.CHAT, messages, Collections.emptyList(), "", "", "", "");
	}

	static WorldFrame roster(List<WorldPresence> occupants) {
		return new WorldFrame(Kind.ROSTER, Collections.emptyList(), occupants, "", "", "", "");
	}

	static WorldFrame error(String code, String detail) {
		return new WorldFrame(
				Kind.ERROR, Collections.emptyList(), Collections.emptyList(), code, detail, "", "");
	}

	private static WorldFrame simple(Kind kind) {
		return new WorldFrame(kind, Collections.emptyList(), Collections.emptyList(), "", "", "", "");
	}
}
