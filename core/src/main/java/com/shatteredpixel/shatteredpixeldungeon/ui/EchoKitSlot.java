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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.inspect.ItemPreview;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;

/**
 * Render-and-inspect slot for an echo's equipped kit. Never reads or writes
 * {@link com.shatteredpixel.shatteredpixeldungeon.Dungeon#hero}.
 */
public class EchoKitSlot extends ItemSlot {

	public static final int NO_TINT = -1;

	private static final int NORMAL = 0x9953564D;
	private static final int EQUIPPED = 0x9991938C;

	private ColorBlock bg;
	private Hero owner;
	private boolean equipped;

	public EchoKitSlot(Item item, Hero owner, boolean equipped) {
		super();
		this.owner = owner;
		this.equipped = equipped;
		item(item);
	}

	/** DEGRADED / MASTERED / NO_TINT for item's STR label as seen by owner. */
	public static int strengthColor(Item item, Hero owner) {
		if (owner == null || item == null) {
			return NO_TINT;
		}
		int str;
		boolean mastery = false;
		if (item instanceof Weapon) {
			str = ((Weapon) item).STRReq();
			mastery = ((Weapon) item).masteryPotionBonus;
		} else if (item instanceof Armor) {
			str = ((Armor) item).STRReq();
			mastery = ((Armor) item).masteryPotionBonus;
		} else {
			return NO_TINT;
		}
		if (str > owner.STR()) {
			return DEGRADED;
		}
		if (mastery) {
			return MASTERED;
		}
		return NO_TINT;
	}

	@Override
	protected void createChildren() {
		bg = new ColorBlock(1, 1, NORMAL);
		add(bg);

		super.createChildren();
	}

	@Override
	protected void layout() {
		bg.size(width, height);
		bg.x = x;
		bg.y = y;

		super.layout();
	}

	@Override
	public void alpha(float value) {
		super.alpha(value);
		bg.alpha(value);
	}

	@Override
	public void item(Item item) {
		super.item(item);

		bg.visible = !(item instanceof Gold || item instanceof Bag);

		if (item != null) {
			bg.texture(TextureCache.createSolid(equipped ? EQUIPPED : NORMAL));
			bg.resetColor();
			if (item.cursed && item.cursedKnown) {
				bg.ra = +0.3f;
				bg.ga = -0.15f;
				bg.ba = -0.15f;
			} else if (!item.isIdentified()) {
				if ((item instanceof EquipableItem || item instanceof Wand) && item.cursedKnown) {
					bg.ba = +0.3f;
					bg.ra = -0.1f;
				} else {
					bg.ra = +0.35f;
					bg.ba = +0.35f;
				}
			}

			if (item.name() == null) {
				enable(false);
			}
		} else {
			bg.texture(TextureCache.createSolid(NORMAL));
			bg.resetColor();
		}
	}

	@Override
	public void updateText() {
		super.updateText();
		if (extra == null || item == null || !item.levelKnown) {
			return;
		}
		if (!(item instanceof Weapon) && !(item instanceof Armor)) {
			return;
		}
		int color = strengthColor(item, owner);
		if (color == NO_TINT) {
			extra.resetColor();
		} else {
			extra.hardlight(color);
		}
	}

	@Override
	protected void onPointerDown() {
		bg.brightness(1.5f);
		Sample.INSTANCE.play(Assets.Sounds.CLICK, 0.7f, 0.7f, 1.2f);
	}

	@Override
	protected void onPointerUp() {
		bg.brightness(1.0f);
	}

	/**
	 * Opens the description of a copy stamped with the echo, so the whole text —
	 * its class, subclass, level and strength verdict — is written for the hero
	 * that owns the kit rather than for whoever is playing. The echo's own item
	 * is left exactly as it was, and {@code Dungeon.hero} is not touched.
	 */
	@Override
	protected void onClick() {
		if (item == null || item instanceof WndBag.Placeholder) {
			return;
		}
		if (owner == null) {
			GameScene.show(new WndInfoItem(item));
			return;
		}
		// Shown as a copy. The original may be a live EchoBoss's fighting kit,
		// and looking at a sword must not identify the sword it is swinging.
		Item preview = ItemPreview.of(item, owner);
		if (preview == null) {
			// No isolated copy, no window: falling back to the original would
			// describe somebody else's kit against the living player.
			Game.reportException(new IllegalStateException(
					"could not copy " + item.getClass().getSimpleName() + " for preview"));
			return;
		}
		GameScene.show(new WndInfoItem(preview));
	}

	@Override
	protected boolean onLongClick() {
		onClick();
		return true;
	}
}
