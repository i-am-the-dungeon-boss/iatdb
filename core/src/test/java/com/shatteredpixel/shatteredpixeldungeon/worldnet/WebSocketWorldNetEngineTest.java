package com.shatteredpixel.shatteredpixeldungeon.worldnet;

import static org.assertj.core.api.Assertions.assertThat;

import com.shatteredpixel.shatteredpixeldungeon.worldnet.websocket.WebSocketWorldNetEngine;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.websocket.WorldSocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

@DisplayName("WebSocket world net engine")
class WebSocketWorldNetEngineTest {

	private static final class FakeSocket implements WorldSocket {
		final List<String> sent = new ArrayList<>();
		Listener listener;
		boolean closed;

		@Override
		public void open(Listener newListener) {
			this.listener = newListener;
		}

		@Override
		public void send(String text) {
			sent.add(text);
		}

		@Override
		public void close() {
			closed = true;
		}

		boolean sentAny(String fragment) {
			for (String frame : sent) {
				if (frame.contains(fragment)) {
					return true;
				}
			}
			return false;
		}

		int countSent(String fragment) {
			int total = 0;
			for (String frame : sent) {
				if (frame.contains(fragment)) {
					total++;
				}
			}
			return total;
		}
	}

	private static final class RecordingListener implements WorldNetEngine.Listener {
		final List<WorldChatMessage> messages = new ArrayList<>();
		final List<List<WorldPresence>> rosters = new ArrayList<>();
		final List<WorldNetEngine.Status> statuses = new ArrayList<>();
		final List<Boolean> mutes = new ArrayList<>();
		final List<String> serverVersions = new ArrayList<>();

		@Override
		public void onServerMute(boolean muted) {
			mutes.add(muted);
		}

		@Override
		public void onServerVersion(String versionName) {
			serverVersions.add(versionName);
		}

		@Override
		public void onChatMessages(List<WorldChatMessage> fresh) {
			messages.addAll(fresh);
		}

		@Override
		public void onPresenceSnapshot(List<WorldPresence> others) {
			rosters.add(others);
		}

		@Override
		public void onStatus(WorldNetEngine.Status status, String detail) {
			statuses.add(status);
		}

		WorldNetEngine.Status last() {
			return statuses.isEmpty() ? null : statuses.get(statuses.size() - 1);
		}
	}

	private List<FakeSocket> sockets;
	private WebSocketWorldNetEngine engine;
	private RecordingListener listener;

	@BeforeEach
	void setUp() {
		sockets = new ArrayList<>();
		engine = new WebSocketWorldNetEngine(() -> {
			FakeSocket socket = new FakeSocket();
			sockets.add(socket);
			return socket;
		});
		listener = new RecordingListener();
	}

	private FakeSocket current() {
		return sockets.get(sockets.size() - 1);
	}

	private void connectAndOpen() {
		engine.connect(new WorldIdentity("device", "Ann"), listener);
		current().listener.onOpen();
		engine.tick(0.01f);
	}

	@Test
	@DisplayName("identifies itself so logs and the debug overlay can tell the transports apart")
	void namesItself() {
		assertThat(engine.name()).isEqualTo("websocket");
	}

	@Test
	@DisplayName("opens a socket immediately on connect and reports it is connecting")
	void opensOnConnect() {
		engine.connect(new WorldIdentity("device", "Ann"), listener);

		assertThat(sockets).hasSize(1);
		assertThat(listener.statuses).containsExactly(WorldNetEngine.Status.CONNECTING);
	}

	@Test
	@DisplayName("reports connected only once the handshake actually completes")
	void reportsConnectedOnOpen() {
		engine.connect(new WorldIdentity("device", "Ann"), listener);
		assertThat(listener.last()).isEqualTo(WorldNetEngine.Status.CONNECTING);

		current().listener.onOpen();
		engine.tick(0.01f);

		assertThat(listener.last()).isEqualTo(WorldNetEngine.Status.CONNECTED);
	}

