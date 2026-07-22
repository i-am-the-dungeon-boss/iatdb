package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * Local player identity for Hero Echoes online writes: stable device id + JWT
 * session.
 * Persisted via {@link FileUtils} Local storage (desktop / Android / iOS).
 */
public final class EchoPlayerSession {

	static final String SESSION_FILE = "echoes-online/player-session.dat";

	private static final String KEY_DEVICE_ID = "device_id";
	private static final String KEY_JWT = "jwt";
	private static final String KEY_USERNAME = "username";
	private static final String KEY_HAS_CREDENTIALS = "has_credentials";
	private static final String KEY_EMAIL = "email";

	private static String deviceId;
	private static String jwt = "";
	private static String username = "";
	private static boolean hasCredentials;
	private static String email = "";
	private static boolean loaded;

	private EchoPlayerSession() {
	}

	public static synchronized String deviceId() {
		ensureLoaded();
		if (deviceId == null || deviceId.isBlank()) {
			deviceId = UUID.randomUUID().toString();
			persist();
		}
		return deviceId;
	}

	public static synchronized String jwt() {
		ensureLoaded();
		return jwt != null ? jwt : "";
	}

	public static synchronized String username() {
		ensureLoaded();
		return username != null ? username : "";
	}

	public static synchronized boolean hasCredentials() {
		ensureLoaded();
		return hasCredentials;
	}

	public static synchronized String email() {
		ensureLoaded();
		return email != null ? email : "";
	}

	public static synchronized boolean hasSession() {
		ensureLoaded();
		return jwt != null && !jwt.isBlank() && username != null && !username.isBlank();
	}

	public static synchronized void applyAuthResponse(
			String token,
			String name,
			boolean credentials,
			String linkedEmail) {
		ensureLoaded();
		jwt = token != null ? token : "";
		username = name != null ? name.trim() : "";
		hasCredentials = credentials;
		email = linkedEmail != null ? linkedEmail.trim() : "";
		persist();
	}

	public static synchronized void clearSession() {
		ensureLoaded();
		jwt = "";
		username = "";
		hasCredentials = false;
		email = "";
		persist();
	}

	/** Test-only: wipe in-memory + file state. */
	public static synchronized void resetForTests() {
		deviceId = null;
		jwt = "";
		username = "";
		hasCredentials = false;
		email = "";
		loaded = false;
		try {
			if (FileUtils.fileExists(SESSION_FILE)) {
				FileUtils.deleteFile(SESSION_FILE);
			}
		} catch (Exception ignored) {
		}
	}

	/** Test-only: force reload from disk. */
	public static synchronized void reloadForTests() {
		loaded = false;
		ensureLoaded();
	}

	private static void ensureLoaded() {
		if (loaded) {
			return;
		}
		loaded = true;
		try {
			if (!FileUtils.fileExists(SESSION_FILE)) {
				return;
			}
			Bundle bundle = FileUtils.bundleFromFile(SESSION_FILE);
			deviceId = bundle.contains(KEY_DEVICE_ID) ? bundle.getString(KEY_DEVICE_ID) : null;
			jwt = bundle.contains(KEY_JWT) ? bundle.getString(KEY_JWT) : "";
			username = bundle.contains(KEY_USERNAME) ? bundle.getString(KEY_USERNAME) : "";
			hasCredentials = bundle.contains(KEY_HAS_CREDENTIALS) && bundle.getBoolean(KEY_HAS_CREDENTIALS);
			email = bundle.contains(KEY_EMAIL) ? bundle.getString(KEY_EMAIL) : "";
		} catch (IOException ignored) {
			deviceId = null;
			jwt = "";
			username = "";
			hasCredentials = false;
			email = "";
		}
	}

	private static void persist() {
		try {
			Bundle bundle = new Bundle();
			if (deviceId != null && !deviceId.isBlank()) {
				bundle.put(KEY_DEVICE_ID, deviceId);
			}
			bundle.put(KEY_JWT, jwt != null ? jwt : "");
			bundle.put(KEY_USERNAME, username != null ? username : "");
			bundle.put(KEY_HAS_CREDENTIALS, hasCredentials);
			bundle.put(KEY_EMAIL, email != null ? email : "");
			FileUtils.bundleToFile(SESSION_FILE, bundle);
		} catch (IOException ignored) {
		}
	}
}
