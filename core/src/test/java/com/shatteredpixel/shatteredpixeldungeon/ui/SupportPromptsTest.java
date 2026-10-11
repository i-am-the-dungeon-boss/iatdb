package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.badlogic.gdx.utils.Os;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.services.billing.SupportBilling;
import com.shatteredpixel.shatteredpixeldungeon.services.billing.SupportBillingService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SupportPromptsTest {

	private Os originalOs;

	@BeforeEach
	void rememberOs() {
		originalOs = SharedLibraryLoader.os;
	}

	@AfterEach
	void clearBilling() {
		SupportBilling.service = null;
		Dungeon.demoPackage = false;
		SharedLibraryLoader.os = originalOs;
	}

	@Test
	@DisplayName("disables external Patreon support surfaces")
	void disablesExternalPatreon() {
		Assertions.assertThat(SupportPrompts.externalSupportEnabled()).isFalse();
	}

	@Test
	@DisplayName("keeps the support screen reachable without billing (Ko-fi link)")
	void supportSceneEnabledWithoutBilling() {
		SupportBilling.service = null;
		Dungeon.demoPackage = false;
		Assertions.assertThat(SupportPrompts.supportSceneEnabled()).isTrue();
	}

	@Test
	@DisplayName("hides the support screen on a demo package")
	void hidesSupportSceneOnDemoPackage() {
		Dungeon.demoPackage = true;
		Assertions.assertThat(SupportPrompts.supportSceneEnabled()).isFalse();
	}

	@Test
	@DisplayName("hides the support screen on Android builds")
	void hidesSupportSceneOnAndroid() {
		SharedLibraryLoader.os = Os.Android;
		Dungeon.demoPackage = false;
		Assertions.assertThat(SupportPrompts.supportSceneEnabled()).isFalse();
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
