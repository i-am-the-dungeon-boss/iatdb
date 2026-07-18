package com.shatteredpixel.shatteredpixeldungeon.ui;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SupportTipProductsTest {

	@Test
	@DisplayName("tip ladder starts at 2.99 and uses support_* product ids")
	void tipLadderMinimumAndIds() {
		Assertions.assertThat(SupportTipProducts.PRODUCT_IDS)
				.containsExactly("support_299", "support_499", "support_999", "support_1999", "support_4999");
		Assertions.assertThat(SupportTipProducts.displayAmountUsd("support_299")).isEqualTo("$2.99");
		Assertions.assertThat(SupportTipProducts.minimumUsdCents()).isEqualTo(299);
	}
}
