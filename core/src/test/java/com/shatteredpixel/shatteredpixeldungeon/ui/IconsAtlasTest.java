package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IconsAtlasTest {

	@Test
	@DisplayName("checkbox and chrome action buttons use the tinted icons atlas")
	void chromeActionButtonsUseTintedAtlas() {
		Assertions.assertThat(Icons.atlasFor(Icons.UNCHECKED)).isEqualTo(Assets.Interfaces.ICONS_TINTED);
		Assertions.assertThat(Icons.atlasFor(Icons.CHECKED)).isEqualTo(Assets.Interfaces.ICONS_TINTED);
		Assertions.assertThat(Icons.atlasFor(Icons.CLOSE)).isEqualTo(Assets.Interfaces.ICONS_TINTED);
		Assertions.assertThat(Icons.atlasFor(Icons.PLUS)).isEqualTo(Assets.Interfaces.ICONS_TINTED);
		Assertions.assertThat(Icons.atlasFor(Icons.REPEAT)).isEqualTo(Assets.Interfaces.ICONS_TINTED);
	}

	@Test
	@DisplayName("non-chrome icons use the untinted icons atlas")
	void nonChromeIconsUseUntintedAtlas() {
		Assertions.assertThat(Icons.atlasFor(Icons.IATDB)).isEqualTo(Assets.Interfaces.ICONS);
		Assertions.assertThat(Icons.atlasFor(Icons.JOURNAL)).isEqualTo(Assets.Interfaces.ICONS);
		Assertions.assertThat(Icons.atlasFor(Icons.INFO)).isEqualTo(Assets.Interfaces.ICONS);
	}
}
