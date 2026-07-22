package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndError;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Game;

/**
 * UI gate: ensure solo/ranked online play has a player username + session.
 * Optional email may be entered; password / credential linking is not prompted
 * here.
 */
public final class EchoPlayerAuthGate {

	private static final int EMAIL_MAX_LENGTH = 254;

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
					continuation.proceed();
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
					continuation.proceed();
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
}
