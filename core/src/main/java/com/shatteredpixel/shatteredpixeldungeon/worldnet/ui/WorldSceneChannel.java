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
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoBackendProbe;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageSession;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNet;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Group;

/**
 * The world channel as one scene sees it: whether to join, and the two pieces of
 * UI that exist only while it is connected.
 *
 * <p>Split out of {@code GameScene}, which had accumulated the join decision,
 * the observer bridge, the chat bar's lifetime and the hero's avatar updates
 * alongside everything else a game scene does. None of them are about the scene:
 * they are about the channel, and they are wrong in the same way if any one of
 * them is forgotten — a bridge left attached writes into a torn-down scene, a
 * bar left added draws over the next one.
 *
 * <p>The scene keeps what genuinely needs it: adding and removing the bar's
 * group, laying it out, and sorting the remote sprites into the mob list.
 */
public final class WorldSceneChannel {

	private WorldChannel channel;
	private WorldChatBar bar;

	/**
	 * Joins the channel for whatever the scene is showing.
	 *
	 * <p>The village always joins — being seen and being able to talk is the
	 * point of it. A run joins only when the player opted chat into runs;
	 * otherwise the channel is dropped so a dive costs no network at all.
	 */
	public void enter() {
		if (!EchoBackendProbe.isOnlineReady()) {
			return;
		}
		if (VillageSession.inVillage()) {
			WorldNet.enterVillage(Dungeon.hero.heroClass.name(), Dungeon.hero.pos, heroFacing());
		} else {
			WorldNet.enterRun();
		}
	}

	/**
	 * Tells the village where the hero is standing.
	 *
	 * <p>Cheap: this only stores a field. The engine coalesces it onto its own
	 * schedule rather than sending a frame per step.
	 */
	public void publishHeroPosition() {
		if (!VillageSession.inVillage()) {
			return;
		}
		WorldNet.updatePresence(Dungeon.hero.pos, heroFacing());
	}

	/**
	 * Brings the chat UI in line with the connection state, either way.
	 *
	 * <p>Idempotent so that toggling world chat in settings mid-run takes effect
	 * immediately, rather than only on the next level.
	 *
	 * @return true when the UI appeared or went away, so the scene knows to lay
	 *         its tags out again — the bar takes room the game log otherwise has
	 */
	public boolean sync(Group scene, Camera uiCamera) {
		boolean connected = WorldNet.isConnected();
		if (connected && channel == null) {
			channel = new WorldChannel();
			channel.attach();

			bar = new WorldChatBar();
			bar.camera = uiCamera;
			scene.add(bar);
			return true;
		}
		if (!connected && channel != null) {
			channel.detach();
			channel = null;
			if (bar != null) {
				scene.remove(bar);
				bar.destroy();
				bar = null;
			}
			return true;
		}
		return false;
	}

	/**
	 * Drops the bridge but leaves the channel itself up.
	 *
	 * <p>Descending is a scene teardown too, and chat is meant to survive it.
	 */
	public void detach() {
		if (channel != null) {
			channel.detach();
			channel = null;
		}
	}

	/** The chat bar while one is on screen, for the scene to lay out. Null otherwise. */
	public WorldChatBar bar() {
		return bar;
	}

	private static int heroFacing() {
		return Dungeon.hero.sprite != null && Dungeon.hero.sprite.flipHorizontal ? -1 : 1;
	}
}
