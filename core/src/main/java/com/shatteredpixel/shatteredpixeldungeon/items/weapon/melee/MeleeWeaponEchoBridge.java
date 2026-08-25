package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoActionContext;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;

/**
 * Package bridge for Echo duelist charge/talent hooks — fork-owned. Status
 * effects on {@link EchoBoss} body; resource trackers on phantom kit.
 */
public final class MeleeWeaponEchoBridge {

	private MeleeWeaponEchoBridge() {
	}

	public static void beforeAbilityUsed(MeleeWeapon wep, EchoActionContext ctx, Char target) {
		Hero kit = ctx.stats();
		kit.belongings.abilityWeapon = wep;
		MeleeWeapon.Charger charger = Buff.affect(kit, MeleeWeapon.Charger.class);

		charger.partialCharge -= wep.abilityChargeUse(kit, target);
		while (charger.partialCharge < 0 && charger.charges > 0) {
			charger.charges--;
			charger.partialCharge++;
		}

		if (kit.heroClass == HeroClass.DUELIST
				&& kit.hasTalent(Talent.AGGRESSIVE_BARRIER)
				&& (ctx.body.HP / (float) ctx.body.HT) <= 0.5f) {
			int shieldAmt = 1 + 2 * kit.pointsInTalent(Talent.AGGRESSIVE_BARRIER);
			Buff.affect(ctx.body, Barrier.class).setShield(shieldAmt);
			if (ctx.canWorldFx()) {
				ctx.body.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shieldAmt),
						FloatingText.SHIELDING);
			}
		}
	}

	public static void afterAbilityUsed(MeleeWeapon wep, EchoActionContext ctx) {
		Hero kit = ctx.stats();
		kit.belongings.abilityWeapon = null;
		if (kit.hasTalent(Talent.PRECISE_ASSAULT)) {
			Buff.prolong(kit, Talent.PreciseAssaultTracker.class, kit.cooldown() + 4f);
		}
		if (kit.hasTalent(Talent.VARIED_CHARGE)) {
			Talent.VariedChargeTracker tracker = kit.buff(Talent.VariedChargeTracker.class);
			if (tracker == null || tracker.weapon == wep.getClass() || tracker.weapon == null) {
				Buff.affect(kit, Talent.VariedChargeTracker.class).weapon = wep.getClass();
			} else {
				tracker.detach();
				MeleeWeapon.Charger charger = Buff.affect(kit, MeleeWeapon.Charger.class);
				charger.gainCharge(kit.pointsInTalent(Talent.VARIED_CHARGE) / 6f);
				ScrollOfRecharging.charge(kit);
			}
		}
		if (kit.hasTalent(Talent.COMBINED_LETHALITY)) {
			Talent.CombinedLethalityAbilityTracker tracker = kit.buff(Talent.CombinedLethalityAbilityTracker.class);
			if (tracker == null || tracker.weapon == wep || tracker.weapon == null) {
				Buff.affect(kit, Talent.CombinedLethalityAbilityTracker.class, kit.cooldown()).weapon = wep;
			} else {
				tracker.detach();
			}
		}
		if (kit.hasTalent(Talent.COMBINED_ENERGY)) {
			Talent.CombinedEnergyAbilityTracker tracker = kit.buff(Talent.CombinedEnergyAbilityTracker.class);
			if (tracker == null || !tracker.monkAbilused) {
				Buff.prolong(kit, Talent.CombinedEnergyAbilityTracker.class, 5f).wepAbilUsed = true;
			} else {
				tracker.wepAbilUsed = true;
				Buff.affect(kit, MonkEnergy.class).processCombinedEnergy(tracker);
			}
		}
		if (kit.buff(Talent.CounterAbilityTacker.class) != null) {
			MeleeWeapon.Charger charger = Buff.affect(kit, MeleeWeapon.Charger.class);
			charger.gainCharge(kit.pointsInTalent(Talent.COUNTER_ABILITY) * 0.375f);
			kit.buff(Talent.CounterAbilityTacker.class).detach();
		}
	}
}
