package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.watabou.utils.Bundle;

/** Captures and restores hero equipment for echo snapshots. */
public final class EchoHeroSnapshot {

	static final String WEAPON = "weapon";
	static final String ARMOR = "armor";
	static final String ARTIFACT = "artifact";
	static final String MISC = "misc";
	static final String RING = "ring";
	static final String SECOND_WEP = "second_wep";

	private EchoHeroSnapshot() {
	}

	public static boolean hasEquippedItems(Bundle echoData) {
		if (echoData == null) {
			return false;
		}
		return echoData.get(WEAPON) != null
				|| echoData.get(ARMOR) != null
				|| echoData.get(ARTIFACT) != null
				|| echoData.get(MISC) != null
				|| echoData.get(RING) != null
				|| echoData.get(SECOND_WEP) != null;
	}

	public static boolean heroHasEquippedItems(Hero hero) {
		if (hero == null) {
			return false;
		}
		return hero.belongings.weapon() != null
				|| hero.belongings.armor() != null
				|| hero.belongings.artifact() != null
				|| hero.belongings.misc() != null
				|| hero.belongings.ring() != null
				|| hero.belongings.secondWep() != null;
	}

	public static Bundle captureFromHero(Hero hero) {
		if (hero == null) {
			return null;
		}

		Bundle data = new Bundle();
		try {
			hero.storeInBundle(data);
		} catch (Throwable ignored) {
			data = new Bundle();
		}
		recordEquippedItems(hero, data);
		return data;
	}

	public static void recordEquippedItems(Hero hero, Bundle echoData) {
		if (hero == null || echoData == null) {
			return;
		}

		if (hero.belongings.weapon() != null && echoData.get(WEAPON) == null) {
			echoData.put(WEAPON, hero.belongings.weapon());
		}
		if (hero.belongings.armor() != null && echoData.get(ARMOR) == null) {
			echoData.put(ARMOR, hero.belongings.armor());
		}
		if (hero.belongings.artifact() != null && echoData.get(ARTIFACT) == null) {
			echoData.put(ARTIFACT, hero.belongings.artifact());
		}
		if (hero.belongings.misc() != null && echoData.get(MISC) == null) {
			echoData.put(MISC, hero.belongings.misc());
		}
		if (hero.belongings.ring() != null && echoData.get(RING) == null) {
			echoData.put(RING, hero.belongings.ring());
		}
		if (hero.belongings.secondWep() != null && echoData.get(SECOND_WEP) == null) {
			echoData.put(SECOND_WEP, hero.belongings.secondWep());
		}
	}

	public static Hero restoreHero(Echo echo) {
		if (echo == null || echo.echoData == null) {
			return null;
		}
		// Hero/item restore mutates shared UI globals; snapshot and put them back.
		Item[] savedQuickslots = snapshotQuickslots();
		ActionIndicator.Action savedAction = ActionIndicator.action;
		boolean savedUpdateItemDisplays = GameScene.updateItemDisplays;
		try {
			Hero hero = new Hero();
			hero.live();
			hero.restoreFromBundle(echo.echoData);
			return hero;
		} finally {
			restoreQuickslots(savedQuickslots);
			restoreActionIndicator(savedAction);
			GameScene.updateItemDisplays = savedUpdateItemDisplays;
			Belongings.bundleRestoring = false;
		}
	}

	private static Item[] snapshotQuickslots() {
		Item[] slots = new Item[QuickSlot.SIZE];
		for (int i = 0; i < QuickSlot.SIZE; i++) {
			slots[i] = Dungeon.quickslot.getItem(i);
		}
		return slots;
	}

	private static void restoreQuickslots(Item[] slots) {
		Dungeon.quickslot.reset();
		for (int i = 0; i < slots.length; i++) {
			if (slots[i] != null) {
				Dungeon.quickslot.setSlot(i, slots[i]);
			}
		}
	}

	private static void restoreActionIndicator(ActionIndicator.Action action) {
		if (action == null) {
			ActionIndicator.clearAction();
		} else {
			ActionIndicator.setAction(action);
		}
	}
}
