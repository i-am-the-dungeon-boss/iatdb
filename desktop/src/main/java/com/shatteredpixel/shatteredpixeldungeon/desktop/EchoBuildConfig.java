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

package com.shatteredpixel.shatteredpixeldungeon.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Desktop bake of {@code ECHO_API_KEY}. Gradle writes the value into
 * {@code /echo-build.properties} at build time from root {@code .env}.
 */
public final class EchoBuildConfig {

	public static final String ECHO_API_KEY = read("ECHO_API_KEY", "");
	/** True when this desktop build was packaged with {@code -Pdemo}. */
	public static final boolean DEMO_PACKAGE = "true".equals(read("DEMO_PACKAGE", "false"));

	private EchoBuildConfig() {
	}

	private static String read(String key, String fallback) {
		InputStream in = EchoBuildConfig.class.getResourceAsStream("/echo-build.properties");
		if (in == null) {
			return fallback;
		}
		try {
			try {
				Properties props = new Properties();
				props.load(in);
				String value = props.getProperty(key, fallback);
				return value != null ? value.trim() : fallback;
			} finally {
				in.close();
			}
		} catch (IOException e) {
			return fallback;
		}
	}
}
