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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoBackendProbe;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPlayerAuthGate;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageGateway;

/**
 * The prompt at the dungeon mouth: the moment a run commits to solo or ranked.
 *
 * <p>Ranked play needs the Hero Echoes backend and an authenticated player, so
 * the ranked option is disabled — with the reason shown — whenever the backend
 * is unreachable. Declining simply leaves the player standing in the village.
 */
public class WndDungeonMode extends WndOptions {

	public static final int SOLO = 0;
	public static final int RANKED = 1;
	public static final int CANCEL = 2;

	public WndDungeonMode() {
		super(Messages.get(WndDungeonMode.class, "title"),
				body(),
				Messages.get(WndDungeonMode.class, "solo"),
				Messages.get(WndDungeonMode.class, "ranked"),
				Messages.get(WndDungeonMode.class, "cancel"));
	}

	private static String body() {
		String body = Messages.get(WndDungeonMode.class, "body");
		if (!rankedAvailable()) {
			body += "\n\n" + Messages.get(WndDungeonMode.class, "ranked_offline");
		}
		return body;
	}

	/** Ranked runs are only offered when the backend can actually be reached. */
	public static boolean rankedAvailable() {
		return EchoBackendProbe.isOnlineReady();
	}

	/** Pure mapping from option index to play mode; null means "stay in the village". */
	public static EchoPlayMode modeForOption(int index) {
		switch (index) {
			case SOLO:
				return EchoPlayMode.SOLO;
			case RANKED:
				return EchoPlayMode.RANKED;
			default:
				return null;
		}
	}

	@Override
	protected boolean enabled(int index) {
		if (index == RANKED) {
			return rankedAvailable();
		}
		return super.enabled(index);
	}

	@Override
	protected void onSelect(int index) {
		EchoPlayMode mode = modeForOption(index);
		if (mode == null) {
			return;
		}
		if (mode == EchoPlayMode.RANKED) {
			EchoPlayerAuthGate.ensureReadyThen(() -> VillageGateway.beginRun(EchoPlayMode.RANKED));
		} else {
			VillageGateway.beginRun(mode);
		}
	}
}
