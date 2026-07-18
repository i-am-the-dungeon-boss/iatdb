package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;

/**
 * Title-screen hero carousel matching the hero-echoes landing {@code HeroLogo}
 * animation.
 */
public class TitleHeroLogo extends Component {

	private static final int ARMOR_TIER = 6;
	private static final int SHATTER_COLS = 4;
	private static final int SHATTER_ROWS = 5;
	private static final int IDLE_FRAME = 0;

	private enum Phase {
		IDLE, TRANSITION
	}

	private final float viewport = TitleHeroLogoTiming.VIEWPORT;
	private final Image hero = new Image();
	private final Image incoming = new Image();

	private int classIndex;
	private Phase phase = Phase.IDLE;
	private float phaseTime;
	private float idleTime;
	private boolean fromRight;
	private float startLeft;
	private float impactLeft;
	private float centerLeft;
	private Flare celebrationFlare;
	private FloatAway celebrationText;
	private boolean celebrationStarted;
	private float celebrationAge;
	private int lastRunStep = -1;
	private int lastAttackStep = -1;
	private int lastFrameIndex = -1;
	private boolean shatterSpawned;

	public TitleHeroLogo() {
		classIndex = 0;
		showHero(hero, TitleHeroLogoTiming.CLASSES[classIndex], IDLE_FRAME);
		hero.scale.set(TitleHeroLogoTiming.HERO_SCALE);
		incoming.scale.set(TitleHeroLogoTiming.HERO_SCALE);
		incoming.visible = false;
		add(hero);
		add(incoming);
		width = preferredWidth();
		height = preferredHeight();
		layoutIdle();
	}

	public float preferredWidth() {
		return viewport;
	}

	public float preferredHeight() {
		return viewport + 8f;
	}

	@Override
	public void update() {
		super.update();
		float elapsed = com.watabou.noosa.Game.elapsed;
		idleTime += elapsed;

		if (phase == Phase.IDLE) {
			phaseTime += elapsed * 1000f;
			layoutIdle();
			if (phaseTime >= TitleHeroLogoTiming.SWAP_MS) {
				beginTransition();
			}
		} else {
			phaseTime += elapsed * 1000f;
			updateTransitionFrames();
			layoutTransition();

			if (!shatterSpawned && phaseTime >= TitleHeroLogoTiming.SHATTER_START_MS) {
				spawnShatter();
				hero.visible = false;
				shatterSpawned = true;
			}

			if (!celebrationStarted && phaseTime >= TitleHeroLogoTiming.HIT_END_MS) {
				startCelebration();
			}

			if (phaseTime >= TitleHeroLogoTiming.TRANSITION_MS) {
				finishTransition();
			}
		}

		updateCelebration(elapsed);
	}

	private void beginTransition() {
		phase = Phase.TRANSITION;
		phaseTime = 0;
		celebrationStarted = false;
		celebrationAge = 0;
		shatterSpawned = false;
		lastRunStep = -1;
		lastAttackStep = -1;
		lastFrameIndex = -1;
		int next = TitleHeroLogoTiming.nextClassIndex(classIndex);
		fromRight = TitleHeroLogoTiming.fromRight(next);
		showHero(incoming, TitleHeroLogoTiming.CLASSES[next], TitleHeroLogoTiming.runFrameIndex(0));
		incoming.visible = true;
		hero.visible = true;
		showHero(hero, TitleHeroLogoTiming.CLASSES[classIndex], IDLE_FRAME);

		float bob = TitleHeroLogoTiming.idleBobY(idleTime);
		centerLeft = (viewport - hero.width()) / 2f;
		float heroTop = y + (viewport - hero.height()) / 2f + bob;
		hero.x = x + centerLeft;
		hero.y = heroTop;

		impactLeft = TitleHeroLogoTiming.impactLeft(centerLeft, hero.width(), incoming.width(), fromRight);
		startLeft = TitleHeroLogoTiming.walkStartLeft(incoming.width(), fromRight, viewport);
		incoming.x = x + startLeft;
		incoming.y = heroTop;
		incoming.flipHorizontal = fromRight;
	}

	private void layoutIdle() {
		float bob = TitleHeroLogoTiming.idleBobY(idleTime);
		hero.x = x + (viewport - hero.width()) / 2f;
		hero.y = y + (viewport - hero.height()) / 2f + bob;
		hero.visible = true;
		incoming.visible = false;
	}

