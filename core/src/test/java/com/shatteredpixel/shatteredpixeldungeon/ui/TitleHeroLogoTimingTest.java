package com.shatteredpixel.shatteredpixeldungeon.ui;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TitleHeroLogoTimingTest {

	@Test
	@DisplayName("transition segment is approach before approach duration")
	void approachSegment() {
		Assertions.assertThat(TitleHeroLogoTiming.segmentAt(0))
				.isEqualTo(TitleHeroLogoTiming.Segment.APPROACH);
		Assertions.assertThat(TitleHeroLogoTiming.segmentAt(TitleHeroLogoTiming.APPROACH_MS - 1))
				.isEqualTo(TitleHeroLogoTiming.Segment.APPROACH);
	}

	@Test
	@DisplayName("transition segment is hit during attack window")
	void hitSegment() {
		Assertions.assertThat(TitleHeroLogoTiming.segmentAt(TitleHeroLogoTiming.APPROACH_MS))
				.isEqualTo(TitleHeroLogoTiming.Segment.HIT);
	}

	@Test
	@DisplayName("walker left eases from start to impact during approach")
	void walkerApproachesImpact() {
		float start = -20f;
		float impact = 10f;
		float center = 40f;
		float mid = TitleHeroLogoTiming.walkerLeft(TitleHeroLogoTiming.APPROACH_MS / 2f, start, impact, center);
		Assertions.assertThat(mid).isGreaterThan(start).isLessThan(impact);
		Assertions.assertThat(TitleHeroLogoTiming.walkerLeft(TitleHeroLogoTiming.APPROACH_MS, start, impact, center))
				.isEqualTo(impact);
	}

	@Test
	@DisplayName("impact left places incoming hero outside outgoing bounds")
	void impactOutsideOutgoing() {
		Assertions.assertThat(TitleHeroLogoTiming.impactLeft(0, 12, 12, true)).isEqualTo(12f);
		Assertions.assertThat(TitleHeroLogoTiming.impactLeft(10, 12, 12, false)).isEqualTo(-2f);
	}

	@Test
	@DisplayName("next hero class cycles through all playable classes")
	void cyclesHeroClasses() {
		Assertions.assertThat(TitleHeroLogoTiming.CLASSES).hasSize(6);
		Assertions.assertThat(TitleHeroLogoTiming.nextClassIndex(0)).isEqualTo(1);
		Assertions.assertThat(TitleHeroLogoTiming.nextClassIndex(5)).isEqualTo(0);
	}

	@Test
	@DisplayName("idle bob matches frontend CSS float keyframes")
	void idleBobMatchesFrontendFloat() {
		Assertions.assertThat(TitleHeroLogoTiming.idleBobY(0f)).isEqualTo(0f);
		Assertions.assertThat(TitleHeroLogoTiming.idleBobY(1.5f)).isEqualTo(-6f);
		Assertions.assertThat(TitleHeroLogoTiming.idleBobY(3f)).isEqualTo(0f);
		Assertions.assertThat(TitleHeroLogoTiming.idleBobY(0.75f))
				.isLessThan(0f)
				.isGreaterThan(-6f);
	}

	@Test
	@DisplayName("cycles attack frames slowly during the hit beat")
	void cyclesAttackFramesDuringHit() {
		Assertions.assertThat(TitleHeroLogoTiming.attackStepAt(0)).isEqualTo(0);
		Assertions.assertThat(TitleHeroLogoTiming.attackStepAt(89)).isEqualTo(0);
		Assertions.assertThat(TitleHeroLogoTiming.attackStepAt(90)).isEqualTo(1);
		Assertions.assertThat(TitleHeroLogoTiming.attackStepAt(270)).isEqualTo(3);
		Assertions.assertThat(TitleHeroLogoTiming.ATTACK_FRAME_INDICES).containsExactly(13, 14, 15, 0);
	}

	@Test
	@DisplayName("run frame indices follow HeroSprite run cycle")
	void runFrameIndicesFollowHeroSprite() {
		Assertions.assertThat(TitleHeroLogoTiming.RUN_FRAME_INDICES).containsExactly(2, 3, 4, 5, 6, 7);
		Assertions.assertThat(TitleHeroLogoTiming.runFrameIndex(0)).isEqualTo(2);
		Assertions.assertThat(TitleHeroLogoTiming.runFrameIndex(1)).isEqualTo(3);
		Assertions.assertThat(TitleHeroLogoTiming.runFrameIndex(6)).isEqualTo(2);
	}

	@Test
	@DisplayName("matches New Boss celebration float timing")
	void matchesCelebrationFloatTiming() {
		Assertions.assertThat(TitleHeroLogoTiming.celebrationEndMs()).isEqualTo(2260f);
		Assertions.assertThat(TitleHeroLogoTiming.celebrationFlareMs()).isEqualTo(1540f);
		Assertions.assertThat(TitleHeroLogoTiming.experiencePotionFloatDistance(60f)).isEqualTo(16f);
		Assertions.assertThat(TitleHeroLogoTiming.experiencePotionFloatDistance(45f)).isEqualTo(12f);
	}

	@Test
	@DisplayName("credits flare spin matches frontend angular speed")
	void creditsFlareSpinMatchesFrontend() {
		Assertions.assertThat(TitleHeroLogoTiming.CREDITS_FLARE_ANGULAR_SPEED_DEG).isEqualTo(40f);
	}

	@Test
	@DisplayName("title flare uses fixed radius 50")
	void titleFlareUsesFixedRadius50() {
		Assertions.assertThat(TitleHeroLogoTiming.CREDITS_FLARE_RADIUS).isEqualTo(50f);
		Assertions.assertThat(TitleHeroLogoTiming.creditsFlareRadius(37.5f)).isEqualTo(50f);
		Assertions.assertThat(TitleHeroLogoTiming.creditsFlareRadius(60f)).isEqualTo(50f);
	}

	@Test
	@DisplayName("potion float alpha fades in, holds, then fades out")
	void potionFloatAlphaEnvelope() {
		Assertions.assertThat(TitleHeroLogoTiming.potionFloatAlpha(0f)).isEqualTo(0f);
		Assertions.assertThat(TitleHeroLogoTiming.potionFloatAlpha(0.10f)).isEqualTo(1f);
		Assertions.assertThat(TitleHeroLogoTiming.potionFloatAlpha(0.50f)).isEqualTo(1f);
		Assertions.assertThat(TitleHeroLogoTiming.potionFloatAlpha(1f)).isEqualTo(0f);
		Assertions.assertThat(TitleHeroLogoTiming.potionFloatAlpha(0.75f))
				.isGreaterThan(0f)
				.isLessThan(1f);
	}

	@Test
	@DisplayName("gold title pulse matches frontend pulse-gold keyframes")
	void goldPulseMatchesFrontend() {
		Assertions.assertThat(TitleHeroLogoTiming.pulseGoldAlpha(0f)).isEqualTo(1f);
		Assertions.assertThat(TitleHeroLogoTiming.pulseGoldAlpha(1.2f)).isEqualTo(0.82f);
		Assertions.assertThat(TitleHeroLogoTiming.pulseGoldAlpha(2.4f)).isEqualTo(1f);
	}

	@Test
	@DisplayName("shatter ease matches frontend cubic-bezier endpoints")
	void shatterEaseMatchesFrontend() {
		Assertions.assertThat(TitleHeroLogoTiming.shatterEase(0f)).isEqualTo(0f);
		Assertions.assertThat(TitleHeroLogoTiming.shatterEase(1f)).isEqualTo(1f);
		// cubic-bezier(0.45, 0, 0.75, 0.15) stays below the diagonal mid-animation
		Assertions.assertThat(TitleHeroLogoTiming.shatterEase(0.5f))
				.isGreaterThan(0f)
				.isLessThan(0.5f);
	}

	@Test
	@DisplayName("incoming frame uses idle pose in final settle window")
	void settleUsesIdleNearEnd() {
		Assertions.assertThat(TitleHeroLogoTiming.usesIdleFrame(TitleHeroLogoTiming.TRANSITION_MS - 40f))
				.isTrue();
		Assertions.assertThat(TitleHeroLogoTiming.usesIdleFrame(TitleHeroLogoTiming.TRANSITION_MS - 41f))
				.isFalse();
	}
}
