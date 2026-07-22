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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

public class ScrollOfRetribution extends Scroll {

	{
		icon = ItemSpriteSheet.Icons.SCROLL_RETRIB;
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
			GLog.i(Messages.get(this, "blast"));
		}

		float hpPercent = (ctx.body.HT - ctx.body.HP) / (float) (ctx.body.HT);
		float power = Math.min(4f, 4.45f * hpPercent);

		ArrayList<Char> targets = new ArrayList<>();
		ctx.forEachVisibleHostile(targets::add);

		for (Char ch : targets) {
			ch.damage(Math.round(ch.HT / 10f + (ch.HP * power * 0.225f)), this);
			if (ch.isAlive()) {
				Buff.prolong(ch, Blindness.class, Blindness.DURATION);
			}
		}

		Buff.prolong(ctx.body, Weakness.class, Weakness.DURATION);
		Buff.prolong(ctx.body, Blindness.class, Blindness.DURATION);
		Dungeon.observe();

		if (ctx.heroFX) {
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
