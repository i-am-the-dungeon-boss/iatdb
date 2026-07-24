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

package com.watabou.utils;

/**
 * Multi-site portable string helpers for APIs RoboVM lacks. Prefer these over
 * {@code String#isBlank} / {@code String#join}; do not grow a full JDK polyfill
 * layer here — one-off gaps should be rewritten inline at the call site.
 */
public final class Strings {

	private Strings() {
	}

	/** Null, empty, or whitespace-only (RoboVM safe). */
	public static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	/** Join like {@code String.join}, safe when RoboVM lacks that API. */
	public static String join(CharSequence delimiter, Iterable<? extends CharSequence> parts) {
		if (parts == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (CharSequence part : parts) {
			if (!first) {
				sb.append(delimiter);
			}
			sb.append(part == null ? "null" : part);
			first = false;
		}
		return sb.toString();
	}
}
