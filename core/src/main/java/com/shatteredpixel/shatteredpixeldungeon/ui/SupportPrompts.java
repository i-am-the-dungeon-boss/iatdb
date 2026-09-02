package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.services.billing.SupportBilling;

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
	 * Whether the Support screen is reachable. Always: every platform can open
	 * the Ko-fi page, even where Play tip billing is unavailable.
	 */
	public static boolean supportSceneEnabled() {
		return true;
	}

	/** Whether the in-app Play tip buttons should be shown. */
	public static boolean playBillingEnabled() {
		return SupportBilling.isAvailable();
	}
}
