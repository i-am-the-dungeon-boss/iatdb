package com.shatteredpixel.shatteredpixeldungeon.ui;

/**
 * Play Billing consumable tip SKUs (Play requires fixed product prices).
 */
public final class SupportTipProducts {

	public static final String[] PRODUCT_IDS = {
			"support_299",
			"support_499",
			"support_999",
			"support_1999",
			"support_4999"
	};

	private SupportTipProducts() {
	}

	public static int minimumUsdCents() {
		return 299;
	}

	public static String displayAmountUsd(String productId) {
		switch (productId) {
			case "support_299":
				return "$2.99";
			case "support_499":
				return "$4.99";
			case "support_999":
				return "$9.99";
			case "support_1999":
				return "$19.99";
			case "support_4999":
				return "$49.99";
			default:
				return productId;
		}
	}
}
