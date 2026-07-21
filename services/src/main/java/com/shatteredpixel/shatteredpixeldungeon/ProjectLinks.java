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

package com.shatteredpixel.shatteredpixeldungeon;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads IATDB public URLs and contact details from
 * {@code /project-links.properties} (classpath). Edit that file — not this
 * class —
 * to change homepage, backend, GitHub, or developer email.
 * <p>
 * Docs cannot import this; {@code ProjectLinksTest} fails the build if public
 * docs drift from the property values.
 */
public final class ProjectLinks {

	public static final String RESOURCE_NAME = "project-links.properties";

	private static final Properties PROPS = load();

	public static final String HOMEPAGE_URL = required("homepage.url");

	/** Host part of {@link #HOMEPAGE_URL} without scheme (for About link label). */
	public static final String HOMEPAGE_HOST = stripScheme(HOMEPAGE_URL);

	public static final String BACKEND_URL = required("backend.url");

	public static final String GITHUB_OWNER_REPO = required("github.owner.repo");

	public static final String GITHUB_REPO_URL = "https://github.com/" + GITHUB_OWNER_REPO;

	public static final String GITHUB_RELEASES_URL = GITHUB_REPO_URL + "/releases";

	public static final String GITHUB_RELEASES_API_URL = "https://api.github.com/repos/" + GITHUB_OWNER_REPO
			+ "/releases";

	public static final String DEVELOPER_EMAIL = required("developer.email");

	/** {@code mailto:} form of {@link #DEVELOPER_EMAIL} for markdown / URI use. */
	public static final String DEVELOPER_EMAIL_MAILTO = "mailto:" + DEVELOPER_EMAIL;

	private ProjectLinks() {
	}

	private static Properties load() {
		Properties props = new Properties();
		try (InputStream in = ProjectLinks.class.getResourceAsStream("/" + RESOURCE_NAME)) {
			if (in == null) {
				throw new IllegalStateException("missing classpath resource /" + RESOURCE_NAME);
			}
			props.load(in);
		} catch (IOException e) {
			throw new IllegalStateException("failed to load /" + RESOURCE_NAME, e);
		}
		return props;
	}

	private static String required(String key) {
		String value = PROPS.getProperty(key);
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalStateException("missing required property: " + key + " in /" + RESOURCE_NAME);
		}
		return value.trim();
	}

	private static String stripScheme(String url) {
		return url.replaceFirst("^https?://", "");
	}
}
