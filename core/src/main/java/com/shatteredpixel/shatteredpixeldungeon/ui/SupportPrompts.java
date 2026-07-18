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

	/** Whether in-app Support entry points (Play tip billing) should be shown. */
	public static boolean playBillingEnabled() {
		return SupportBilling.isAvailable();
	}
}
