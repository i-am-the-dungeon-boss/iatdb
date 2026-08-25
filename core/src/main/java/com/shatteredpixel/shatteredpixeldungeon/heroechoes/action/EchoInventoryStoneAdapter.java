package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.InventoryStone;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.InventoryStoneEchoBridge;

/**
 * Echo inventory-stone auto-selection — fork-owned. Hero keeps upstream bag UI.
 */
public final class EchoInventoryStoneAdapter {

	private EchoInventoryStoneAdapter() {
	}

	public static boolean use(EchoBoss boss, InventoryStone stone) {
		if (boss == null || stone == null) {
			return false;
		}
		EchoActionContext ctx = EchoActionContext.of(boss);
		if (ctx.stats().buff(MagicImmune.class) != null) {
			return false;
		}
		Item pick = InventoryStoneEchoBridge.firstUsable(stone, ctx.stats());
		if (pick == null) {
			return false;
		}
		return InventoryStoneEchoBridge.applyEcho(ctx, stone, pick);
	}
}
