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

import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldChatMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Which of the messages the server just sent are ones the player has not seen.
 *
 * <p>Every reconnect replays the recent backlog, so a client that missed
 * messages while dialling gets them. Reconnects are routine — the socket lives
 * inside a serverless invocation and is rotated every few minutes — which makes
 * this the difference between a chat log and a chat log that repeats itself
 * every few minutes.
 *
 * <p>Remembers ids rather than messages, and only the most recent of those, so
 * a session that runs for hours costs a bounded amount of memory.
 */
final class ChatBacklog {

	/** Enough ids to cover the server's replay buffer several times over. */
	private static final int DELIVERED_ID_CAP = 200;

	private final Set<String> deliveredIds = new LinkedHashSet<>();

	void clear() {
		deliveredIds.clear();
	}

	/** The messages worth showing, in the order they arrived; empty when all are repeats. */
	List<WorldChatMessage> fresh(List<WorldChatMessage> messages) {
		List<WorldChatMessage> unseen = new ArrayList<>();
		for (WorldChatMessage message : messages) {
			if (deliveredIds.add(message.id)) {
				unseen.add(message);
			}
		}
		trim();
		return unseen;
	}

	/** Iterator removal rather than {@code removeIf} — see cross-platform.mdc. */
	private void trim() {
		if (deliveredIds.size() <= DELIVERED_ID_CAP) {
			return;
		}
		int excess = deliveredIds.size() - DELIVERED_ID_CAP;
		Iterator<String> oldest = deliveredIds.iterator();
		while (oldest.hasNext() && excess > 0) {
			oldest.next();
			oldest.remove();
			excess--;
		}
	}
}