	private void layoutTransition() {
		float bob = TitleHeroLogoTiming.idleBobY(idleTime);
		float heroTop = y + (viewport - hero.height()) / 2f + bob;
		incoming.x = x + TitleHeroLogoTiming.walkerLeft(phaseTime, startLeft, impactLeft, centerLeft);
		incoming.y = heroTop;
		hero.x = x + centerLeft;
		hero.y = heroTop;
	}

	private void updateTransitionFrames() {
		TitleHeroLogoTiming.Segment segment = TitleHeroLogoTiming.segmentAt(phaseTime);
		HeroClass cls = TitleHeroLogoTiming.CLASSES[TitleHeroLogoTiming.nextClassIndex(classIndex)];
		int frameIndex;

		if (segment == TitleHeroLogoTiming.Segment.HIT) {
			float hitElapsed = phaseTime - TitleHeroLogoTiming.APPROACH_MS;
			int attackStep = TitleHeroLogoTiming.attackStepAt(hitElapsed);
			if (attackStep != lastAttackStep) {
				lastAttackStep = attackStep;
			}
			frameIndex = TitleHeroLogoTiming.attackFrameIndex(attackStep);
		} else if (segment == TitleHeroLogoTiming.Segment.WAIT) {
			frameIndex = TitleHeroLogoTiming.attackFrameIndex(TitleHeroLogoTiming.ATTACK_FRAME_INDICES.length - 1);
		} else if (segment == TitleHeroLogoTiming.Segment.SETTLE
				&& TitleHeroLogoTiming.usesIdleFrame(phaseTime)) {
			frameIndex = IDLE_FRAME;
		} else {
			int runStep = (int) Math.floor(phaseTime / TitleHeroLogoTiming.RUN_FRAME_MS);
			if (runStep != lastRunStep) {
				lastRunStep = runStep;
			}
			frameIndex = TitleHeroLogoTiming.runFrameIndex(runStep);
		}

		if (frameIndex != lastFrameIndex) {
			lastFrameIndex = frameIndex;
			showHero(incoming, cls, frameIndex);
			incoming.flipHorizontal = fromRight;
			// Keep walk geometry stable if frame width differs slightly
			impactLeft = TitleHeroLogoTiming.impactLeft(centerLeft, hero.width(), incoming.width(), fromRight);
			startLeft = TitleHeroLogoTiming.walkStartLeft(incoming.width(), fromRight, viewport);
		}
	}

	private void startCelebration() {
		celebrationStarted = true;
		celebrationAge = 0;
		clearCelebration();

		float radius = TitleHeroLogoTiming.creditsFlareRadius(incoming.height());
		celebrationFlare = new Flare(7, radius);
		celebrationFlare.color(0xFFAA33, true);
		celebrationFlare.angularSpeed = TitleHeroLogoTiming.CREDITS_FLARE_ANGULAR_SPEED_DEG;
		// Behind hero + "New Boss!" (same layering as AboutScene credits flare).
		attachCreditsFlareBehind(celebrationFlare, incoming);
		celebrationFlare.scale.set(1f);
		celebrationFlare.alpha(0f);

		RenderedTextBlock status = PixelScene.renderTextBlock("New Boss!", 7);
		status.hardlight(CharSprite.POSITIVE);
		add(status);
		celebrationText = new FloatAway(status, TitleHeroLogoTiming.celebrationFlareMs() / 1000f);
		add(celebrationText);
		layoutCelebrationText(0f);
	}

	/**
	 * Attaches flare via {@link Flare#show(Visual, float)} so it draws behind the
	 * hero.
	 */
	static void attachCreditsFlareBehind(Flare flare, Visual hero) {
		flare.show(hero, 0f);
	}

	private void updateCelebration(float elapsed) {
		if (!celebrationStarted) {
			return;
		}
		celebrationAge += elapsed;
		float life = TitleHeroLogoTiming.celebrationFlareMs() / 1000f;
		float progress = celebrationAge / life;

		if (celebrationFlare != null) {
			celebrationFlare.point(celebrationCenter());
			celebrationFlare.scale.set(1f);
			celebrationFlare.alpha(TitleHeroLogoTiming.potionFloatAlpha(progress));
		}
		layoutCelebrationText(progress);

		if (celebrationAge >= life) {
			clearCelebration();
			celebrationStarted = false;
		}
	}

	private void layoutCelebrationText(float progress) {
		if (celebrationText == null) {
			return;
		}
		Image active = activeHero();
		float eased = TitleHeroLogoTiming.potionFloatProgress(progress);
		float anchorY = active.y;
		celebrationText.layout(
				active.center().x,
				anchorY,
				TitleHeroLogoTiming.experiencePotionFloatDistance(active.height()) * eased,
				TitleHeroLogoTiming.potionFloatAlpha(progress));
	}

