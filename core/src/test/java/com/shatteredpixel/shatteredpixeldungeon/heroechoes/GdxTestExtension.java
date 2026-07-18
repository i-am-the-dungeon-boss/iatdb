package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Initializes libGDX headless runtime once for unit tests that touch game actors. */
public class GdxTestExtension implements BeforeAllCallback {

	private static boolean initialized;

	@Override
	public void beforeAll(ExtensionContext context) {
		initIfNeeded();
	}

	public static void initIfNeeded() {
		if (initialized) return;
		HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
		config.updatesPerSecond = 30;
		new HeadlessApplication(new ApplicationAdapter() {}, config);
		FileUtils.setDefaultFileProperties(Files.FileType.Local, "");
		Game.version = EchoTestSupport.TEST_GAME_VERSION;
		Messages.setup(Languages.ENGLISH);
		Scroll.initLabels();
		Potion.initColors();
		Badges.loadGlobal();
		initialized = true;
	}
}
