package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ExtendWith(GdxTestExtension.class)
class DebugArenaItemsTest {

	@Test
	@DisplayName("builds one identified item instance per catalog Item class")
	void buildsOneIdentifiedItemPerCatalogItemClass() {
		List<Item> items = DebugArenaItems.createAll();

		Set<Class<?>> expected = new HashSet<>();
		for (Catalog catalog : Catalog.values()) {
			if (catalog == Catalog.ENCHANTMENTS || catalog == Catalog.GLYPHS) {
				continue;
			}
			for (Class<?> cls : catalog.items()) {
				if (Item.class.isAssignableFrom(cls)) {
					expected.add(cls);
				}
			}
		}

		Assertions.assertThat(items).isNotEmpty();
		Assertions.assertThat(items).hasSize(expected.size());
		Set<Class<?>> actual = new HashSet<>();
		for (Item item : items) {
			Assertions.assertThat(item.isIdentified()).isTrue();
			actual.add(item.getClass());
		}
		Assertions.assertThat(actual).isEqualTo(expected);
	}
}
