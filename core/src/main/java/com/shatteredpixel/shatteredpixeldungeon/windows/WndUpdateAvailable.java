package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.AvailableUpdateData;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.watabou.noosa.Game;

/**
 * Forced title-screen update gate when the installed game version does not
 * match the backend {@code version_name}. Cannot be dismissed or bypassed.
 */
public class WndUpdateAvailable extends WndOptions {

	@FunctionalInterface
	public interface Listener {
		void onUpdate();
	}

	private final Listener listener;

	public WndUpdateAvailable(AvailableUpdateData update, Listener listener) {
		super(
				Icons.get(Icons.CHANGES),
				titleFor(update),
				bodyFor(update, Game.version),
				Messages.get(WndUpdateAvailable.class, "update"));
		this.listener = listener;
	}

	static String titleFor(AvailableUpdateData update) {
		if (update != null && update.versionName != null && !update.versionName.isEmpty()) {
			return Messages.get(WndUpdateAvailable.class, "versioned_title", update.versionName);
		}
		return Messages.get(WndUpdateAvailable.class, "title");
	}

	static String bodyFor(AvailableUpdateData update, String installedVersion) {
		String installed = installedVersion == null || installedVersion.isEmpty() ? "?" : installedVersion;
		String required = update != null && update.versionName != null && !update.versionName.isEmpty()
				? update.versionName
				: "?";
		if (update != null && update.desc != null && !update.desc.isEmpty()) {
			return Messages.get(WndUpdateAvailable.class, "desc_versions_notes", installed, required, update.desc);
		}
		return Messages.get(WndUpdateAvailable.class, "desc_versions", installed, required);
	}

	static boolean allowsBackDismiss() {
		return false;
	}

	static boolean allowsHide() {
		return false;
	}

	@Override
	protected void onSelect(int index) {
		notifySelect(listener, index);
	}

	static void notifySelect(Listener listener, int index) {
		if (index == 0) {
			listener.onUpdate();
		}
	}

	@Override
	public void onBackPressed() {
		// Forced update — cannot leave without installing the matching version.
	}

	@Override
	public void hide() {
		if (allowsHide()) {
			super.hide();
		}
		// Otherwise stay on screen so the title menu cannot be used.
	}
}
