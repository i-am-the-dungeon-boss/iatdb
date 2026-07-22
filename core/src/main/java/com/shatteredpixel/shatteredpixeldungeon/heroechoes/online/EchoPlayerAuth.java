package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.watabou.noosa.Game;

import org.json.JSONObject;

/**
 * Ensures a Payload player session exists for online play.
 */
public final class EchoPlayerAuth {

	public enum SessionResult {
		OK,
		USERNAME_TAKEN,
		FAILED
	}

	private EchoPlayerAuth() {
	}

	/**
	 * Registers or refreshes device auth using {@code username}.
	 * Empty username fails (caller should prompt first).
	 */
	public static SessionResult ensureSession(EchoClient client, String username) {
		if (client == null) {
			return SessionResult.FAILED;
		}
		try {
			if (EchoPlayerSession.hasSession()) {
				return refreshExistingDevice(client) ? SessionResult.OK : SessionResult.FAILED;
			}
			String name = username != null ? username.trim() : "";
			if (name.isEmpty()) {
				return SessionResult.FAILED;
			}
			boolean ok = client.authenticateDevice(EchoPlayerSession.deviceId(), name);
			if (ok && !EchoPlayerSession.username().isEmpty()) {
				SPDSettings.playerName(EchoPlayerSession.username());
			}
			return ok ? SessionResult.OK : SessionResult.FAILED;
		} catch (EchoHttpException e) {
			if (isUsernameTaken(e)) {
				EchoPlayerSession.clearSession();
				return SessionResult.USERNAME_TAKEN;
			}
			Game.reportException(e);
			EchoPlayerSession.clearSession();
			return SessionResult.FAILED;
		} catch (Exception e) {
			Game.reportException(e);
			EchoPlayerSession.clearSession();
			return SessionResult.FAILED;
		}
	}

	/**
	 * On app launch: if a local JWT exists, validate it with the server.
	 * <ul>
	 * <li>Valid → keep session</li>
	 * <li>Invalid/expired but device still registered → refresh JWT only (no new
	 * account)</li>
	 * <li>Player/device gone → clear session; next Solo/Ranked prompts for a new
	 * username</li>
	 * </ul>
	 */
	public static boolean validateOrRefreshOnLaunch(EchoClient client) {
		if (client == null || !EchoOnlineSettings.isConfigured()) {
			return true;
		}
		if (!EchoPlayerSession.hasSession()) {
			return true;
		}
		try {
			if (client.fetchMe()) {
				if (!EchoPlayerSession.username().isEmpty()) {
					SPDSettings.playerName(EchoPlayerSession.username());
				}
				return true;
			}
		} catch (Exception e) {
			Game.reportException(e);
		}

		// Do not pass a username here — that would silently recreate a deleted player.
		EchoPlayerSession.clearSession();
		return refreshExistingDevice(client);
	}

	/**
	 * Re-issues a JWT for an already-registered device. Never creates a player.
	 */
	static boolean refreshExistingDevice(EchoClient client) {
		try {
			return client.authenticateDevice(EchoPlayerSession.deviceId(), null);
		} catch (EchoHttpException e) {
			if (e.statusCode == 404 || e.statusCode == 401 || e.statusCode == 422) {
				EchoPlayerSession.clearSession();
				return false;
			}
			Game.reportException(e);
			EchoPlayerSession.clearSession();
			return false;
		} catch (Exception e) {
			Game.reportException(e);
			EchoPlayerSession.clearSession();
			return false;
		}
	}

	/** Preferred display name: session username, else SPDSettings player name. */
	public static String preferredUsername() {
		if (EchoPlayerSession.hasSession() && !EchoPlayerSession.username().isBlank()) {
			return EchoPlayerSession.username();
		}
		String local = SPDSettings.playerName();
		return local != null ? local.trim() : "";
	}

	static boolean isUsernameTaken(EchoHttpException e) {
		if (e == null || e.statusCode != 422) {
			return false;
		}
		try {
			JSONObject json = new JSONObject(e.responseBody);
			Object detail = json.opt("detail");
			if (!(detail instanceof JSONObject)) {
				return false;
			}
			return ((JSONObject) detail).has("username");
		} catch (Exception ignored) {
			return false;
		}
	}
}
