package com.shatteredpixel.shatteredpixeldungeon.worldnet;

import static org.assertj.core.api.Assertions.assertThat;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPlayerSession;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.ServerMuteRefresh;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtendWith(GdxTestExtension.class)
@DisplayName("World net lifecycle owner")
class WorldNetTest {

	private static final class FakeEngine implements WorldNetEngine {
		Listener listener;
		boolean connected;
		WorldPresence presence;
		int connects;
		int disconnects;
		float ticked;
		final List<String> sent = new ArrayList<>();

		WorldIdentity identity;

		@Override
		public void connect(WorldIdentity identity, Listener listener) {
			this.identity = identity;
			this.listener = listener;
			this.connected = true;
			this.connects++;
		}

		@Override
		public void disconnect() {
			this.connected = false;
			this.disconnects++;
			this.presence = null;
		}

		@Override
		public boolean isConnected() {
			return connected;
		}

		boolean muted;
		final List<Long> appliedMutes = new ArrayList<>();

		@Override
		public boolean isServerMuted() {
			return muted;
		}

		@Override
		public void applyServerMute(long mutedUntil) {
			appliedMutes.add(mutedUntil);
			muted = mutedUntil > System.currentTimeMillis();
		}

		@Override
		public void setPresence(WorldPresence local) {
			this.presence = local;
		}

		@Override
		public void sendChat(String text) {
			sent.add(text);
		}

		@Override
		public void report(String messageId, String reason) {
			sent.add("report:" + messageId);
		}

		boolean chatFocused;

		@Override
		public void setChatFocused(boolean focused) {
			this.chatFocused = focused;
		}

		@Override
		public void tick(float elapsed) {
			ticked += elapsed;
		}

		@Override
		public String name() {
			return "fake";
		}
	}

	private FakeEngine engine;

	@BeforeEach
	void setUp() {
		engine = new FakeEngine();
		WorldNet.setEngineForTests(engine);
		MuteList.clear();
	}

	@AfterEach
	void tearDown() {
		WorldNet.reset();
		ServerMuteRefresh.resetForTests();
		EchoPlayerSession.resetForTests();
	}

	private static WorldChatMessage message(String id, String text) {
		return new WorldChatMessage(id, "p1", "Alice", text, 1L);
	}

