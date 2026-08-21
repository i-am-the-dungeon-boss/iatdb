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

package com.shatteredpixel.shatteredpixeldungeon.worldnet.websocket;

import com.shatteredpixel.shatteredpixeldungeon.worldnet.ServerMute;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldChatMessage;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldIdentity;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNetEngine;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldPresence;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.wire.WorldFrame;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.wire.WorldFrameCodec;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The push engine: one long-lived socket to {@code GET /v1/world/socket}.
 *
 * <p>The only engine the game ships. It is reached solely through
 * {@link WorldNetEngine}, so replacing it stays a decision made in one place.
 *
 * <p>All network work lives behind {@link WorldSocket}; this class is pure state
 * machine. Frames arriving on the socket's own thread are queued and applied
 * during {@link #tick(float)} on the render thread, so nothing here needs
 * {@code Game.runOnRenderThread}.
 *
 * <p>What is left here is the part that needs the sockets: dialling them, handing
 * over between them, and dispatching what arrives. The decisions that do not —
 * when to redial and rotate ({@link ReconnectPolicy}), what the server still has
 * to be told about the avatar ({@link PresencePublisher}), and which replayed
 * chat lines the player has already seen ({@link ChatBacklog}) — live beside it,
 * where they are testable without a socket at all.
 *
 * <p>Reconnecting is treated as routine rather than exceptional. A Vercel
 * function closes the socket when it reaches its maximum duration, so a healthy
 * client is expected to be dropped and to dial again periodically; the engine
 * re-publishes its avatar on every fresh connection and de-duplicates the chat
 * backlog the server replays.
 */
public final class WebSocketWorldNetEngine implements WorldNetEngine {

	/** Quiet-connection keepalive; also how the client learns a link has died silently. */
	private static final float PING_IDLE_SECONDS = 20f;
	/** No frame at all for this long means the link is gone, whatever the socket believes. */
	private static final float SILENT_TIMEOUT_SECONDS = 60f;
	/** A handshake that has not completed by now is not going to. */
	private static final float HANDSHAKE_TIMEOUT_SECONDS = 15f;
	/** How long to leave the live socket alone after a replacement failed to open. */
	private static final float STANDBY_RETRY_SECONDS = 15f;
	/** The server's error code for a send refused because the player is muted. */
	private static final String MUTED_CODE = "muted";

	private enum Signal {
		OPENED,
		TEXT,
		CLOSED
	}

	/**
	 * One socket callback, tagged with the socket it came from.
	 *
	 * <p>Tagged rather than assumed because two sockets are deliberately open at
	 * once during a handover, and a third may still be shutting down. The token
	 * says which; anything from a socket the engine has moved on from is dropped.
	 */
	private static final class Event {
		final int token;
		final Signal signal;
		final String text;

		Event(int token, Signal signal, String text) {
			this.token = token;
			this.signal = signal;
			this.text = text;
		}
	}

	private final WorldSocket.Factory factory;
	private final ConcurrentLinkedQueue<Event> inbox = new ConcurrentLinkedQueue<>();
	private final ChatBacklog backlog = new ChatBacklog();
	private final ReconnectPolicy reconnect = new ReconnectPolicy();
	private final PresencePublisher presence = new PresencePublisher();
	/** Lines typed before the handshake finished; flushed in order once it does. */
	private final List<String> pendingChat = new ArrayList<>();

	private volatile int nextToken;
	private boolean connected;
	private Listener listener;
	private WorldSocket socket;
	private boolean socketOpen;
	/** Identifies the live socket's callbacks. Negative when there is no live socket. */
	private volatile int liveToken = -1;
	/** The replacement being dialled while the live one is still carrying traffic. */
	private WorldSocket standby;
	private volatile int standbyToken = -1;
	private float standbyAge;
	private float standbyWait;

	/** This client's own id, learned from the greeting. Empty until it arrives. */
	private String localPlayerId = "";

	private boolean chatFocused;
	private final ServerMute mute = new ServerMute();
	/** What the listener was last told, so a mute is announced once rather than per tick. */
	private boolean lastMuteTold;
	private Status lastStatus = Status.DISCONNECTED;

	private float sinceSend;
	private float sinceReceive;
	private float sinceOpenAttempt;

	public WebSocketWorldNetEngine(WorldSocket.Factory factory) {
		this.factory = factory;
	}

	public static WebSocketWorldNetEngine createDefault() {
		return new WebSocketWorldNetEngine(RawWorldSocket::createDefault);
	}

	@Override
	public void connect(WorldIdentity identity, Listener newListener) {
		this.listener = newListener;
		this.connected = true;
		// The mute arrives with the identity, from the authentication response the
		// session already holds. Nothing on the socket will ever mention it again.
		this.mute.reset(identity.mutedUntil);
		this.reconnect.reset();
		this.chatFocused = false;
		this.presence.clear();
		this.localPlayerId = "";
		this.backlog.clear();
		this.pendingChat.clear();
		this.inbox.clear();
		this.lastStatus = Status.DISCONNECTED;
		setStatus(Status.CONNECTING, null);
		// Deliberately not reset alongside the mute: a reconnect whose fresh
		// identity carries no mute is exactly when the player needs telling that
		// they can speak again.
		syncMute();
		openSocket();
	}

	@Override
	public void disconnect() {
		if (!connected) {
			return;
		}
		connected = false;
		// Said before the socket goes, never after. A close on its own is
		// ambiguous — the server cannot tell a player who walked into a run from
		// one whose connection blipped, so it holds the avatar for a reconnect
		// that is not coming and the village keeps a ghost. This is the one word
		// that makes the departure deliberate.
		sayGoodbye();
		// Cleared so any frame still in flight on an abandoned socket's thread is
		// dropped rather than applied to a torn-down scene.
		liveToken = -1;
		inbox.clear();
		discardStandby();
		closeSocket();
		presence.clear();
		localPlayerId = "";
		setStatus(Status.DISCONNECTED, null);
		listener = null;
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public boolean isServerMuted() {
		return mute.isActive();
	}

	@Override
	public void applyServerMute(long mutedUntil) {
		mute.reset(mutedUntil);
		syncMute();
	}

	@Override
	public void setPresence(WorldPresence local) {
		presence.set(local);
	}

	@Override
	public void setChatFocused(boolean focused) {
		if (this.chatFocused == focused) {
			return;
		}
		this.chatFocused = focused;
		if (socketOpen) {
			sendFocus();
		}
	}

	@Override
	public void sendChat(String text) {
		if (!connected) {
			return;
		}
		// Known locally from the identity, so this costs nothing and tells the
		// player at once instead of after a round trip. The server still refuses a
		// muted send on its own account; this only saves the trip.
		if (mute.isActive()) {
			syncMute();
			return;
		}
		if (!socketOpen) {
			pendingChat.add(text);
			return;
		}
		try {
			write(WorldFrameCodec.encodeChat(text));
		} catch (JSONException impossible) {
			setStatus(Status.DEGRADED, impossible.getMessage());
		}
	}

	@Override
	public void requestEchoBundle(String echoId) {
		if (!connected || !socketOpen) {
			return;
		}
		try {
			write(WorldFrameCodec.encodeEchoReq(echoId));
		} catch (JSONException impossible) {
			setStatus(Status.DEGRADED, impossible.getMessage());
		}
	}

	@Override
	public void report(String messageId, String reason) {
		if (!connected || !socketOpen) {
			return;
		}
		try {
			write(WorldFrameCodec.encodeReport(messageId, reason));
		} catch (JSONException impossible) {
			setStatus(Status.DEGRADED, impossible.getMessage());
		}
	}

	@Override
	public void tick(float elapsed) {
		if (!connected) {
			return;
		}
		drain();
		if (!connected) {
			return;
		}

		if (socketOpen) {
			reconnect.elapse(elapsed);
		}
		tickStandby(elapsed);

		if (socket == null) {
			// A replacement is already on its way; dialling a third would be a race
			// with it, not a recovery.
			if (standby != null) {
				return;
			}
			if (reconnect.redialDue(elapsed)) {
				openSocket();
			}
			return;
		}

		if (!socketOpen) {
			sinceOpenAttempt += elapsed;
			if (sinceOpenAttempt >= HANDSHAKE_TIMEOUT_SECONDS) {
				abandonSocket("handshake timed out");
			}
			return;
		}

		// The deadline came with the identity, so a lift needs nothing from the
		// server — but nobody is watching the clock, so it has to be noticed here.
		syncMute();

		sinceSend += elapsed;
		sinceReceive += elapsed;

		if (sinceReceive >= SILENT_TIMEOUT_SECONDS) {
			abandonSocket("no traffic");
			return;
		}
		flushPresence();
		if (sinceSend >= PING_IDLE_SECONDS) {
			sendPing();
		}
	}

	@Override
	public String name() {
		return "websocket";
	}

	private void openSocket() {
		liveToken = nextToken++;
		socketOpen = false;
		sinceOpenAttempt = 0f;
		socket = dial(liveToken);
	}

	/** Dials the socket that will take over from the live one. */
	private void openStandby() {
		standbyToken = nextToken++;
		standbyAge = 0f;
		standby = dial(standbyToken);
	}

	private WorldSocket dial(final int token) {
		WorldSocket dialled = factory.create();
		dialled.open(new WorldSocket.Listener() {
			@Override
			public void onOpen() {
				inbox.add(new Event(token, Signal.OPENED, null));
			}

			@Override
			public void onText(String text) {
				inbox.add(new Event(token, Signal.TEXT, text));
			}

			@Override
			public void onClosed(String reason) {
				inbox.add(new Event(token, Signal.CLOSED, reason));
			}
		});
		return dialled;
	}

	/**
	 * Runs the replacement's clock: when to dial one, and how long to wait on it.
	 *
	 * <p>Deliberately separate from the live socket's own timers, because a
	 * replacement that never opens must not disturb a connection that is working.
	 * It is dropped, and another is tried after a pause.
	 */
	private void tickStandby(float elapsed) {
		if (standby != null) {
			standbyAge += elapsed;
			if (standbyAge >= HANDSHAKE_TIMEOUT_SECONDS) {
				discardStandby();
				standbyWait = STANDBY_RETRY_SECONDS;
			}
			return;
		}
		if (standbyWait > 0f) {
			standbyWait -= elapsed;
			return;
		}
		if (socketOpen && reconnect.rotationDue()) {
			openStandby();
		}
	}

	/**
	 * Hands the connection over: the replacement is up, so the old one can go.
	 *
	 * <p>The old socket is closed only here, after its successor is carrying
	 * traffic — that is the whole of make-before-break. The server tolerates the
	 * overlap because its roster is keyed by player, so the two connections
	 * resolve to one avatar rather than a twin.
	 */
	private void promote() {
		if (standby == null) {
			return;
		}
		if (socket != null) {
			socket.close();
		}
		socket = standby;
		liveToken = standbyToken;
		standby = null;
		standbyToken = -1;
		standbyAge = 0f;
		standbyWait = 0f;
		onOpened();
	}

	private void discardStandby() {
		if (standby != null) {
			standby.close();
			standby = null;
		}
		standbyToken = -1;
		standbyAge = 0f;
	}

	/** Drops a socket the engine has decided is dead, and schedules another. */
	private void abandonSocket(String reason) {
		closeSocket();
		noteFailure(reason);
	}

	private void closeSocket() {
		if (socket != null) {
			socket.close();
			socket = null;
		}
		socketOpen = false;
	}

	private void noteFailure(String reason) {
		reconnect.noteFailure();
		setStatus(Status.DEGRADED, reason);
	}

	private void drain() {
		Event event;
		while ((event = inbox.poll()) != null) {
			if (listener == null) {
				continue;
			}
			if (event.token == standbyToken) {
				// Nothing a replacement says before it is live is worth acting on:
				// its backlog is replayed to the promoted socket anyway.
				if (event.signal == Signal.OPENED) {
					promote();
				} else if (event.signal == Signal.CLOSED) {
					discardStandby();
					standbyWait = STANDBY_RETRY_SECONDS;
				}
				continue;
			}
			if (event.token != liveToken) {
				continue;
			}
			switch (event.signal) {
				case OPENED:
					onOpened();
					break;
				case TEXT:
					sinceReceive = 0f;
					apply(WorldFrameCodec.decode(event.text));
					break;
				case CLOSED:
					onClosed(event.text);
					break;
			}
		}
	}

	/**
	 * The server closing a socket it has held for a while is routine, not an
	 * outage: the connection lives inside a serverless invocation, and that
	 * invocation has a duration cap it will always eventually reach.
	 *
	 * <p>So a healthy socket's close is redialled at once and never reported. The
	 * alternative — the old behaviour — announced the player offline and then back
	 * again every few minutes, for a channel that was working perfectly.
	 *
	 * <p>A socket that dies before it was ever healthy is a different thing, and
	 * still backs off and reports.
	 */
	private void onClosed(String reason) {
		boolean wasHealthy = socketOpen && reconnect.wasHealthy();
		closeSocket();
		if (!wasHealthy) {
			noteFailure(reason);
			return;
		}
		reconnect.noteRoutineClose();
	}

	private void onOpened() {
		socketOpen = true;
		reconnect.noteOpened();
		sinceSend = 0f;
		sinceReceive = 0f;
		sinceOpenAttempt = 0f;
		presence.forget();
		setStatus(Status.CONNECTED, null);
		if (chatFocused) {
			sendFocus();
		}
		flushPresence();
		for (String queued : pendingChat) {
			sendChat(queued);
		}
		pendingChat.clear();
	}

	private void apply(WorldFrame frame) {
		switch (frame.kind) {
			case CHAT:
				deliverChat(frame.messages);
				break;
			case ROSTER:
				listener.onPresenceSnapshot(Collections.unmodifiableList(withoutSelf(frame.occupants)));
				break;
			case FIGURES:
				listener.onWorldFigures(Collections.unmodifiableList(frame.figures));
				break;
			case ECHO:
				listener.onEchoBundle(frame.echoId, frame.echoData);
				break;
			case ERROR:
				// A refused message says nothing about the link, so the socket stays up.
				// A mute is the one refusal worth remembering: it will refuse every
				// later send too, and the player should be told rather than left
				// typing into a wall.
				if (MUTED_CODE.equals(frame.errorCode)) {
					mute.refusedByServer();
					syncMute();
				} else {
					setStatus(Status.DEGRADED, frame.detail);
				}
				break;
			case HELLO:
				localPlayerId = frame.playerId;
				// A server too old to name a version says nothing, rather than
				// being reported as running an unknown one.
				if (!frame.versionName.isEmpty()) {
					listener.onServerVersion(frame.versionName);
				}
				break;
			case PONG:
			case UNKNOWN:
			default:
				break;
		}
	}

	/**
	 * Drops the local player from a roster that carries everyone.
	 *
	 * <p>The server broadcasts one identical roster frame to every connection on
	 * its instance so it can serialise it a single time; building a private copy
	 * per player was quadratic in the size of the village. The exclusion has to
	 * happen somewhere, and here it costs one pass over a list the client is
	 * already walking.
	 *
	 * <p>Before the greeting lands there is nothing to compare against, so the
	 * roster passes through whole rather than being guessed at.
	 */
	private List<WorldPresence> withoutSelf(List<WorldPresence> occupants) {
		if (localPlayerId.isEmpty()) {
			return occupants;
		}
		List<WorldPresence> others = new ArrayList<>(occupants.size());
		for (WorldPresence occupant : occupants) {
			if (!localPlayerId.equals(occupant.playerId)) {
				others.add(occupant);
			}
		}
		return others;
	}

	/** Tells the listener when, and only when, the answer has changed. */
	private void syncMute() {
		boolean muted = mute.isActive();
		if (muted == lastMuteTold) {
			return;
		}
		lastMuteTold = muted;
		if (listener != null) {
			listener.onServerMute(muted);
		}
	}

	private void deliverChat(List<WorldChatMessage> messages) {
		List<WorldChatMessage> fresh = backlog.fresh(messages);
		if (!fresh.isEmpty()) {
			listener.onChatMessages(Collections.unmodifiableList(fresh));
		}
	}

	private void flushPresence() {
		try {
			String frame = presence.nextFrame();
			if (frame != null) {
				write(frame);
			}
		} catch (JSONException impossible) {
			setStatus(Status.DEGRADED, impossible.getMessage());
		}
	}

	/**
	 * Retracts the avatar on the way out, if there is one the server still
	 * believes in.
	 *
	 * <p>Written straight to the socket rather than queued: the caller is about
	 * to close it, so there is no later tick to flush a pending leave on.
	 * Silent when nothing was ever published, and silent when the leave has
	 * already gone — a second one would be answered with the same emptiness.
	 */
	private void sayGoodbye() {
		if (!socketOpen || !presence.owesGoodbye()) {
			return;
		}
		try {
			write(WorldFrameCodec.encodeLeave());
		} catch (JSONException impossible) {
			// Nothing left to degrade: the channel is closing either way, and the
			// server's reconnect grace period covers what this frame would have.
			setStatus(Status.DEGRADED, impossible.getMessage());
		}
	}

	private void sendFocus() {
		try {
			write(WorldFrameCodec.encodeFocus(chatFocused));
		} catch (JSONException impossible) {
			setStatus(Status.DEGRADED, impossible.getMessage());
		}
	}

	private void sendPing() {
		try {
			write(WorldFrameCodec.encodePing());
		} catch (JSONException impossible) {
			setStatus(Status.DEGRADED, impossible.getMessage());
		}
	}

	private void write(String frame) {
		if (socket == null) {
			return;
		}
		socket.send(frame);
		sinceSend = 0f;
	}

	/** Only emitted on change, so a steady connection produces no status churn. */
	private void setStatus(Status status, String detail) {
		if (status == lastStatus) {
			return;
		}
		lastStatus = status;
		if (listener != null) {
			listener.onStatus(status, detail);
		}
	}

}
