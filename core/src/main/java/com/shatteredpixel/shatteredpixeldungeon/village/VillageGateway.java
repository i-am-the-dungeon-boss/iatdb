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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.StartScene;

import java.io.IOException;

/**
 * The seam between the ground level and a dungeon run.
 *
 * <p>The village is a real level, but it is deliberately <em>not</em> part of
 * any run: it lives in its own save slot and is always stored under the solo
 * namespace, because {@code GamesInProgress.gameFolder} is keyed by play mode
 * and the mode is not known until the player reaches the dungeon mouth. Runs
 * therefore begin the way they always have — mode first, then hero select, then
 * depth 1 — with the mode chosen in the world rather than on the title screen.
 */
public final class VillageGateway {

	/**
	 * The village's own save slot, past the run slots so it can never collide
	 * with one ({@code GamesInProgress.firstEmpty} only scans 1..MAX_SLOTS).
	 */
	public static final int VILLAGE_SLOT = GamesInProgress.MAX_SLOTS + 1;

	/**
	 * Set while a village level is being generated, so {@code Dungeon.init}
	 * starts on depth 0 instead of depth 1. Transient by design — it never
	 * belongs in a save.
	 */
	private static boolean startingInVillage = false;

	private VillageGateway() {
	}

	public static boolean startingInVillage() {
		return startingInVillage;
	}

	public static void clearStartingInVillage() {
		startingInVillage = false;
	}

	/** The village is a solo, private place; its saves live in the solo folder. */
	public static EchoPlayMode villageStorageMode() {
		return EchoPlayMode.SOLO;
	}

	/**
	 * Which hero class the town avatar wears. Reuses the last class the player
	 * picked so the villager on screen matches who they have been playing.
	 */
	public static HeroClass villageHeroClass() {
		HeroClass selected = GamesInProgress.selectedClass;
		return selected != null ? selected : HeroClass.WARRIOR;
	}

	/** True when an existing village save should be resumed rather than rebuilt. */
	public static boolean villageExists() {
		return GamesInProgress.gameExists(VILLAGE_SLOT);
	}

	/**
	 * Enter the ground level from the title screen: resume the saved village if
	 * there is one, otherwise build a fresh one on depth 0.
	 */
	public static void enterVillage() {
		GamesInProgress.selectEchoPlayMode(villageStorageMode());
		GamesInProgress.curSlot = VILLAGE_SLOT;

		if (villageExists()) {
			startingInVillage = false;
			InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
		} else {
			GamesInProgress.selectedClass = villageHeroClass();
			Dungeon.hero = null;
			Dungeon.initSeed();
			startingInVillage = true;
			InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		}

		ShatteredPixelDungeon.switchScene(InterlevelScene.class);
	}

	/**
	 * Answer to the dungeon-mouth prompt: commit to a play mode and hand off to
	 * the ordinary run-start flow.
	 *
	 * <p>Order matters. The village must be saved <em>before</em> the mode is
	 * selected, because selecting it repoints {@code GamesInProgress.gameFolder}
	 * at that mode's folder and the village belongs in the solo one.
	 */
	public static void beginRun(EchoPlayMode mode) {
		saveVillage();

		Mob.clearHeldAllies();
		Dungeon.hero = null;
		startingInVillage = false;

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
	 * Slot a fresh run starts in. Mirrors the title screen's old behaviour of
	 * dropping straight into slot 1 when nothing is saved.
	 */
	public static int runSlot() {
		int empty = GamesInProgress.firstEmpty();
		return empty == -1 ? 1 : empty;
	}

	/** Persists the village so it is unchanged when the player comes back. */
	public static void saveVillage() {
		if (Dungeon.hero == null || !VillageLevel.isGroundLevel(Dungeon.depth)) {
			return;
		}
		try {
			Dungeon.saveAll();
		} catch (IOException e) {
			// a village that fails to save is not worth losing the run over
			ShatteredPixelDungeon.reportException(e);
		}
	}
}
