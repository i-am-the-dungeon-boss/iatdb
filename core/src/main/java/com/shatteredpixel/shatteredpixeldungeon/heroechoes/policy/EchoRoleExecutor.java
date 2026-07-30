package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDragonsBreath;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.InventoryStone;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.watabou.utils.DeviceCompat;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Executes a resolved role via SPD item/movement APIs (canvas §9).
 * Inventory from {@code echoHero}; effects/VFX/turn on {@link EchoBoss}
 * via shared {@link UseContext} paths ({@code drinkAs}/{@code throwAs}/
 * {@code zapAs}/{@code activateAs}/{@code readAs}).
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
		String itemId = choice.itemId != null
				? choice.itemId
				: EchoRoleResolver.resolveItemId(cap, available);
		if (itemId == null || (choice.itemId != null && !EchoRoleResolver.isAvailable(itemId, available))) {
			debugExec("resolve miss role=" + choice.useRole + " available=" + available);
			return false;
		}
		debugExec("resolve role=" + choice.useRole + " → item=" + itemId);

		if (itemId.startsWith("*")) {
			boolean ok = executeVirtual(boss, policy, status, itemId);
			debugExec("virtual " + itemId + " → " + (ok ? "spent" : "fallthrough"));
			return ok;
		}

		Item item = EchoInventory.find(boss.getEchoHero(), itemId);
		if (item == null) {
			debugExec("inventory miss item=" + itemId);
			return false;
		}

		boolean doorBreak = "DOOR_BREAK".equals(choice.useRole)
				|| "door_break".equals(choice.layer);
		int cell;
		if (doorBreak) {
			cell = boss.doorStallCell();
			debugExec("door_break aim cell=" + cell);
		} else {
			cell = EchoTargetPicker.pick(boss, status, itemId, isSplashAimHazard(cap));
		}

		boolean spent;
		if (item instanceof Potion) {
			spent = executePotion(boss, (Potion) item, choice.useRole, cell);
			debugExec("potion " + itemId + " cell=" + cell + " → " + (spent ? "spent" : "fail"));
		} else {
			spent = executeNonPotion(boss, item, itemId, cell, choice, cap);
		}
		if (spent && doorBreak) {
			boss.clearDoorStall();
		}
		if (spent && !doorBreak && !status.enemyInLos) {
			boss.consumeBlindDefenseShot();
		}
		return spent;
	}

	/**
	 * Shared non-potion branches; extracted so door_break can clear stall after
	 * success.
	 */
	private static boolean executeNonPotion(
			EchoBoss boss,
			Item item,
			String itemId,
			int cell,
			EchoPolicyChoice choice,
			JSONObject cap) {
		if (item instanceof Scroll) {
			boolean ok = ((Scroll) item).readAs(UseContext.echo(boss));
			debugExec("scroll " + itemId + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof ClassArmor) {
			boolean ok = executeArmorAbility(boss, (ClassArmor) item, cell);
			debugExec("armor ability " + itemId + " cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof Wand) {
			boolean ok = cell >= 0 && Dungeon.level != null
					&& ((Wand) item).zapAs(UseContext.echo(boss), cell);
			debugExec("wand " + itemId + " cell=" + cell + " charges=" + ((Wand) item).curCharges
					+ " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof SpiritBow) {
			// Visible melee: fall through to mob AI. Cloaked hero: keep blind-defense
			// shots.
			if (adjacentToVisibleHero(boss)) {
				debugExec("spirit bow refused at melee");
				return false;
			}
			boolean ok = cell >= 0 && Dungeon.level != null
					&& ((SpiritBow) item).knockArrow().throwAs(UseContext.echo(boss), cell);
			debugExec("spirit bow cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof MagesStaff) {
			boolean ok = cell >= 0 && Dungeon.level != null
					&& ((MagesStaff) item).zapAs(UseContext.echo(boss), cell);
			debugExec("staff zap cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof MeleeWeapon && !(item instanceof MagesStaff)) {
			Hero kit = boss.getEchoHero();
			if (kit != null && kit.heroClass == HeroClass.DUELIST) {
				MeleeWeapon weapon = (MeleeWeapon) item;
				Integer target = weapon.targetingPrompt() != null
						? (cell >= 0 ? cell : null)
						: null;
				if (weapon.targetingPrompt() != null && target == null) {
					debugExec("no aim cell for melee ability " + itemId);
					return false;
				}
				boolean ok = weapon.abilityAs(UseContext.echo(boss), target);
				debugExec("melee ability " + itemId + " cell=" + target + " → " + (ok ? "spent" : "fail"));
				return ok;
			}
		}
		if (item instanceof MissileWeapon || item instanceof Bomb || isThrowableRunestone(item)) {
			if (cell < 0 || Dungeon.level == null) {
				debugExec("no aim cell for " + itemId);
				return false;
			}
			boolean ok = item.throwAs(UseContext.echo(boss), cell);
			debugExec("throwable " + itemId + " cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof InventoryStone) {
			boolean ok = ((InventoryStone) item).useAs(UseContext.echo(boss));
			debugExec("inventory stone " + itemId + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof HolyTome) {
			boolean ok = executeHolyTome(boss, (HolyTome) item, cap, cell);
			debugExec("holy tome cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof CloakOfShadows) {
			boolean ok = ((CloakOfShadows) item).useAs(UseContext.echo(boss));
			debugExec("artifact CloakOfShadows → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof HornOfPlenty) {
			boolean ok = ((HornOfPlenty) item).useAs(UseContext.echo(boss));
			debugExec("artifact HornOfPlenty → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof EtherealChains) {
			if (cell < 0) {
				debugExec("artifact EtherealChains no aim");
				return false;
			}
			boolean ok = ((EtherealChains) item).useAs(UseContext.echo(boss), cell);
			debugExec("artifact EtherealChains cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		debugExec("unsupported item class=" + item.getClass().getSimpleName());
		return false;
	}

	/** Inventory stones need a bag UI; throwable runestones activate on land. */
	private static boolean isThrowableRunestone(Item item) {
		return item instanceof Runestone && !(item instanceof InventoryStone);
	}

	/**
	 * Visible melee adjacency — refuse SpiritBow point-blank. Cloaked heroes are
	 * excluded so blind-defense shots can still land / dispel invisibility.
	 */
	private static boolean adjacentToVisibleHero(EchoBoss boss) {
		Hero enemy = Dungeon.hero;
		return enemy != null
				&& enemy.invisible <= 0
				&& Dungeon.level != null
				&& Dungeon.level.adjacent(boss.pos, enemy.pos);
	}

	private static void debugExec(String message) {
		if (DeviceCompat.isDebug()) {
			DeviceCompat.log("EchoBoss", "exec " + message);
		}
	}

	/**
	 * Self-drink when the role is an explicit drink role (dual-mode / must-throw
	 * exceptions like CLEANSE_BURN+Frost), or the potion's default action is
	 * {@link Potion#AC_DRINK} (not must-throw / choose).
	 */
	private static boolean shouldSelfDrink(Potion potion, String role) {
		if (isThrowRole(role)) {
			return false;
		}
		if (isSelfDrinkRole(role)) {
			return true;
		}
		return Potion.AC_DRINK.equals(potion.defaultAction());
	}

	/**
	 * Dual-mode ({@code AC_CHOOSE}) and must-throw potions that policy still
	 * drinks via role (e.g. Purity, Cleansing, Frost cleanse).
	 */
	private static boolean isSelfDrinkRole(String role) {
		return "HEAL".equals(role)
				|| "DRINK".equals(role)
				|| "CLEANSE_BURN".equals(role)
				|| "CLEANSE".equals(role)
				|| "PURITY".equals(role)
				|| "HASTE".equals(role)
				|| "INVIS".equals(role)
				|| "LEVITATE".equals(role);
	}

	/** Force shatter / throw regardless of potion default action. */
	private static boolean isThrowRole(String role) {
		return "THROW".equals(role)
				|| "THROW_POTION".equals(role)
				|| "GAS".equals(role);
	}

	/**
	 * Only known splash hazards use neighbour-of-hero aim. Unknown strings
	 * (e.g. legacy debug {@code "aoe"}) must not offset point throwables.
	 */
	private static boolean isSplashAimHazard(JSONObject cap) {
		if (cap == null) {
			return false;
		}
		String hazard = cap.optString("hazard", "");
		return EchoPolicyHazards.FIRE_AOE.equals(hazard)
				|| EchoPolicyHazards.PAYOFF_AOE.equals(hazard);
	}

	/**
	 * {@code apply(Char)} is a no-op on non-Hero — refuse without consuming.
	 */
	private static boolean isHeroOnlyDrink(Potion potion) {
		return potion instanceof PotionOfStrength
				|| potion instanceof PotionOfExperience
				|| potion instanceof ElixirOfMight
				|| potion instanceof PotionOfMindVision
				|| potion instanceof PotionOfMagicalSight;
	}

	private static boolean executeVirtual(
			EchoBoss boss, EchoPolicy policy, EchoPolicyStatus status, String tag) {
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
		if ("*leave_aoe".equals(tag)) {
			if (enemy == null) {
				return false;
			}
			boolean kite = EchoPolicyMatcher.wantsKeepDistance(policy, status);
			return boss.policyStepOutOfAoe(enemy.pos, kite);
		}
		if (tag.startsWith("*move_to_terrain:")) {
			String terrain = tag.substring("*move_to_terrain:".length());
			Integer cell = status.terrainNearCell.get(terrain);
			return cell != null && boss.policyStepCloser(cell);
		}
		return false;
	}

	/**
	 * Potion execute: self-drink via {@link Potion#drinkAs}, throw via
	 * {@link Item#throwAs}.
	 */
	private static boolean executePotion(EchoBoss boss, Potion potion, String role, int cell) {
		UseContext ctx = UseContext.echo(boss);
		// Targeted cone — not self-drink / shatter
		if (potion instanceof PotionOfDragonsBreath) {
			if (cell < 0 || Dungeon.level == null) {
				return false;
			}
			return ((PotionOfDragonsBreath) potion).breatheAs(ctx, cell);
		}
		if (shouldSelfDrink(potion, role)) {
			if (isHeroOnlyDrink(potion)) {
				return false;
			}
			return potion.drinkAs(ctx);
		}
		if (cell < 0 || Dungeon.level == null) {
			return false;
		}
		return potion.throwAs(ctx, cell);
	}

	/** ClassArmor charge skill via {@link ArmorAbility#activateAs}. */
	private static boolean executeArmorAbility(EchoBoss boss, ClassArmor armor, int cell) {
		Hero kit = boss.getEchoHero();
		ArmorAbility ability = kit != null ? kit.armorAbility : null;
		if (ability == null) {
			return false;
		}
		Integer target = null;
		// Match activateAs: a non-null targetingPrompt requires a cell, even when
		// useTargeting() is false (ShadowClone / SpiritHawk / PowerOfMany).
		if (ability.targetingPrompt() != null) {
			if (cell < 0) {
				return false;
			}
			target = cell;
		}
		return ability.activateAs(UseContext.echo(boss), armor, target);
	}

	private static boolean executeHolyTome(EchoBoss boss, HolyTome tome, JSONObject cap, int cell) {
		ClericSpell spell = resolveClericSpell(cap);
		if (spell == null) {
			return false;
		}
		Integer target = null;
		if (spell.targetingFlags() != -1) {
			if (cell < 0 || Dungeon.level == null) {
				return false;
			}
			target = cell;
		}
		return tome.castAs(UseContext.echo(boss), spell, target);
	}

	/**
	 * Reads optional {@code spell} on capability; else first items entry that maps
	 * to a spell.
	 */
	static ClericSpell resolveClericSpell(JSONObject cap) {
		if (cap == null) {
			return null;
		}
		String spellName = cap.optString("spell", "");
		if (!spellName.isEmpty()) {
			ClericSpell spell = ClericSpell.bySimpleName(spellName);
			if (spell != null) {
				return spell;
			}
		}
		JSONArray items = cap.optJSONArray("items");
		if (items != null) {
			for (int i = 0; i < items.length(); i++) {
				String id = items.optString(i, "");
				if ("HolyTome".equals(id)) {
					continue;
				}
				ClericSpell spell = ClericSpell.bySimpleName(id);
				if (spell != null) {
					return spell;
				}
			}
		}
		return null;
	}

}
