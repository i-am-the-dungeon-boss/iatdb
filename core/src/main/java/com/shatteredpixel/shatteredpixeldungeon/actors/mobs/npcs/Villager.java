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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BlacksmithSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GhostSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WandmakerSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

/**
 * A villager standing around the ground level, reusing existing NPC art.
 *
 * <p>Purely conversational: villagers cannot move, be damaged, be buffed, or be
 * attacked. They exist to make the village feel inhabited and to point the
 * player at the dungeon.
 */
public class Villager extends NPC {

	/** Which villager this is — picks the sprite and the dialogue. */
	public enum Kind {
		SMITH,
		SAGE,
		ELDER
	}

	private static final String KIND = "kind";

	private Kind kind = Kind.ELDER;

	{
		state = PASSIVE;
		properties.add(Property.IMMOVABLE);
		spriteClass = BlacksmithSprite.class;
	}

	public void setKind(Kind kind) {
		this.kind = kind == null ? Kind.ELDER : kind;
		switch (this.kind) {
			case SMITH:
				spriteClass = BlacksmithSprite.class;
				break;
			case SAGE:
				spriteClass = WandmakerSprite.class;
				break;
			case ELDER:
			default:
				spriteClass = GhostSprite.class;
				break;
		}
	}

	public Kind kind() {
		return kind;
	}

	@Override
	public String name() {
		return Messages.get(this, kindKey() + "_name");
	}

	@Override
	public String description() {
		return Messages.get(this, kindKey() + "_desc");
	}

	private String kindKey() {
		return kind.name().toLowerCase(java.util.Locale.ROOT);
	}

	@Override
	public int defenseSkill(Char enemy) {
		return INFINITE_EVASION;
	}

	@Override
	protected Char chooseEnemy() {
		return null;
	}

	@Override
	public void damage(int dmg, Object src) {
		// villagers are scenery; nothing here can hurt them
	}

	@Override
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	protected boolean act() {
		// stand still and keep the turn moving
		spend(TICK);
		return true;
	}

	@Override
	public boolean interact(Char c) {
		if (c != Dungeon.hero) {
			return true;
		}
		Game.runOnRenderThread(() -> GameScene.show(new WndTitledMessage(
				sprite(),
				Messages.titleCase(name()),
				Messages.get(Villager.this, kindKey() + "_chat"))));
		return true;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(KIND, kind.name());
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(KIND)) {
			try {
				setKind(Kind.valueOf(bundle.getString(KIND)));
			} catch (IllegalArgumentException e) {
				setKind(Kind.ELDER);
			}
		}
	}
}
