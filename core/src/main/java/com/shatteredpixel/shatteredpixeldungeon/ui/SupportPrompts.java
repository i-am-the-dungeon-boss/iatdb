package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.services.billing.SupportBilling;
import com.watabou.utils.DeviceCompat;

/**
 * Gates support UI surfaces.
 * Patreon / external payment stays off; Play tip billing is enabled when the
 * platform provides it.
 */
public final class SupportPrompts {

	private SupportPrompts() {
	}

	/** Patreon / external-payment flows — always disabled for Play policy. */
	public static boolean externalSupportEnabled() {
		return false;
	}

	/**
	 * Whether the Support screen is reachable. Off on demo packages and on
	 * Android for now. On everywhere else, including where Play billing is
	 * unavailable, because those builds can open Ko-fi.
	 */
	public static boolean supportSceneEnabled() {
		return !Dungeon.demoPackage && !DeviceCompat.isAndroid();
	}

	/** Whether the in-app Play tip buttons should be shown. */
	public static boolean playBillingEnabled() {
		return SupportBilling.isAvailable();
	}
}
