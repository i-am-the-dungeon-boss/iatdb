package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.services.billing.SupportBilling;
import com.shatteredpixel.shatteredpixeldungeon.services.billing.SupportBillingService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SupportPromptsTest {

	@AfterEach
	void clearBilling() {
		SupportBilling.service = null;
	}

	@Test
	@DisplayName("disables external Patreon support surfaces")
	void disablesExternalPatreon() {
		Assertions.assertThat(SupportPrompts.externalSupportEnabled()).isFalse();
	}

	@Test
	@DisplayName("enables Play tip billing when billing service is available")
	void enablesPlayBillingWhenAvailable() {
		SupportBilling.service = new SupportBillingService() {
			@Override
			public boolean isAvailable() {
				return true;
			}

			@Override
			public void purchase(String productId, PurchaseCallback callback) {
			}
		};
		Assertions.assertThat(SupportPrompts.playBillingEnabled()).isTrue();
	}

	@Test
	@DisplayName("hides Play tip billing when billing service is missing")
	void hidesPlayBillingWhenUnavailable() {
		SupportBilling.service = null;
		Assertions.assertThat(SupportPrompts.playBillingEnabled()).isFalse();
	}
}
