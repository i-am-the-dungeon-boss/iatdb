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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Door;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MirrorSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.Callback;

public class Feint extends ArmorAbility {

	{
		baseChargeUse = 50;
		// do nothing, attack is purely visual
	}

	@Override
	public int icon() {
		return HeroIcon.FEINT;
	}

	public boolean useTargeting() {
		return false;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos(Char user, int dst) {
		return dst;
	}

	@Override
	protected void activate(ClassArmor armor, UseContext ctx, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.kit;
		if (target == null) {
			return;
		}

		if (!Dungeon.level.adjacent(body.pos, target)) {
			if (ctx.heroFX) {
				GLog.w(Messages.get(this, "too_far"));
			}
			return;
		}

		if (body.rooted) {
			if (ctx.heroFX) {
				PixelScene.shake(1, 1f);
				GLog.w(Messages.get(this, "bad_location"));
			}
			return;
		}

		if (Dungeon.level.solid[target] || Actor.findChar(target) != null) {
			if (ctx.heroFX) {
				GLog.w(Messages.get(this, "bad_location"));
			}
			return;
		}

		if (UseContext.canWorldFx(body)) {
			Sample.INSTANCE.play(Assets.Sounds.MISS);
		}
		int from = body.pos;
		if (Dungeon.level.map[from] == Terrain.OPEN_DOOR) {
			Door.leave(from);
		}

		AfterImage image = new AfterImage();
		image.pos = from;
		image.alignment = body.alignment;
		GameScene.add(image);
		// Headless tests have no GameScene — still register the actor for aggro/defense
		if (image.sprite == null) {
			Actor.add(image);
		}
		image.syncOwner(kit, body);

		Invisibility.dispel(body);
		if (UseContext.canWorldFx(body)) {
			body.pos = target;
			Dungeon.level.occupyCell(body);
			body.sprite.jump(from, target, 0, 0.1f, null);
		} else {
			body.move(target, false);
		}
		if (ctx.heroFX) {
			kit.spend(1f);
			kit.next();
		}

		int imageAttackPos;
		Char enemyTarget = resolveFeintEnemy(ctx, body);
		if (enemyTarget != null) {
			imageAttackPos = enemyTarget.pos;
		} else {
			imageAttackPos = image.pos + (image.pos - target);
		}
		if (UseContext.canWorldFx(body) && image.sprite != null) {
			// do a purely visual attack
			body.sprite.parent.add(new Delayer(0f) {
				@Override
				protected void onComplete() {
					image.sprite.attack(imageAttackPos, new Callback() {
						@Override
						public void call() {
							// do nothing, attack is purely visual
						}
					});
				}
			});
		}

		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if ((m.isTargeting(body) && m.state == m.HUNTING) ||
					(m.alignment == Char.Alignment.ENEMY && m.state != m.PASSIVE
							&& Dungeon.level.distance(m.pos, image.pos) <= 2)) {
				m.aggro(image);
			}
		}

		armor.charge -= chargeUse(kit);
		armor.updateQuickslot();
	}

	/** Same enemy resolution for Hero (TargetHealthIndicator) and Echo (player). */
	private static Char resolveFeintEnemy(UseContext ctx, Char body) {
		Char fromUi = TargetHealthIndicator.instance != null
				? TargetHealthIndicator.instance.target()
				: null;
		if (fromUi != null && fromUi != body
				&& fromUi.alignment != body.alignment) {
			return fromUi;
		}
		if (!ctx.heroFX && Dungeon.hero != null && Dungeon.hero.isAlive()
				&& Dungeon.hero != body) {
			return Dungeon.hero;
		}
		return null;
	}

	@Override
	public Talent[] talents() {
		return new Talent[] { Talent.FEIGNED_RETREAT, Talent.EXPOSE_WEAKNESS, Talent.COUNTER_ABILITY,
				Talent.HEROIC_ENERGY };
	}

	public static class AfterImage extends Mob {

		{
			spriteClass = AfterImageSprite.class;
			defenseSkill = 0;

			properties.add(Property.IMMOVABLE);

			alignment = Alignment.ALLY;
			state = PASSIVE;

			HP = HT = 1;

			// fades just before the hero's next action
			actPriority = Actor.HERO_PRIO + 1;
		}

		private Hero ownerKit;
		private Char ownerBody;

		@Override
		public String name() {
			return ""; // shouldn't be examinable
		}

		@Override
		public String description() {
			return ""; // shouldn't be examinable
		}

		@Override
		public boolean canInteract(Char c) {
			return false;
		}

		@Override
		protected boolean act() {
			destroy();
			sprite.die();
			return true;
		}

		public void syncOwner(Hero kit, Char body) {
			this.ownerKit = kit;
			this.ownerBody = body;
			if (kit != null && cooldown() != kit.cooldown()) {
				spendConstant(kit.cooldown() - cooldown());
			}
		}

		/** @deprecated use {@link #syncOwner(Hero, Char)} */
		@Deprecated
		public void syncToHero(Hero hero) {
			syncOwner(hero, hero);
		}

		@Override
		public void damage(int dmg, Object src) {

		}

		@Override
		public int defenseSkill(Char enemy) {
			Char body = ownerBody != null ? ownerBody : Dungeon.hero;
			Hero kit = ownerKit != null ? ownerKit : Dungeon.hero;
			if (enemy != body && body != null && enemy.alignment != body.alignment) {
				if (enemy instanceof Mob) {
					((Mob) enemy).clearEnemy();
				}
				Buff.affect(enemy, FeintConfusion.class, 1);
				if (enemy.sprite != null)
					enemy.sprite.showLost();
				if (kit != null && kit.hasTalent(Talent.FEIGNED_RETREAT)) {
					Buff.prolong(body, Haste.class, 2f * kit.pointsInTalent(Talent.FEIGNED_RETREAT));
				}
				if (kit != null && kit.hasTalent(Talent.EXPOSE_WEAKNESS)) {
					Buff.prolong(enemy, Vulnerable.class, 2f * kit.pointsInTalent(Talent.EXPOSE_WEAKNESS));
					Buff.prolong(enemy, Weakness.class, 2f * kit.pointsInTalent(Talent.EXPOSE_WEAKNESS));
				}
				if (kit != null && kit.hasTalent(Talent.COUNTER_ABILITY)) {
					Buff.prolong(kit, Talent.CounterAbilityTacker.class, 3f);
				}
			}
			return 0;
		}

		@Override
		public boolean add(Buff buff) {
			return false;
		}

		{
			immunities.addAll(new BlobImmunity().immunities());
		}

		@Override
		public CharSprite sprite() {
			CharSprite s = super.sprite();
			((AfterImageSprite) s).updateArmor();
			return s;
		}

		public static class FeintConfusion extends FlavourBuff {

		}

		public static class AfterImageSprite extends MirrorSprite {
			@Override
			public void updateArmor() {
				updateArmor(6); // we can assume heroic armor
			}

			@Override
			public void resetColor() {
				super.resetColor();
				alpha(0.6f);
			}

			@Override
			public void die() {
				// don't interrupt current animation to start fading
				// this ensures the fake attack animation plays
				if (parent != null) {
					parent.add(new AlphaTweener(this, 0, 3f) {
						@Override
						protected void onComplete() {
							AfterImageSprite.this.killAndErase();
						}
					});
				}
			}
		}

	}
}
