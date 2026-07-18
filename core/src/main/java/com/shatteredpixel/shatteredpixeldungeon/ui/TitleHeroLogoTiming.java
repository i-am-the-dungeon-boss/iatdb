package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;

/**
 * Pure timing/layout helpers for the title-screen hero logo carousel
 * (ported from hero-echoes landing {@code HeroLogo}).
 */
public final class TitleHeroLogoTiming {

	public static final float SWAP_MS = 3200f;
	public static final float APPROACH_MS = 360f;
	public static final float ATTACK_FRAME_MS = 90f;
	public static final int ATTACK_FRAMES = 4;
	public static final float HIT_MS = ATTACK_FRAMES * ATTACK_FRAME_MS;
	public static final float SHATTER_MS = 480f;
	public static final float SHATTER_IMPACT_STEP = 2f;
	public static final float SHATTER_START_MS = APPROACH_MS + SHATTER_IMPACT_STEP * ATTACK_FRAME_MS;
	public static final float SETTLE_MS = 240f;
	public static final float SETTLE_START_MS = Math.max(APPROACH_MS + HIT_MS, SHATTER_START_MS + SHATTER_MS);
	public static final float TRANSITION_MS = SETTLE_START_MS + SETTLE_MS;
	public static final float HIT_END_MS = APPROACH_MS + HIT_MS;
	public static final float POTION_FLOAT_MS = 1000f;
	public static final float POTION_FLOAT_DISTANCE = 16f;
	public static final float POTION_HERO_HEIGHT_PX = 60f;
	public static final float VIEWPORT = 40f;
	public static final float HERO_SCALE = 2.5f;
	public static final float RUN_FRAME_MS = 50f;
	public static final float FLOAT_PERIOD_SEC = 3f;
	public static final float FLOAT_AMPLITUDE_PX = -6f;
	public static final float PULSE_GOLD_PERIOD_SEC = 2.4f;
	public static final float PULSE_GOLD_MIN_ALPHA = 0.82f;
	public static final float CREDITS_FLARE_RADIUS = 50f;
	public static final float CREDITS_FLARE_ANGULAR_SPEED_DEG = 40f;
	public static final float SETTLE_IDLE_FRAME_MS = 40f;
	public static final float SHATTER_DELAY_MAX_SEC = 0.14f;

	public static final int[] RUN_FRAME_INDICES = { 2, 3, 4, 5, 6, 7 };
	public static final int[] ATTACK_FRAME_INDICES = { 13, 14, 15, 0 };

	public static final HeroClass[] CLASSES = {
			HeroClass.WARRIOR,
			HeroClass.MAGE,
			HeroClass.ROGUE,
			HeroClass.HUNTRESS,
			HeroClass.DUELIST,
			HeroClass.CLERIC
	};

	public enum Segment {
		APPROACH, HIT, WAIT, SETTLE
	}

	private TitleHeroLogoTiming() {
	}

	public static Segment segmentAt(float elapsedMs) {
		if (elapsedMs < APPROACH_MS) {
			return Segment.APPROACH;
		}
		if (elapsedMs < APPROACH_MS + HIT_MS) {
			return Segment.HIT;
		}
		if (elapsedMs < SETTLE_START_MS) {
			return Segment.WAIT;
		}
		return Segment.SETTLE;
	}

	public static float walkerLeft(float elapsedMs, float startLeft, float impactLeft, float centerLeft) {
		Segment segment = segmentAt(elapsedMs);
		if (segment == Segment.APPROACH) {
			float progress = walkEase(elapsedMs / APPROACH_MS);
			return startLeft + (impactLeft - startLeft) * progress;
		}
		if (segment == Segment.HIT || segment == Segment.WAIT) {
			return impactLeft;
		}
		float settleElapsed = elapsedMs - SETTLE_START_MS;
		float progress = walkEase(Math.min(1f, settleElapsed / SETTLE_MS));
		return impactLeft + (centerLeft - impactLeft) * progress;
	}

	public static float impactLeft(float outgoingLeft, float outgoingWidth, float incomingWidth, boolean fromRight) {
		if (fromRight) {
			return outgoingLeft + outgoingWidth;
		}
		return outgoingLeft - incomingWidth;
	}

	public static float walkStartLeft(float incomingWidth, boolean fromRight, float viewport) {
		if (fromRight) {
			return viewport + 4f;
		}
		return -incomingWidth - 4f;
	}

	public static int nextClassIndex(int current) {
		return (current + 1) % CLASSES.length;
	}

	public static boolean fromRight(int incomingIndex) {
		return incomingIndex % 2 == 0;
	}

	public static float celebrationEndMs() {
		return TRANSITION_MS + POTION_FLOAT_MS;
	}

	public static float celebrationFlareMs() {
		return celebrationEndMs() - HIT_END_MS;
	}

	public static float experiencePotionFloatDistance(float heroHeightPx) {
		return POTION_FLOAT_DISTANCE * (heroHeightPx / POTION_HERO_HEIGHT_PX);
	}

	public static float creditsFlareRadius(float heroHeightPx) {
		return CREDITS_FLARE_RADIUS;
	}

