package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Preparation;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoActionContext;
import com.watabou.noosa.audio.Sample;

/**
 * Same-package bridge for Echo cloak stealth attach/detach.
 */
public final class CloakOfShadowsEchoBridge {

	private CloakOfShadowsEchoBridge() {
	}

	public static boolean toggleStealth(EchoBoss boss, CloakOfShadows cloak) {
		if (boss == null || cloak == null) {
			return false;
		}
		EchoActionContext ctx = EchoActionContext.of(boss);
		Char body = ctx.body;

		if (body.buff(MagicImmune.class) != null) {
			return false;
		}

		// Echo kits restore mid-stealth cloaks onto the phantom hero; that is not
		// fight invisibility. Clear it so we can activate on ctx.body.
		clearStealthIfNotOn(cloak, body);

		if (cloak.activeBuff == null) {
			if (!cloak.isEquipped(ctx.kit) && !ctx.kit.hasTalent(Talent.LIGHT_CLOAK)) {
				return false;
			}
			if (cloak.cursed) {
				return false;
			}
			if (cloak.charge <= 0) {
				return false;
			}

			if (ctx.canWorldFx()) {
				Sample.INSTANCE.play(Assets.Sounds.MELD);
				body.sprite.operate(body.pos);
			}

			cloak.activeBuff = newStealthBuff(cloak);
			cloak.activeBuff.attachTo(body);
			return true;
		}

		cloak.activeBuff.detach();
		cloak.activeBuff = null;
		if (body.invisible <= 0 && body.buff(Preparation.class) != null) {
			body.buff(Preparation.class).detach();
		}
		if (ctx.canWorldFx()) {
			body.sprite.operate(body.pos);
		}
		return true;
	}

	private static CloakOfShadows.cloakStealth newStealthBuff(CloakOfShadows cloak) {
		return (CloakOfShadows.cloakStealth) cloak.activeBuff();
	}

	/** Detach/drop active stealth unless it is already on {@code body}. */
	private static void clearStealthIfNotOn(CloakOfShadows cloak, Char body) {
		if (cloak.activeBuff == null || cloak.activeBuff.target == body) {
			return;
		}
		if (cloak.activeBuff.target != null) {
			cloak.activeBuff.detach();
		} else {
			cloak.activeBuff = null;
		}
	}
}