	@Test
	@DisplayName("delivers a pushed chat frame without waiting for a poll")
	void deliversPushedChat() {
		connectAndOpen();

		current().listener.onText(
				"{\"t\":\"chat\",\"messages\":[{\"id\":\"m1\",\"name\":\"Ann\",\"text\":\"hi\",\"at\":1}]}");
		engine.tick(0.01f);

		assertThat(listener.messages).hasSize(1);
		assertThat(listener.messages.get(0).text).isEqualTo("hi");
	}

	@Test
	@DisplayName("never delivers the same message twice across a reconnect replay")
	void deduplicatesReplayedChat() {
		connectAndOpen();
		String frame =
				"{\"t\":\"chat\",\"messages\":[{\"id\":\"m1\",\"name\":\"Ann\",\"text\":\"hi\",\"at\":1}]}";

		current().listener.onText(frame);
		engine.tick(0.01f);
		current().listener.onText(frame);
		engine.tick(0.01f);

		assertThat(listener.messages).hasSize(1);
	}

	@Test
	@DisplayName("hands a roster frame straight to the listener as a snapshot")
	void deliversRoster() {
		connectAndOpen();

		current().listener.onText(
				"{\"t\":\"roster\",\"occupants\":[{\"player_id\":\"p1\",\"name\":\"Bo\",\"hero_class\":\"MAGE\",\"cell\":4,\"facing\":1}]}");
		engine.tick(0.01f);

		assertThat(listener.rosters).hasSize(1);
		assertThat(listener.rosters.get(0)).hasSize(1);
	}

	@Test
	@DisplayName("keeps chat and presence independent, so a chat frame raises no roster update")
	void chatDoesNotImplyRoster() {
		connectAndOpen();

		current().listener.onText("{\"t\":\"chat\",\"messages\":[]}");
		engine.tick(0.01f);

		assertThat(listener.rosters).isEmpty();
	}

	@Test
	@DisplayName("publishes the village avatar once the socket is open")
	void sendsPresence() {
		connectAndOpen();
		engine.setPresence(WorldPresence.local("WARRIOR", 12, 1));
		engine.tick(0.01f);

		assertThat(current().sentAny("\"t\":\"presence\"")).isTrue();
	}

	@Test
	@DisplayName("does not re-send an unchanged avatar on every frame")
	void coalescesUnchangedPresence() {
		connectAndOpen();
		engine.setPresence(WorldPresence.local("WARRIOR", 12, 1));
		for (int i = 0; i < 20; i++) {
			engine.tick(0.05f);
		}

		assertThat(current().countSent("\"t\":\"presence\"")).isEqualTo(1);
	}

	@Test
	@DisplayName("never re-publishes a standing avatar, because the server has no entry to refresh")
	void doesNotRefreshStandingPresence() {
		connectAndOpen();
		engine.setPresence(WorldPresence.local("WARRIOR", 12, 1));
		engine.tick(0.01f);
		engine.tick(15f);
		engine.tick(15f);

		assertThat(current().countSent("\"t\":\"presence\"")).isEqualTo(1);
	}

	/**
	 * Advances the clock until a replacement is dialled, then stops.
	 *
	 * <p>The moment cannot be written down: the rotation deadline carries a
	 * per-client jitter, so it lands somewhere in a window rather than on a
	 * number. Stopping on the event rather than at a time keeps these tests about
	 * the handover itself. Steps are short enough that neither the silence
	 * timeout nor the replacement's handshake timeout can fire in one, and the
	 * live socket is fed a frame each time so it stays healthy.
	 */
	private void runUntilReplacementDialled(FakeSocket live) {
		int before = sockets.size();
		for (float elapsed = 0f; elapsed < 300f; elapsed += 10f) {
			live.listener.onText("{\"t\":\"pong\"}");
			engine.tick(10f);
			if (sockets.size() > before) {
				return;
			}
		}
		throw new AssertionError("no replacement was dialled within the socket's lifetime");
	}

