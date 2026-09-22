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

import com.shatteredpixel.shatteredpixeldungeon.village.VillageFigure;
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

	/**
	 * Village bodies. Skipped-not-thrown for the same reason as the rows above,
	 * and with one extra rule: a body has to be identifiable to be inspectable,
	 * so a mention with no {@code echo_id} is dropped. A <em>depth</em> post with
	 * no id is kept — that is how the server says the depth is unheld and the
	 * regional boss should stand there instead.
	 *
	 * @see #decodeMessages(JSONArray) for why this is shared rather than private
	 */
	static List<VillageFigure> decodeFigures(JSONArray array) {
		List<VillageFigure> figures = new ArrayList<>();
		if (array == null) {
			return figures;
		}
		for (int i = 0; i < array.length(); i++) {
			JSONObject entry = array.optJSONObject(i);
			if (entry == null) {
				continue;
			}
			VillageFigure.Post post = postOf(entry.optString("post", ""));
			if (post == null) {
				continue;
			}
			String echoId = entry.optString("echo_id", "");
			if (Strings.isBlank(echoId)) {
				if (post != VillageFigure.Post.DEPTH) {
					continue;
				}
				echoId = null;
			}
			VillageFigure figure = new VillageFigure();
			figure.post = post;
			figure.depth = entry.optInt("depth", 0);
			figure.echoId = echoId;
			figure.userName = entry.optString("user_name", "");
			figure.heroClass = entry.optString("hero_class", "");
			figure.armorTier = entry.optInt("armor_tier", VillageFigure.UNKNOWN_TIER);
			figure.lvl = entry.optInt("lvl", 0);
			figure.hp = entry.optInt("hp", 0);
			figure.ht = entry.optInt("ht", 0);
			figure.killCount = entry.optInt("kill_count", 0);
			figure.timestamp = entry.optLong("timestamp", 0L);
			decodeBadges(entry.optJSONArray("badges"), figure.badges);
			figures.add(figure);
		}
		return figures;
	}

	private static VillageFigure.Post postOf(String raw) {
		if ("depth".equals(raw)) {
			return VillageFigure.Post.DEPTH;
		}
		if ("mention".equals(raw)) {
			return VillageFigure.Post.MENTION;
		}
		return null;
	}

	private static void decodeBadges(JSONArray array, List<VillageFigure.Badge> into) {
		if (array == null) {
			return;
		}
		for (int i = 0; i < array.length(); i++) {
			JSONObject entry = array.optJSONObject(i);
			if (entry == null) {
				continue;
			}
			String kind = entry.optString("kind", "");
			if (Strings.isBlank(kind)) {
				continue;
			}
			into.add(new VillageFigure.Badge(
					kind, entry.optInt("count", VillageFigure.Badge.NO_COUNT)));
		}
	}
}
