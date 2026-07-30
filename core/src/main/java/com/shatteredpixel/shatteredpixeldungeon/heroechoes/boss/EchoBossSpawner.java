package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.SentryCrashReporting;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.DeviceCompat;

/**
 * Spawns and presents a hero echo boss instead of a regional boss when one is
 * pending.
 */
public final class EchoBossSpawner {

	public enum BossSpawnChoice {
		ECHO,
		DEFAULT,
		ABORT
	}

	/**
	 * Callbacks when spawn must recover asynchronously (mid-seal on render thread).
	 */
	public interface SpawnRecoveryActions {
		void onEcho();

		void onDefault();

		void onAbort();
	}

	private EchoBossSpawner() {
	}

	public static boolean shouldSpawn() {
		boolean spawn = canSpawnEchoBoss();
		DeviceCompat.log("EchoBoss", shouldSpawnDecision(spawn));
		return spawn;
	}

	/**
	 * True only when the last lookup for the current depth was {@code NOT_FOUND}.
	 * Regional default bosses must not spawn for ERROR / unset.
	 */
	public static boolean shouldSpawnDefault() {
		return Dungeon.wasEchoLookupNotFound();
	}

	/** Diagnostic line for {@link #shouldSpawn()} (also used by tests). */
	static String shouldSpawnDecision() {
		return shouldSpawnDecision(canSpawnEchoBoss());
	}

	private static boolean canSpawnEchoBoss() {
		Echo pending = Dungeon.getPendingEcho();
		return pending != null
				&& Dungeon.getPendingEchoPolicy() != null
				&& pending.depth == Dungeon.depth;
	}

	private static String shouldSpawnDecision(boolean spawn) {
		if (spawn) {
			return "shouldSpawn=true depth=" + Dungeon.depth;
		}
		return "shouldSpawn=false depth=" + Dungeon.depth + " reason=" + whyNotShouldSpawn();
	}

	private static String whyNotShouldSpawn() {
		Echo pending = Dungeon.getPendingEcho();
		if (pending == null) {
			return "no_pending_echo";
		}
		if (Dungeon.getPendingEchoPolicy() == null) {
			return "no_pending_policy";
		}
		if (pending.depth != Dungeon.depth) {
			return "pending_depth_mismatch pending=" + pending.depth;
		}
		return "unknown";
	}

	/**
	 * Resolves which boss to spawn. On unset/ERROR, reports to Sentry then runs
	 * prefetch recovery (Retry/Abort). Sync — safe from Interlevel loading thread
	 * and tests; mid-seal on the render thread should use
	 * {@link #ensureReadyThen(SpawnRecoveryActions)} instead.
	 */
	public static BossSpawnChoice resolveBossSpawn(Dungeon.PrefetchErrorHandler onError) {
		BossSpawnChoice ready = readyChoice();
		if (ready != null) {
			return ready;
		}
		reportSpawnRecoveryNeeded();
		EchoLookupOutcome outcome = Dungeon.prefetchEchoBossWithRankedRecovery(Dungeon.depth, onError);
		return choiceAfterRecovery(outcome);
	}

	/**
	 * Like {@link #resolveBossSpawn} but never blocks the render thread: if a
	 * fetch recovery is needed, runs it on a background thread and invokes
	 * {@code actions} on the render thread when done.
	 */
	public static void ensureReadyThen(SpawnRecoveryActions actions) {
		BossSpawnChoice ready = readyChoice();
		if (ready == BossSpawnChoice.ECHO) {
			actions.onEcho();
			return;
		}
		if (ready == BossSpawnChoice.DEFAULT) {
			actions.onDefault();
			return;
		}
		reportSpawnRecoveryNeeded();
		Thread recovery = new Thread(() -> {
			EchoLookupOutcome outcome = Dungeon.prefetchEchoBossWithRankedRecovery(
					Dungeon.depth, EchoBossFetchRecovery::promptRetryOrAbort);
			final BossSpawnChoice choice = choiceAfterRecovery(outcome);
			com.watabou.noosa.Game.runOnRenderThread(() -> {
				if (choice == BossSpawnChoice.ECHO) {
					actions.onEcho();
				} else if (choice == BossSpawnChoice.DEFAULT) {
					actions.onDefault();
				} else {
					actions.onAbort();
				}
			});
		}, "EchoBossSpawnRecovery");
		recovery.setDaemon(true);
		recovery.start();
	}

	private static BossSpawnChoice readyChoice() {
		if (canSpawnEchoBoss()) {
			return BossSpawnChoice.ECHO;
		}
		if (shouldSpawnDefault()) {
			return BossSpawnChoice.DEFAULT;
		}
		return null;
	}

	private static BossSpawnChoice choiceAfterRecovery(EchoLookupOutcome outcome) {
		if (outcome != null && outcome.isFound() && canSpawnEchoBoss()) {
			return BossSpawnChoice.ECHO;
		}
		if (outcome != null && outcome.isNotFound() && shouldSpawnDefault()) {
			return BossSpawnChoice.DEFAULT;
		}
		return BossSpawnChoice.ABORT;
	}

	private static void reportSpawnRecoveryNeeded() {
		String reason;
		if (Dungeon.isEchoLookupUnset()) {
			reason = "unset";
		} else if (Dungeon.wasEchoLookupError()) {
			reason = "ERROR";
		} else {
			reason = "inconsistent";
		}
		String message = "echo boss spawn requires recovery"
				+ " depth=" + Dungeon.depth
				+ " mode=" + Dungeon.echoPlayMode
				+ " reason=" + reason;
		DeviceCompat.log("EchoBoss", message);
		SentryCrashReporting.report(new IllegalStateException(message));
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