	/** As above, but stops at a fixed time without expecting anything to happen. */
	private void runFor(FakeSocket live, float seconds) {
		for (float elapsed = 0f; elapsed < seconds; elapsed += 10f) {
			live.listener.onText("{\"t\":\"pong\"}");
			engine.tick(10f);
		}
	}

	@Test
	@DisplayName("passes on the game build the server expects, so an outdated client can be told")
	void reportsTheServersGameVersion() {
		connectAndOpen();
		current().listener.onText(
				"{\"t\":\"hello\",\"server_time\":1,\"player_id\":\"me\",\"version_name\":\"2.4.0\"}");
		engine.tick(0.01f);

		assertThat(listener.serverVersions).containsExactly("2.4.0");
	}

	@Test
	@DisplayName("says nothing about the version when a greeting carries none")
	void staysQuietWithoutAVersion() {
		connectAndOpen();
		current().listener.onText("{\"t\":\"hello\",\"server_time\":1,\"player_id\":\"me\"}");
		engine.tick(0.01f);

		assertThat(listener.serverVersions).isEmpty();
	}

	@Test
	@DisplayName("dials a replacement before the server can close the socket in use")
	void dialsReplacementBeforeTheServerCloses() {
		connectAndOpen();
		FakeSocket first = current();
		runUntilReplacementDialled(first);

		assertThat(sockets).hasSize(2);
		assertThat(first.closed).isFalse();
	}

	@Test
	@DisplayName("leaves a young socket alone, however the jitter fell")
	void doesNotRotateBeforeTheBaseDeadline() {
		connectAndOpen();
		runFor(current(), 210f);

		assertThat(sockets).hasSize(1);
	}

	@Test
	@DisplayName("keeps the old socket carrying traffic until the replacement is actually open")
	void handsOverOnlyWhenTheReplacementOpens() {
		connectAndOpen();
		FakeSocket first = current();
		runUntilReplacementDialled(first);

		first.listener.onText(
				"{\"t\":\"chat\",\"messages\":[{\"id\":\"m9\",\"name\":\"Ann\",\"text\":\"still here\",\"at\":9}]}");
		engine.tick(0.01f);

		assertThat(listener.messages).extracting(message -> message.text).contains("still here");
		assertThat(first.closed).isFalse();
	}

	@Test
	@DisplayName("closes the old socket once the replacement has taken over")
	void closesTheOldSocketAfterHandover() {
		connectAndOpen();
		FakeSocket first = current();
		runUntilReplacementDialled(first);

		sockets.get(1).listener.onOpen();
		engine.tick(0.01f);

		assertThat(first.closed).isTrue();
	}

	@Test
	@DisplayName("republishes the avatar on the replacement, which has never heard of it")
	void republishesPresenceAfterHandover() {
		connectAndOpen();
		engine.setPresence(WorldPresence.local("WARRIOR", 12, 1));
		engine.tick(0.01f);
		runUntilReplacementDialled(current());

		sockets.get(1).listener.onOpen();
		engine.tick(0.01f);

		assertThat(sockets.get(1).sentAny("\"t\":\"presence\"")).isTrue();
	}

	@Test
	@DisplayName("leaves the live connection alone when a replacement fails to open")
	void aFailedReplacementDoesNotDisturbTheLiveSocket() {
		connectAndOpen();
		FakeSocket first = current();
		runUntilReplacementDialled(first);

		sockets.get(1).listener.onClosed("refused");
		engine.tick(0.01f);

		assertThat(first.closed).isFalse();
		assertThat(listener.last()).isEqualTo(WorldNetEngine.Status.CONNECTED);
	}

	@Test
	@DisplayName("redials at once, and says nothing, when the server rotates a healthy socket")
	void treatsAnExpectedCloseAsRoutine() {
		connectAndOpen();
		engine.tick(10f);
		listener.statuses.clear();

		current().listener.onClosed("function duration reached");
		engine.tick(0.01f);

		assertThat(sockets).hasSize(2);
		assertThat(listener.statuses).doesNotContain(WorldNetEngine.Status.DEGRADED);
	}

