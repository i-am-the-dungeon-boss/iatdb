package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoArmorAbilityAdapter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoClericAdapter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoChainsAdapter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoCloakAdapter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoDuelistAdapter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoHornAdapter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoInventoryStoneAdapter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoPotionAdapter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoScrollAdapter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoThrowAdapter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoWandAdapter;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDragonsBreath;
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
 * Executes a resolved role via SPD item/movement APIs.
 * <p>
 * Design record: {@code hero-echoes/docs/features/echo-policy.md} § "Phase 3 — Execute".
 * Inventory from {@code echoHero}; effects/VFX/turn on {@link EchoBoss}
 * via fork-owned adapters ({@link EchoThrowAdapter}, {@link EchoWandAdapter},
 * {@link EchoPotionAdapter}, {@link EchoScrollAdapter}, and related action adapters).
 *
 * @return true if the turn was spent; false to let the boss fall through (e.g.
 *         melee).
 */
public final class EchoRoleExecutor {

	private EchoRoleExecutor() {
	}

	/**
	 * Narrows a capability to the items that can still affect this hero. Under
	 * the shared stun lockout the rest of {@code SETUP_CC} is fine, but stun
	 * items would be thrown away — so they are dropped from the pick list
	 * rather than the whole role being disabled. One of the client-owned rules
	 * catalogued in {@link EchoPolicySafety}.
	 */
	static JSONObject capForEnemy(String role, JSONObject cap, EchoPolicyStatus status) {
		if (cap == null || status == null) {
			return cap;
		}
		if (EchoPolicyHazards.SETUP_CC.equals(role)
				&& status.enemyStatuses.contains(EchoPolicyHazards.PARALYSIS_IMMUNITY)) {
			return EchoPolicySafety.withoutParalyticGas(cap);
		}
		return cap;
	}

	public static boolean execute(
			EchoBoss boss,
			EchoPolicy policy,
			EchoPolicyStatus status,
			EchoPlan plan) {
		JSONObject caps = policy.root().optJSONObject("capabilities");
		JSONObject cap = capForEnemy(plan.useRole, caps != null ? caps.optJSONObject(plan.useRole) : null, status);
		java.util.Set<String> available = EchoInventory.availableIds(boss.getEchoHero());
		String itemId = plan.itemId != null
				? plan.itemId
				: EchoRoleResolver.resolveItemId(cap, available);
		if (itemId == null || (plan.itemId != null && !EchoRoleResolver.isAvailable(itemId, available))) {
			debugExec("resolve miss role=" + plan.useRole + " available=" + available);
			return false;
		}
		debugExec("resolve role=" + plan.useRole + " → item=" + itemId);

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

		// The plan wins: only the sense phase knows a role's own geometry (the
		// blocking plant, the bush on the line). Aim is re-derived only for roles
		// that carry none.
		int cell;
		if (plan.targetCell >= 0) {
			cell = plan.targetCell;
			debugExec("plan aim role=" + plan.useRole + " cell=" + cell);
		} else {
			cell = EchoTargetPicker.pick(boss, status, itemId, isSplashAimHazard(cap));
		}

		boolean spent;
		if (item instanceof Potion) {
			spent = executePotion(boss, (Potion) item, plan.useRole, cell);
			debugExec("potion " + itemId + " cell=" + cell + " → " + (spent ? "spent" : "fail"));
		} else {
			spent = executeNonPotion(boss, item, itemId, cell, cap);
		}
		if (spent && !status.enemyInLos) {
			boss.consumeBlindDefenseShot();
		}
		return spent;
	}

