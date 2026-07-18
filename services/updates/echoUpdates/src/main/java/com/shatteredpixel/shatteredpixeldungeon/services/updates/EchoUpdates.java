/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * I am the Dungeon Boss
 * Copyright (C) 2014-2026 Marwan Elzainy
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
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

/**
 * Checks hero-echoes {@code GET /v1/game-version} for a newer
 * {@code version_name}.
 */
public class EchoUpdates extends UpdateService {

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
		if (!useMetered && !Game.platform.connectedToUnmeteredNetwork()) {
			callback.onConnectionFailed();
			return;
		}

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
					if (!isRemoteNewer(remoteName, Game.version)) {
						callback.onNoUpdateFound();
						return;
					}

					AvailableUpdateData update = new AvailableUpdateData();
					update.versionName = remoteName;
					update.versionCode = Game.versionCode + 1;
					update.desc = root.contains("release_notes") ? root.getString("release_notes") : null;
					update.URL = root.getString("update_url");
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
	 * True when remote semver is greater than local (ignores local -INDEV suffix).
	 */
	static boolean isRemoteNewer(String remoteName, String localVersion) {
		if (remoteName == null || remoteName.isEmpty()) {
			return false;
		}
		String local = localVersion == null ? "" : localVersion;
		return compareVersionNames(remoteName, local) > 0;
	}

	static int compareVersionNames(String left, String right) {
		int[] a = parseVersionParts(left);
		int[] b = parseVersionParts(right);
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

	private static int[] parseVersionParts(String raw) {
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
				parts[i] = Integer.parseInt(bits[i]);
			} catch (NumberFormatException e) {
				parts[i] = 0;
			}
		}
		return parts;
	}

	private static String trimTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
