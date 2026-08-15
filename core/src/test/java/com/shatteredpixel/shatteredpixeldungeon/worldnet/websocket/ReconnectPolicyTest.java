package com.shatteredpixel.shatteredpixeldungeon.worldnet.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The engine's sense of time: when to dial again, and when the socket it is
 * holding has earned a replacement.
 *
 * <p>These were previously reachable only by driving a fake socket through
 * several minutes of ticks, which is why the backoff ladder had never been
 * asserted past its first rung.
 */
@DisplayName("World socket reconnect policy")
class ReconnectPolicyTest {

	@Test
	@DisplayName("redials at once when nothing has failed yet")
	void redialsImmediatelyOnAFreshSession() {
		ReconnectPolicy policy = new ReconnectPolicy();
		policy.reset();

		assertThat(policy.redialDue(0f)).isTrue();
	}

	@Test
	@DisplayName("waits longer after each consecutive failure")
	void backsOffFurtherTheLongerItKeepsFailing() {
		ReconnectPolicy policy = new ReconnectPolicy();

		policy.noteFailure();
		assertThat(policy.redialDue(0.9f)).isFalse();
		assertThat(policy.redialDue(0.2f)).isTrue();

		policy.noteFailure();
		assertThat(policy.redialDue(1.9f)).isFalse();
		assertThat(policy.redialDue(0.2f)).isTrue();
	}

	@Test
	@DisplayName("stops lengthening the wait once the ladder is exhausted")
	void theBackoffIsBounded() {
		ReconnectPolicy policy = new ReconnectPolicy();
		for (int attempt = 0; attempt < 20; attempt++) {
			policy.noteFailure();
		}

		assertThat(policy.redialDue(29f)).isFalse();
		assertThat(policy.redialDue(2f)).isTrue();
	}

	@Test
	@DisplayName("forgets the streak once a socket comes up, so one bad patch is not held forever")
	void openingClearsTheStreak() {
		ReconnectPolicy policy = new ReconnectPolicy();
		policy.noteFailure();
		policy.noteFailure();
		policy.noteOpened();
		policy.noteFailure();

		// Back to the first rung rather than the third.
		assertThat(policy.redialDue(1.1f)).isTrue();
	}

	@Test
	@DisplayName("calls a socket healthy only once it has carried traffic for a while")
	void aSocketHasToLiveBeforeItCountsAsHealthy() {
		ReconnectPolicy policy = new ReconnectPolicy();
		policy.noteOpened();

		policy.elapse(4f);
		assertThat(policy.wasHealthy()).isFalse();

		policy.elapse(2f);
		assertThat(policy.wasHealthy()).isTrue();
	}

	@Test
	@DisplayName("redials a routine close at once, holding nothing against it")
	void aRoutineCloseCostsNoWait() {
		ReconnectPolicy policy = new ReconnectPolicy();
		policy.noteFailure();
		policy.noteRoutineClose();

		assertThat(policy.redialDue(0f)).isTrue();
	}

	@Test
	@DisplayName("does not rotate a socket that has only just opened")
	void rotationWaitsOutTheWholeDeadline() {
		ReconnectPolicy policy = new ReconnectPolicy();
		policy.noteOpened();

		policy.elapse(ReconnectPolicy.rotationDeadline(0f) - 1f);
		assertThat(policy.rotationDue()).isFalse();

		// Past the top of the jitter window, so the draw cannot matter.
		policy.elapse(ReconnectPolicy.rotationDeadline(1f));
		assertThat(policy.rotationDue()).isTrue();
	}
}
