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
 * One avatar standing in the village.
 *
 * <p>Deliberately not a {@code Char}: remote players have no collision, no
 * combat and no turn scheduling. Ghost-through movement avoids every desync
 * question authoritative movement would raise, and the village has no mechanics
 * to protect.
 */
public final class WorldPresence {

	public final String playerId;
	public final String displayName;
	public final String heroClass;
	public final int cell;
	public final int facing;
	/** True while this player is composing a chat message, so the village can say so. */
	public final boolean typing;

	public WorldPresence(String playerId, String displayName, String heroClass, int cell, int facing) {
		this(playerId, displayName, heroClass, cell, facing, false);
	}

	public WorldPresence(String playerId, String displayName, String heroClass, int cell, int facing,
			boolean typing) {
		this.playerId = playerId != null ? playerId : "";
		this.displayName = displayName != null ? displayName : "";
		this.heroClass = heroClass != null ? heroClass : "";
		this.cell = cell;
		this.facing = facing < 0 ? -1 : 1;
		this.typing = typing;
	}

	/**
	 * The local avatar. Identity is left blank on purpose — the server takes it
	 * from the player JWT, so a client cannot claim to be someone else.
	 */
	public static WorldPresence local(String heroClass, int cell, int facing) {
		return new WorldPresence("", "", heroClass, cell, facing);
	}

	/** True when only the position changed, so the sprite can be moved rather than rebuilt. */
	public boolean sameAvatarAs(WorldPresence other) {
		return other != null
				&& playerId.equals(other.playerId)
				&& heroClass.equals(other.heroClass);
	}
}
