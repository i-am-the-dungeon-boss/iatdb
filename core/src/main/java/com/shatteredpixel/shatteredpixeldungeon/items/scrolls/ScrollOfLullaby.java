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

package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

public class ScrollOfLullaby extends Scroll {

	{
		icon = ItemSpriteSheet.Icons.SCROLL_LULLABY;
	}

	@Override
	public void doRead() {
		doReadAs(UseContext.hero(curUser));
	}

	@Override
	protected boolean doReadAs(UseContext ctx) {
		detach(ctx.kit.belongings.backpack);

		if (UseContext.canWorldFx(ctx.body)) {
			ctx.body.sprite.centerEmitter().start(Speck.factory(Speck.NOTE), 0.3f, 5);
			Sample.INSTANCE.play(Assets.Sounds.LULLABY);
		}

		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (Dungeon.level.heroFOV[mob.pos]) {
				Buff.affect(mob, Drowsy.class, Drowsy.DURATION);
				if (UseContext.canWorldFx(mob)) {
					mob.sprite.centerEmitter().start(Speck.factory(Speck.NOTE), 0.3f, 5);
				}
			}
		}
		// Echo: also soothe the opposing Hero (not in level.mobs)
		ctx.forEachVisibleHostile(ch -> {
			if (ch instanceof Mob) {
				return; // already handled
			}
			Buff.affect(ch, Drowsy.class, Drowsy.DURATION);
			if (UseContext.canWorldFx(ch)) {
				com.watabou.noosa.particles.Emitter e = ch.sprite.centerEmitter();
				if (e != null) {
					e.start(Speck.factory(Speck.NOTE), 0.3f, 5);
				}
			}
		});

		Buff.affect(ctx.body, Drowsy.class, Drowsy.DURATION);

		if (ctx.heroFX) {
			GLog.i(Messages.get(this, "sooth"));
			identify();
		}

		readAnimation(ctx);
		return true;
	}

	@Override
	public int value() {
		return isKnown() ? 40 * quantity : super.value();
	}
}
