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

package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.StartScene;

/**
 * The one-way door from the village into a dungeon run.
 *
 * <p>The village and the dungeon share nothing. While the player is in the
 * village there is no dungeon, no hero and no run save: the dungeon is created
 * here, when they answer the prompt at the dungeon entrance, and the run that
 * follows is exactly the run the game has always started.
 */
public final class VillageGateway {

	private VillageGateway() {
	}

	/** Opens the ground level. */
	public static void enterVillage() {
		VillageSave.load();
		ShatteredPixelDungeon.switchScene(VillageScene.class);
	}

	/**
	 * Commits to a play mode and starts a run.
	 *
	 * <p>The village is saved first and then left alone. Selecting the mode is
	 * what decides the run's save namespace ({@code GamesInProgress.gameFolder}
	 * is keyed by play mode), which is why it cannot happen any earlier than
	 * this — and why nothing about the village may live in a run folder.
	 */
	public static void beginRun(EchoPlayMode mode) {
		VillageSave.save();

		GamesInProgress.selectEchoPlayMode(mode);
		GamesInProgress.selectedClass = null;
		GamesInProgress.curSlot = runSlot();

		if (GamesInProgress.checkAll().isEmpty()) {
			ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
		} else {
			ShatteredPixelDungeon.switchNoFade(StartScene.class);
		}
	}

	/**
	 * Slot a fresh run starts in, mirroring the title screen's old behaviour of
	 * dropping straight into slot 1 when nothing is saved.
	 */
	public static int runSlot() {
		int empty = GamesInProgress.firstEmpty();
		return empty == -1 ? 1 : empty;
	}
}