	/** Shared non-potion branches. */
	private static boolean executeNonPotion(
			EchoBoss boss,
			Item item,
			String itemId,
			int cell,
			JSONObject cap) {
		if (item instanceof Scroll) {
			boolean ok = EchoScrollAdapter.read(boss, (Scroll) item);
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
					&& EchoWandAdapter.zap(boss, (Wand) item, cell);
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
					&& EchoThrowAdapter.throwItem(boss, ((SpiritBow) item).knockArrow(), cell);
			debugExec("spirit bow cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof MagesStaff) {
			boolean ok = cell >= 0 && Dungeon.level != null
					&& EchoWandAdapter.zapStaff(boss, (MagesStaff) item, cell);
			debugExec("staff zap cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		// MagesStaff already returned above, so plain MeleeWeapon is unambiguous here.
		if (item instanceof MeleeWeapon) {
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
				boolean ok = EchoDuelistAdapter.useAbility(boss, weapon, target);
				debugExec("melee ability " + itemId + " cell=" + target + " → " + (ok ? "spent" : "fail"));
				return ok;
			}
		}
		if (item instanceof MissileWeapon || item instanceof Bomb || isThrowableRunestone(item)) {
			if (cell < 0 || Dungeon.level == null) {
				debugExec("no aim cell for " + itemId);
				return false;
			}
			boolean ok = EchoThrowAdapter.throwItem(boss, item, cell);
			debugExec("throwable " + itemId + " cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof InventoryStone) {
			boolean ok = EchoInventoryStoneAdapter.use(boss, (InventoryStone) item);
			debugExec("inventory stone " + itemId + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof HolyTome) {
			boolean ok = executeHolyTome(boss, (HolyTome) item, cap, cell);
			debugExec("holy tome cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof CloakOfShadows) {
			boolean ok = EchoCloakAdapter.toggleStealth(boss, (CloakOfShadows) item);
			debugExec("artifact CloakOfShadows → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof HornOfPlenty) {
			boolean ok = EchoHornAdapter.snack(boss, (HornOfPlenty) item);
			debugExec("artifact HornOfPlenty → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof EtherealChains) {
			if (cell < 0) {
				debugExec("artifact EtherealChains no aim");
				return false;
			}
			boolean ok = EchoChainsAdapter.cast(boss, (EtherealChains) item, cell);
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
		EchoRole known = EchoRole.byId(role);
		return known != null && known.drinksPotion();
	}

	/** Force shatter / throw regardless of potion default action. */
	private static boolean isThrowRole(String role) {
		EchoRole known = EchoRole.byId(role);
		return known != null && known.throwsPotion();
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
			// A kiting echo wants a harmful plant between itself and the hero —
			// Level.pressCell triggers it for the hero too, so it is real cover.
			boolean kite = EchoPolicyMatcher.wantsKeepDistance(policy, status);
			// The playbook only arms kite_step when RANGED is ready, so the step
			// is being spent to buy a shot. Refuse one that lands where no shot
			// exists: that is the door-dance loop, and standing to fight beats it.
			boolean requireLineOfFire = status.isRoleReady(EchoPolicyHazards.RANGED);
			return enemy != null
					&& boss.policyStepFurther(enemy.pos, kite, requireLineOfFire);
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
	 * Potion execute: self-drink via {@link EchoPotionAdapter}, throw via
	 * {@link EchoThrowAdapter}.
	 */
	private static boolean executePotion(EchoBoss boss, Potion potion, String role, int cell) {
		// Targeted cone — not self-drink / shatter
		if (potion instanceof PotionOfDragonsBreath) {
			if (cell < 0 || Dungeon.level == null) {
				return false;
			}
			return EchoPotionAdapter.breathe(boss, (PotionOfDragonsBreath) potion, cell);
		}
		if (shouldSelfDrink(potion, role)) {
			return EchoPotionAdapter.drink(boss, potion);
		}
		if (cell < 0 || Dungeon.level == null) {
			return false;
		}
		return EchoPotionAdapter.throwPotion(boss, potion, cell);
	}

	/** ClassArmor charge skill via {@link com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoArmorAbilityAdapter}. */
	private static boolean executeArmorAbility(EchoBoss boss, ClassArmor armor, int cell) {
		Hero kit = boss.getEchoHero();
		ArmorAbility ability = kit != null ? kit.armorAbility : null;
		if (ability == null) {
			return false;
		}
		Integer target = null;
		// Match adapter: a non-null targetingPrompt requires a cell, even when
		// useTargeting() is false (ShadowClone / SpiritHawk / PowerOfMany).
		if (ability.targetingPrompt() != null) {
			if (cell < 0) {
				return false;
			}
			target = cell;
		}
		return EchoArmorAbilityAdapter.activate(boss, armor, ability, target);
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
		return EchoClericAdapter.cast(boss, tome, spell, target);
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
