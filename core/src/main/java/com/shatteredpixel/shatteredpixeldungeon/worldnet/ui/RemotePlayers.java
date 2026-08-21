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

package com.shatteredpixel.shatteredpixeldungeon.worldnet.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldPresence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps the village's remote avatars in step with the latest roster snapshot.
 *
 * <p>Snapshots are absolute, not deltas, so reconciliation is a three-way diff:
 * ids that are new get a sprite, ids that vanished lose theirs, and ids that
 * stayed are moved if their cell changed.
 */
public final class RemotePlayers {

	/** What one snapshot implies, separated from the sprite work so it can be tested. */
	public static final class Diff {
		public final List<String> added = new ArrayList<>();
		public final List<String> moved = new ArrayList<>();
		public final List<String> removed = new ArrayList<>();
	}

	/**
	 * Which indicators a snapshot implies raising and taking down.
	 *
	 * <p>Kept apart from {@link Diff} because it answers a different question:
	 * that one is about which sprites exist, this one is about what is written
	 * over the sprites that already do.
	 */
	public static final class TypingChanges {
		public final List<String> started = new ArrayList<>();
		public final List<String> stopped = new ArrayList<>();
	}

	private final Map<String, RemotePlayerSprite> sprites = new LinkedHashMap<>();
	private final Map<String, NameTag> tags = new LinkedHashMap<>();
	private final Map<String, TypingTag> typing = new LinkedHashMap<>();
	private final Map<String, Integer> cells = new HashMap<>();

	/**
	 * Pure diff between what is on screen and what the server just reported.
	 * A player whose cell is unchanged appears in none of the three lists.
	 */
	public static Diff diff(Map<String, Integer> currentCells, List<WorldPresence> snapshot) {
		Diff diff = new Diff();
		Map<String, Integer> seen = new HashMap<>();

		for (WorldPresence presence : snapshot) {
			seen.put(presence.playerId, presence.cell);
			Integer knownCell = currentCells.get(presence.playerId);
			if (knownCell == null) {
				diff.added.add(presence.playerId);
			} else if (knownCell.intValue() != presence.cell) {
				// intValue() on purpose: cells run past 127, so reference
				// comparison would fall outside Integer's cache and report a
				// spurious move on every sync.
				diff.moved.add(presence.playerId);
			}
		}

		for (String existing : currentCells.keySet()) {
			if (!seen.containsKey(existing)) {
				diff.removed.add(existing);
			}
		}
		return diff;
	}

	/**
	 * Pure diff between the indicators on screen and the ones the snapshot asks
	 * for. A player who has left is in neither list — {@link #despawn} takes
	 * their tag with the rest of their avatar.
	 */
	public static TypingChanges typingChanges(
			Set<String> currentlyShown, List<WorldPresence> snapshot) {
		TypingChanges changes = new TypingChanges();
		for (WorldPresence presence : snapshot) {
			boolean showing = currentlyShown.contains(presence.playerId);
			if (presence.typing && !showing) {
				changes.started.add(presence.playerId);
			} else if (!presence.typing && showing) {
				changes.stopped.add(presence.playerId);
			}
		}
		return changes;
	}

	public void apply(List<WorldPresence> snapshot) {
		Diff diff = diff(cells, snapshot);

		for (String gone : diff.removed) {
			despawn(gone);
		}

		for (WorldPresence presence : snapshot) {
			if (diff.added.contains(presence.playerId)) {
				spawn(presence);
			} else if (diff.moved.contains(presence.playerId)) {
				RemotePlayerSprite sprite = sprites.get(presence.playerId);
				Integer from = cells.get(presence.playerId);
				if (sprite != null && from != null) {
					sprite.move(from, presence.cell);
				}
				cells.put(presence.playerId, presence.cell);
			}
		}

		// After the spawns, so an arrival who is already mid-sentence has a
		// sprite to hang the indicator on.
		TypingChanges changes = typingChanges(typing.keySet(), snapshot);
		for (String id : changes.stopped) {
			stopTyping(id);
		}
		for (String id : changes.started) {
			startTyping(id);
		}
	}

	private void startTyping(String playerId) {
		RemotePlayerSprite sprite = sprites.get(playerId);
		if (sprite == null || typing.containsKey(playerId)) {
			return;
		}
		TypingTag tag = new TypingTag(sprite);
		GameScene.addToMobLayer(tag);
		typing.put(playerId, tag);
	}

	private void stopTyping(String playerId) {
		TypingTag tag = typing.remove(playerId);
		if (tag != null) {
			GameScene.removeFromMobLayer(tag);
			tag.killAndErase();
		}
	}

	/** Shows a chat line over its speaker, when that speaker is standing here. */
	public void bubble(String playerId, String text) {
		RemotePlayerSprite sprite = sprites.get(playerId);
		if (sprite == null) {
			return;
		}
		// The line they were composing has arrived, so the ellipsis has nothing
		// left to promise. The server clears the flag too, but the chat frame
		// beats the next roster, and the two overlapping would read as a stutter.
		stopTyping(playerId);
		ChatBubble.show(sprite, text);
	}

	public void clear() {
		List<String> ids = new ArrayList<>(sprites.keySet());
		for (String id : ids) {
			despawn(id);
		}
		cells.clear();
	}

	public boolean isShowing(String playerId) {
		return sprites.containsKey(playerId);
	}

	private void spawn(WorldPresence presence) {
		// The avatar no longer depends on the reported class, so a newer client
		// playing a class this build has never heard of still shows up rather than
		// silently vanishing from the village.
		RemotePlayerSprite sprite = new RemotePlayerSprite();
		GameScene.addToMobLayer(sprite);
		sprite.place(presence.cell);

		NameTag tag = new NameTag(sprite, presence.displayName);
		GameScene.addToMobLayer(tag);

		sprites.put(presence.playerId, sprite);
		tags.put(presence.playerId, tag);
		cells.put(presence.playerId, presence.cell);
	}

	private void despawn(String playerId) {
		stopTyping(playerId);
		RemotePlayerSprite sprite = sprites.remove(playerId);
		if (sprite != null) {
			GameScene.removeFromMobLayer(sprite);
			sprite.killAndErase();
		}
		NameTag tag = tags.remove(playerId);
		if (tag != null) {
			GameScene.removeFromMobLayer(tag);
			tag.killAndErase();
		}
		cells.remove(playerId);
	}

	/** True while the village is the current level — ghosts must never follow a run. */
	public static boolean shouldRender() {
		return Dungeon.level != null && Dungeon.depth == 0;
	}
}
