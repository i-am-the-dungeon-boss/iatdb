package com.shatteredpixel.shatteredpixeldungeon.ui;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SupportTipProductsTest {

	@Test
	@DisplayName("offers a single $5 USD tip product")
	void singleFiveDollarTip() {
		Assertions.assertThat(SupportTipProducts.PRODUCT_IDS).containsExactly("support_500");
		Assertions.assertThat(SupportTipProducts.displayAmountUsd("support_500")).isEqualTo("$5");
		Assertions.assertThat(SupportTipProducts.minimumUsdCents()).isEqualTo(500);
	}
}
