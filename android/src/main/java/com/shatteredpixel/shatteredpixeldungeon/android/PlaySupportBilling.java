package com.shatteredpixel.shatteredpixeldungeon.android;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.shatteredpixel.shatteredpixeldungeon.services.billing.SupportBillingService;
import com.shatteredpixel.shatteredpixeldungeon.ui.SupportTipProducts;
import com.watabou.noosa.Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Play Billing tip purchases (consumable SKUs).
 */
public class PlaySupportBilling extends SupportBillingService implements PurchasesUpdatedListener {

	private final Activity activity;
	private BillingClient client;
	private boolean ready;
	private PurchaseCallback pendingCallback;
	private String pendingProductId;
	private final Map<String, ProductDetails> products = new HashMap<>();

	public PlaySupportBilling(Activity activity) {
		this.activity = activity;
		client = BillingClient.newBuilder(activity)
				.setListener(this)
				.enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
				.build();
		client.startConnection(new BillingClientStateListener() {
			@Override
			public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
				if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
					queryProducts();
				}
			}

			@Override
			public void onBillingServiceDisconnected() {
				ready = false;
			}
		});
	}

	private void queryProducts() {
		List<QueryProductDetailsParams.Product> list = new ArrayList<>();
		for (String id : SupportTipProducts.PRODUCT_IDS) {
			list.add(QueryProductDetailsParams.Product.newBuilder()
					.setProductId(id)
					.setProductType(BillingClient.ProductType.INAPP)
					.build());
		}
		client.queryProductDetailsAsync(
				QueryProductDetailsParams.newBuilder().setProductList(list).build(),
				(billingResult, productDetailsList) -> {
					if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
							&& productDetailsList != null) {
						products.clear();
						for (ProductDetails details : productDetailsList) {
							products.put(details.getProductId(), details);
						}
						ready = !products.isEmpty();
					}
				});
	}

	@Override
	public boolean isAvailable() {
		// Show Support entry on Play builds immediately; purchase waits for catalog if
		// needed.
		return true;
	}

	@Override
	public void purchase(String productId, PurchaseCallback callback) {
		ProductDetails details = products.get(productId);
		if (client == null || !client.isReady() || details == null) {
			callback.onError(ready ? "Product unavailable" : "Billing not ready yet");
			return;
		}
		pendingCallback = callback;
		pendingProductId = productId;
		List<BillingFlowParams.ProductDetailsParams> params = new ArrayList<>();
		params.add(BillingFlowParams.ProductDetailsParams.newBuilder()
				.setProductDetails(details)
				.build());
		BillingResult result = client.launchBillingFlow(activity,
				BillingFlowParams.newBuilder().setProductDetailsParamsList(params).build());
		if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
			PurchaseCallback cb = pendingCallback;
			pendingCallback = null;
			cb.onError(result.getDebugMessage());
		}
	}

	@Override
	public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
		PurchaseCallback callback = pendingCallback;
		pendingCallback = null;
		if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
			if (callback != null) {
				Game.runOnRenderThread(callback::onCancelled);
			}
			return;
		}
		if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK
				|| purchases == null || purchases.isEmpty()) {
			if (callback != null) {
				String msg = billingResult.getDebugMessage();
				Game.runOnRenderThread(() -> callback.onError(msg == null ? "Purchase failed" : msg));
			}
			return;
		}
		for (Purchase purchase : purchases) {
			consume(purchase, callback);
		}
	}

	private void consume(Purchase purchase, PurchaseCallback callback) {
		client.consumeAsync(
				ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
				(billingResult, purchaseToken) -> {
					if (callback == null) {
						return;
					}
					if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
						Game.runOnRenderThread(callback::onSuccess);
					} else {
						// Still acknowledge so Play doesn't refund if consume fails oddly
						client.acknowledgePurchase(
								AcknowledgePurchaseParams.newBuilder()
										.setPurchaseToken(purchase.getPurchaseToken())
										.build(),
								result -> {
								});
						String msg = billingResult.getDebugMessage();
						Game.runOnRenderThread(() -> callback.onError(msg == null ? "Consume failed" : msg));
					}
				});
	}
}
