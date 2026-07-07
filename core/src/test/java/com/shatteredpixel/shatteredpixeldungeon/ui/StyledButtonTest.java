package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
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
class StyledButtonTest {

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
	@DisplayName("disabled button stays dimmed after fade alpha is applied")
	void disabledButtonStaysDimmedAfterFadeAlpha() {
		StyledButton button = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Ranked");

		button.enable(false);
		button.alpha(1f);

		Assertions.assertThat(button.alpha()).isEqualTo(0.3f);
	}

	@Test
	@DisplayName("enabled button uses full fade alpha")
	void enabledButtonUsesFadeAlpha() {
		StyledButton button = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "Ranked");

		button.enable(true);
		button.alpha(0.8f);

		Assertions.assertThat(button.alpha()).isEqualTo(0.8f);
	}

}