	@Test
	@DisplayName("still backs off when a socket dies before it was ever healthy")
	void backsOffOnAnUnhealthyClose() {
		connectAndOpen();

		current().listener.onClosed("connection reset");
		engine.tick(0.01f);

		assertThat(listener.last()).isEqualTo(WorldNetEngine.Status.DEGRADED);
		assertThat(sockets).hasSize(1);
	}

	@Test
	@DisplayName("drops its own entry from the roster the server broadcasts to everyone")
	void filtersItselfOutOfTheRoster() {
		connectAndOpen();
		current().listener.onText("{\"t\":\"hello\",\"server_time\":1,\"player_id\":\"me\"}");
		current().listener.onText(
				"{\"t\":\"roster\",\"occupants\":["
						+ "{\"player_id\":\"me\",\"name\":\"Ann\",\"hero_class\":\"WARRIOR\",\"cell\":2,\"facing\":1},"
						+ "{\"player_id\":\"p1\",\"name\":\"Bo\",\"hero_class\":\"MAGE\",\"cell\":4,\"facing\":1}]}");
		engine.tick(0.01f);

		assertThat(listener.rosters).hasSize(1);
		assertThat(listener.rosters.get(0)).hasSize(1);
		assertThat(listener.rosters.get(0).get(0).playerId).isEqualTo("p1");
	}

	@Test
	@DisplayName("keeps the whole roster while it has not been told which player it is")
	void keepsRosterIntactWithoutAGreeting() {
		connectAndOpen();
		current().listener.onText(
				"{\"t\":\"roster\",\"occupants\":["
						+ "{\"player_id\":\"p1\",\"name\":\"Bo\",\"hero_class\":\"MAGE\",\"cell\":4,\"facing\":1}]}");
		engine.tick(0.01f);

		assertThat(listener.rosters.get(0)).hasSize(1);
	}

	@Test
	@DisplayName("forgets which player it is when the engine disconnects")
	void forgetsIdentityOnDisconnect() {
		connectAndOpen();
		current().listener.onText("{\"t\":\"hello\",\"server_time\":1,\"player_id\":\"me\"}");
		engine.tick(0.01f);
		engine.disconnect();

		engine.connect(new WorldIdentity("device", "Ann"), listener);
		current().listener.onOpen();
		current().listener.onText(
				"{\"t\":\"roster\",\"occupants\":["
						+ "{\"player_id\":\"me\",\"name\":\"Ann\",\"hero_class\":\"WARRIOR\",\"cell\":2,\"facing\":1}]}");
		engine.tick(0.01f);

		assertThat(listener.rosters.get(0)).hasSize(1);
	}

	@Test
	@DisplayName("publishes a moved avatar at once rather than waiting for the refresh")
	void sendsMovedPresencePromptly() {
		connectAndOpen();
		engine.setPresence(WorldPresence.local("WARRIOR", 12, 1));
		engine.tick(0.01f);
		engine.setPresence(WorldPresence.local("WARRIOR", 13, 1));
		engine.tick(0.01f);

		assertThat(current().countSent("\"t\":\"presence\"")).isEqualTo(2);
	}

	@Test
	@DisplayName("asks the server to drop the avatar exactly once when descending into a run")
	void sendsLeaveOnce() {
		connectAndOpen();
		engine.setPresence(WorldPresence.local("WARRIOR", 12, 1));
		engine.tick(0.01f);
		engine.setPresence(null);
		for (int i = 0; i < 10; i++) {
			engine.tick(1f);
		}

		assertThat(current().countSent("\"t\":\"leave\"")).isEqualTo(1);
	}

	@Test
	@DisplayName("says goodbye before closing, so a descending player's avatar goes at once")
	void sendsLeaveBeforeDisconnecting() {
		// Descending with world chat off drops the whole channel. Without this the
		// server sees only a closed socket, which is indistinguishable from a
		// player whose connection blipped — so it holds the avatar for its
		// reconnect grace period and the village keeps a ghost for twenty seconds.
		connectAndOpen();
		engine.setPresence(WorldPresence.local("WARRIOR", 12, 1));
		engine.tick(0.01f);

		engine.disconnect();

		assertThat(current().countSent("\"t\":\"leave\"")).isEqualTo(1);
		assertThat(current().closed).isTrue();
	}

