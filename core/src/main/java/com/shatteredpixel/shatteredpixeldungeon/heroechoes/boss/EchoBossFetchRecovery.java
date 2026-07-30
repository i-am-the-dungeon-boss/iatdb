package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPrefetchUserChoice;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEchoFetchFailed;
import com.watabou.noosa.Game;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared Retry/Abort prompt and abort-to-title path for echo boss fetch
 * recovery.
 */
public final class EchoBossFetchRecovery {

	private EchoBossFetchRecovery() {
	}

	/**
	 * Blocks the calling (non-render) thread until the user chooses Retry or Abort.
	 * Schedules {@link WndEchoFetchFailed} on the render thread.
	 */
	public static EchoPrefetchUserChoice promptRetryOrAbort(EchoLookupOutcome failed) {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<EchoPrefetchUserChoice> choice = new AtomicReference<>(EchoPrefetchUserChoice.ABORT);
		String hint = failed != null ? failed.failureHint() : "";
		Game.runOnRenderThread(() -> {
			Game.scene().add(new WndEchoFetchFailed(new WndEchoFetchFailed.Listener() {
				@Override
				public void onRetry() {
					choice.set(EchoPrefetchUserChoice.RETRY);
					latch.countDown();
				}

				@Override
				public void onAbort() {
					choice.set(EchoPrefetchUserChoice.ABORT);
					latch.countDown();
				}
			}, hint));
		});
		try {
			latch.await();
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
		return choice.get();
	}

	/**
	 * Persists the run and returns to the title (same outcome as Interlevel abort).
	 */
	public static void abortToTitle() {
		try {
			if (Dungeon.hero != null) {
				Dungeon.saveAll();
			}
		} catch (Exception ignored) {
		}
		Game.switchScene(TitleScene.class);
	}

	/**
	 * Thrown when boss spawn recovery aborts during level generation (loading
	 * thread).
	 * Interlevel treats this like echo fetch abort (return to title).
	 */
	public static final class SpawnAbortedException extends RuntimeException {
		public SpawnAbortedException() {
			super("echo boss spawn aborted");
		}
	}

	public static void throwIfAborted(EchoBossSpawner.BossSpawnChoice choice) {
		if (choice == EchoBossSpawner.BossSpawnChoice.ABORT) {
			throw new SpawnAbortedException();
		}
	}
}
