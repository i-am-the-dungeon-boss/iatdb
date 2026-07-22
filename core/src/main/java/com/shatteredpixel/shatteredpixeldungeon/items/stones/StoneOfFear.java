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

package com.shatteredpixel.shatteredpixeldungeon.items.stones;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;

public class StoneOfFear extends Runestone {

	{
		image = ItemSpriteSheet.STONE_FEAR;
	}

	@Override
	protected void activate(int cell) {

		Char ch = Actor.findChar(cell);
		// Echo throwAs borrows kit onto body — hostility vs the live body Char.
		Char source = Actor.findChar(curUser.pos);
		if (source == null) {
			source = curUser;
		}

		if (ch != null && ch.alignment != source.alignment) {
			Buff.affect(ch, Terror.class, Terror.DURATION).object = source.id();
		}

		if (curUser != null && curUser.sprite != null && curUser.sprite.parent != null) {
			new Flare(5, 16).color(0xFF0000, true).show(curUser.sprite.parent, DungeonTilemap.tileCenterToWorld(cell),
					2f);
		}
		Sample.INSTANCE.play(Assets.Sounds.READ);

	}

}