	@Test
	@DisplayName("does not say goodbye for an avatar that was never standing in the village")
	void sendsNoLeaveWithoutAPresence() {
		connectAndOpen();

		engine.disconnect();

		assertThat(current().countSent("\"t\":\"leave\"")).isZero();
	}

	@Test
	@DisplayName("does not repeat a goodbye it has already sent")
	void doesNotRepeatLeaveOnDisconnect() {
		connectAndOpen();
		engine.setPresence(WorldPresence.local("WARRIOR", 12, 1));
		engine.tick(0.01f);
		engine.setPresence(null);
		engine.tick(0.01f);

		engine.disconnect();

		assertThat(current().countSent("\"t\":\"leave\"")).isEqualTo(1);
	}

	@Test
	@DisplayName("sends chat as a frame rather than a request")
	void sendsChat() {
		connectAndOpen();
		engine.sendChat("hello");

		assertThat(current().sentAny("\"t\":\"chat\"")).isTrue();
	}

	@Test
	@DisplayName("holds a message typed before the handshake finished and sends it on open")
	void queuesChatUntilOpen() {
		engine.connect(new WorldIdentity("device", "Ann"), listener);
		engine.sendChat("early");
		assertThat(current().sentAny("early")).isFalse();

		current().listener.onOpen();
		engine.tick(0.01f);

		assertThat(current().sentAny("early")).isTrue();
	}

	private void connectAndOpen(WorldIdentity identity) {
		engine.connect(identity, listener);
		current().listener.onOpen();
		engine.tick(0.01f);
	}

	@Test
	@DisplayName("refuses to spend a frame on chat while the mute it was given is still running")
	void refusesChatWhileMuted() {
		connectAndOpen(new WorldIdentity("device", "Ann", System.currentTimeMillis() + 60_000L));

		engine.sendChat("hello");

		assertThat(current().sentAny("hello")).isFalse();
		assertThat(engine.isServerMuted()).isTrue();
	}

	@Test
	@DisplayName("stays connected while muted, since a mute says nothing about the link")
	void aMuteIsNotAConnectionState() {
		connectAndOpen(new WorldIdentity("device", "Ann", System.currentTimeMillis() + 60_000L));

		assertThat(listener.last()).isEqualTo(WorldNetEngine.Status.CONNECTED);
		assertThat(listener.mutes).containsExactly(true);
	}

	@Test
	@DisplayName("lets the player speak once the mute timestamp has passed, with no server involved")
	void allowsChatOnceMuteLapses() {
		connectAndOpen(new WorldIdentity("device", "Ann", System.currentTimeMillis() - 1L));

		engine.sendChat("hello");

		assertThat(current().sentAny("hello")).isTrue();
		assertThat(engine.isServerMuted()).isFalse();
	}

	@Test
	@DisplayName("announces a mute that lapses mid-session, without a reconnect or a send")
	void liftsALapsedMuteOnTick() throws InterruptedException {
		connectAndOpen(new WorldIdentity("device", "Ann", System.currentTimeMillis() + 100L));
		assertThat(engine.isServerMuted()).isTrue();

		Thread.sleep(150L);
		engine.tick(0.15f);

		assertThat(engine.isServerMuted()).isFalse();
		assertThat(listener.mutes).containsExactly(true, false);
	}

	@Test
	@DisplayName("announces a mute only when the answer changes, not on every tick")
	void announcesAMuteOnlyOnChange() {
		connectAndOpen(new WorldIdentity("device", "Ann", System.currentTimeMillis() + 60_000L));

		engine.tick(0.01f);
		engine.tick(0.01f);

		assertThat(listener.mutes).containsExactly(true);
	}