	@Test
	@DisplayName("the websocket is the only transport; there is no polling fallback")
	void theWebsocketIsTheOnlyTransport() throws java.io.IOException {
		// A fallback nobody can select is a second implementation of the whole
		// protocol kept alive by nothing. Removing it means a socket problem has
		// to be fixed rather than switched around.
		String source = readSource(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/WorldNet.java");

		assertThat(source).contains("WebSocketWorldNetEngine.createDefault()");
		assertThat(source).doesNotContain("Polling");
		assertThat(source).doesNotContain("worldTransport");

		java.nio.file.Path polling = repoRoot().resolve(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/worldnet/polling");
		assertThat(java.nio.file.Files.exists(polling)).isFalse();
	}

	private static String readSource(String relativePath) throws java.io.IOException {
		return new String(
				java.nio.file.Files.readAllBytes(repoRoot().resolve(relativePath)),
				java.nio.charset.StandardCharsets.UTF_8);
	}

	private static java.nio.file.Path repoRoot() {
		java.nio.file.Path dir = java.nio.file.Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && dir != null; i++) {
			if (java.nio.file.Files.isRegularFile(dir.resolve("settings.gradle"))) {
				return dir;
			}
			dir = dir.getParent();
		}
		throw new AssertionError("Could not find the repository root");
	}

	@Test
	@DisplayName("entering the village connects and publishes the local avatar")
	void villageConnects() {
		WorldNet.enterVillage("WARRIOR", 10, 1);

		assertThat(engine.connects).isEqualTo(1);
		assertThat(engine.presence).isNotNull();
		assertThat(engine.presence.heroClass).isEqualTo("WARRIOR");
		assertThat(engine.presence.cell).isEqualTo(10);
	}

	@Test
	@DisplayName("hands the engine the mute deadline the session got at authentication")
	void connectCarriesTheSessionMute() {
		// The engine is told once, at connect, and never asks again — so if this
		// deadline does not make the trip the mute silently stops existing.
		long until = System.currentTimeMillis() + 60_000L;
		EchoPlayerSession.applyAuthResponse("jwt", "Ann", false, null, until);

		WorldNet.enterVillage("WARRIOR", 10, 1);

		assertThat(engine.identity.mutedUntil).isEqualTo(until);
	}

	@Test
	@DisplayName("entering the village connects even when the chat setting is off")
	void villageIgnoresOptOut() {
		SPDSettings.worldChat(false);

		WorldNet.enterVillage("WARRIOR", 10, 1);

		assertThat(engine.isConnected()).isTrue();
	}

	@Test
	@DisplayName("moving in the village republishes the cell without reconnecting")
	void movePublishesCell() {
		WorldNet.enterVillage("WARRIOR", 10, 1);

		WorldNet.updatePresence(42, -1);

		assertThat(engine.connects).isEqualTo(1);
		assertThat(engine.presence.cell).isEqualTo(42);
		assertThat(engine.presence.facing).isEqualTo(-1);
	}

	@Test
	@DisplayName("descending keeps chat but clears the village avatar when chat is opted in")
	void runKeepsChatWhenOptedIn() {
		SPDSettings.worldChat(true);
		WorldNet.enterVillage("WARRIOR", 10, 1);

		WorldNet.enterRun();

		assertThat(engine.isConnected()).isTrue();
		assertThat(engine.presence).isNull();
	}

	@Test
	@DisplayName("descending disconnects entirely when chat is opted out")
	void runDisconnectsWhenOptedOut() {
		SPDSettings.worldChat(false);
		WorldNet.enterVillage("WARRIOR", 10, 1);

		WorldNet.enterRun();

		assertThat(engine.isConnected()).isFalse();
	}

	@Test
	@DisplayName("leaving disconnects and forgets the roster")
	void leaveDisconnects() {
		WorldNet.enterVillage("WARRIOR", 10, 1);
		engine.listener.onPresenceSnapshot(
				Collections.singletonList(new WorldPresence("p2", "Bob", "MAGE", 5, 1)));

		WorldNet.leave();

		assertThat(engine.disconnects).isEqualTo(1);
		assertThat(WorldNet.roster()).isEmpty();
	}

	@Test
	@DisplayName("keeps a rolling chat history for the chat window")
	void keepsHistory() {
		WorldNet.enterVillage("WARRIOR", 10, 1);

		engine.listener.onChatMessages(Collections.singletonList(message("m1", "hello")));
		engine.listener.onChatMessages(Collections.singletonList(message("m2", "again")));

		assertThat(WorldNet.history()).hasSize(2);
		assertThat(WorldNet.history().get(1).text).isEqualTo("again");
	}

	@Test
	@DisplayName("caps chat history so a long session cannot grow without bound")
	void capsHistory() {
		WorldNet.enterVillage("WARRIOR", 10, 1);

		for (int i = 0; i < WorldNet.HISTORY_CAP + 25; i++) {
			engine.listener.onChatMessages(Collections.singletonList(message("m" + i, "line " + i)));
		}

		assertThat(WorldNet.history()).hasSize(WorldNet.HISTORY_CAP);
		assertThat(WorldNet.history().get(WorldNet.HISTORY_CAP - 1).text)
				.isEqualTo("line " + (WorldNet.HISTORY_CAP + 24));
	}

	@Test
	@DisplayName("exposes the roster from the latest snapshot")
	void exposesRoster() {
		WorldNet.enterVillage("WARRIOR", 10, 1);

		engine.listener.onPresenceSnapshot(
				Collections.singletonList(new WorldPresence("p2", "Bob", "MAGE", 5, 1)));

		assertThat(WorldNet.roster()).hasSize(1);
		assertThat(WorldNet.roster().get(0).displayName).isEqualTo("Bob");
	}

	@Test
	@DisplayName("notifies an observer of incoming chat")
	void notifiesObserver() {
		List<String> seen = new ArrayList<>();
		WorldNet.observe(new WorldNet.Observer() {
			@Override
			public void onWorldChat(WorldChatMessage incoming) {
				seen.add(incoming.text);
			}

			@Override
			public void onLocalChat(String text) {
			}

			@Override
			public void onWorldRoster(List<WorldPresence> occupants) {
			}

			@Override
			public void onWorldStatus(WorldNetEngine.Status status) {
			}

			@Override
			public void onServerMute(boolean muted) {
			}
		});
		WorldNet.enterVillage("WARRIOR", 10, 1);

		engine.listener.onChatMessages(Collections.singletonList(message("m1", "hello")));

		assertThat(seen).containsExactly("hello");
	}

	@Test
	@DisplayName("withholds chat from a player the user has muted")
	void hidesMutedPlayer() {
		WorldNet.enterVillage("WARRIOR", 10, 1);
		MuteList.mute("Alice");

		engine.listener.onChatMessages(Collections.singletonList(message("m1", "spam")));

		assertThat(WorldNet.history()).isEmpty();
	}

	@Test
	@DisplayName("still delivers chat from players who are not muted")
	void keepsUnmutedPlayers() {
		WorldNet.enterVillage("WARRIOR", 10, 1);
		MuteList.mute("Mallory");

		engine.listener.onChatMessages(Collections.singletonList(message("m1", "hello")));

		assertThat(WorldNet.history()).hasSize(1);
	}

	@Test
	@DisplayName("does not notify observers about a muted player's chat")
	void doesNotNotifyForMutedPlayer() {
		List<String> seen = new ArrayList<>();
		WorldNet.observe(new WorldNet.Observer() {
			@Override
			public void onWorldChat(WorldChatMessage incoming) {
				seen.add(incoming.text);
			}

			@Override
			public void onLocalChat(String text) {
			}

			@Override
			public void onWorldRoster(List<WorldPresence> occupants) {
			}

			@Override
			public void onWorldStatus(WorldNetEngine.Status status) {
			}

			@Override
			public void onServerMute(boolean muted) {
			}
		});
		WorldNet.enterVillage("WARRIOR", 10, 1);
		MuteList.mute("Alice");

		engine.listener.onChatMessages(Collections.singletonList(message("m1", "spam")));

		assertThat(seen).isEmpty();
	}

	@Test
	@DisplayName("asks the engine about a server-side mute so the composer can be disabled")
	void exposesServerMute() {
		WorldNet.enterVillage("WARRIOR", 10, 1);
		engine.muted = true;

		assertThat(WorldNet.serverMuted()).isTrue();
	}

	@Test
	@DisplayName("re-asks the engine every time, so a mute that lapses stops blocking sends")
	void doesNotCacheTheServerMute() {
		WorldNet.enterVillage("WARRIOR", 10, 1);
		engine.muted = true;
		assertThat(WorldNet.serverMuted()).isTrue();

		engine.muted = false;

		assertThat(WorldNet.serverMuted()).isFalse();
		WorldNet.sendChat("hello");
		assertThat(engine.sent).containsExactly("hello");
	}

	@Test
	@DisplayName("hands the engine a deadline that a refresh changed under it")
	void pushesARefreshedMuteToTheEngine() {
		WorldNet.enterVillage("WARRIOR", 10, 1);
		long extended = System.currentTimeMillis() + 60_000L;

		// What a background /v1/auth/me refresh does: writes the session and leaves.
		EchoPlayerSession.applyAuthResponse("jwt", "Ann", false, null, extended);
		WorldNet.tick(0.01f);

		assertThat(engine.appliedMutes).containsExactly(extended);
		assertThat(WorldNet.serverMuted()).isTrue();
	}

	@Test
	@DisplayName("pushes a deadline once rather than every frame, so a refusal is not reset")
	void pushesARefreshedMuteOnlyOnce() {
		WorldNet.enterVillage("WARRIOR", 10, 1);
		EchoPlayerSession.applyAuthResponse("jwt", "Ann", false, null, System.currentTimeMillis() + 60_000L);

		WorldNet.tick(0.01f);
		WorldNet.tick(0.01f);
		WorldNet.tick(0.01f);

		assertThat(engine.appliedMutes).hasSize(1);
	}

	@Test
	@DisplayName("does not re-push the deadline the engine was already given at connect")
	void doesNotPushTheConnectDeadline() {
		EchoPlayerSession.applyAuthResponse("jwt", "Ann", false, null, System.currentTimeMillis() + 60_000L);
		WorldNet.enterVillage("WARRIOR", 10, 1);

		WorldNet.tick(0.01f);

		assertThat(engine.appliedMutes).isEmpty();
	}

	@Test
	@DisplayName("reports no mute when there is no engine, rather than building one to ask")
	void noEngineMeansNoMute() {
		WorldNet.reset();

		assertThat(WorldNet.serverMuted()).isFalse();
	}

	@Test
	@DisplayName("passes a mute change on to observers so the log can announce it")
	void forwardsMuteChanges() {
		List<Boolean> seen = new ArrayList<>();
		WorldNet.observe(new WorldNet.Observer() {
			@Override
			public void onWorldChat(WorldChatMessage incoming) {
			}

			@Override
			public void onLocalChat(String text) {
			}

			@Override
			public void onWorldRoster(List<WorldPresence> occupants) {
			}

			@Override
			public void onWorldStatus(WorldNetEngine.Status status) {
			}

			@Override
			public void onServerMute(boolean muted) {
				seen.add(muted);
			}
		});
		WorldNet.enterVillage("WARRIOR", 10, 1);

		engine.listener.onServerMute(true);
		engine.listener.onServerMute(false);

		assertThat(seen).containsExactly(true, false);
	}

	@Test
	@DisplayName("does not tick an engine that is not connected")
	void ignoresTickWhenDisconnected() {
		WorldNet.tick(1f);

		assertThat(engine.ticked).isZero();
	}

	@Test
	@DisplayName("drops a chat send when not connected rather than throwing")
	void ignoresSendWhenDisconnected() {
		WorldNet.sendChat("nobody home");

		assertThat(engine.sent).isEmpty();
	}

	@Test
	@DisplayName("echoes an outgoing line back locally so the sender sees their own bubble")
	void echoesOwnChatLocally() {
		List<String> spoken = new ArrayList<>();
		WorldNet.observe(new WorldNet.Observer() {
			@Override
			public void onWorldChat(WorldChatMessage incoming) {
			}

			@Override
			public void onLocalChat(String text) {
				spoken.add(text);
			}

			@Override
			public void onWorldRoster(List<WorldPresence> occupants) {
			}

			@Override
			public void onWorldStatus(WorldNetEngine.Status status) {
			}

			@Override
			public void onServerMute(boolean muted) {
			}
		});
		WorldNet.enterVillage("WARRIOR", 10, 1);

		WorldNet.sendChat("hello village");

		assertThat(spoken).containsExactly("hello village");
	}

	@Test
	@DisplayName("does not echo locally when the send never left the client")
	void noLocalEchoWhenDisconnected() {
		List<String> spoken = new ArrayList<>();
		WorldNet.observe(new WorldNet.Observer() {
			@Override
			public void onWorldChat(WorldChatMessage incoming) {
			}

			@Override
			public void onLocalChat(String text) {
				spoken.add(text);
			}

			@Override
			public void onWorldRoster(List<WorldPresence> occupants) {
			}

			@Override
			public void onWorldStatus(WorldNetEngine.Status status) {
			}

			@Override
			public void onServerMute(boolean muted) {
			}
		});

		WorldNet.sendChat("nobody home");

		assertThat(spoken).isEmpty();
	}

	/** Records only what it was told, so a test can tell live chat from a replay. */
	private static final class RecordingObserver implements WorldNet.Observer {
		final List<String> live = new ArrayList<>();
		final List<String> backlog = new ArrayList<>();

		@Override
		public void onWorldChat(WorldChatMessage incoming) {
			live.add(incoming.text);
		}

		@Override
		public void onWorldChatBacklog(List<WorldChatMessage> messages) {
			for (WorldChatMessage incoming : messages) {
				backlog.add(incoming.text);
			}
		}

		@Override
		public void onLocalChat(String text) {
		}

		@Override
		public void onWorldRoster(List<WorldPresence> occupants) {
		}

		@Override
		public void onWorldStatus(WorldNetEngine.Status status) {
		}

		@Override
		public void onServerMute(boolean muted) {
		}
	}

	@Test
	@DisplayName("replays what arrived before anyone was watching to the first observer")
	void replaysBacklogToALateObserver() {
		// The backlog frame lands the instant the socket opens, which is before
		// the scene has finished building the channel that displays it.
		SPDSettings.worldChat(true);
		WorldNet.enterRun();
		engine.listener.onChatMessages(
				Collections.singletonList(message("m1", "said before you looked")));

		RecordingObserver observer = new RecordingObserver();
		WorldNet.observe(observer);

		assertThat(observer.backlog).containsExactly("said before you looked");
		assertThat(observer.live).isEmpty();
	}

	@Test
	@DisplayName("replays the backlog in a run, not only in the village")
	void replaysBacklogInARun() {
		SPDSettings.worldChat(true);
		WorldNet.enterRun();
		engine.listener.onChatMessages(Collections.singletonList(message("m1", "heard downstairs")));

		RecordingObserver observer = new RecordingObserver();
		WorldNet.observe(observer);

		assertThat(observer.backlog).containsExactly("heard downstairs");
	}

	@Test
	@DisplayName("does not replay a line the previous observer was already shown")
	void doesNotReplayWhatWasAlreadySeen() {
		// Changing floors tears one channel down and builds another. The player
		// has already read these lines, and a second copy per staircase would
		// bury the log.
		SPDSettings.worldChat(true);
		WorldNet.enterRun();
		RecordingObserver first = new RecordingObserver();
		WorldNet.observe(first);
		engine.listener.onChatMessages(Collections.singletonList(message("m1", "hello")));
		WorldNet.unobserve(first);

		RecordingObserver second = new RecordingObserver();
		WorldNet.observe(second);

		assertThat(first.live).containsExactly("hello");
		assertThat(second.backlog).isEmpty();
		assertThat(second.live).isEmpty();
	}

	@Test
	@DisplayName("keeps a muted player out of the replay as well as out of the live feed")
	void doesNotReplayMutedChat() {
		MuteList.mute("Alice");
		SPDSettings.worldChat(true);
		WorldNet.enterRun();
		engine.listener.onChatMessages(Collections.singletonList(message("m1", "hidden")));

		RecordingObserver observer = new RecordingObserver();
		WorldNet.observe(observer);

		assertThat(observer.backlog).isEmpty();
		MuteList.unmute("Alice");
	}

	@Test
	@DisplayName("drops the replay when the channel is left, so a fresh join starts clean")
	void clearsBacklogOnLeave() {
		SPDSettings.worldChat(true);
		WorldNet.enterRun();
		engine.listener.onChatMessages(Collections.singletonList(message("m1", "stale")));
		WorldNet.leave();

		RecordingObserver observer = new RecordingObserver();
		WorldNet.observe(observer);

		assertThat(observer.backlog).isEmpty();
	}
}
