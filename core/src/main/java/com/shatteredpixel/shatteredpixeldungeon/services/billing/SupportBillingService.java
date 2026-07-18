package com.shatteredpixel.shatteredpixeldungeon.services.billing;

/**
 * Platform billing for optional tip purchases (Play Billing on Android).
 */
public abstract class SupportBillingService {

	public interface PurchaseCallback {
		void onSuccess();

		void onCancelled();

		void onError(String message);
	}

	public abstract boolean isAvailable();

	public abstract void purchase(String productId, PurchaseCallback callback);
}
