/*
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(GdxTestExtension.class)
class QuickSlotTest {

	@Test
	@DisplayName("isNonePlaceholder is false for an empty slot")
	void isNonePlaceholderFalseWhenEmpty() {
		QuickSlot slots = new QuickSlot();
		Assertions.assertThat(slots.isNonePlaceholder(0)).isFalse();
	}

	@Test
	@DisplayName("isNonePlaceholder is true for a real stackable item")
	void isNonePlaceholderTrueForRealItem() {
		QuickSlot slots = new QuickSlot();
		Item potion = new PotionOfHealing();
		slots.setSlot(0, potion);
		Assertions.assertThat(slots.isNonePlaceholder(0)).isTrue();
	}

	@Test
	@DisplayName("isNonePlaceholder is false for a quantity-0 placeholder")
	void isNonePlaceholderFalseForPlaceholder() {
		QuickSlot slots = new QuickSlot();
		Item potion = new PotionOfHealing();
		potion.quantity(0);
		slots.setSlot(0, potion);
		Assertions.assertThat(slots.isPlaceholder(0)).isTrue();
		Assertions.assertThat(slots.isNonePlaceholder(0)).isFalse();
	}

	@Test
	@DisplayName("isNonePlaceholder does not NPE when the slot is cleared between null-check and quantity")
	void isNonePlaceholderDoesNotNpeUnderConcurrentClear() throws InterruptedException {
		QuickSlot slots = new QuickSlot();
		Item potion = new PotionOfHealing();
		slots.setSlot(0, potion);

		AtomicBoolean sawNpe = new AtomicBoolean(false);
		Thread clearer = new Thread(() -> {
			for (int i = 0; i < 200_000; i++) {
				slots.clearSlot(0);
				slots.setSlot(0, potion);
			}
		});
		clearer.start();
		try {
			for (int i = 0; i < 200_000; i++) {
				try {
					slots.isNonePlaceholder(0);
				} catch (NullPointerException npe) {
					sawNpe.set(true);
					break;
				}
			}
		} finally {
			clearer.join();
		}

		Assertions.assertThat(sawNpe.get())
				.as("ANDROID-1J: double getItem() races with Actor-thread clear during GL refresh")
				.isFalse();
	}
}
