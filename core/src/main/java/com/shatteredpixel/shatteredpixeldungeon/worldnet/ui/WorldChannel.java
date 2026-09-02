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
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageEchoBundles;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageFigure;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageFigures;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageSession;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldChatMessage;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNet;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNetEngine;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldPresence;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds the world channel to whatever {@code GameScene} is on screen.
 *
 * <p>Chat is rendered through {@link GLog} rather than a bespoke overlay: the
 * game log already owns its layout, its scene lifecycle and its position
 * relative to the toolbar, and it exists in the village and the dungeon alike —
 * so world chat gets an ambient view in both places for no new UI. Composing
 * happens in {@code WorldChatBar}.
 *
 * <p>{@code WndWorldChat} still exists as a scrollback view but is currently
 * unreachable — the menu entry that opened it was removed in favour of the
 * chat bar, and nothing has replaced that entry point yet.
 *
 * <p>One instance is owned by the scene; it registers on create and unregisters
 * on destroy, so a stale bridge can never write into a torn-down scene.
 */
public final class WorldChannel implements WorldNet.Observer {

	private final RemotePlayers remotePlayers = new RemotePlayers();
	private WorldNetEngine.Status lastStatus = WorldNetEngine.Status.DISCONNECTED;

	public void attach() {
		WorldNet.observe(this);
		// The figures land with the greeting, several frames before this scene
		// exists to stand them in, so the village fills from what is already known
		// rather than waiting for whatever changes next. The roster for the same
		// reason, and one more: a scene rebuilt mid-visit — a window resize — took
		// the ghosts down with it, and the next snapshot is seconds away.
		onWorldFigures(WorldNet.figures());
		onWorldRoster(WorldNet.roster());
	}

	/**
	 * Gives up the scene's own things, and only those.
	 *
	 * <p>This runs whenever the scene ends, which is not the same event as the
	 * player leaving town — resizing the window ends one too. So the avatars and
	 * the labels go, because they are furniture in a scene that no longer exists,
	 * and nothing that belongs to the village itself is touched: the figures are
	 * the level's, and the outstanding bundle requests are dropped when town is
	 * actually left.
	 */
	public void detach() {
		WorldNet.unobserve(this);
		remotePlayers.clear();
		VillageFigures.dropLabels();
	}

	public RemotePlayers remotePlayers() {
		return remotePlayers;
	}

	@Override
	public void onWorldChat(WorldChatMessage message) {
		log(message);
		remotePlayers.bubble(message.playerId, message.text);
	}

	/**
	 * Whatever was said before this scene existed, written straight into the log.
	 *
	 * <p>No bubbles: these lines are already minutes old by the time anyone reads
	 * them, and hanging them over the avatars standing here now would say those
	 * players had just spoken.
	 */
	@Override
	public void onWorldChatBacklog(List<WorldChatMessage> messages) {
		for (WorldChatMessage message : messages) {
			log(message);
		}
	}

	private void log(WorldChatMessage message) {
		GLog.h(Messages.get(this, "line", message.name, message.text));
		GLog.newLine();
	}

	@Override
	public void onLocalChat(String text) {
		if (Dungeon.hero == null || Dungeon.hero.sprite == null) {
			return;
		}
		ChatBubble.show(Dungeon.hero.sprite, text);
	}

	@Override
	public void onWorldRoster(List<WorldPresence> occupants) {
		// Ghosts belong to the village only; a run must never inherit them.
		if (!VillageSession.inVillage()) {
			remotePlayers.clear();
			return;
		}
		remotePlayers.apply(occupants);
	}

	/**
	 * Stands the pushed bodies in the town square.
	 *
	 * <p>Dropped rather than queued when the player is not in town, for the same
	 * reason ghosts are: a run must not inherit the village, and the whole set is
	 * re-sent on the next greeting anyway.
	 */
	@Override
	public void onWorldFigures(List<VillageFigure> figures) {
		if (!VillageSession.inVillage()) {
			VillageFigures.dropLabels();
			VillageEchoBundles.clear();
			return;
		}
		VillageFigures.apply(villageLevel(), new ArrayList<>(figures));
	}

	private VillageLevel villageLevel() {
		return Dungeon.level instanceof VillageLevel ? (VillageLevel) Dungeon.level : null;
	}

	@Override
	public void onEchoBundle(String echoId, String echoData) {
		VillageEchoBundles.deliver(echoId, echoData);
	}

	@Override
	public void onWorldStatus(WorldNetEngine.Status status) {
		if (status == lastStatus) {
			return;
		}
		// Only announce transitions — a degraded channel retries on every tick
		// and would otherwise spam the log once per poll.
		if (status == WorldNetEngine.Status.DEGRADED
				&& lastStatus == WorldNetEngine.Status.CONNECTED) {
			GLog.w(Messages.get(this, "offline"));
		} else if (status == WorldNetEngine.Status.CONNECTED
				&& lastStatus == WorldNetEngine.Status.DEGRADED) {
			GLog.p(Messages.get(this, "reconnected"));
		}
		lastStatus = status;
	}

	@Override
	public void onServerMute(boolean muted) {
		// Both directions: a mute lifts on a deadline the player was never shown,
		// so without this they would have no way of knowing they may speak again
		// short of trying.
		if (muted) {
			GLog.w(Messages.get(this, "muted"));
		} else {
			GLog.p(Messages.get(this, "unmuted"));
		}
	}
}
