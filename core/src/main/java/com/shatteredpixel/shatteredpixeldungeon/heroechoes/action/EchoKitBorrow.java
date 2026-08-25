package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.Callback;

/**
 * Maps the phantom kit's {@code pos} onto the EchoBoss body for legacy item
 * hooks, then restores it. Deferred wrappers keep the borrow until VFX
 * completion callbacks run.
 *
 * <p>The sprite is <em>not</em> borrowed: it is mirrored for the kit's whole
 * life by {@code EchoBoss.mirrorKitSprite}. {@code pos} still is, because it
 * goes stale mid-turn whenever the body leaps or blinks, so an effect fired
 * after the move must read the tile the body is standing on now.
 */
public final class EchoKitBorrow {

	private EchoKitBorrow() {
	}

	public static void run(EchoActionContext ctx, Runnable action) {
		if (ctx == null || action == null) {
			throw new IllegalArgumentException("EchoKitBorrow requires context and action");
		}
		Hero kit = ctx.stats();
		int savedPos = kit.pos;
		kit.pos = ctx.body.pos;
		try {
			action.run();
		} finally {
			kit.pos = savedPos;
		}
	}

	/**
	 * Borrows immediately; restores after {@code onComplete} runs (even on throw).
	 * Returns a callback the caller must invoke when deferred work finishes.
	 */
	public static Callback defer(EchoActionContext ctx, Callback onComplete) {
		if (ctx == null) {
			throw new IllegalArgumentException("EchoKitBorrow.defer requires context");
		}
		Hero kit = ctx.stats();
		int savedPos = kit.pos;
		kit.pos = ctx.body.pos;
		return () -> {
			try {
				if (onComplete != null) {
					onComplete.call();
				}
			} finally {
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
