package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.DragonsBreathEchoBridge;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDragonsBreath;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight;
import com.watabou.noosa.audio.Sample;

/**
 * Echo potion drink / throw — fork-owned. Rejects Hero-only drinks before
 * mutation;
 * thrown potions route through {@link EchoThrowAdapter}.
 */
public final class EchoPotionAdapter {

	private EchoPotionAdapter() {
	}

	/**
	 * {@code apply(Char)} is a no-op on non-Hero — refuse without consuming.
	 */
	public static boolean isHeroOnlyDrink(Potion potion) {
		return potion instanceof PotionOfStrength
				|| potion instanceof PotionOfExperience
				|| potion instanceof ElixirOfMight
				|| potion instanceof PotionOfMindVision
				|| potion instanceof PotionOfMagicalSight;
	}

	public static boolean drink(EchoBoss boss, Potion potion) {
		if (boss == null || potion == null) {
			return false;
		}
		// Targeted cone — Echo must use breathe with a cell, never self-drink.
		if (potion instanceof PotionOfDragonsBreath || isHeroOnlyDrink(potion)) {
			return false;
		}
		EchoActionContext ctx = EchoActionContext.of(boss);
		potion.detach(ctx.gear().backpack);
		potion.setCurrent(ctx.stats());
		potion.apply(ctx.body);
		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.DRINK);
			ctx.body.sprite.operate(ctx.body.pos);
		}
		return true;
	}

	public static boolean throwPotion(EchoBoss boss, Potion potion, int cell) {
		return EchoThrowAdapter.throwItem(boss, potion, cell);
	}

	/**
	 * Dragon's Breath fire cone from the boss body. Consumes the kit potion;
	 * does not spend phantom-kit time.
	 */
	public static boolean breathe(EchoBoss boss, PotionOfDragonsBreath potion, int cell) {
		if (boss == null || potion == null || cell < 0) {
			return false;
		}
		EchoActionContext ctx = EchoActionContext.of(boss);
		if (ctx.gear().backpack.contains(potion)) {
			potion.detach(ctx.gear().backpack);
		}
		potion.setCurrent(ctx.stats());

		DragonsBreathEchoBridge.applyCone(ctx.body.pos, cell);
		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.DRINK);
			Sample.INSTANCE.play(Assets.Sounds.BURNING);
			ctx.body.sprite.operate(ctx.body.pos);
			ctx.body.sprite.zap(cell);
			DragonsBreathEchoBridge.playConeVfx(ctx.body, cell, null);
		}
		return true;
	}

}
