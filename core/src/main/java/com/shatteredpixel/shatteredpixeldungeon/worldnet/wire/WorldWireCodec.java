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
import com.watabou.utils.Strings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON for the world channel.
 *
 * <p>Restricted to the Android {@code org.json} subset — no {@code keySet()},
 * no {@code write()}, no non-String {@code JSONTokener} — because MobiVM uses
 * the same incomplete implementation on iOS. Desktop-only helpers compile
 * happily and then crash on device.
 */
public final class WorldWireCodec {

	private WorldWireCodec() {
	}

	/**
	 * A single malformed entry is skipped rather than failing the whole sync —
	 * one bad row must not blank the village or stall the chat cursor.
	 *
	 * <p>Package-visible so the socket codec decodes chat identically. The two
	 * transports carry the same nouns in the same shape, and duplicating this
	 * would let them drift into disagreeing about what a message is.
	 */
	static List<WorldChatMessage> decodeMessages(JSONArray array) {
		List<WorldChatMessage> messages = new ArrayList<>();
		if (array == null) {
			return messages;
		}
		for (int i = 0; i < array.length(); i++) {
			JSONObject entry = array.optJSONObject(i);
			if (entry == null) {
				continue;
			}
			String id = entry.optString("id", "");
			if (Strings.isBlank(id)) {
				continue;
			}
			messages.add(new WorldChatMessage(
					id,
					entry.optString("player_id", ""),
					entry.optString("name", ""),
					entry.optString("text", ""),
					entry.optLong("at", 0L)));
		}
		return messages;
	}

	/** @see #decodeMessages(JSONArray) for why this is shared rather than private. */
	static List<WorldPresence> decodeOccupants(JSONArray array) {
		List<WorldPresence> occupants = new ArrayList<>();
		if (array == null) {
			return occupants;
		}
		for (int i = 0; i < array.length(); i++) {
			JSONObject entry = array.optJSONObject(i);
			if (entry == null) {
				continue;
			}
			String playerId = entry.optString("player_id", "");
			if (Strings.isBlank(playerId)) {
				continue;
			}
			occupants.add(new WorldPresence(
					playerId,
					entry.optString("name", ""),
					entry.optString("hero_class", ""),
					entry.optInt("cell", 0),
					entry.optInt("facing", 1),
					entry.optBoolean("typing", false)));
		}
		return occupants;
	}
}
