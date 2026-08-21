/*
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.worldnet;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPlayerAuth;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPlayerSession;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.ServerMuteRefresh;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.VersionNames;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageFigure;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.websocket.WebSocketWorldNetEngine;
import com.watabou.noosa.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns the world-channel engine and the state the UI reads from it.
 *
 * <p>Engine selection is a {@code static} here rather than a separate factory
 * type — {@code .cursor/rules/no-pass-through-wrappers.mdc} rejects a class
 * whose only job is to {@code new} a singleton, while an async/lifecycle owner
 * is on that rule's allowed list. Swapping the shipped socket engine for
 * anything else is a change to {@link #engine()} and nothing else.
 *
 * <p>All methods are render-thread only. The engine does its own queueing, so
 * nothing here needs {@code Game.runOnRenderThread}.
 */
public final class WorldNet {

	/** Enough scrollback to be useful without letting a long session grow unbounded. */
	public static final int HISTORY_CAP = 200;

	/** Single global village for now; the field exists so sharding stays a server change. */
	private static final int INSTANCE = 0;

	public interface Observer {
		void onWorldChat(WorldChatMessage message);

		/**
		 * Chat that was said before this observer existed, handed over once when
		 * it registers.
		 *
		 * <p>Separate from {@link #onWorldChat} because these lines are history,
		 * not events: they belong in the log, but raising a speech bubble over
		 * somebody for a sentence they finished a minute ago would be a lie about
		 * what is happening in the village right now.
		 *
		 * <p>Defaulted to nothing so an observer that only cares about live
		 * traffic — a chat window opened mid-conversation, which reads
		 * {@link WorldNet#history()} for itself — needs no say in this.
		 */
		default void onWorldChatBacklog(List<WorldChatMessage> messages) {
		}

		/**
		 * The local player just said something.
		 *
		 * <p>Separate from {@link #onWorldChat} because the server stamps
		 * messages with its own player id, which this client never learns — so
		 * an incoming line can't be recognised as one's own. The sender's own
		 * bubble is therefore raised optimistically here, and the echoed copy
		 * that arrives on the next poll matches no avatar (the roster excludes
		 * the caller) and so raises nothing.
		 */
		void onLocalChat(String text);

		void onWorldRoster(List<WorldPresence> occupants);

		/**
		 * The whole set of village bodies.
		 *
		 * <p>Defaulted to nothing: an observer that only cares about chat — a chat
		 * window — has no say in what is standing in the town square.
		 *
		 * @see WorldNetEngine.Listener#onWorldFigures(List)
		 */
		default void onWorldFigures(List<VillageFigure> figures) {
		}

		/**
		 * The bundle behind a figure somebody asked to inspect.
		 *
		 * @see WorldNetEngine.Listener#onEchoBundle(String, String)
		 */
		default void onEchoBundle(String echoId, String echoData) {
		}

		void onWorldStatus(WorldNetEngine.Status status);

		/** @see WorldNetEngine.Listener#onServerMute(boolean) */
		void onServerMute(boolean muted);
	}

	private static WorldNetEngine engine;
	/**
	 * More than one at a time on purpose: the scene bridge is registered for as
	 * long as the channel is up, while an open chat window adds itself only
	 * while it is on screen.
	 */
	private static final List<Observer> observers = new ArrayList<>();
	private static final List<WorldChatMessage> history = new ArrayList<>();
	/**
	 * Chat that arrived while nobody was listening, held for whoever attaches next.
	 *
	 * <p>The server hands over its whole in-memory backlog the instant a socket
	 * opens, which is several frames before the scene has finished building the
	 * channel that displays it. Without this those lines would reach
	 * {@link #history} and never be seen — the player would sit in a village that
	 * had just been talking and read nothing until somebody said the next thing.
	 *
	 * <p>Drained rather than replayed, so it is a one-time catch-up: walking down
	 * a staircase tears one channel down and builds another, and a player who has
	 * already read these lines must not be shown them again per floor.
	 */
	private static final List<WorldChatMessage> unshown = new ArrayList<>();
	private static List<WorldPresence> roster = Collections.emptyList();
	/**
	 * The last set of bodies the server pushed.
	 *
	 * <p>Kept so a village built after the greeting still fills — the figures
	 * arrive with {@code hello}, several frames before the scene exists to stand
	 * them in.
	 */
	private static List<VillageFigure> figures = Collections.emptyList();
	private static WorldNetEngine.Status status = WorldNetEngine.Status.DISCONNECTED;
	private static String heroClass = "";
	/** The deadline the engine was last given, so an unchanged one is not re-pushed. */
	private static long pushedMute;
	/** The build the server last said it expects, or empty before any greeting. */
	private static String serverVersion = "";

	private WorldNet() {
	}

