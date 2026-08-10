package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

/**
 * Shared Echo action validation and refusal cleanup — no family-specific
 * effects.
 */
public final class EchoActionSupport {

	private EchoActionSupport() {
	}

	/**
	 * Clears busy / VFX turn ownership without spending actor time.
	 *
	 * @return always {@code false} so callers can {@code return refuse(ctx)}
	 */
	public static boolean refuse(EchoActionContext ctx) {
		if (ctx != null) {
			ctx.cancel();
		}
		return false;
	}

	/** Returns false (and refuses) when target is missing or negative. */
	public static boolean hasTarget(EchoActionContext ctx, Integer target) {
		if (ctx == null || target == null || target < 0) {
			refuse(ctx);
			return false;
		}
		return true;
	}
}
