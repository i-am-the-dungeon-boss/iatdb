package com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoActionContext;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;

/**
 * Echo cleric spend/charge — same-package bridge. Hero keeps {@link ClericSpell#onSpellCast(HolyTome, Hero)}.
 */
public final class ClericSpellEchoBridge {

	private ClericSpellEchoBridge() {
	}

	public static void onSpellCast(ClericSpell spell, EchoActionContext ctx, HolyTome tome) {
		Hero kit = ctx.kit;
		Invisibility.dispel(ctx.body);
		if (kit.hasTalent(Talent.SATIATED_SPELLS) && kit.buff(Talent.SatiatedSpellsTracker.class) != null) {
			int amount = 1 + 2 * kit.pointsInTalent(Talent.SATIATED_SPELLS);
			Buff.affect(ctx.body, Barrier.class).setShield(amount);
			Char ally = PowerOfMany.getPoweredAlly();
			if (ally != null && ally.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null) {
				Buff.affect(ally, Barrier.class).setShield(amount);
			}
			kit.buff(Talent.SatiatedSpellsTracker.class).detach();
		}
		tome.spendCharge(spell.chargeUse(kit));
		if (kit.subClass == HeroSubClass.PALADIN) {
			if (spell != HolyWeapon.INSTANCE && kit.buff(HolyWeapon.HolyWepBuff.class) != null) {
				kit.buff(HolyWeapon.HolyWepBuff.class).extend(10 * spell.chargeUse(kit));
			}
			if (spell != HolyWard.INSTANCE && kit.buff(HolyWard.HolyArmBuff.class) != null) {
				kit.buff(HolyWard.HolyArmBuff.class).extend(10 * spell.chargeUse(kit));
			}
		}
		if (kit.buff(AscendedForm.AscendBuff.class) != null) {
			kit.buff(AscendedForm.AscendBuff.class).spellCasts++;
			kit.buff(AscendedForm.AscendBuff.class).incShield((int) (10 * spell.chargeUse(kit)));
		}
	}

	/** First kit item this spell can affect. */
	public static Item firstUsableItem(InventoryClericSpell spell, Hero kit) {
		for (Item item : kit.belongings) {
			if (spell.usableOnItem(item)) {
				return item;
			}
		}
		return null;
	}
}
