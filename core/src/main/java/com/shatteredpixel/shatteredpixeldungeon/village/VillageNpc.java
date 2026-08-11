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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;

/**
 * A villager: a sprite, a cell, and something to say.
 *
 * <p>Not a {@code Mob} or an {@code NPC}. Villagers have no health, no
 * alignment and no turn, because there is no combat in the village and nothing
 * here should be reachable by dungeon mechanics.
 */
public class VillageNpc extends MovieClip {

	/** Which villager this is: picks the art and the dialogue. */
	public enum Kind {
		KEEPER(Assets.Sprites.KEEPER, 14, 14),
		SMITH(Assets.Sprites.TROLL, 12, 14),
		SAGE(Assets.Sprites.MAKER, 14, 14),
		ELDER(Assets.Sprites.GHOST, 14, 15);

		final String texture;
		final int frameWidth;
		final int frameHeight;

		Kind(String texture, int frameWidth, int frameHeight) {
			this.texture = texture;
			this.frameWidth = frameWidth;
			this.frameHeight = frameHeight;
		}

		String key() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	public final Kind kind;
	public final int cell;

	public VillageNpc(VillageMap map, Kind kind, int cell) {
		super();
		this.kind = kind;
		this.cell = cell;

		texture(kind.texture);
		TextureFilm film = new TextureFilm(texture, kind.frameWidth, kind.frameHeight);

		Animation idle = new Animation(4, true);
		idle.frames(film, 0, 0, 0, 1);
		play(idle);

		x = VillageTilemap.cellX(map, cell)
				+ (VillageTilemap.SIZE - kind.frameWidth) / 2f;
		y = VillageTilemap.cellY(map, cell) + VillageTilemap.SIZE - kind.frameHeight;
	}

	public String npcName() {
		return Messages.get(VillageNpc.class, kind.key() + "_name");
	}

	public String chat() {
		return Messages.get(VillageNpc.class, kind.key() + "_chat");
	}
}
