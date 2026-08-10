package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Callback;

/**
 * Maps the phantom kit's {@code pos}/{@code sprite} onto the EchoBoss body for
 * legacy item hooks, then restores them. Deferred wrappers keep the borrow
 * until
 * VFX completion callbacks run.
 * Keep borrowed sprite/pos until deferred onZap finishes (ANDROID-1N /
 * ANDROID-1K).
 */
public final class EchoKitBorrow {

	private EchoKitBorrow() {
	}

	public static void run(EchoActionContext ctx, Runnable action) {
		if (ctx == null || action == null) {
			throw new IllegalArgumentException("EchoKitBorrow requires context and action");
		}
		Hero kit = ctx.kit;
		int savedPos = kit.pos;
		CharSprite savedSprite = kit.sprite;
		kit.pos = ctx.body.pos;
		kit.sprite = ctx.body.sprite;
		try {
			action.run();
		} finally {
			kit.sprite = savedSprite;
			kit.pos = savedPos;
		}
	}

	/**
	 * Borrows immediately; restores after {@code onComplete} runs (even on throw).
	 * Returns a callback the caller must invoke when deferred work finishes.
	 * Keep borrowed sprite/pos until deferred onZap finishes (ANDROID-1N /
	 * ANDROID-1K).
	 */
	public static Callback defer(EchoActionContext ctx, Callback onComplete) {
		if (ctx == null) {
			throw new IllegalArgumentException("EchoKitBorrow.defer requires context");
		}
		Hero kit = ctx.kit;
		int savedPos = kit.pos;
		CharSprite savedSprite = kit.sprite;
		kit.pos = ctx.body.pos;
		kit.sprite = ctx.body.sprite;
		return () -> {
			try {
				if (onComplete != null) {
					onComplete.call();
				}
			} finally {
				kit.sprite = savedSprite;
				kit.pos = savedPos;
			}
		};
	}

	/**
	 * Borrows for {@code whileBorrowed}, then keeps the borrow until the callback
	 * handed to {@code captureDeferred} is invoked.
	 */
	public static void runDeferred(
			EchoActionContext ctx,
			DeferredCapture captureDeferred,
			Runnable whileBorrowed) {
		if (ctx == null || captureDeferred == null) {
			throw new IllegalArgumentException("EchoKitBorrow.runDeferred requires context and capture");
		}
		Callback finish = defer(ctx, null);
		if (whileBorrowed != null) {
			whileBorrowed.run();
		}
		captureDeferred.accept(finish);
	}

	/** RoboVM-safe capture for the deferred restore callback. */
	public interface DeferredCapture {
		void accept(Callback deferredRestore);
	}
}
