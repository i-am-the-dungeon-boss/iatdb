package com.shatteredpixel.shatteredpixeldungeon.worldnet.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The spread applied to make-before-break rotation.
 *
 * <p>Every client rotates on a timer of its own, so in normal play the moments
 * are already scattered. The jitter is insurance against the one event that
 * gathers them: an instance being reaped, or an availability zone failing over,
 * drops every socket on it in the same second. Without a spread those clients
 * would then rotate together at the same fixed interval for the rest of the
 * session, presenting the scheduler with a periodic spike of simultaneous
 * dials — and, because a replacement overlaps the socket it replaces, briefly
 * twice as many connections as there are players. With one, that grouping
 * decays over the next few rotations.
 */
@DisplayName("World socket rotation jitter")
class SocketRotationJitterTest {

	@Test
	@DisplayName("rotates no earlier than the base deadline, so a healthy socket is not cut short")
	void neverRotatesEarly() {
		assertThat(ReconnectPolicy.rotationDeadline(0f)).isEqualTo(215f);
	}

	@Test
	@DisplayName("stays under the server's own minimum lifetime, so the client still goes first")
	void neverOutlastsTheServersPatience() {
		assertThat(ReconnectPolicy.rotationDeadline(1f)).isLessThan(240f);
	}

	@Test
	@DisplayName("spreads clients that were dropped together across a window")
	void differentDrawsRotateAtDifferentMoments() {
		assertThat(ReconnectPolicy.rotationDeadline(0.25f))
				.isNotEqualTo(ReconnectPolicy.rotationDeadline(0.75f));
	}

	@Test
	@DisplayName("moves the deadline later the higher the draw, never backwards")
	void alaterDrawIsAlwaysALaterDeadline() {
		assertThat(ReconnectPolicy.rotationDeadline(0.75f))
				.isGreaterThan(ReconnectPolicy.rotationDeadline(0.25f));
	}
}
