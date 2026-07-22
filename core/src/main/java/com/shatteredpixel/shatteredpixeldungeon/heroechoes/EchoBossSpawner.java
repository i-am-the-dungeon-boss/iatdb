package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * Spawns and presents a hero echo boss instead of a regional boss when one is
 * pending.
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

	/**
	 * Adds the boss to the scene. Notices when a sprite exists; otherwise assigns
	 * the boss bar
	 * (headless / no GameScene sprite path).
	 */
	public static void present(Mob boss) {
		present(boss, 0f, true);
	}

	/** {@link #present(Mob)} with a turn delay (Prison Tengu/echo spawn). */
	public static void present(Mob boss, float delay) {
		present(boss, delay, true);
	}

	/**
	 * Adds the boss without calling {@link Mob#notice()}. Still assigns the boss
	 * bar when headless.
	 * Use for Halls (no seal-time notice) or City (custom FOV notice / fade).
	 */
	public static void present(Mob boss, boolean notice) {
		present(boss, 0f, notice);
	}

	public static void present(Mob boss, float delay, boolean notice) {
		GameScene.add(boss, delay);
		if (boss.sprite != null) {
			if (notice) {
				boss.notice();
			}
		} else {
			BossHealthBar.assignBoss(boss);
		}
	}

	/**
	 * Banner for an echo fight. Call only from echo start paths after
	 * {@link #shouldSpawn()}.
	 */
	public static void announceIntro() {
		GLog.h(introBannerText(Dungeon.getPendingEcho()));
	}

	public static String introBannerText(Echo echo) {
		if (echo == null) {
			return Messages.get(EchoBoss.class, "intro_default");
		}
		return Messages.get(
				EchoBoss.class,
				"intro",
				Echo.resolveUserName(echo.userName, echo.heroClass),
				heroClassTitle(echo.heroClass),
				Math.max(0, echo.killCount));
	}

	/**
	 * Defeat line shown when an echo boss falls — always names the echo by
	 * username.
	 */
	public static String defeatBannerText(Echo echo) {
		if (echo == null) {
			return Messages.get(EchoBoss.class, "defeated_default");
		}
		return Messages.get(
				EchoBoss.class,
				"defeated",
				Echo.resolveUserName(echo.userName, echo.heroClass));
	}

	static String heroClassTitle(String heroClass) {
		if (heroClass == null || heroClass.isEmpty()) {
			return Messages.get(EchoBoss.class, "name");
		}
		try {
			return HeroClass.valueOf(heroClass).title();
		} catch (IllegalArgumentException ignored) {
			return heroClass;
		}
	}
}
