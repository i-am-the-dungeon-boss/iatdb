package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.AiItemActions;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandBridge;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.Callback;

/**
 * Echo wand / Mage's Staff zap — fork-owned. Skips Hero QuickSlot, backup
 * barrier,
 * identification, and talent riders.
 */
public final class EchoWandAdapter {

	private static final float TIME_TO_ZAP = 1f;

	private EchoWandAdapter() {
	}

	public static boolean zap(EchoBoss boss, Wand wand, int target) {
		if (boss == null || wand == null || Dungeon.level == null || target < 0) {
			return false;
		}
		EchoActionContext ctx = EchoActionContext.of(boss);
		Hero kit = ctx.kit;

		final Ballistica shot = new Ballistica(ctx.body.pos, target, wand.collisionProperties(target));
		int cell = shot.collisionPos;
		if (target == ctx.body.pos || cell == ctx.body.pos) {
			return EchoActionSupport.refuse(ctx);
		}
		if (!wand.tryToZap(kit, target)) {
			return false;
		}

		ctx.busy();

		// Keep borrowed sprite/pos until deferred onZap finishes (ANDROID-1N /
		// ANDROID-1K).
		Callback afterZap = EchoKitBorrow.defer(ctx, () -> {
			AiItemActions.withUser(kit, wand, () -> {
				wand.onZap(shot);
				finishZap(ctx, wand);
			});
		});

		wand.setCurrent(kit);

		if (ctx.canWorldFx()) {
			kit.sprite.zap(cell);
			if (wand.cursed) {
				CursedWand.cursedZap(wand, kit,
						new Ballistica(kit.pos, target, Ballistica.MAGIC_BOLT),
						afterZap);
			} else {
				wand.fx(shot, afterZap);
			}
			wand.cursedKnown = true;
			return true;
		}

		afterZap.call();
		wand.cursedKnown = true;
		return true;
	}

	public static boolean zapStaff(EchoBoss boss, MagesStaff staff, int target) {
		if (staff == null) {
			return false;
		}
		Wand wand = staff.wand();
		if (wand == null) {
			return false;
		}
		return zap(boss, wand, target);
	}

	private static void finishZap(EchoActionContext ctx, Wand wand) {
		WandBridge.spendCharges(wand);
		Invisibility.dispel(ctx.body);
		wand.updateQuickslot();
		ctx.complete(TIME_TO_ZAP);
	}
}
