/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
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

package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

/**
 * Whether a hero that is not the local player is being materialised right now.
 *
 * <p>Restoring a hero from a bundle collects every item it carries, and
 * collecting is how the living player earns badges, catalog entries, discovered
 * item types and talent procs. None of that is ours to earn when the hero being
 * restored belongs to somebody else — a village figure, or an
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss}'s kit.
 *
 * <p>This asks one question — <em>may this write to the player?</em> — and so it
 * carries no hero. Which hero the player happens to be looking at is a separate
 * question, answered per item by
 * {@link com.shatteredpixel.shatteredpixeldungeon.items.Item#owner()}.
 *
 * <p>Single-threaded by construction: the socket queues frames and
 * {@code WebSocketWorldNetEngine.tick} applies them on the render thread, which
 * is also where the game loop and every window live.
 */
public final class ForeignRestore {

	private static int depth;

	private ForeignRestore() {
	}

	/** Opens a foreign restore. Always paired with {@link #exit()} in a finally. */
	public static void enter() {
		depth++;
	}

	/** Closes the innermost foreign restore. */
	public static void exit() {
		if (depth == 0) {
			throw new IllegalStateException("ForeignRestore.exit() without a matching enter");
		}
		depth--;
	}

	/** True while somebody else's hero is being restored. */
	public static boolean inProgress() {
		return depth > 0;
	}

	/** Test teardown only. */
	public static void resetForTests() {
		depth = 0;
	}
}
