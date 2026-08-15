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

import com.shatteredpixel.shatteredpixeldungeon.ProjectLinks;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.AvailableUpdateData;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNet;

/**
 * Holds the forced-update gate until the player is somewhere it can be shown.
 *
 * <p>The world socket announces a new build the moment a client reconnects onto
 * a fresh deployment, which happens every few minutes and can land on a player
 * forty floors down. The update window cannot be dismissed, so showing it there
 * would strand them and cost the run. The village is the one place where being
 * sent to the store costs nothing, and an out-of-date client mid-run is not
 * talking to anything the new server would break anyway.
 *
 * <p>Everything needed is already to hand: the version came down the socket, and
 * the release page is {@link ProjectLinks#LATEST_RELEASE_URL}. So this asks the
 * update service nothing — no second request, and no dependency on a platform
 * that may have no update service at all.
 *
 * <p>Decides only *whether* and *what*; the scene puts the window on screen,
 * because that is the part that needs a scene. Which is also what makes the
 * decision testable on its own.
 */
public final class VillageUpdateGate {

	/** One gate per visit — {@link #due(boolean)} is asked on every frame. */
	private boolean shown;

	/**
	 * The update to gate on right now, or null when there is nothing to show.
	 *
	 * <p>Answers non-null at most once per scene, so a caller on the frame clock
	 * does not have to remember whether it already asked.
	 */
	public AvailableUpdateData due(boolean inVillage) {
		if (!shouldGate(inVillage, WorldNet.updateRequired(), shown)) {
			return null;
		}
		shown = true;
		AvailableUpdateData update = new AvailableUpdateData();
		update.versionName = WorldNet.serverVersion();
		update.URL = ProjectLinks.LATEST_RELEASE_URL;
		return update;
	}

	/** True when the gate should go up now. Shown once per visit, not per frame. */
	public static boolean shouldGate(
			boolean inVillage, boolean serverWantsNewerBuild, boolean alreadyShown) {
		return inVillage && serverWantsNewerBuild && !alreadyShown;
	}
}
