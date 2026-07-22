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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.effects.Enchanting;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

public class HolyWeapon extends ClericSpell {

	public static final HolyWeapon INSTANCE = new HolyWeapon();

	@Override
	public int icon() {
		return HeroIcon.HOLY_WEAPON;
	}

	@Override
	public float chargeUse(Hero hero) {
		return 2;
	}

	@Override
	public void onCast(HolyTome tome, Hero hero) {
		castAs(UseContext.hero(hero), tome, null);
	}

	@Override
	public boolean castAs(UseContext ctx, HolyTome tome, Integer target) {
		if (ctx == null || ctx.body == null || ctx.kit == null || tome == null) {
			return false;
		}

		Buff.affect(ctx.body, HolyWepBuff.class, 50f);
		if (UseContext.canWorldFx(ctx.body)) {
			Sample.INSTANCE.play(Assets.Sounds.READ);
			ctx.body.sprite.operate(ctx.body.pos);
			if (ctx.kit.belongings.weapon() != null) {
				Enchanting.show(ctx.kit, ctx.kit.belongings.weapon());
			}
		}
		if (ctx.heroFX) {
			Item.updateQuickslot();
		}

		onSpellCast(ctx, tome);
		return true;
	}

	@Override
	public String desc() {
		String desc = Messages.get(this, "desc");
		if (Dungeon.hero.subClass == HeroSubClass.PALADIN) {
			desc += "\n\n" + Messages.get(this, "desc_paladin");
		}
		return desc + "\n\n" + Messages.get(this, "charge_cost", (int) chargeUse(Dungeon.hero));
	}

	public static class HolyWepBuff extends FlavourBuff {

		public static final float DURATION = 50f;

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.HOLY_WEAPON;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - visualcooldown()) / DURATION);
		}

		@Override
		public String desc() {
			if (Dungeon.hero.subClass == HeroSubClass.PALADIN) {
				return Messages.get(this, "desc_paladin", dispTurns());
			} else {
				return Messages.get(this, "desc", dispTurns());
			}
		}

		@Override
		public void detach() {
			super.detach();
			Item.updateQuickslot();
		}

		public void extend(float extension) {
			if (cooldown() + extension <= 2 * DURATION) {
				spend(extension);
			} else {
				postpone(2 * DURATION);
			}
		}
	}

}
