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
 * One line of world chat.
 *
 * <p>{@link #id} is the de-duplication key. The server matches the sync cursor
 * inclusively so that two messages written in the same millisecond are never
 * dropped, which means the boundary message arrives twice and the client is
 * responsible for discarding the repeat.
 */
public final class WorldChatMessage {

	/** Mirrors {@code ECHO_WORLD_MAX_TEXT_LENGTH}; the server rejects anything longer. */
	public static final int MAX_LENGTH = 240;

	public final String id;
	public final String playerId;
	public final String name;
	public final String text;
	public final long at;

	public WorldChatMessage(String id, String playerId, String name, String text, long at) {
		this.id = id != null ? id : "";
		this.playerId = playerId != null ? playerId : "";
		this.name = name != null ? name : "";
		this.text = text != null ? text : "";
		this.at = at;
	}
}