	@Test
	@DisplayName("adopts a deadline refreshed mid-session without needing a reconnect")
	void adoptsARefreshedMute() {
		connectAndOpen();

		engine.applyServerMute(System.currentTimeMillis() + 60_000L);

		assertThat(engine.isServerMuted()).isTrue();
		assertThat(listener.mutes).containsExactly(true);
		engine.sendChat("hello");
		assertThat(current().sentAny("hello")).isFalse();
	}

	@Test
	@DisplayName("a refreshed deadline clears a refusal the server gave without one")
	void aRefreshedMuteReplacesARefusal() {
		connectAndOpen();
		current().listener.onText("{\"t\":\"error\",\"code\":\"muted\",\"detail\":\"You are muted\"}");
		engine.tick(0.01f);

		engine.applyServerMute(0L);

		assertThat(engine.isServerMuted()).isFalse();
		assertThat(listener.mutes).containsExactly(true, false);
	}

	@Test
	@DisplayName("reports a mute the server refused a send with, rather than calling the link degraded")
	void reportsARefusedSendAsMuted() {
		connectAndOpen();

		current().listener.onText("{\"t\":\"error\",\"code\":\"muted\",\"detail\":\"You are muted\"}");
		engine.tick(0.01f);

		assertThat(engine.isServerMuted()).isTrue();
		assertThat(listener.mutes).containsExactly(true);
		assertThat(listener.last()).isEqualTo(WorldNetEngine.Status.CONNECTED);
	}

	@Test
	@DisplayName("stops spending frames on chat after the server has refused one as muted")
	void refusesChatAfterAServerMute() {
		connectAndOpen();
		current().listener.onText("{\"t\":\"error\",\"code\":\"muted\",\"detail\":\"You are muted\"}");
		engine.tick(0.01f);

		engine.sendChat("hello");

		assertThat(current().sentAny("hello")).isFalse();
	}

	@Test
	@DisplayName("carries no mute across a reconnect, since the fresh identity brings its own")
	void forgetsAServerMuteOnReconnect() {
		connectAndOpen();
		current().listener.onText("{\"t\":\"error\",\"code\":\"muted\",\"detail\":\"You are muted\"}");
		engine.tick(0.01f);

		engine.disconnect();
		connectAndOpen(new WorldIdentity("device", "Ann"));
		engine.sendChat("hello");

		assertThat(current().sentAny("hello")).isTrue();
		assertThat(engine.isServerMuted()).isFalse();
		assertThat(listener.mutes).containsExactly(true, false);
	}

	@Test
	@DisplayName("ignores a mute frame from a server older than this build")
	void ignoresLegacyMuteFrame() {
		connectAndOpen();

		current().listener.onText("{\"t\":\"muted\",\"muted_until\":99999999999}");
		engine.tick(0.01f);

		engine.sendChat("hello");
		assertThat(current().sentAny("hello")).isTrue();
	}

	@Test
	@DisplayName("treats a rejected send as degraded without dropping the connection")
	void surfacesAnErrorFrameWithoutDisconnecting() {
		connectAndOpen();

		current().listener.onText("{\"t\":\"error\",\"code\":\"rate_limited\",\"detail\":\"slow down\"}");
		engine.tick(0.01f);

		assertThat(listener.last()).isEqualTo(WorldNetEngine.Status.DEGRADED);
		assertThat(engine.isConnected()).isTrue();
		assertThat(current().closed).isFalse();
	}

	@Test
	@DisplayName("ignores a frame type it does not recognise")
	void ignoresUnknownFrames() {
		connectAndOpen();
		int statusesBefore = listener.statuses.size();

		current().listener.onText("{\"t\":\"weather\",\"rain\":true}");
		engine.tick(0.01f);

		assertThat(listener.statuses).hasSize(statusesBefore);
		assertThat(listener.messages).isEmpty();
	}

	@Test
	@DisplayName("reconnects with a fresh socket after the server closes the connection")
	void reconnectsAfterClose() {
		connectAndOpen();

		current().listener.onClosed("max duration");
		engine.tick(0.01f);
		assertThat(listener.last()).isEqualTo(WorldNetEngine.Status.DEGRADED);

		engine.tick(2f);

		assertThat(sockets).hasSize(2);
	}