	private Image activeHero() {
		return incoming.visible ? incoming : hero;
	}

	private PointF celebrationCenter() {
		return activeHero().center();
	}

	private void clearCelebration() {
		if (celebrationFlare != null) {
			celebrationFlare.killAndErase();
			celebrationFlare = null;
		}
		if (celebrationText != null) {
			celebrationText.killAndErase();
			celebrationText = null;
		}
	}

	private void spawnShatter() {
		RectF full = hero.frame();
		float uW = (full.right - full.left) / SHATTER_COLS;
		float vH = (full.bottom - full.top) / SHATTER_ROWS;
		float pieceW = hero.width() / SHATTER_COLS;
		float pieceH = hero.height() / SHATTER_ROWS;

		for (int row = 0; row < SHATTER_ROWS; row++) {
			for (int col = 0; col < SHATTER_COLS; col++) {
				Image shard = new Image(hero);
				RectF cell = new RectF(
						full.left + col * uW,
						full.top + row * vH,
						full.left + (col + 1) * uW,
						full.top + (row + 1) * vH);
				shard.frame(cell);
				shard.scale.set(TitleHeroLogoTiming.HERO_SCALE);
				shard.x = hero.x + col * pieceW;
				shard.y = hero.y + row * pieceH;
				add(shard);
				add(new ShatterPiece(
						shard,
						shard.x,
						shard.y,
						(Random.Float() - 0.5f) * 64f,
						(Random.Float() - 0.5f) * 52f + 18f,
						(Random.Float() - 0.5f) * 140f,
						Random.Float() * TitleHeroLogoTiming.SHATTER_DELAY_MAX_SEC,
						TitleHeroLogoTiming.SHATTER_MS / 1000f));
			}
		}
	}

	private void finishTransition() {
		classIndex = TitleHeroLogoTiming.nextClassIndex(classIndex);
		showHero(hero, TitleHeroLogoTiming.CLASSES[classIndex], IDLE_FRAME);
		hero.visible = true;
		incoming.visible = false;
		phase = Phase.IDLE;
		phaseTime = 0;
		layoutIdle();
	}

	private static void showHero(Image target, HeroClass cls, int frameIndex) {
		Image src = HeroSprite.sheetFrame(cls, ARMOR_TIER, frameIndex);
		target.texture(src.texture);
		target.frame(src.frame());
		target.flipHorizontal = false;
	}

	private static class FloatAway extends com.watabou.noosa.Gizmo {
		private final RenderedTextBlock text;
		private final float life;
		private float age;

		FloatAway(RenderedTextBlock text, float life) {
			this.text = text;
			this.life = life;
		}

		void layout(float centerX, float anchorY, float rise, float alpha) {
			text.setPos(centerX - text.width() / 2f, anchorY - rise);
			text.alpha(alpha);
		}

		@Override
		public void update() {
			age += com.watabou.noosa.Game.elapsed;
			if (age >= life) {
				killAndErase();
			}
		}

		@Override
		public void killAndErase() {
			if (text != null) {
				text.killAndErase();
			}
			super.killAndErase();
		}
	}

	private static class ShatterPiece extends com.watabou.noosa.Gizmo {
		private final Image image;
		private final float originX;
		private final float originY;
		private final float dx;
		private final float dy;
		private final float rot;
		private final float delay;
		private final float life;
		private float age;

		ShatterPiece(Image image, float originX, float originY,
				float dx, float dy, float rot, float delay, float life) {
			this.image = image;
			this.originX = originX;
			this.originY = originY;
			this.dx = dx;
			this.dy = dy;
			this.rot = rot;
			this.delay = delay;
			this.life = life;
		}

		@Override
		public void update() {
			float elapsed = com.watabou.noosa.Game.elapsed;
			age += elapsed;
			float local = age - delay;
			if (local < 0f) {
				image.x = originX;
				image.y = originY;
				image.angle = 0f;
				image.alpha(1f);
				return;
			}
			float t = TitleHeroLogoTiming.shatterEase(Math.min(1f, local / life));
			image.x = originX + dx * t;
			image.y = originY + dy * t;
			image.angle = rot * t;
			image.alpha(Math.max(0f, 1f - t));
			if (local >= life) {
				image.killAndErase();
				killAndErase();
			}
		}
	}
}
