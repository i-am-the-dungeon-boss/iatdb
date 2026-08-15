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
 * Who this client is on the world channel.
 *
 * <p>Carries no credential: the bearer token stays in {@code EchoPlayerSession}
 * and is attached by the transport, so an engine implementation never has to
 * handle it. {@link #playerId} exists only so the UI can tell the player's own
 * lines apart from everyone else's.
 */
public final class WorldIdentity {

	public final String playerId;
	public final String displayName;
	/**
	 * Epoch ms at which a server-side chat mute lifts; 0 when there is none.
	 *
	 * <p>Part of the identity because the engine is told it once, at connect, and
	 * never asks again — the deadline is enough for the engine to refuse a send
	 * and to stop refusing when it passes.
	 */
	public final long mutedUntil;

	/** An identity carrying no mute. */
	public WorldIdentity(String playerId, String displayName) {
		this(playerId, displayName, 0L);
	}

	public WorldIdentity(String playerId, String displayName, long mutedUntil) {
		this.playerId = playerId != null ? playerId : "";
		this.displayName = displayName != null ? displayName : "";
		this.mutedUntil = Math.max(mutedUntil, 0L);
	}

	public boolean isSelf(String otherPlayerId) {
		return !playerId.isEmpty() && playerId.equals(otherPlayerId);
	}
}
