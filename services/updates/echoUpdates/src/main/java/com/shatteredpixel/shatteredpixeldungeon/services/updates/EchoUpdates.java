/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.shatteredpixel.shatteredpixeldungeon.ProjectLinks;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

/**
 * Checks hero-echoes {@code GET /v1/game-version} and requires an update
 * when {@code version_name} differs from the installed game version.
 */
public class EchoUpdates extends UpdateService {

	static final String DEFAULT_UPDATE_URL = ProjectLinks.LATEST_RELEASE_URL;

	/** Optional override set by launchers (e.g. from EchoOnlineSettings). */
	public static String baseUrlOverride = "";

	@Override
	public boolean supportsUpdatePrompts() {
		return true;
	}

	@Override
	public boolean supportsBetaChannel() {
		return false;
	}

	@Override
	public void checkForUpdate(boolean useMetered, boolean includeBetas, UpdateResultCallback callback) {
		// Always-online game: the version check runs on any connection, so useMetered
		// is accepted for interface compatibility and deliberately ignored.
		String base = resolveBaseUrl();
		if (base.isEmpty()) {
			callback.onConnectionFailed();
			return;
		}

		Net.HttpRequest httpGet = new Net.HttpRequest(Net.HttpMethods.GET);
		httpGet.setUrl(base + "/v1/game-version");
		httpGet.setHeader("Accept", "application/json");

		Gdx.net.sendHttpRequest(httpGet, new Net.HttpResponseListener() {
			@Override
			public void handleHttpResponse(Net.HttpResponse httpResponse) {
				try {
					int status = httpResponse.getStatus().getStatusCode();
					if (status < 200 || status >= 300) {
						callback.onConnectionFailed();
						return;
					}
					Bundle root = Bundle.read(httpResponse.getResultAsStream());
					String remoteName = root.getString("version_name");
					if (!requiresUpdate(remoteName, Game.version)) {
						callback.onNoUpdateFound();
						return;
					}

					AvailableUpdateData update = new AvailableUpdateData();
					update.versionName = remoteName;
					update.versionCode = Game.versionCode + 1;
					update.desc = root.contains("release_notes") ? root.getString("release_notes") : null;
					update.URL = resolveUpdateUrl(root.contains("update_url") ? root.getString("update_url") : null);
					callback.onUpdateAvailable(update);
				} catch (Exception e) {
					Game.reportException(e);
					callback.onConnectionFailed();
				}
			}

			@Override
			public void failed(Throwable t) {
				Game.reportException(t);
				callback.onConnectionFailed();
			}

			@Override
			public void cancelled() {
				callback.onConnectionFailed();
			}
		});
	}

	@Override
	public void initializeUpdate(AvailableUpdateData update) {
		Game.platform.openURI(update.URL);
	}

	@Override
	public boolean supportsReviews() {
		return false;
	}

	@Override
	public void initializeReview(ReviewResultCallback callback) {
		callback.onComplete();
	}

	@Override
	public void openReviewURI() {
	}

	static String resolveBaseUrl() {
		if (baseUrlOverride != null && !baseUrlOverride.trim().isEmpty()) {
			return trimTrailingSlash(baseUrlOverride.trim());
		}
		String fromProp = System.getProperty("ECHO_BACKEND_URL");
		if (fromProp != null && !fromProp.trim().isEmpty()) {
			return trimTrailingSlash(fromProp.trim());
		}
		String fromEnv = System.getenv("ECHO_BACKEND_URL");
		if (fromEnv != null && !fromEnv.trim().isEmpty()) {
			return trimTrailingSlash(fromEnv.trim());
		}
		return "";
	}

	/**
	 * True when remote and local version cores differ (ignores -INDEV / other
	 * suffixes).
	 */
	static boolean requiresUpdate(String remoteName, String localVersion) {
		if (remoteName == null || remoteName.isEmpty()) {
			return false;
		}
		String local = localVersion == null ? "" : localVersion;
		return VersionNames.differ(remoteName, local);
	}

	static String resolveUpdateUrl(String fromBackend) {
		if (fromBackend != null && !fromBackend.trim().isEmpty()) {
			return fromBackend.trim();
		}
		return DEFAULT_UPDATE_URL;
	}

	private static String trimTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
