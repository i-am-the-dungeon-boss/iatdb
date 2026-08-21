package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoActionContext;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

/**
 * Same-package bridge for Echo horn snack/eat without Hero UI riders.
 */
public final class HornOfPlentyEchoBridge {

	private HornOfPlentyEchoBridge() {
	}

	public static boolean snack(EchoBoss boss, HornOfPlenty horn, int chargesToUse) {
		if (boss == null || horn == null) {
			return false;
		}
		EchoActionContext ctx = EchoActionContext.of(boss);
		if (ctx.body.buff(MagicImmune.class) != null) {
			return false;
		}
		if (!horn.isEquipped(ctx.stats())) {
			return false;
		}
		if (horn.charge <= 0 || chargesToUse <= 0) {
			return false;
		}
		if (chargesToUse > horn.charge) {
			chargesToUse = horn.charge;
		}

		int satietyPerCharge = (int) (Hunger.STARVING / 5f);
		if (Dungeon.isChallenged(Challenges.NO_FOOD)) {
			satietyPerCharge /= 3;
		}

		Buff.affect(ctx.body, Hunger.class).satisfy(satietyPerCharge * chargesToUse);
		Statistics.foodEaten++;
		horn.charge -= chargesToUse;

		if (ctx.canWorldFx()) {
			ctx.body.sprite.operate(ctx.body.pos);
			SpellSprite.show(ctx.body, SpellSprite.FOOD);
			Sample.INSTANCE.play(Assets.Sounds.EAT);
		}

		updateHornImage(horn);
		horn.updateQuickslot();
		return true;
	}

	private static void updateHornImage(HornOfPlenty horn) {
		if (horn.charge >= 8) {
			horn.image = ItemSpriteSheet.ARTIFACT_HORN4;
		} else if (horn.charge >= 5) {
			horn.image = ItemSpriteSheet.ARTIFACT_HORN3;
		} else if (horn.charge >= 2) {
			horn.image = ItemSpriteSheet.ARTIFACT_HORN2;
		} else {
			horn.image = ItemSpriteSheet.ARTIFACT_HORN1;
		}
	}
}