	public static int attackStepAt(float hitElapsedMs) {
		int step = (int) Math.floor(hitElapsedMs / ATTACK_FRAME_MS);
		return Math.min(ATTACK_FRAME_INDICES.length - 1, Math.max(0, step));
	}

	public static int runFrameIndex(int runStep) {
		int idx = ((runStep % RUN_FRAME_INDICES.length) + RUN_FRAME_INDICES.length) % RUN_FRAME_INDICES.length;
		return RUN_FRAME_INDICES[idx];
	}

	public static int attackFrameIndex(int attackStep) {
		int idx = ((attackStep % ATTACK_FRAME_INDICES.length) + ATTACK_FRAME_INDICES.length)
				% ATTACK_FRAME_INDICES.length;
		return ATTACK_FRAME_INDICES[idx];
	}

	public static boolean usesIdleFrame(float elapsedMs) {
		return elapsedMs >= TRANSITION_MS - SETTLE_IDLE_FRAME_MS;
	}

	/**
	 * Vertical bob matching hero-echoes {@code animate-float}
	 * ({@code float 3s ease-in-out}, 0 → -6px → 0).
	 */
	public static float idleBobY(float timeSec) {
		float period = FLOAT_PERIOD_SEC;
		float t = timeSec / period;
		t = t - (float) Math.floor(t);
		if (t <= 0.5f) {
			return FLOAT_AMPLITUDE_PX * cssEaseInOut(t / 0.5f);
		}
		return FLOAT_AMPLITUDE_PX * (1f - cssEaseInOut((t - 0.5f) / 0.5f));
	}

	/**
	 * Gold title pulse matching hero-echoes {@code animate-pulse-gold}
	 * ({@code pulse-gold 2.4s ease-in-out}, opacity 1 → 0.82 → 1).
	 */
	public static float pulseGoldAlpha(float timeSec) {
		float period = PULSE_GOLD_PERIOD_SEC;
		float t = timeSec / period;
		t = t - (float) Math.floor(t);
		float min = PULSE_GOLD_MIN_ALPHA;
		if (t <= 0.5f) {
			return 1f + (min - 1f) * cssEaseInOut(t / 0.5f);
		}
		return min + (1f - min) * cssEaseInOut((t - 0.5f) / 0.5f);
	}

	/**
	 * Opacity envelope matching hero-echoes {@code potion-float-fade} /
	 * {@code credits-flare-fade} (10% fade-in, hold to 50%, then out).
	 */
	public static float potionFloatAlpha(float progress) {
		float t = clamp01(progress);
		if (t <= 0.10f) {
			return t / 0.10f;
		}
		if (t <= 0.50f) {
			return 1f;
		}
		return 1f - (t - 0.50f) / 0.50f;
	}

	/** CSS {@code ease-out} progress for potion float distance. */
	public static float potionFloatProgress(float progress) {
		float t = clamp01(progress);
		return 1f - (1f - t) * (1f - t);
	}

	/**
	 * CSS {@code cubic-bezier(0.45, 0, 0.75, 0.15)} used by
	 * hero-echoes {@code shatter-out}.
	 */
	public static float shatterEase(float progress) {
		return cubicBezier(clamp01(progress), 0.45f, 0f, 0.75f, 0.15f);
	}

	private static float walkEase(float progress) {
		float t = clamp01(progress);
		return 1f - (1f - t) * (1f - t);
	}

	/** CSS {@code ease-in-out} = cubic-bezier(0.42, 0, 0.58, 1). */
	private static float cssEaseInOut(float progress) {
		return cubicBezier(clamp01(progress), 0.42f, 0f, 0.58f, 1f);
	}

	private static float clamp01(float value) {
		return Math.max(0f, Math.min(1f, value));
	}

	/**
	 * Unit cubic-bezier solver (WebKit UnitBezier), control points
	 * (0,0), (p1x,p1y), (p2x,p2y), (1,1).
	 */
	static float cubicBezier(float x, float p1x, float p1y, float p2x, float p2y) {
		float cx = 3f * p1x;
		float bx = 3f * (p2x - p1x) - cx;
		float ax = 1f - cx - bx;

		float cy = 3f * p1y;
		float by = 3f * (p2y - p1y) - cy;
		float ay = 1f - cy - by;

		float t = solveCubicBezierX(x, ax, bx, cx);
		return ((ay * t + by) * t + cy) * t;
	}

	private static float solveCubicBezierX(float x, float ax, float bx, float cx) {
		float t2 = x;
		for (int i = 0; i < 8; i++) {
			float x2 = ((ax * t2 + bx) * t2 + cx) * t2 - x;
			if (Math.abs(x2) < 1e-6f) {
				return t2;
			}
			float d2 = (3f * ax * t2 + 2f * bx) * t2 + cx;
			if (Math.abs(d2) < 1e-6f) {
				break;
			}
			t2 = t2 - x2 / d2;
		}

		float t0 = 0f;
		float t1 = 1f;
		t2 = clamp01(x);
		while (t0 < t1) {
			float x2 = ((ax * t2 + bx) * t2 + cx) * t2;
			if (Math.abs(x2 - x) < 1e-6f) {
				return t2;
			}
			if (x > x2) {
				t0 = t2;
			} else {
				t1 = t2;
			}
			t2 = (t1 - t0) * 0.5f + t0;
		}
		return t2;
	}
}
