package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(GdxTestExtension.class)
class OptionSliderTest {

	@BeforeEach
	void setupPlatform() {
		if (Game.platform == null) {
			PlatformSupport platform = mock(PlatformSupport.class);
			when(platform.splitforTextBlock(anyString(), anyBoolean()))
					.thenAnswer(invocation -> new String[]{ invocation.getArgument(0) });
			Game.platform = platform;
		}
	}

	@Test
	@DisplayName("setTitleText updates the visible slider title")
	void setTitleTextUpdatesVisibleTitle() {
		OptionSlider slider = new OptionSlider("Start Floor", "1", "26", 1, 26) {
			@Override
			protected void onChange() {
			}
		};

		slider.setTitleText("Start Floor: 14");

		Assertions.assertThat(slider.titleText()).isEqualTo("Start Floor: 14");
	}
}
