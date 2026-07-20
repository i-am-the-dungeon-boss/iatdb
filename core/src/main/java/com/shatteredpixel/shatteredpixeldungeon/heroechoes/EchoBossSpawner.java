package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * Spawns a hero echo boss instead of a regional boss when one is pending.
 */
public final class EchoBossSpawner {

	private EchoBossSpawner() {
	}

	public static boolean shouldSpawn() {
		Echo pending = Dungeon.getPendingEcho();
		return Dungeon.isEchoBossActive()
				&& pending != null
				&& Dungeon.getPendingEchoPolicy() != null
				&& pending.depth == Dungeon.depth;
	}

	public static EchoBoss create(int depth) {
		return new EchoBoss(Dungeon.getPendingEcho(), depth);
	}

	public static Mob createRegionalBoss(Mob defaultBoss) {
		if (shouldSpawn()) {
			EchoBoss echoBoss = create(Dungeon.depth);
			return echoBoss;
		}
		return defaultBoss;
	}

	public static void announceIntroIfNeeded() {
		if (shouldSpawn()) {
			GLog.h(introBannerText(Dungeon.getPendingEcho()));
		}
	}

	public static String introBannerText(Echo echo) {
		if (echo == null) {
			return Messages.get(EchoBoss.class, "intro_default");
		}
		return Messages.get(EchoBoss.class, "intro", echo.heroClass, echo.lvl);
	}
}
