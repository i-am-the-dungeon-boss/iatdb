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

import com.watabou.utils.Random;

/**
 * When to redial, when to rotate, and whether a close was worth reporting.
 *
 * <p>Every one of these is a decision about elapsed time, and none of them needs
 * a socket to make. Split out of the engine because it was the engine's only
 * arithmetic: six floats and a streak counter, interleaved with the socket
 * handling that surrounds them, and reachable in a test only by driving a fake
 * socket through several minutes of ticks.
 *
 * <p>Holds no socket and opens nothing. The engine asks it what is due and does
 * the dialling itself.
 */
final class ReconnectPolicy {

	/**
	 * How long a socket must have carried traffic before its closing counts as the
	 * server rotating a healthy connection rather than something being wrong.
	 *
	 * <p>Guards against a hot loop: a server that accepted and immediately dropped
	 * every socket would otherwise be redialled with no backoff, forever.
	 */
	private static final float HEALTHY_AFTER_SECONDS = 5f;
	/**
	 * When to start dialling the socket that will replace this one.
	 *
	 * <p>Make-before-break. The server closes a connection once its invocation
	 * nears the duration cap, and waiting for that leaves a gap — short, but a gap
	 * — in which nothing arrives. Opening the replacement first and handing over
	 * only when it is up means there is no moment without a live socket.
	 *
	 * <p>Must stay below the server's own minimum lifetime, or the server closes
	 * first and this never gets the chance to run.
	 */
	private static final float ROTATE_AFTER_SECONDS = 215f;
	/**
	 * Spread added to the rotation deadline, drawn once per socket.
	 *
	 * <p>In ordinary play this changes nothing: players enter the village at
	 * their own moments, so their rotations are already scattered. It matters
	 * after an event that drops many sockets at once — an instance reaped, a zone
	 * failing over — because a fixed interval would then hold those clients in
	 * lockstep permanently, with nothing to ever separate them again. Since a
	 * replacement overlaps the socket it replaces, such a group would present the
	 * platform with a periodic burst of twice its number in connections, which is
	 * the sort of thing that gets a village split across two instances for no
	 * reason. A per-socket draw lets the grouping decay instead.
	 *
	 * <p>Bounded so the sum stays under the server's minimum lifetime: the client
	 * must always be the one that moves first. The five seconds left between the
	 * two are the replacement's handshake budget — miss it and the server closes
	 * the live socket first, which costs a short gap rather than anything worse.
	 */
	private static final float ROTATE_JITTER_SECONDS = 20f;
	private static final float[] BACKOFF_SECONDS = { 1f, 2f, 5f, 15f, 30f };

	private int failureStreak;
	private float reconnectWait;
	/** How long the current socket has been open, for judging whether it was healthy. */
	private float socketUptime;
	/** This socket's own rotation deadline, drawn when it opened. */
	private float rotateAfter = ROTATE_AFTER_SECONDS;

	/** Starts a fresh session: nothing has failed yet, so nothing is owed a wait. */
	void reset() {
		failureStreak = 0;
		reconnectWait = 0f;
	}

	/**
	 * A socket came up.
	 *
	 * <p>The deadline is drawn per socket rather than per client, so a group that
	 * was dropped together comes apart a little further on every rotation.
	 */
	void noteOpened() {
		socketUptime = 0f;
		failureStreak = 0;
		rotateAfter = rotationDeadline(Random.Float());
	}

	void elapse(float elapsed) {
		socketUptime += elapsed;
	}

	/**
	 * Whether the socket has been up long enough that the server closing it reads
	 * as routine rotation rather than as an outage.
	 */
	boolean wasHealthy() {
		return socketUptime >= HEALTHY_AFTER_SECONDS;
	}

	/** A healthy socket closed: redial at once, and hold nothing against it. */
	void noteRoutineClose() {
		failureStreak = 0;
		reconnectWait = 0f;
	}

	/** Something went wrong: back off further the longer it keeps going wrong. */
	void noteFailure() {
		failureStreak++;
		int index = Math.min(failureStreak - 1, BACKOFF_SECONDS.length - 1);
		reconnectWait = BACKOFF_SECONDS[index];
	}

	/** Counts down the backoff, answering true on the tick the redial is due. */
	boolean redialDue(float elapsed) {
		reconnectWait -= elapsed;
		return reconnectWait <= 0f;
	}

	/** Whether this socket has lived long enough to start dialling its replacement. */
	boolean rotationDue() {
		return socketUptime >= rotateAfter;
	}

	/**
	 * Where this socket's rotation falls, given a draw in {@code [0, 1)}.
	 *
	 * <p>Separated from the draw itself so the window can be stated as a fact
	 * rather than sampled: the floor is what keeps a healthy socket from being
	 * cut short, and the ceiling is what keeps the client ahead of the server.
	 */
	static float rotationDeadline(float roll) {
		return ROTATE_AFTER_SECONDS + roll * ROTATE_JITTER_SECONDS;
	}
}
