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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

public class BlessSpell extends TargetedClericSpell {

	public static final BlessSpell INSTANCE = new BlessSpell();

	@Override
	public int icon() {
		return HeroIcon.BLESS;
	}

	@Override
	public int targetingFlags() {
		return -1; // auto-targeting behaviour is often wrong, so we don't use it
	}

	@Override
	public boolean canCast(Hero hero) {
		return super.canCast(hero) && hero.hasTalent(Talent.BLESS);
	}

	@Override
	protected boolean castAtTarget(UseContext ctx, HolyTome tome, Integer target) {
		if (target == null || Dungeon.level == null) {
			return false;
		}

		Char ch = Actor.findChar(target);
		boolean inFov = ctx.body.fieldOfView != null && target < ctx.body.fieldOfView.length
				? ctx.body.fieldOfView[target]
				: Dungeon.level.heroFOV[target];
		if (ch == null || !inFov) {
			return false;
		}

		affectChar(ctx.kit, ch, ctx.body);

		Char ally = PowerOfMany.getPoweredAlly();
		if (ally != null && ally.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null) {
			if (ch == ctx.body) {
				affectChar(ctx.kit, ally, ctx.body);
			} else if (ally == ch) {
				affectChar(ctx.kit, ctx.body, ctx.body);
			}
		}

		onSpellCast(ctx, tome);
		return true;
	}

	@Override
	protected void onTargetSelected(HolyTome tome, Hero hero, Integer target) {
		if (target == null) {
			return;
		}

		Char ch = Actor.findChar(target);
		if (ch == null || !Dungeon.level.heroFOV[target]) {
			GLog.w(Messages.get(this, "no_target"));
			return;
		}

		Sample.INSTANCE.play(Assets.Sounds.TELEPORT);

		affectChar(hero, ch, hero);

		if (ch == hero) {
			hero.busy();
			hero.sprite.operate(ch.pos);
			hero.spend(1f);
		} else {
			hero.sprite.zap(ch.pos);
			hero.spendAndNext(1f);
		}

		Char ally = PowerOfMany.getPoweredAlly();
		if (ally != null && ally.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null) {
			if (ch == hero) {
				affectChar(hero, ally, hero); // if cast on hero, duplicate to ally
			} else if (ally == ch) {
				affectChar(hero, hero, hero); // if cast on ally, duplicate to hero
			}
		}

		onSpellCast(tome, hero);
	}

	private void affectChar(Hero kit, Char ch, Char self) {
		if (ctxHeroFxSafe(ch)) {
			new Flare(6, 32).color(0xFFFF00, true).show(ch.sprite, 2f);
		}
		if (ch == self) {
			Buff.prolong(ch, Bless.class, 2f + 4 * kit.pointsInTalent(Talent.BLESS));
			Buff.affect(ch, Barrier.class).setShield(5 + 5 * kit.pointsInTalent(Talent.BLESS));
			if (ch.sprite != null) {
				ch.sprite.showStatusWithIcon(CharSprite.POSITIVE,
						Integer.toString(5 + 5 * kit.pointsInTalent(Talent.BLESS)), FloatingText.SHIELDING);
			}
		} else {
			Buff.prolong(ch, Bless.class, 5f + 5 * kit.pointsInTalent(Talent.BLESS));
			int totalHeal = 5 + 5 * kit.pointsInTalent(Talent.BLESS);
			if (ch.HT - ch.HP < totalHeal) {
				int barrier = totalHeal - (ch.HT - ch.HP);
				barrier = Math.max(barrier, 0);
				if (ch.HP != ch.HT) {
					ch.HP = ch.HT;
					if (ch.sprite != null) {
						ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(totalHeal - barrier),
								FloatingText.HEALING);
					}
				}
				if (barrier > 0) {
					Buff.affect(ch, Barrier.class).setShield(barrier);
					if (ch.sprite != null) {
						ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(barrier),
								FloatingText.SHIELDING);
					}
				}
			} else {
				ch.HP = ch.HP + totalHeal;
				if (ch.sprite != null) {
					ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(totalHeal),
							FloatingText.HEALING);
				}
			}
		}

		if (ch.alignment != Char.Alignment.ALLY && kit.subClass == HeroSubClass.PRIEST) {
			Buff.affect(ch, GuidingLight.Illuminated.class);
		}
	}

	private static boolean ctxHeroFxSafe(Char ch) {
		return ch.sprite != null && ch.sprite.parent != null;
	}

	public String desc() {
		int talentLvl = Dungeon.hero.pointsInTalent(Talent.BLESS);
		return Messages.get(this, "desc", 2 + 4 * talentLvl, 5 + 5 * talentLvl, 5 + 5 * talentLvl, 5 + 5 * talentLvl)
				+ "\n\n" + Messages.get(this, "charge_cost", (int) chargeUse(Dungeon.hero));
	}

}
