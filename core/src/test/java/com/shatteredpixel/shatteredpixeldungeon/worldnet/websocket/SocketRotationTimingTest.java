package com.shatteredpixel.shatteredpixeldungeon.worldnet.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * When make-before-break rotation starts, relative to the server closing the socket.
 *
 * <p>The client must always move first, and must have a usable window between
 * dialling its replacement and losing the socket it is replacing. Ten seconds is
 * that window: enough for a handshake and the greeting frame over a slow mobile
 * connection, without giving up meaningful connection time.
 *
 * <p>The server's own deadline lives in the hero-echoes repo
 * (`worldConfig.socketLifetimeSeconds`), which deploys separately from the game.
 * It is restated here because the relationship between the two is the thing
 * under test, and nothing else in either repo can express it.
 */
@DisplayName("World socket rotation timing")
class SocketRotationTimingTest {

	/** `worldConfig.socketLifetimeSeconds` in hero-echoes. */
	private static final float SERVER_CLOSES_AT_SECONDS = 290f;
	/** The handshake budget the client is owed before the old socket goes away. */
	private static final float REQUIRED_OVERLAP_SECONDS = 10f;

	@Test
	@DisplayName("rotates on a fixed deadline, the same for every socket")
	void rotationIsDeterministic() {
		assertThat(ReconnectPolicy.rotationDeadline(0f))
				.isEqualTo(ReconnectPolicy.rotationDeadline(1f));
	}

	@Test
	@DisplayName("leaves at least ten seconds to stand up the replacement before the server closes")
	void keepsATenSecondOverlap() {
		float latest = ReconnectPolicy.rotationDeadline(1f);
		assertThat(SERVER_CLOSES_AT_SECONDS - latest).isGreaterThanOrEqualTo(REQUIRED_OVERLAP_SECONDS);
	}

	@Test
	@DisplayName("uses as much of the connection as that overlap allows, rather than rotating early")
	void doesNotRotateEarlierThanItMust() {
		// The overlap is a floor, not a target: rotating well before it is due
		// throws away connection time that has already been paid for.
		assertThat(ReconnectPolicy.rotationDeadline(0f))
				.isGreaterThanOrEqualTo(SERVER_CLOSES_AT_SECONDS - 2f * REQUIRED_OVERLAP_SECONDS);
	}
}
