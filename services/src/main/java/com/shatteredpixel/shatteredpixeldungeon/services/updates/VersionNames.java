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

package com.shatteredpixel.shatteredpixeldungeon.services.updates;

/**
 * Compares game version names. The single answer to "is this build the one the
 * server expects", shared by the update service and the world channel so the
 * two can never disagree.
 *
 * <p>Only the numeric core counts. A local desktop run calls itself
 * {@code 0.0.13-INDEV} (see {@code desktop/build.gradle}) and is the same build
 * as the {@code 0.0.13} the backend serves, so a suffix must never raise the
 * forced-update window.
 *
 * <p>Lives in {@code :services} rather than {@code core} because
 * {@code EchoUpdates} needs it and that module does not depend on core.
 */
public final class VersionNames {

	private VersionNames() {
	}

	/** True when the two names are different game versions, ignoring suffixes. */
	public static boolean differ(String left, String right) {
		return compare(left, right) != 0;
	}

	/**
	 * Orders two version names by their numeric parts; missing trailing parts
	 * count as zero, so {@code 1.0} and {@code 1.0.0} are the same version.
	 */
	public static int compare(String left, String right) {
		int[] a = parseParts(left);
		int[] b = parseParts(right);
		int len = Math.max(a.length, b.length);
		for (int i = 0; i < len; i++) {
			int va = i < a.length ? a[i] : 0;
			int vb = i < b.length ? b[i] : 0;
			if (va != vb) {
				return Integer.compare(va, vb);
			}
		}
		return 0;
	}

	/** Numeric parts of the name, stopping at the first {@code -} suffix. */
	private static int[] parseParts(String raw) {
		if (raw == null || raw.isEmpty()) {
			return new int[0];
		}
		String core = raw.split("-", 2)[0].trim();
		if (core.isEmpty()) {
			return new int[0];
		}
		String[] bits = core.split("\\.");
		int[] parts = new int[bits.length];
		for (int i = 0; i < bits.length; i++) {
			try {
				parts[i] = Integer.parseInt(bits[i].trim());
			} catch (NumberFormatException e) {
				parts[i] = 0;
			}
		}
		return parts;
	}
}
