package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
import com.watabou.utils.Strings;

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
	private static final String KEY_MUTED_UNTIL = "muted_until";

	private static String deviceId;
	private static String jwt = "";
	private static String username = "";
	private static boolean hasCredentials;
	private static String email = "";
	private static long mutedUntil;
	private static boolean loaded;

	private EchoPlayerSession() {
	}

	public static synchronized String deviceId() {
		ensureLoaded();
		if (Strings.isBlank(deviceId)) {
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
		return !Strings.isBlank(jwt) && !Strings.isBlank(username);
	}

	/**
	 * Epoch ms at which a server-side chat mute lifts; 0 when there is none.
	 *
	 * <p>Delivered by the authentication response rather than asked for, and held
	 * here as a raw deadline. That is the whole reason the world socket carries no
	 * mute traffic: the deadline is known locally, so the client can grey its own
	 * composer out and let the mute expire without another round trip.
	 *
	 * <p>Whether it is still running is
	 * {@link com.shatteredpixel.shatteredpixeldungeon.worldnet.ServerMute}'s
	 * question, not this class's — the session stores what the server said and
	 * nothing more.
	 */
	public static synchronized long mutedUntil() {
		ensureLoaded();
		return mutedUntil;
	}

	public static synchronized void applyAuthResponse(
			String token,
			String name,
			boolean credentials,
			String linkedEmail,
			long muteExpiry) {
		ensureLoaded();
		jwt = token != null ? token : "";
		username = name != null ? name.trim() : "";
		hasCredentials = credentials;
		email = linkedEmail != null ? linkedEmail.trim() : "";
		// Taken as given rather than merged with what is already held: the server
		// is the authority, and an absent value means the mute has been lifted.
		mutedUntil = Math.max(muteExpiry, 0L);
		persist();
	}

	public static synchronized void clearSession() {
		ensureLoaded();
		jwt = "";
		username = "";
		hasCredentials = false;
		email = "";
		mutedUntil = 0L;
		persist();
	}

	/** Test-only: wipe in-memory + file state. */
	public static synchronized void resetForTests() {
		deviceId = null;
		jwt = "";
		username = "";
		hasCredentials = false;
		email = "";
		mutedUntil = 0L;
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
			mutedUntil = bundle.contains(KEY_MUTED_UNTIL) ? bundle.getLong(KEY_MUTED_UNTIL) : 0L;
		} catch (IOException ignored) {
			deviceId = null;
			jwt = "";
			username = "";
			hasCredentials = false;
			email = "";
			mutedUntil = 0L;
		}
	}

	private static void persist() {
		try {
			Bundle bundle = new Bundle();
			if (!Strings.isBlank(deviceId)) {
				bundle.put(KEY_DEVICE_ID, deviceId);
			}
			bundle.put(KEY_JWT, jwt != null ? jwt : "");
			bundle.put(KEY_USERNAME, username != null ? username : "");
			bundle.put(KEY_HAS_CREDENTIALS, hasCredentials);
			bundle.put(KEY_EMAIL, email != null ? email : "");
			bundle.put(KEY_MUTED_UNTIL, mutedUntil);
			FileUtils.bundleToFile(SESSION_FILE, bundle);
		} catch (IOException ignored) {
		}
	}
}
