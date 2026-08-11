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
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.ui.GameLog;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import java.io.IOException;
import java.util.HashSet;

/**
 * Village setup, deliberately not run setup.
 *
 * <p>{@code Dungeon.init()} is <em>run</em> initialization: it resets quests,
 * badges, statistics and the item-appearance tables, and hands the hero its
 * starting gear. Walking into town must do none of that — a run begins at the
 * dungeon mouth, not at the village gate. {@link #prepareGlobals()} is the
 * audited subset of {@code init()} the town actually needs.
 */
public final class VillageSession {

	private VillageSession() {
	}

	/**
	 * True while the player is in town.
	 *
	 * <p>Keyed on the avatar rather than on {@code Dungeon.depth}: depth is
	 * mutated by the loading thread while the render thread paints, so a depth
	 * check is transiently wrong mid-transition. This also covers the house
	 * (a branch of depth 0) for free.
	 */
	public static boolean inVillage() {
		return Dungeon.hero instanceof VillageHero;
	}

	/**
	 * The parts of {@code Dungeon.init()} the village needs, and no more.
	 *
	 * <p>Skipped on purpose: the quest resets, {@code Badges.reset()},
	 * {@code Generator.fullReset()}, the special/secret room tables,
	 * {@code LimitedDrops}, the debug start, and above all
	 * {@code HeroClass.initHero} — the starting-gear grant. Nothing the player
	 * has done in the dungeon is disturbed by walking into town.
	 */
	public static void prepareGlobals() {
		Dungeon.initialVersion = Dungeon.version = Game.versionCode;

		// neutral literals, not settings: challenges, easy mode and daily are
		// properties of a run, and the village is not one
		Dungeon.challenges = 0;
		Dungeon.easyMode = false;
		Dungeon.daily = false;
		Dungeon.dailyReplay = false;
		Dungeon.mobsToChampion = 1;

		// a previous run's actors would otherwise keep acting in town
		Actor.clear();
		Actor.resetNextID();

		// static appearance tables have no other initialiser; a fresh install
		// that boots into town would NPE on them
		Scroll.initLabels();
		Potion.initColors();
		Ring.initGems();

		Random.resetGenerators();

		Statistics.reset();
		Notes.reset();

		Dungeon.quickslot.reset();
		QuickSlotButton.reset();
		Toolbar.swappedQuickslots = false;

		Dungeon.depth = VillageLevel.VILLAGE_DEPTH;
		Dungeon.branch = 0;
		Dungeon.generatedLevels.clear();

		Dungeon.gold = 0;
		Dungeon.energy = 0;

		Dungeon.droppedItems = new SparseArray<>();
		Dungeon.chapters = new HashSet<>();

		// drop stale item acting state left over from a previous run
		Item.clearCurrent();

		Dungeon.hero = new VillageHero(VillageGateway.villageHeroClass());
	}

	/**
	 * Tears the village down on the way to the dungeon.
	 *
	 * <p>The town avatar and the run hero share nothing: the run builds its own
	 * {@code Hero} in {@code Dungeon.init()}, and everything the village put on
	 * the global state — the avatar itself, its level, its actors, its
	 * quickslots, its save — is dropped here first. Nothing carries down the
	 * stairs; a run always starts from hero select.
	 */
	public static void leave() {
		if (!inVillage()) {
			return;
		}

		Mob.clearHeldAllies();
		Actor.clear();
		Actor.resetNextID();

		Dungeon.hero = null;
		Dungeon.level = null;

		Dungeon.quickslot.reset();
		QuickSlotButton.reset();
		Item.clearCurrent();

		VillageGateway.discardStoredVillage();
	}

	/**
	 * Builds the ground level and drops the town avatar onto it. Runs on the
	 * {@code InterlevelScene} loading thread, like every other level switch.
	 */
	public static void enter() throws IOException {
		Mob.clearHeldAllies();
		GameLog.wipe();

		prepareGlobals();

		Level level = Dungeon.newLevel();
		// named explicitly: the village has no entrance transition to fall back
		// on, because it has no way up
		int arrival = level instanceof VillageLevel ? ((VillageLevel) level).arrivalCell() : -1;
		Dungeon.switchLevel(level, arrival);
	}

}
