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
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.StartScene;

import java.io.IOException;

/**
 * The seam between the ground level and a dungeon run.
 *
 * <p>The village is a real level, but it is deliberately <em>not</em> part of
 * any run: it uses a save slot of its own under the solo namespace, and is
 * rebuilt on every entry rather than resumed. {@code GamesInProgress.gameFolder}
 * is keyed by play mode and the mode is not known until the dungeon mouth. Runs
 * therefore begin the way they always have — mode first, then hero select, then
 * depth 1 — with the mode chosen in the world rather than on the title screen.
 */
public final class VillageGateway {

	/**
	 * The village's own save slot, past the run slots so it can never collide
	 * with one ({@code GamesInProgress.firstEmpty} only scans 1..MAX_SLOTS).
	 */
	public static final int VILLAGE_SLOT = GamesInProgress.MAX_SLOTS + 1;

	private VillageGateway() {
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

	/** True when village save data is sitting on disk. */
	public static boolean villageExists() {
		return GamesInProgress.gameExists(VILLAGE_SLOT);
	}

	/**
	 * Throws away any stored ground level.
	 *
	 * <p>Saved levels are restored from their bundle, which carries its own
	 * width and height — so a village written by an older build keeps that
	 * build's map forever, no matter what the level code now says. The ground
	 * level holds nothing worth that risk, so it is never resumed from disk.
	 */
	public static void discardStoredVillage() {
		if (villageExists()) {
			Dungeon.deleteGame(VILLAGE_SLOT, true);
		}
		GamesInProgress.delete(VILLAGE_SLOT);
	}

	/**
	 * Enter the ground level from the title screen.
	 *
	 * <p>Always rebuilt, never restored. The village is hand-authored and
	 * carries no progress — no experience, no inventory that survives, no
	 * quest state — so regenerating it costs the player nothing and keeps the
	 * map in step with the code that draws it. Anything the house gains later
	 * (a stash, cosmetics) must therefore be stored on its own, not left to
	 * level persistence.
	 */
	public static void enterVillage() {
		prepareVillageEntry();
		ShatteredPixelDungeon.switchScene(InterlevelScene.class);
	}

	/**
	 * Everything {@link #enterVillage()} changes before the scene switch, split
	 * out so the state it sets can be asserted directly.
	 *
	 * <p>The village never runs {@code Dungeon.init()} — that is run setup, and
	 * town is not a run. {@code InterlevelScene.Mode.VILLAGE} routes the loading
	 * thread to {@link VillageSession#enter()} instead.
	 */
	public static void prepareVillageEntry() {
		GamesInProgress.selectEchoPlayMode(villageStorageMode());
		GamesInProgress.curSlot = VILLAGE_SLOT;
		discardStoredVillage();

		GamesInProgress.selectedClass = villageHeroClass();
		Dungeon.hero = null;
		Dungeon.level = null;
		Dungeon.initSeed();
		InterlevelScene.mode = InterlevelScene.Mode.VILLAGE;
	}

	/**
	 * Climb out of depth 1 and walk home.
	 *
	 * <p>The run is not abandoned: it is saved to its own slot first, exactly as
	 * quitting to the title screen would, and can be resumed from the dungeon
	 * mouth later. What is left behind is the dungeon hero — the player arrives
	 * in town as a {@link VillageHero}, carrying nothing out of the dungeon.
	 *
	 * <p>Order matters: the run must be written while {@code gameFolder} still
	 * points at its play mode, before entering the village repoints it at solo.
	 *
	 * @return true once the walk home has been committed
	 */
	public static boolean returnToVillage() {
		if (!prepareReturnToVillage()) {
			return false;
		}
		ShatteredPixelDungeon.switchScene(InterlevelScene.class);
		return true;
	}

	/**
	 * Everything {@link #returnToVillage()} changes before the scene switch.
	 * Returns false if the run could not be written, in which case nothing has
	 * moved and the player stays where they are — a walk home is never worth
	 * losing a floor over.
	 */
	public static boolean prepareReturnToVillage() {
		try {
			Dungeon.saveAll();
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException(e);
			return false;
		}

		// the town avatar wears the class the player has been playing
		if (Dungeon.hero != null) {
			GamesInProgress.selectedClass = Dungeon.hero.heroClass;
		}
		Mob.clearHeldAllies();

		prepareVillageEntry();
		return true;
	}

	/**
	 * Answer to the dungeon-mouth prompt: commit to a play mode and hand off to
	 * the ordinary run-start flow.
	 *
	 * <p>Order matters. The stored ground level is dropped <em>before</em> the
	 * mode is selected, because selecting it repoints
	 * {@code GamesInProgress.gameFolder} at that mode's folder, and the village
	 * lives in the solo one.
	 */
	public static void beginRun(EchoPlayMode mode) {
		// the town avatar, its level and its actors are dropped here; the run
		// builds a hero of its own in Dungeon.init()
		VillageSession.leave();

		discardStoredVillage();
		Mob.clearHeldAllies();
		Dungeon.hero = null;
		Dungeon.level = null;

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

}
