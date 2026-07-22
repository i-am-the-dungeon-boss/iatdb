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

package com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class PotionOfDragonsBreath extends ExoticPotion {

	{
		icon = ItemSpriteSheet.Icons.POTION_DRGBREATH;
	}

	protected static boolean identifiedByUse = false;

	/**
	 * Targeted cone — not a self-drink. Echo / shared callers use
	 * {@link #breatheAs(UseContext, int)}.
	 */
	@Override
	public boolean drinkAs(UseContext ctx) {
		return false;
	}

	/**
	 * Shared fire-cone execute for Hero and Echo. Mechanics apply from
	 * {@code ctx.body}; inventory/talents on {@code ctx.kit}; VFX / spend when
	 * {@code ctx.heroFX}.
	 */
	public boolean breatheAs(UseContext ctx, int cell) {
		if (ctx == null || ctx.kit == null || ctx.body == null || cell < 0) {
			return false;
		}
		if (Dungeon.level == null) {
			return false;
		}

		if (ctx.kit.belongings.backpack.contains(this)) {
			detach(ctx.kit.belongings.backpack);
		}
		setCurrent(ctx.kit);

		Char body = ctx.body;
		applyCone(body.pos, cell);

		if (UseContext.canWorldFx(body)) {
			Sample.INSTANCE.play(Assets.Sounds.DRINK);
			Sample.INSTANCE.play(Assets.Sounds.BURNING);
			body.sprite.operate(body.pos);
			body.sprite.zap(cell);
			playConeVfx(body, cell);
		}
		if (ctx.heroFX) {
			if (!anonymous) {
				Catalog.countUse(PotionOfDragonsBreath.class);
				if (Random.Float() < talentChance) {
					Talent.onPotionUsed(ctx.kit, body.pos, talentFactor);
				}
			}
			ctx.kit.spend(1f);
			ctx.turns.busy();
		}
		return true;
	}

	/** Fire blobs, Burning, Cripple, doors — same for Hero and Echo. */
	private static void applyCone(int sourcePos, int cell) {
		Ballistica bolt = new Ballistica(sourcePos, cell, Ballistica.WONT_STOP);
		ConeAOE cone = new ConeAOE(bolt, 6, 60,
				Ballistica.STOP_SOLID | Ballistica.STOP_TARGET | Ballistica.IGNORE_SOFT_SOLID);

		ArrayList<Integer> adjacentCells = new ArrayList<>();
		for (int c : cone.cells) {
			if (c == bolt.sourcePos) {
				continue;
			}

			if (Dungeon.level.map[c] == Terrain.DOOR) {
				Level.set(c, Terrain.OPEN_DOOR);
				GameScene.updateMap(c);
			}

			if (Dungeon.level.adjacent(bolt.sourcePos, c) && !Dungeon.level.flamable[c]) {
				adjacentCells.add(c);
			} else {
				GameScene.add(Blob.seed(c, 5, Fire.class));
			}

			Char ch = Actor.findChar(c);
			if (ch != null) {
				Buff.affect(ch, Burning.class).reignite(ch);
				Buff.prolong(ch, Cripple.class, 5f);
			}
		}

		for (int c : adjacentCells) {
			for (int i : PathFinder.NEIGHBOURS4) {
				if (Dungeon.level.trueDistance(c + i, bolt.sourcePos) > Dungeon.level.trueDistance(c, bolt.sourcePos)
						&& Dungeon.level.flamable[c + i]
						&& Fire.volumeAt(c + i, Fire.class) == 0) {
					GameScene.add(Blob.seed(c + i, 5, Fire.class));
				}
			}
		}
	}

	private static void playConeVfx(Char body, int cell) {
		if (body.sprite == null || body.sprite.parent == null) {
			return;
		}
		Ballistica bolt = new Ballistica(body.pos, cell, Ballistica.WONT_STOP);
		int dist = Math.min(bolt.dist, 6);
		ConeAOE cone = new ConeAOE(bolt, 6, 60,
				Ballistica.STOP_SOLID | Ballistica.STOP_TARGET | Ballistica.IGNORE_SOFT_SOLID);
		for (Ballistica ray : cone.outerRays) {
			((MagicMissile) body.sprite.parent.recycle(MagicMissile.class)).reset(
					MagicMissile.FIRE_CONE,
					body.sprite,
					ray.path.get(ray.dist),
					null);
		}
		MagicMissile.boltFromChar(
				body.sprite.parent,
				MagicMissile.FIRE_CONE,
				body.sprite,
				bolt.path.get(dist / 2),
				null);
	}

	@Override
	// need to override drink so that time isn't spent right away
	protected void drink(final Hero hero) {

		if (!isKnown()) {
			identify();
			curItem = detach(hero.belongings.backpack);
			identifiedByUse = true;
		} else {
			identifiedByUse = false;
		}

		GameScene.selectCell(targeter);
	}

	private CellSelector.Listener targeter = new CellSelector.Listener() {

		private boolean showingWindow = false;
		private boolean potionAlreadyUsed = false;

		@Override
		public void onSelect(final Integer cell) {

			if (showingWindow) {
				return;
			}
			if (potionAlreadyUsed) {
				potionAlreadyUsed = false;
				return;
			}

			if (cell == null && identifiedByUse) {
				showingWindow = true;
				ShatteredPixelDungeon.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new WndOptions(new ItemSprite(PotionOfDragonsBreath.this),
								Messages.titleCase(name()),
								Messages.get(ExoticPotion.class, "warning"),
								Messages.get(ExoticPotion.class, "yes"),
								Messages.get(ExoticPotion.class, "no")) {
							@Override
							protected void onSelect(int index) {
								showingWindow = false;
								switch (index) {
									case 0:
										curUser.spendAndNext(1f);
										identifiedByUse = false;
										break;
									case 1:
										GameScene.selectCell(targeter);
										break;
								}
							}

							public void onBackPressed() {
							}
						});
					}
				});
			} else if (cell != null) {
				PotionOfDragonsBreath potion = PotionOfDragonsBreath.this;
				if (identifiedByUse) {
					// already detached when identified-by-use
					potion = (PotionOfDragonsBreath) curItem;
				}
				potionAlreadyUsed = true;
				identifiedByUse = false;
				potion.breatheAs(UseContext.hero(curUser), cell);
			}
		}

		@Override
		public String prompt() {
			return Messages.get(PotionOfDragonsBreath.class, "prompt");
		}
	};
}