	private static final WorldNetEngine.Listener LISTENER = new WorldNetEngine.Listener() {
		@Override
		public void onChatMessages(List<WorldChatMessage> messages) {
			for (WorldChatMessage message : messages) {
				// Filtered here rather than in the UI so a muted player stays out
				// of the scrollback, the game log and their own speech bubble.
				if (MuteList.isMuted(message.name)) {
					continue;
				}
				history.add(message);
				if (observers.isEmpty()) {
					unshown.add(message);
					continue;
				}
				// Copy before iterating: an observer may unregister itself from
				// inside its own callback (a chat window closing on a tap).
				for (Observer observer : new ArrayList<>(observers)) {
					observer.onWorldChat(message);
				}
			}
			trimToCap(history);
			trimToCap(unshown);
		}

		@Override
		public void onPresenceSnapshot(List<WorldPresence> others) {
			roster = others;
			for (Observer observer : new ArrayList<>(observers)) {
				observer.onWorldRoster(others);
			}
		}

		@Override
		public void onWorldFigures(List<VillageFigure> pushed) {
			figures = pushed;
			for (Observer observer : new ArrayList<>(observers)) {
				observer.onWorldFigures(pushed);
			}
		}

		@Override
		public void onEchoBundle(String echoId, String echoData) {
			for (Observer observer : new ArrayList<>(observers)) {
				observer.onEchoBundle(echoId, echoData);
			}
		}

		@Override
		public void onStatus(WorldNetEngine.Status newStatus, String detail) {
			status = newStatus;
			for (Observer observer : new ArrayList<>(observers)) {
				observer.onWorldStatus(newStatus);
			}
		}

		@Override
		public void onServerMute(boolean muted) {
			for (Observer observer : new ArrayList<>(observers)) {
				observer.onServerMute(muted);
			}
		}

		@Override
		public void onServerVersion(String versionName) {
			serverVersion = versionName;
		}
	};

	/**
	 * Whether the server this client is talking to expects a different build.
	 *
	 * <p>The village gate reads this. Deliberately not acted on here: the world
	 * channel's job is to know, and the scene's job is to decide when a player
	 * can be interrupted — which is never while they are down a dungeon.
	 */
	/** The build the server said it expects, or empty before any greeting. */
	public static String serverVersion() {
		return serverVersion;
	}

	public static boolean updateRequired() {
		return versionsDiffer(serverVersion, Game.version);
	}

	/**
	 * Empty on either side means "not known", never "mismatched": a server too old
	 * to state its version, or a build without one, must not gate anybody out.
	 *
	 * <p>Build suffixes are not versions — {@link VersionNames} compares the
	 * numeric core, so a {@code -INDEV} desktop run is not gated out of its own
	 * dev server's village.
	 */
	static boolean versionsDiffer(String server, String installed) {
		return server != null
				&& !server.isEmpty()
				&& installed != null
				&& !installed.isEmpty()
				&& VersionNames.differ(server, installed);
	}

	public static WorldNetEngine engine() {
		if (engine == null) {
			// The only place in the game that names a transport. Everything else
			// talks to WorldNetEngine, so swapping one in is a change here.
			engine = WebSocketWorldNetEngine.createDefault();
		}
		return engine;
	}

	/**
	 * Connection check that does <em>not</em> create an engine.
	 *
	 * <p>{@link #engine()} spins up a worker thread on first call, so UI asking
	 * "should I show the chat button?" must not go through it.
	 */
	public static boolean isConnected() {
		return engine != null && engine.isConnected();
	}

	public static void setEngineForTests(WorldNetEngine replacement) {
		engine = replacement;
	}

	public static void observe(Observer newObserver) {
		if (newObserver == null || observers.contains(newObserver)) {
			return;
		}
		observers.add(newObserver);
		if (unshown.isEmpty()) {
			return;
		}
		// Copied and cleared before the callback: the observer may say something
		// of its own from inside it, and re-entering here with a half-drained
		// list would show a line twice.
		List<WorldChatMessage> caughtUp = new ArrayList<>(unshown);
		unshown.clear();
		newObserver.onWorldChatBacklog(Collections.unmodifiableList(caughtUp));
	}

	public static void unobserve(Observer goneObserver) {
		observers.remove(goneObserver);
	}

	/**
	 * Town always joins the channel.
	 *
	 * <p>{@link SPDSettings#worldChat()} deliberately does not gate this: being
	 * seen and being able to talk is what the village is for, and a player who
	 * wants neither can stay out of town. The setting governs runs only.
	 */
	public static void enterVillage(String localHeroClass, int cell, int facing) {
		heroClass = localHeroClass != null ? localHeroClass : "";
		if (!engine().isConnected()) {
			history.clear();
		unshown.clear();
			unshown.clear();
			engine().connect(identity(), LISTENER);
		}
		engine().setPresence(WorldPresence.local(heroClass, cell, facing));
	}

	/** Republishes the avatar after a move. Cheap — the engine coalesces onto its own tick. */
	public static void updatePresence(int cell, int facing) {
		if (engine == null || !engine.isConnected()) {
			return;
		}
		engine.setPresence(WorldPresence.local(heroClass, cell, facing));
	}

	/**
	 * In a run: drop out of the village roster, and drop the channel entirely
	 * unless the player opted chat into runs.
	 *
	 * <p>Connects when needed rather than requiring a prior village visit — a
	 * player who starts a run straight from the title screen should still get
	 * chat if they asked for it.
	 */
	public static void enterRun() {
		if (!SPDSettings.worldChat()) {
			leave();
			return;
		}
		if (!engine().isConnected()) {
			history.clear();
			unshown.clear();
			engine().connect(identity(), LISTENER);
		}
		engine().setPresence(null);
	}