	@Test
	@DisplayName("backs off further on each successive failure instead of hammering the server")
	void backsOffOnRepeatedFailures() {
		connectAndOpen();

		current().listener.onClosed("boom");
		engine.tick(1.5f);
		assertThat(sockets).hasSize(2);

		current().listener.onClosed("boom");
		engine.tick(1.5f);
		assertThat(sockets).hasSize(2);

		engine.tick(1.5f);
		assertThat(sockets).hasSize(3);
	}

	@Test
	@DisplayName("re-publishes the avatar after reconnecting, so it does not vanish from the village")
	void republishesPresenceAfterReconnect() {
		connectAndOpen();
		engine.setPresence(WorldPresence.local("WARRIOR", 12, 1));
		engine.tick(0.01f);

		current().listener.onClosed("dropped");
		engine.tick(2f);
		current().listener.onOpen();
		engine.tick(0.01f);

		assertThat(current().sentAny("\"t\":\"presence\"")).isTrue();
	}

	@Test
	@DisplayName("sends a keepalive on a quiet connection so a dead link is noticed")
	void sendsKeepalive() {
		connectAndOpen();

		engine.tick(25f);

		assertThat(current().sentAny("\"t\":\"ping\"")).isTrue();
	}

	@Test
	@DisplayName("gives up on a silent socket and reconnects rather than waiting forever")
	void reconnectsWhenTheLinkGoesSilent() {
		connectAndOpen();
		FakeSocket original = current();

		for (int i = 0; i < 8; i++) {
			engine.tick(10f);
		}

		assertThat(original.closed).isTrue();
		assertThat(sockets.size()).isGreaterThan(1);
	}

	@Test
	@DisplayName("abandons a handshake that never completes instead of stalling on it forever")
	void retriesAStalledHandshake() {
		engine.connect(new WorldIdentity("device", "Ann"), listener);
		FakeSocket original = current();

		for (int i = 0; i < 6; i++) {
			engine.tick(10f);
		}

		assertThat(original.closed).isTrue();
		assertThat(sockets.size()).isGreaterThan(1);
	}

	@Test
	@DisplayName("closes the socket and reports disconnected on an explicit disconnect")
	void disconnectClosesTheSocket() {
		connectAndOpen();
		FakeSocket socket = current();

		engine.disconnect();

		assertThat(socket.closed).isTrue();
		assertThat(engine.isConnected()).isFalse();
		assertThat(listener.last()).isEqualTo(WorldNetEngine.Status.DISCONNECTED);
	}

	@Test
	@DisplayName("ignores frames that arrive from a socket abandoned by a disconnect")
	void ignoresLateFramesFromAnOldSocket() {
		connectAndOpen();
		FakeSocket stale = current();
		engine.disconnect();
		int messagesBefore = listener.messages.size();

		stale.listener.onText(
				"{\"t\":\"chat\",\"messages\":[{\"id\":\"m9\",\"name\":\"Ann\",\"text\":\"late\",\"at\":9}]}");
		engine.tick(0.01f);

		assertThat(listener.messages).hasSize(messagesBefore);
	}

	@Test
	@DisplayName("does not reconnect after an explicit disconnect")
	void doesNotReconnectAfterDisconnect() {
		connectAndOpen();
		engine.disconnect();

		engine.tick(60f);

		assertThat(sockets).hasSize(1);
	}

	@Test
	@DisplayName("tells the server the player is reading chat so it can tick faster")
	void forwardsChatFocus() {
		connectAndOpen();

		engine.setChatFocused(true);
		engine.tick(0.01f);

		assertThat(current().sentAny("\"t\":\"focus\"")).isTrue();
	}

	@Test
	@DisplayName("sends a report as a frame on the existing connection")
	void sendsReport() {
		connectAndOpen();

		engine.report("m1", "spam");

		assertThat(current().sentAny("\"t\":\"report\"")).isTrue();
	}
}
