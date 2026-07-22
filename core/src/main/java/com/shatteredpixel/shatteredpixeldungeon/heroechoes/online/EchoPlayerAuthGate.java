package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndError;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Game;

import org.json.JSONObject;

/**
 * UI gate: ensure solo/ranked online play has a player username + session.
 */
public final class EchoPlayerAuthGate {

	private static final int EMAIL_MAX_LENGTH = 254;
	private static final int PASSWORD_MIN_LENGTH = 8;
	private static final int PASSWORD_MAX_LENGTH = 128;

	private EchoPlayerAuthGate() {
	}

	public interface Continuation {
		void proceed();
	}

	public static void ensureReadyThen(Continuation continuation) {
		promptForUsername(continuation, null, "");
	}

	private static void promptForUsername(Continuation continuation, String fieldError, String emailPrefill) {
		if (!EchoOnlineSettings.isConfigured()) {
			ShatteredPixelDungeon.scene().add(new WndError(Messages.get(TitleScene.class, "auth_offline")));
			return;
		}
		// Existing session with no field error → play. Field errors (e.g. email taken)
		// still show the form so the player can fix optional credentials.
		if (EchoPlayerSession.hasSession() && (fieldError == null || fieldError.isBlank())) {
			refreshInBackground();
			continuation.proceed();
			return;
		}
		String prefill = EchoPlayerAuth.preferredUsername();
		Game.scene().add(new WndTextInput(
				Messages.get(TitleScene.class, "auth_title"),
				Messages.get(TitleScene.class, "auth_body"),
				prefill,
				32,
				false,
				Messages.get(TitleScene.class, "auth_ok"),
				Messages.get(TitleScene.class, "auth_cancel"),
				fieldError,
				Messages.get(TitleScene.class, "auth_email_hint"),
				emailPrefill != null ? emailPrefill : "",
				EMAIL_MAX_LENGTH,
				false) {
			@Override
			public void onSelect(boolean positive, String text, String email) {
				if (!positive) {
					if (EchoPlayerSession.hasSession()) {
						continuation.proceed();
					}
					return;
				}
				String name = text != null ? text.trim() : "";
				String linkedEmail = email != null ? email.trim() : "";
				if (name.isEmpty()) {
					promptForUsername(continuation, Messages.get(TitleScene.class, "auth_empty"), linkedEmail);
					return;
				}
				if (!linkedEmail.isEmpty() && !looksLikeEmail(linkedEmail)) {
					promptForUsername(continuation, Messages.get(TitleScene.class, "auth_email_invalid"), linkedEmail);
					return;
				}
				SPDSettings.playerName(name);
				if (EchoPlayerSession.hasSession()) {
					if (linkedEmail.isEmpty()) {
						continuation.proceed();
					} else {
						promptForPassword(linkedEmail, continuation, null);
					}
					return;
				}
				registerThen(name, linkedEmail, continuation);
			}
		});
	}

	private static void registerThen(String username, String email, Continuation continuation) {
		new Thread(() -> {
			EchoPlayerAuth.SessionResult result = EchoPlayerAuth.ensureSession(EchoClient.createDefault(), username);
			Game.runOnRenderThread(() -> {
				if (result == EchoPlayerAuth.SessionResult.OK) {
					if (email != null && !email.isEmpty()) {
						promptForPassword(email, continuation, null);
					} else {
						continuation.proceed();
					}
					return;
				}
				if (result == EchoPlayerAuth.SessionResult.USERNAME_TAKEN) {
					promptForUsername(continuation, Messages.get(TitleScene.class, "auth_username_taken"), email);
					return;
				}
				ShatteredPixelDungeon.scene().add(
						new WndError(Messages.get(TitleScene.class, "auth_failed")));
			});
		}, "echo-player-auth").start();
	}

	private static void promptForPassword(String email, Continuation continuation, String fieldError) {
		WndTextInput wnd = new WndTextInput(
				Messages.get(TitleScene.class, "auth_password_title"),
				Messages.get(TitleScene.class, "auth_password_body", email),
				"",
				PASSWORD_MAX_LENGTH,
				false,
				Messages.get(TitleScene.class, "auth_ok"),
				Messages.get(TitleScene.class, "auth_cancel"),
				fieldError) {
			@Override
			public void onSelect(boolean positive, String text) {
				if (!positive) {
					// Session already exists; linking email is optional — continue play.
					continuation.proceed();
					return;
				}
				String password = text != null ? text : "";
				if (password.length() < PASSWORD_MIN_LENGTH) {
					promptForPassword(email, continuation, Messages.get(TitleScene.class, "auth_password_short"));
					return;
				}
				setCredentialsThen(email, password, continuation);
			}
		};
		wnd.setPasswordMode(true);
		Game.scene().add(wnd);
	}

	private static void setCredentialsThen(String email, String password, Continuation continuation) {
		new Thread(() -> {
			EchoClient client = EchoClient.createDefault();
			try {
				client.setCredentials(email, password);
				Game.runOnRenderThread(continuation::proceed);
			} catch (EchoHttpException e) {
				Game.runOnRenderThread(() -> {
					if (isEmailTaken(e)) {
						promptForUsername(continuation, Messages.get(TitleScene.class, "auth_email_taken"), email);
						return;
					}
					promptForPassword(email, continuation, Messages.get(TitleScene.class, "auth_credentials_failed"));
				});
			} catch (Exception e) {
				Game.reportException(e);
				Game.runOnRenderThread(() -> promptForPassword(
						email, continuation, Messages.get(TitleScene.class, "auth_credentials_failed")));
			}
		}, "echo-player-credentials").start();
	}

	private static void refreshInBackground() {
		new Thread(() -> EchoPlayerAuth.ensureSession(EchoClient.createDefault(), null), "echo-player-auth")
				.start();
	}

	static boolean looksLikeEmail(String email) {
		if (email == null) {
			return false;
		}
		int at = email.indexOf('@');
		return at > 0 && at < email.length() - 1 && email.indexOf('.', at) > at + 1;
	}

	static boolean isEmailTaken(EchoHttpException e) {
		if (e == null || e.statusCode != 422) {
			return false;
		}
		try {
			JSONObject json = new JSONObject(e.responseBody);
			Object detail = json.opt("detail");
			if (!(detail instanceof JSONObject)) {
				return false;
			}
			return ((JSONObject) detail).has("email");
		} catch (Exception ignored) {
			return false;
		}
	}
}
