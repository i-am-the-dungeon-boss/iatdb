package com.shatteredpixel.shatteredpixeldungeon.worldnet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Server mute")
class ServerMuteTest {

	@Test
	@DisplayName("a fresh mute with no deadline lets the player speak")
	void silentByDefault() {
		assertThat(new ServerMute().isActive()).isFalse();
	}

	@Test
	@DisplayName("holds while the deadline it was given is still ahead")
	void holdsUntilTheDeadline() {
		ServerMute mute = new ServerMute();

		mute.reset(System.currentTimeMillis() + 60_000L);

		assertThat(mute.isActive()).isTrue();
	}

	@Test
	@DisplayName("lapses on its own once the deadline passes, with nothing to refresh")
	void lapsesOnItsOwn() {
		ServerMute mute = new ServerMute();

		mute.reset(System.currentTimeMillis() - 1L);

		assertThat(mute.isActive()).isFalse();
	}

	@Test
	@DisplayName("treats a negative or absent deadline as no mute at all")
	void ignoresANonsenseDeadline() {
		ServerMute mute = new ServerMute();

		mute.reset(-5_000L);

		assertThat(mute.isActive()).isFalse();
	}

	@Test
	@DisplayName("a refusal from the server holds even though it carries no deadline")
	void serverRefusalHoldsWithoutADeadline() {
		ServerMute mute = new ServerMute();

		mute.refusedByServer();

		assertThat(mute.isActive()).isTrue();
	}

	@Test
	@DisplayName("a refusal outlives a deadline that has already lapsed")
	void serverRefusalOutlivesALapsedDeadline() {
		ServerMute mute = new ServerMute();
		mute.reset(System.currentTimeMillis() - 1L);

		mute.refusedByServer();

		assertThat(mute.isActive()).isTrue();
	}

	@Test
	@DisplayName("a fresh deadline clears a refusal, since authentication is the authority")
	void resetClearsAServerRefusal() {
		ServerMute mute = new ServerMute();
		mute.refusedByServer();

		mute.reset(0L);

		assertThat(mute.isActive()).isFalse();
	}
}
