package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/**
 * Localization keys for hero-boss UX.
 */
public final class EchoMessages {

	public static final String INTRO_BANNER = "actors.mobs.echoboss.intro";
	public static final String DEFEAT_LINE = "levels.echoboss.defeat";
	public static final String MOB_DESC = "actors.mobs.echoboss.desc";

	private EchoMessages() {}

	public static String introBannerKey() {
		return INTRO_BANNER;
	}

	public static String introBannerText(Echo echo) {
		if (echo == null) {
			return Messages.get(EchoBoss.class, "intro_default");
		}
		return Messages.get(EchoBoss.class, "intro", echo.heroClass, echo.lvl);
	}
}
