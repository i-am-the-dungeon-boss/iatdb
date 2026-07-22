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

package com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class ScrollOfPsionicBlast extends ExoticScroll {

	{
		icon = ItemSpriteSheet.Icons.SCROLL_PSIBLAST;
	}

	@Override
	public void doRead() {
		doReadAs(UseContext.hero(curUser));
	}

	@Override
	protected boolean doReadAs(UseContext ctx) {
		detach(ctx.kit.belongings.backpack);

		if (UseContext.canWorldFx(ctx.body)) {
			GameScene.flash(0x80FFFFFF);
			Sample.INSTANCE.play(Assets.Sounds.BLAST);
		}
		if (ctx.heroFX) {
			GLog.i(Messages.get(ScrollOfRetribution.class, "blast"));
		}

		ArrayList<Char> targets = new ArrayList<>();
		ctx.forEachVisibleHostile(targets::add);

		for (Char ch : targets) {
			ch.damage(Math.round(ch.HT / 2f + ch.HP / 2f), this);
			if (ch.isAlive()) {
				Buff.prolong(ch, Blindness.class, Blindness.DURATION);
			}
		}

		ctx.body.damage(Math.max(0, Math.round(ctx.body.HT * (0.5f * (float) Math.pow(0.9, targets.size())))), this);
		if (ctx.body.isAlive()) {
			Buff.prolong(ctx.body, Blindness.class, Blindness.DURATION);
			Buff.prolong(ctx.body, Weakness.class, Weakness.DURATION * 5f);
			if (ctx.heroFX) {
				Dungeon.observe();
			}
			readAnimation(ctx);
		} else if (ctx.heroFX) {
			Badges.validateDeathFromFriendlyMagic();
			Dungeon.fail(this);
			GLog.n(Messages.get(this, "ondeath"));
		}

		if (ctx.heroFX) {
			identify();
		}
		return true;
	}
}
