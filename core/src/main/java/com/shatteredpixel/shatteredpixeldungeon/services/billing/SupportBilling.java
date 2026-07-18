package com.shatteredpixel.shatteredpixeldungeon.services.billing;

/**
 * Facade for optional Play tip billing. Desktop/iOS leave {@link #service}
 * null.
 */
public final class SupportBilling {

	public static SupportBillingService service;

	private SupportBilling() {
	}

	public static boolean isAvailable() {
		return service != null && service.isAvailable();
	}

	public static void purchase(String productId, SupportBillingService.PurchaseCallback callback) {
		if (!isAvailable()) {
			callback.onError("Billing unavailable");
			return;
		}
		service.purchase(productId, callback);
	}
}
