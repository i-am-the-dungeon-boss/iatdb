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

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.watabou.utils.Strings;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Players this client has chosen not to hear from.
 *
 * <p><b>Not to be confused with {@link ServerMute}</b>, the other mute in this
 * package. This one is the player silencing <em>others</em>: local, chosen by
 * them, and it filters what arrives. {@link ServerMute} is an admin silencing
 * <em>this</em> player: set on their account server-side, and it stops them
 * sending. They share a word and nothing else.
 *
 * <p>Purely local — nothing is sent to the server. That makes it instant and
 * private, at the cost of not surviving a reinstall and doing nothing for
 * anyone else being harassed; the server-side mute an admin sets is the tool
 * for that.
 *
 * <p>Keyed on <em>username</em> rather than player id because that is what a
 * player actually sees in chat. The trade-off is that a rename escapes the
 * mute, which is acceptable for a nuisance filter and is exactly why it does
 * not replace reporting.
 */
public final class MuteList {

	/** Generous, but bounded — the whole list is re-serialised on every change. */
	public static final int CAP = 100;

	private static final String SEPARATOR = ",";

	private static Set<String> muted;

	private MuteList() {
	}

	private static Set<String> entries() {
		if (muted == null) {
			reload();
		}
		return muted;
	}

	/** Re-reads from settings, dropping anything cached. */
	public static void reload() {
		muted = new LinkedHashSet<>();
		String stored = SPDSettings.worldChatMuted();
		if (Strings.isBlank(stored)) {
			return;
		}
		for (String name : stored.split(SEPARATOR)) {
			String key = normalise(name);
			if (key != null && muted.size() < CAP) {
				muted.add(key);
			}
		}
	}

	public static boolean isMuted(String username) {
		String key = normalise(username);
		return key != null && entries().contains(key);
	}

	public static void mute(String username) {
		String key = normalise(username);
		if (key == null || entries().size() >= CAP || !entries().add(key)) {
			return;
		}
		save();
	}

	public static void unmute(String username) {
		String key = normalise(username);
		if (key != null && entries().remove(key)) {
			save();
		}
	}

	/** Normalised (lowercased) names, for display and for tests. */
	public static List<String> muted() {
		return new ArrayList<>(entries());
	}

	public static void clear() {
		muted = new LinkedHashSet<>();
		save();
	}

	/**
	 * Lowercased and trimmed, or null when the name is unusable.
	 *
	 * <p>Names containing the separator are rejected outright: usernames are
	 * server-validated and never contain one, so a name that does is either
	 * corrupt or an attempt to inject extra entries into the stored list.
	 */
	private static String normalise(String username) {
		if (username == null) {
			return null;
		}
		String trimmed = username.trim();
		if (trimmed.isEmpty() || trimmed.contains(SEPARATOR)) {
			return null;
		}
		return trimmed.toLowerCase();
	}

	private static void save() {
		// Strings.join, not String.join — see the forbidden-API table in
		// .cursor/rules/cross-platform.mdc; RoboVM lacks the JDK 8 overload.
		SPDSettings.worldChatMuted(Strings.join(SEPARATOR, entries()));
	}
}
