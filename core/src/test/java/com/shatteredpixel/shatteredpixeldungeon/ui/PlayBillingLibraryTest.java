package com.shatteredpixel.shatteredpixeldungeon.ui;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class PlayBillingLibraryTest {

	@Test
	@DisplayName("Play Billing is 8.0.0 and reads QueryProductDetailsResult")
	void playBillingIs8() throws IOException {
		String gradle = readSource("android/build.gradle");
		Assertions.assertThat(gradle).contains("com.android.billingclient:billing:8.0.0");
		Assertions.assertThat(gradle).doesNotContain("billing:7.");

		String billing = readSource(
				"android/src/main/java/com/shatteredpixel/shatteredpixeldungeon/android/PlaySupportBilling.java");
		Assertions.assertThat(billing).contains("QueryProductDetailsResult");
		Assertions.assertThat(billing).contains("getProductDetailsList()");
	}

	private static String readSource(String relativePath) throws IOException {
		Path dir = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			Path candidate = dir.resolve(relativePath);
			if (Files.isRegularFile(dir.resolve("settings.gradle")) && Files.isRegularFile(candidate)) {
				return Files.readString(candidate, StandardCharsets.UTF_8);
			}
			dir = dir.getParent();
		}
		throw new AssertionError("Could not find " + relativePath);
	}
}
