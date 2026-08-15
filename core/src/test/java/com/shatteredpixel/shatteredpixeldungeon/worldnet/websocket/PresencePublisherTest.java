package com.shatteredpixel.shatteredpixeldungeon.worldnet.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldPresence;

import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What the village is told about this player, and how little of it there is.
 *
 * <p>The hero's position is offered every frame. Almost none of those are worth
 * a frame on the wire, and the ones that are have to be exactly right — a
 * missed move leaves an avatar standing somewhere the player is not.
 */
@DisplayName("World presence publisher")
class PresencePublisherTest {

	private static WorldPresence at(int cell) {
		return new WorldPresence("p1", "One", "WARRIOR", cell, 1);
	}

	@Test
	@DisplayName("says nothing at all when there is no avatar to speak of")
	void silentBeforeAnyoneIsInTheVillage() throws JSONException {
		assertThat(new PresencePublisher().nextFrame()).isNull();
	}

	@Test
	@DisplayName("states a new position once, then stays quiet until it changes")
	void anUnmovedAvatarIsSaidExactlyOnce() throws JSONException {
		PresencePublisher publisher = new PresencePublisher();
		publisher.set(at(10));

		assertThat(publisher.nextFrame()).contains("\"cell\":10");
		assertThat(publisher.nextFrame()).isNull();

		publisher.set(at(10));
		assertThat(publisher.nextFrame()).isNull();

		publisher.set(at(11));
		assertThat(publisher.nextFrame()).contains("\"cell\":11");
	}

	@Test
	@DisplayName("restates the position on a fresh socket, which has never heard it")
	void aReplacementSocketIsToldEverythingAgain() throws JSONException {
		PresencePublisher publisher = new PresencePublisher();
		publisher.set(at(10));
		publisher.nextFrame();

		publisher.forget();
		assertThat(publisher.nextFrame()).contains("\"cell\":10");
	}

	@Test
	@DisplayName("retracts the avatar once when the player descends into a run")
	void leavingIsAnnouncedOnceAndNotRepeated() throws JSONException {
		PresencePublisher publisher = new PresencePublisher();
		publisher.set(at(10));
		publisher.nextFrame();

		publisher.set(null);
		assertThat(publisher.nextFrame()).contains("\"t\":\"leave\"");
		assertThat(publisher.nextFrame()).isNull();
	}

	@Test
	@DisplayName("has no goodbye to say until the server believes in an avatar")
	void nothingToRetractBeforeAnythingWasPublished() throws JSONException {
		PresencePublisher publisher = new PresencePublisher();
		assertThat(publisher.owesGoodbye()).isFalse();

		publisher.set(at(10));
		// Still nothing: setting a position is not the same as having sent it.
		assertThat(publisher.owesGoodbye()).isFalse();

		publisher.nextFrame();
		assertThat(publisher.owesGoodbye()).isTrue();
	}

	@Test
	@DisplayName("still owes a goodbye when the leave was queued but never sent")
	void aQueuedLeaveIsStillOwed() {
		PresencePublisher publisher = new PresencePublisher();
		publisher.set(at(10));
		publisher.set(null);

		assertThat(publisher.owesGoodbye()).isTrue();
	}

	@Test
	@DisplayName("forgets everything on disconnect, so a new session starts silent")
	void clearingLeavesNothingOwed() throws JSONException {
		PresencePublisher publisher = new PresencePublisher();
		publisher.set(at(10));
		publisher.nextFrame();

		publisher.clear();
		assertThat(publisher.owesGoodbye()).isFalse();
		assertThat(publisher.nextFrame()).isNull();
	}
}
