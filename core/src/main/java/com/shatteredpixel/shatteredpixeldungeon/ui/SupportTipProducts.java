package com.shatteredpixel.shatteredpixeldungeon.ui;

/**
 * Play Billing consumable tip SKUs (Play requires fixed product prices).
 */
public final class SupportTipProducts {

	public static final String[] PRODUCT_IDS = {
			"support_500"
	};

	private SupportTipProducts() {
	}

	public static int minimumUsdCents() {
		return 500;
	}

	public static String displayAmountUsd(String productId) {
		switch (productId) {
			case "support_500":
				return "$5";
			default:
				return productId;
		}
	}
}
