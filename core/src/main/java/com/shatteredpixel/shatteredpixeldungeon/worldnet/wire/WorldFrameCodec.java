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

import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldPresence;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * JSON for the socket transport's frames.
 *
 * <p>Chat lines and roster entries are decoded by {@link WorldWireCodec}, which
 * both transports share — only the envelope around them differs. Restricted to
 * the Android {@code org.json} subset for the same reason as its sibling: MobiVM
 * ships the same incomplete implementation on iOS.
 */
public final class WorldFrameCodec {

	private WorldFrameCodec() {
	}

	public static String encodePresence(WorldPresence presence) throws JSONException {
		JSONObject frame = new JSONObject();
		frame.put("t", "presence");
		frame.put("hero_class", presence.heroClass);
		frame.put("cell", presence.cell);
		frame.put("facing", presence.facing);
		return frame.toString();
	}

	/** Asks the server to drop the village avatar now rather than let it lapse. */
	public static String encodeLeave() throws JSONException {
		return typeOnly("leave");
	}

	public static String encodeChat(String text) throws JSONException {
		JSONObject frame = new JSONObject();
		frame.put("t", "chat");
		frame.put("text", text != null ? text : "");
		return frame.toString();
	}

	public static String encodeFocus(boolean focused) throws JSONException {
		JSONObject frame = new JSONObject();
		frame.put("t", "focus");
		frame.put("on", focused);
		return frame.toString();
	}

	public static String encodePing() throws JSONException {
		return typeOnly("ping");
	}

	/** Asks for the inspect-tier bundle behind one standing figure. */
	public static String encodeEchoReq(String echoId) throws JSONException {
		JSONObject frame = new JSONObject();
		frame.put("t", "echo_req");
		frame.put("echo_id", echoId != null ? echoId : "");
		return frame.toString();
	}

	public static String encodeReport(String messageId, String reason) throws JSONException {
		JSONObject frame = new JSONObject();
		frame.put("t", "report");
		frame.put("message_id", messageId != null ? messageId : "");
		frame.put("reason", reason != null ? reason : "other");
		return frame.toString();
	}

	/**
	 * Anything unparseable becomes {@link WorldFrame.Kind#UNKNOWN} instead of
	 * throwing. A garbled frame on a long-lived socket is a thing to skip, not a
	 * reason to tear the connection down and lose the player their chat.
	 */
	public static WorldFrame decode(String raw) {
		JSONObject json;
		try {
			json = new JSONObject(raw);
		} catch (JSONException malformed) {
			return WorldFrame.unknown();
		}
		String type = json.optString("t", "");
		if ("chat".equals(type)) {
			return WorldFrame.chat(WorldWireCodec.decodeMessages(json.optJSONArray("messages")));
		}
		if ("roster".equals(type)) {
			return WorldFrame.roster(WorldWireCodec.decodeOccupants(json.optJSONArray("occupants")));
		}
		if ("figures".equals(type)) {
			return WorldFrame.figures(WorldWireCodec.decodeFigures(json.optJSONArray("figures")));
		}
		if ("echo".equals(type)) {
			return WorldFrame.echo(
					json.optString("echo_id", ""), json.optString("echo_data_base64", ""));
		}
		if ("error".equals(type)) {
			return WorldFrame.error(json.optString("code", ""), json.optString("detail", ""));
		}
		if ("hello".equals(type)) {
			return WorldFrame.hello(
					json.optString("player_id", ""), json.optString("version_name", ""));
		}
		if ("pong".equals(type)) {
			return WorldFrame.pong();
		}
		return WorldFrame.unknown();
	}

	private static String typeOnly(String type) throws JSONException {
		JSONObject frame = new JSONObject();
		frame.put("t", type);
		return frame.toString();
	}
}
