package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Levitation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stamina;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.AiItemActions;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStamina;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.Callback;
import com.watabou.utils.DeviceCompat;
import org.json.JSONObject;

/**
 * Executes a resolved role via SPD item/movement APIs (canvas §9).
 * Inventory from {@code echoHero}; effects/VFX/turn on {@link EchoBoss}.
 *
 * @return true if the turn was spent; false to let the boss fall through (e.g.
 *         melee).
 */
public final class EchoRoleExecutor {

	private EchoRoleExecutor() {
	}

	public static boolean execute(
			EchoBoss boss,
			EchoPolicy policy,
			EchoPolicyStatus status,
			EchoPolicyChoice choice) {
		JSONObject caps = policy.root().optJSONObject("capabilities");
		JSONObject cap = caps != null ? caps.optJSONObject(choice.useRole) : null;
		java.util.Set<String> available = EchoInventory.availableIds(boss.getEchoHero());
		String itemId = EchoRoleResolver.resolveItemId(cap, available);
		if (itemId == null) {
			debugExec("resolve miss role=" + choice.useRole + " available=" + available);
			return false;
		}
		debugExec("resolve role=" + choice.useRole + " → item=" + itemId);

		if (itemId.startsWith("*")) {
			boolean ok = executeVirtual(boss, status, itemId);
			debugExec("virtual " + itemId + " → " + (ok ? "spent" : "fallthrough"));
			return ok;
		}

		boolean aoe = cap != null && cap.has("hazard") && !cap.optString("hazard").isEmpty();
		int cell = EchoTargetPicker.pick(boss, status, itemId, aoe);
		if (needsAimCell(itemId) && cell < 0) {
			debugExec("no aim cell for " + itemId);
			return false;
		}

		Item item = EchoInventory.find(boss.getEchoHero(), itemId);
		if (item == null) {
			debugExec("inventory miss item=" + itemId);
			return false;
		}

		if (item instanceof Potion) {
			boolean ok = executePotion(boss, (Potion) item, choice.useRole, cell);
			debugExec("potion " + itemId + " cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof Wand) {
			int aim = cell >= 0 ? cell : Dungeon.hero.pos;
			boolean ok = executeWand(boss, (Wand) item, aim);
			debugExec("wand " + itemId + " cell=" + aim + " charges=" + ((Wand) item).curCharges
					+ " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof SpiritBow) {
			int aim = cell >= 0 ? cell : (Dungeon.hero != null ? Dungeon.hero.pos : -1);
			boolean ok = executeSpiritBow(boss, (SpiritBow) item, aim);
			debugExec("spirit bow cell=" + aim + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof MagesStaff) {
			// Staff zap still needs an AI path (imbued wand is private); fall through.
			debugExec("ranged weapon fallthrough item=" + itemId);
			return false;
		}
		debugExec("unsupported item class=" + item.getClass().getSimpleName());
		return false;
	}

	private static void debugExec(String message) {
		if (DeviceCompat.isDebug()) {
			DeviceCompat.log("EchoBoss", "exec " + message);
		}
	}

	/** Self-drink potions apply on the boss; others are thrown at a cell. */
	private static boolean isSelfDrinkPotion(String itemId) {
		return "PotionOfHealing".equals(itemId)
				|| "PotionOfShielding".equals(itemId)
				|| "ElixirOfHoneyedHealing".equals(itemId)
				|| "PotionOfPurity".equals(itemId)
				|| "PotionOfFrost".equals(itemId)
				|| "PotionOfHaste".equals(itemId)
				|| "PotionOfStamina".equals(itemId)
				|| "PotionOfInvisibility".equals(itemId)
				|| "PotionOfCleansing".equals(itemId)
				|| "PotionOfLevitation".equals(itemId);
	}

	private static boolean isSelfDrinkRole(String role) {
		return "HEAL".equals(role)
				|| "CLEANSE_BURN".equals(role)
				|| "CLEANSE".equals(role)
				|| "PURITY".equals(role)
				|| "HASTE".equals(role)
				|| "INVIS".equals(role)
				|| "LEVITATE".equals(role);
	}

	private static boolean needsAimCell(String itemId) {
		return (itemId.startsWith("PotionOf") || itemId.startsWith("ElixirOf"))
				&& !isSelfDrinkPotion(itemId);
	}

	private static boolean executeVirtual(EchoBoss boss, EchoPolicyStatus status, String tag) {
		Hero enemy = Dungeon.hero;
		if ("*wait".equals(tag)) {
			return true;
		}
		if ("*melee".equals(tag)) {
			return false;
		}
		if ("*move_further".equals(tag)) {
			return enemy != null && boss.policyStepFurther(enemy.pos);
		}
		if ("*move_closer".equals(tag)) {
			return enemy != null && boss.policyStepCloser(enemy.pos);
		}
		if (tag.startsWith("*move_to_terrain:")) {
			String terrain = tag.substring("*move_to_terrain:".length());
			Integer cell = status.terrainNearCell.get(terrain);
			return cell != null && boss.policyStepCloser(cell);
		}
		return false;
	}

	/**
	 * Shared potion pattern: detach from echo inventory → self-apply on boss or
	 * throw at cell via {@link Item#castVisual}.
	 */
	private static boolean executePotion(EchoBoss boss, Potion potion, String role, int cell) {
		Hero echoHero = boss.getEchoHero();

		boolean selfDrink = isSelfDrinkRole(role);
		if (!selfDrink && (cell < 0 || Dungeon.level == null)) {
			return false;
		}

		potion.detach(echoHero.belongings.backpack);
		if (selfDrink) {
			return applySelfDrink(boss, echoHero, potion, role);
		}
		return throwPotion(boss, echoHero, potion, cell);
	}

	/**
	 * Uses {@link Item#castVisual} (same missile path as hero {@link Item#cast}),
	 * then {@link Item#onThrow} on arrival. Instant when boss sprite is off-stage.
	 */
	private static boolean throwPotion(EchoBoss boss, Hero echoHero, Potion potion, int cell) {
		potion.setCurrent(echoHero);
		Callback shatter = () -> AiItemActions.withUser(echoHero, potion,
				() -> AiItemActions.onThrow(potion, cell));

		if (!canPlayThrowVisual(boss, cell)) {
			shatter.call();
			return true;
		}
		potion.castVisual(boss.sprite, boss.pos, cell, shatter);
		return true;
	}

	private static boolean canPlayThrowVisual(EchoBoss boss, int cell) {
		return boss.sprite != null
				&& boss.sprite.parent != null
				&& (boss.sprite.visible
						|| (Dungeon.level != null && cell >= 0
								&& cell < Dungeon.level.heroFOV.length
								&& Dungeon.level.heroFOV[cell]));
	}

	/** Effects target the EchoBoss mob, not the phantom echo hero. */
	private static boolean applySelfDrink(EchoBoss boss, Hero echoHero, Potion potion, String role) {
		if ("HEAL".equals(role)) {
			if (potion instanceof PotionOfShielding) {
				Buff.affect(boss, Barrier.class).setShield((int) (0.6f * boss.HT + 10));
				return true;
			}
			if (potion instanceof PotionOfHealing || potion instanceof ElixirOfHoneyedHealing) {
				PotionOfHealing.cure(boss);
				PotionOfHealing.heal(boss);
				return true;
			}
			return false;
		}
		if ("CLEANSE_BURN".equals(role)) {
			Buff.detach(boss, Burning.class);
			potion.setCurrent(echoHero);
			AiItemActions.withUser(echoHero, potion, () -> potion.shatter(boss.pos));
			return true;
		}
		if ("CLEANSE".equals(role)) {
			PotionOfCleansing.cleanse(boss);
			return true;
		}
		if ("PURITY".equals(role)) {
			Buff.prolong(boss, BlobImmunity.class, BlobImmunity.DURATION);
			return true;
		}
		if ("HASTE".equals(role)) {
			if (potion instanceof PotionOfStamina) {
				Buff.prolong(boss, Stamina.class, Stamina.DURATION);
			} else {
				Buff.prolong(boss, Haste.class, Haste.DURATION);
			}
			return true;
		}
		if ("INVIS".equals(role)) {
			Buff.prolong(boss, Invisibility.class, Invisibility.DURATION);
			return true;
		}
		if ("LEVITATE".equals(role)) {
			Buff.prolong(boss, Levitation.class, Levitation.DURATION);
			return true;
		}
		return false;
	}

	/**
	 * Shoot without {@link SpiritBow.SpiritArrow#cast} (that path uses hero
	 * {@code spendAndNext}). Uses {@link Item#castVisual} from the boss when
	 * on-stage; otherwise applies the hit immediately (tests / off-screen).
	 */
	private static boolean executeSpiritBow(EchoBoss boss, SpiritBow bow, int cell) {
		Hero echoHero = boss.getEchoHero();
		Hero enemy = Dungeon.hero;
		if (enemy == null || Dungeon.level == null || cell < 0)
			return false;

		SpiritBow.SpiritArrow arrow = bow.knockArrow();
		int throwCell = arrow.throwPos(boss.pos, cell);
		Char found = Actor.findChar(throwCell);
		if (found == null && throwCell == enemy.pos) {
			found = enemy;
		}
		if (found == null || found == echoHero || found == boss)
			return false;

		final Char target = found;
		Callback onArrive = () -> applySpiritBowShot(boss, arrow, target);

		if (boss.sprite != null && boss.sprite.parent != null
				&& (boss.sprite.visible || (target.sprite != null && target.sprite.visible))) {
			arrow.castVisual(boss.sprite, boss.pos, cell, onArrive);
		} else {
			onArrive.call();
		}
		return true;
	}

	/** Sync phantom hero pos/sprite, then {@link Hero#shoot}. */
	private static void applySpiritBowShot(EchoBoss boss, SpiritBow.SpiritArrow arrow, Char target) {
		Hero echoHero = boss.getEchoHero();
		int savedPos = echoHero.pos;
		var savedSprite = echoHero.sprite;
		echoHero.pos = boss.pos;
		// Phantom echo hero is never placed on the level; Char.attack hit VFX
		// calls attacker.sprite.center() when the defender has a sprite.
		echoHero.sprite = boss.sprite;
		try {
			AiItemActions.withUser(echoHero, arrow, () -> echoHero.shoot(target, arrow));
		} finally {
			echoHero.sprite = savedSprite;
			echoHero.pos = savedPos;
		}
	}

	/**
	 * Manual zap pipeline: Ballistica → onZap → AI charge spend.
	 * Does not call {@link Wand#wandUsed()} (that spends the phantom echo hero).
	 */
	private static boolean executeWand(EchoBoss boss, Wand wand, int cell) {
		Hero echoHero = boss.getEchoHero();
		if (Dungeon.hero == null || wand.curCharges <= 0)
			return false;

		int savedPos = echoHero.pos;
		echoHero.pos = boss.pos;
		try {
			Ballistica shot = new Ballistica(boss.pos, cell, wand.collisionProperties(cell));
			wand.setCurrent(echoHero);
			final boolean[] ok = { false };
			AiItemActions.withUser(echoHero, wand, () -> {
				if (wand.tryToZap(echoHero, cell)) {
					wand.onZap(shot);
					wand.spendChargesForAi();
					ok[0] = true;
				}
			});
			return ok[0];
		} finally {
			echoHero.pos = savedPos;
		}
	}

}
