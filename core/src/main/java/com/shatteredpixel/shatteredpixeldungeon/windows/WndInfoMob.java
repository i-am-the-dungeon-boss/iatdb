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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.VillageEchoInfoBlock;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageEcho;
import com.watabou.noosa.ui.Component;

public class WndInfoMob extends WndTitledMessage {

	public WndInfoMob(Mob mob) {

		super(new MobTitle(mob), infoBlock(mob));

	}

	/**
	 * A mob's info text. A village figure's is several blocks rather than one:
	 * its honours each carry their own colour, the same colour the figure shouts
	 * in the square.
	 */
	public static RenderedTextBlock infoBlock(Mob mob) {
		if (mob instanceof VillageEcho) {
			return new VillageEchoInfoBlock((VillageEcho) mob, 6, WIDTH_MIN);
		}
		RenderedTextBlock text = PixelScene.renderTextBlock(6);
		text.text(mob.info(), WIDTH_MIN);
		return text;
	}

	static class MobTitle extends Component {

		private static final int GAP = 2;

		private CharSprite image;
		private RenderedTextBlock name;
		private HealthBar health;
		private BuffIndicator buffs;

		public MobTitle(Mob mob) {

			name = PixelScene.renderTextBlock(Messages.titleCase(mob.name()), 9);
			name.hardlight(TITLE_COLOR);
			add(name);

			image = mob.sprite();
			add(image);

			health = new HealthBar();
			health.level(mob);
			add(health);

			buffs = new BuffIndicator(mob, false);
			add(buffs);
		}

		@Override
		protected void layout() {

			image.x = 0;
			image.y = Math.max(0, name.height() + health.height() - image.height());

			float w = width - image.width() - GAP;

			name.setPos(x + image.width() + GAP,
					image.height() > name.height() ? y + (image.height() - name.height()) / 2 : y);

			health.setRect(image.width() + GAP, name.bottom() + GAP, w, health.height());

			buffs.maxBuffs = 50; // infinite, effectively
			buffs.setRect(name.right(), name.bottom() - BuffIndicator.SIZE_SMALL - 2, w - name.width(), 8);

			// If buff bar doesn't have enough room, move it below
			if (!buffs.allBuffsVisible()) {
				buffs.setRect(0, health.bottom(), width, 8);
				height = Math.max(image.y + image.height(), buffs.bottom());
			} else {
				height = Math.max(image.y + image.height(), health.bottom());
			}
		}
	}
}