	public static void leave() {
		if (engine != null && engine.isConnected()) {
			engine.disconnect();
		}
		roster = Collections.emptyList();
		status = WorldNetEngine.Status.DISCONNECTED;
		// Nothing is owed to a channel that has been left. Whatever was waiting
		// to be caught up on is now somebody else's conversation.
		unshown.clear();
	}

	/** Called once per frame from {@code GameScene.update()}. */
	public static void tick(float elapsed) {
		// Outside the connected check: a mute outlives any one connection, and the
		// moment its deadline passes is worth a look whether or not chat is up.
		ServerMuteRefresh.verifyIfLapsed();
		if (engine == null || !engine.isConnected()) {
			return;
		}
		pushMuteIfChanged();
		engine.tick(elapsed);
	}

	/**
	 * Hands the engine a deadline that changed under it.
	 *
	 * <p>A refresh writes to the session from its own thread, so this is how the
	 * new value crosses back onto the render thread. Only on change: pushing every
	 * frame would keep resetting a refusal the server gave without a deadline.
	 */
	private static void pushMuteIfChanged() {
		long stored = EchoPlayerSession.mutedUntil();
		if (stored == pushedMute) {
			return;
		}
		pushedMute = stored;
		engine.applyServerMute(stored);
	}

	public static void sendChat(String text) {
		if (engine == null || !engine.isConnected()) {
			return;
		}
		// Guarded here as well as in the engine so a muted player does not even
		// get the optimistic speech bubble for a line nobody will receive.
		if (serverMuted()) {
			return;
		}
		engine.sendChat(text);
		for (Observer observer : new ArrayList<>(observers)) {
			observer.onLocalChat(text);
		}
	}

	/** The local player's display name, so UI need not know where identity lives. */
	public static String localName() {
		return EchoPlayerAuth.preferredUsername();
	}

	/** @see WorldNetEngine#setChatFocused(boolean) */
	public static void setChatFocused(boolean focused) {
		if (engine == null || !engine.isConnected()) {
			return;
		}
		engine.setChatFocused(focused);
	}

	public static void report(String messageId, String reason) {
		if (engine == null || !engine.isConnected()) {
			return;
		}
		engine.report(messageId, reason);
	}

	public static List<WorldChatMessage> history() {
		return Collections.unmodifiableList(history);
	}

	public static List<WorldPresence> roster() {
		return Collections.unmodifiableList(roster);
	}

	/**
	 * The bodies the server last said are standing in town.
	 *
	 * <p>Read by a village that finishes building after the greeting arrived, so
	 * it fills from what is already known rather than waiting for the next push.
	 */
	public static List<VillageFigure> figures() {
		return Collections.unmodifiableList(figures);
	}

	/**
	 * Asks for the hero behind one standing figure.
	 *
	 * <p>Silently does nothing without an engine, which is the same answer as a
	 * request that goes out and is never answered — and the window that asked is
	 * built to keep showing the broadcast facts either way.
	 */
	public static void requestEchoBundle(String echoId) {
		if (engine != null) {
			engine.requestEchoBundle(echoId);
		}
	}

	public static WorldNetEngine.Status status() {
		return status;
	}

	/**
	 * True while an admin has muted this player server-side.
	 *
	 * <p>Distinct from {@link MuteList}, which is this client choosing not to
	 * hear others. This one means the server will reject anything we send, so
	 * the composer should refuse rather than let the player type into a 403.
	 *
	 * <p>Asked of the engine rather than cached from the last notification: the
	 * deadline expires on the clock, and a cached copy would still say "muted"
	 * for as long as nothing happened to refresh it.
	 */
	public static boolean serverMuted() {
		return engine != null && engine.isServerMuted();
	}

	public static int instance() {
		return INSTANCE;
	}

	/** Test hook: drops the engine and all cached state. */
	public static void reset() {
		engine = null;
		observers.clear();
		history.clear();
		roster = Collections.emptyList();
		status = WorldNetEngine.Status.DISCONNECTED;
		heroClass = "";
		pushedMute = 0L;
		serverVersion = "";
	}

	static WorldIdentity identity() {
		pushedMute = EchoPlayerSession.mutedUntil();
		return new WorldIdentity(
				EchoPlayerSession.deviceId(),
				EchoPlayerSession.username(),
				EchoPlayerSession.mutedUntil());
	}

	/**
	 * Drops the oldest lines once a list has outgrown {@link #HISTORY_CAP}.
	 *
	 * <p>Both lists are bounded, the replay for a reason of its own on top of the
	 * scrollback's: a player who alt-tabs away in town leaves nobody watching,
	 * and every line said meanwhile would otherwise queue up to be dumped into
	 * the log all at once when they come back.
	 */
	private static void trimToCap(List<WorldChatMessage> messages) {
		while (messages.size() > HISTORY_CAP) {
			messages.remove(0);
		}
	}
}
