package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndError;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Game;
import com.watabou.utils.Strings;

/**
 * UI gate: ensure solo/ranked online play has a player username + session.
 */
public final class EchoPlayerAuthGate {

	private EchoPlayerAuthGate() {
	}

	public interface Continuation {
		void proceed();
	}

	public static void ensureReadyThen(Continuation continuation) {
		promptForUsername(continuation, null);
	}

	private static void promptForUsername(Continuation continuation, String fieldError) {
		if (!EchoOnlineSettings.isConfigured()) {
			ShatteredPixelDungeon.scene().add(new WndError(Messages.get(TitleScene.class, "auth_offline")));
			return;
		}
		if (EchoPlayerSession.hasSession() && Strings.isBlank(fieldError)) {
			refreshInBackground();
			continuation.proceed();
			return;
		}
		String prefill = EchoPlayerAuth.preferredUsername();
		// Paused email field: Messages.get(TitleScene.class, "auth_email_hint"),
		// email prefill, EMAIL_MAX_LENGTH 254, and looksLikeEmail before register.
		Game.scene().add(new WndTextInput(
				Messages.get(TitleScene.class, "auth_title"),
				Messages.get(TitleScene.class, "auth_body"),
				prefill,
				32,
				false,
				Messages.get(TitleScene.class, "auth_ok"),
				Messages.get(TitleScene.class, "auth_cancel"),
				fieldError,
				false) {
			@Override
			public void onSelect(boolean positive, String text) {
				if (!positive) {
					if (EchoPlayerSession.hasSession()) {
						continuation.proceed();
					}
					return;
				}
				String name = text != null ? text.trim() : "";
				if (name.isEmpty()) {
					promptForUsername(continuation, Messages.get(TitleScene.class, "auth_empty"));
					return;
				}
				SPDSettings.playerName(name);
				if (EchoPlayerSession.hasSession()) {
					continuation.proceed();
					return;
				}
				registerThen(name, continuation);
			}
		});
	}

	private static void registerThen(String username, Continuation continuation) {
		new Thread(() -> {
			EchoPlayerAuth.SessionResult result = EchoPlayerAuth.ensureSession(EchoClient.createDefault(), username);
			Game.runOnRenderThread(() -> {
				if (result == EchoPlayerAuth.SessionResult.OK) {
					continuation.proceed();
					return;
				}
				if (result == EchoPlayerAuth.SessionResult.USERNAME_TAKEN) {
					promptForUsername(continuation, Messages.get(TitleScene.class, "auth_username_taken"));
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

}
