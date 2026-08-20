> **Related:** [NAMING.md](NAMING.md) · [architecture.md](../architecture.md) · [echo-policy (backend)](../../../hero-echoes/docs/features/echo-policy/README.md) · [echo-policy client contract](../../../hero-echoes/docs/features/echo-policy/client.md)

# Hero vs EchoBoss — Gap Analysis

Compares the player [`Hero`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java) with the boss [`EchoBoss`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/EchoBoss.java). Design: a captured kit fights as a `Mob` on the actor clock; it is not a second player.

Hunting AI is [`EchoPolicyStatusBuilder`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy/EchoPolicyStatusBuilder.java) → [`EchoPolicyMatcher`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy/EchoPolicyMatcher.java) → [`EchoRoleExecutor`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy/EchoRoleExecutor.java). Server generates `echo_policy`; the client matches and executes. Flat-action stubs (`decideAction`, `wantsToHeal`, `PolicyInterpreter`, 50-turn armor cooldown) are gone.

---

## Executive Summary

**Shipped:** Capture and restore, all boss depths (5/10/15/20/25), combat-stat and proc delegation through the kit `Hero`, role-based policy AI plus a Java untouchable floor, adapters for potions/scrolls/wands/throws/armor ults/duelist abilities/cleric spells/cloak/chains/horn, class+armor-tier sprite, i18n intro/defeat banners, fight recorder hooks, online lookup/upload.

**Still a gap:** Subclass _identity_ that lives on `Hero.this` during player attacks does not all land on the on-stage body. Gladiator Combo and Duelist ComboStrikeTracker increment in [`Hero.onAttackComplete`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java) / `shoot()`, which EchoBoss melee never calls. Berserk shield is applied on the kit in `defenseProc`, while [`Char.attack`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/Char.java) reads `Berserk` / `Preparation` on the **body**. Some artifacts and alchemy spells have no executor adapter.

**Architecture:** `EchoBoss extends Mob` holds a detached kit `Hero` from [`EchoHeroSnapshot.restoreHero`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/EchoHeroSnapshot.java) (`live()` then bundle restore). Combat queries run inside `withEchoHeroCombat`: copy body `pos` / `paralysed` / `sprite` / `alignment` / HP onto the kit, run the Hero method, copy HP and new combat-proc buffs back via [`EchoCombatBuffTransfer`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action/EchoCombatBuffTransfer.java). Kit chargers stay on the Actor clock via `scheduleEchoKitBuffs`.

---

## Feature Parity Checklist

| Category                      | Hero                                 | EchoBoss                                                                                                                                                                                                                                                                               | Status                                                              |
| ----------------------------- | ------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| **HP**                        | `Hero.updateHT()` + rings/elixirs    | `scaledHT()` × 1.3 × depth bonus; body HP separate from kit                                                                                                                                                                                                                            | ✅ Buffed on purpose                                                |
| **Attack accuracy**           | `Hero.attackSkill()`                 | Delegates to kit                                                                                                                                                                                                                                                                       | ✅                                                                  |
| **Evasion**                   | `Hero.defenseSkill()`                | Overrides `defenseSkill(Char)` → kit (copies `paralysed`)                                                                                                                                                                                                                              | ✅                                                                  |
| **Damage**                    | `Hero.damageRoll()`                  | Delegates to kit                                                                                                                                                                                                                                                                       | ✅                                                                  |
| **Damage reduction**          | `Hero.drRoll()`                      | Kit `drRoll(body)` so body Barkskin counts                                                                                                                                                                                                                                             | ✅                                                                  |
| **Attack speed**              | `Hero.attackDelay()`                 | Delegates to kit                                                                                                                                                                                                                                                                       | ✅                                                                  |
| **Move speed**                | `Hero.speed()`                       | Kit `combatSpeed(body)` so body Haste counts                                                                                                                                                                                                                                           | ✅                                                                  |
| **Weapon enchants**           | `Hero.attackProc` → `wep.proc`       | Same via kit; sprite borrowed so VFX does not NPE                                                                                                                                                                                                                                      | ✅                                                                  |
| **Armor glyphs**              | `Hero.defenseProc` → `armor.proc`    | Same via kit; Earthroot on **body** still absorbs                                                                                                                                                                                                                                      | ✅                                                                  |
| **Talent on-hit**             | `Talent.onAttackProc`                | Runs on kit during `attackProc`                                                                                                                                                                                                                                                        | 🟡 Enemy-side procs work; self-buffs stay on kit unless transferred |
| **Subclass passives**         | Combo, Berserk, Sniper mark, …       | Partial — see [§7](#7-special-class-mechanics)                                                                                                                                                                                                                                         | 🟡                                                                  |
| **Armor abilities**           | Real `ArmorAbility`                  | [`EchoArmorAbilityAdapter`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action/EchoArmorAbilityAdapter.java) when policy picks `ARMOR_ABILITY`                                                                                                        | ✅                                                                  |
| **Weapon abilities**          | Duelist charged skills               | [`EchoDuelistAdapter`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action/EchoDuelistAdapter.java)                                                                                                                                                    | ✅                                                                  |
| **Ranged / missiles**         | `Hero.shoot()`                       | [`EchoThrowAdapter`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action/EchoThrowAdapter.java); Spirit Bow; staff zap                                                                                                                                 | ✅                                                                  |
| **Wands / scrolls**           | Player input                         | [`EchoWandAdapter`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action/EchoWandAdapter.java) / [`EchoScrollAdapter`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action/EchoScrollAdapter.java)                      | ✅                                                                  |
| **Healing / drink**           | Inventory                            | Policy `HEAL` / self-drink roles via [`EchoPotionAdapter`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action/EchoPotionAdapter.java)                                                                                                                 | ✅                                                                  |
| **Cleric spells**             | Holy Tome                            | [`EchoClericAdapter`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action/EchoClericAdapter.java) (Guiding Light, Holy Weapon/Ward, Smite, Sunray, Lay on Hands)                                                                                       | ✅ Covered spells                                                   |
| **Equipment display**         | Class sheet + armor tier             | [`EchoBossSprite`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/EchoBossSprite.java) same film (zap / fly / read); SPD has no weapon overlay                                                                                                              | ✅                                                                  |
| **Visual distinction**        | Player camera                        | Username in `name()`, boss bar; no class tint                                                                                                                                                                                                                                          | ✅ By chrome, not tint                                              |
| **Talents / inventory**       | Live hero                            | Restored on kit; policy spends kit belongings                                                                                                                                                                                                                                          | ✅                                                                  |
| **AI**                        | Player                               | Policy match/execute; untouchable Java floor; `MELEE` falls through to Mob hunting                                                                                                                                                                                                     | ✅                                                                  |
| **Boss depths 5/10/15/20/25** | N/A                                  | Regional boss levels + [`EchoBossSpawner`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/boss/EchoBossSpawner.java); [`EchoBossLevel`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/EchoBossLevel.java) is save-compat only | ✅                                                                  |
| **Echo on boss kill**         | N/A                                  | [`EchoCaptureTrigger`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/EchoCaptureTrigger.java) from Goo / Tengu / DM-300 / Dwarf King / Yog and echo death                                                                                               | ✅                                                                  |
| **Ankh revive**               | Player `WndResurrect`                | Kit ankhs revive the boss in place (blessed = cure + invuln)                                                                                                                                                                                                                           | ✅                                                                  |
| **Leaderboard**               | N/A                                  | `trackTurn` / `trackDamageDealt` / `trackDamageTaken`; defeat and player-death victory                                                                                                                                                                                                 | ✅                                                                  |
| **Online**                    | N/A                                  | [`EchoClient`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/EchoClient.java), [`CompositeEchoLookup`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online/CompositeEchoLookup.java), ranked upload              | ✅                                                                  |
| **Hard stun / door evasion**  | Planned fight rules                  | Not shipped — [hard-stun-and-evasion](../../../hero-echoes/docs/features/echo-policy/hard-stun-and-evasion.md)                                                                                                                                                                         | ❌                                                                  |
| **Remaining artifacts**       | Hourglass, rose, sandals, toolkit, … | No adapter; executor refuses unknown classes                                                                                                                                                                                                                                           | ❌                                                                  |

---

## 1. Stats (HP, Armor, Evasion, Accuracy, Damage, Speed)

Init requires combat data + a supported policy. `defenseSkill` seed is `echoHero.defenseSkill(null)`; live evasion is the `defenseSkill(Char)` override.

Delegated: `damageRoll`, `attackSkill`, `defenseSkill`, `drRoll`, `attackDelay`, `speed` (`combatSpeed(this)`), `attackProc`, `defenseProc`, `canSurpriseAttack`.

Body-only extras in `defenseProc`: Earthroot armor on the **boss** still absorbs if the kit has no copy.

HP scaling stays `BOSS_HP_MULTIPLIER` (1.3) × 2% per depth.

---

## 2. Equipment & Inventory

Capture serializes the full hero (`Echo.fromHero` → `Hero.storeInBundle`). Restore is [`EchoHeroSnapshot.restoreHero`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/EchoHeroSnapshot.java) (`hero.live()` then restore). Fight kits get [`refillCharges`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/EchoHeroSnapshot.java) so drained captures still zap / cloak / ult.

Runtime: policy reads [`EchoInventory.availableIds`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy/EchoInventory.java) (charges, cloak, class armor, duelist charger). Effects apply through adapters onto the **body**; items come from the kit.

No mid-fight equip swap. Unsupported item classes fail closed (no consume) — see `EchoRoleExecutorSupportMatrixTest`.

---

## 3. Abilities & Talents

Talents on the kit drive delegated `attackSkill` / `damageRoll` / `Talent.onAttackProc`.

Armor / weapon / cleric / cloak actions are **policy roles**, not a global cooldown. Ready-gates live in `EchoInventory` (armor charge, duelist `Charger`, wand `canZap`).

---

## 4. Combat Mechanics

| Mechanic                        | Hero                                                               | EchoBoss                                                                                                                                                                                                                                                                                     |
| ------------------------------- | ------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Melee flow                      | `Hero.act` → attack anim → `Hero.onAttackComplete` → `Hero.attack` | Hunting / policy `MELEE` → `doAttack` → `Char.attack` on **body** → kit `attackProc`                                                                                                                                                                                                         |
| Enchant / glyph / on-hit talent | In `Hero.attackProc` / `defenseProc`                               | Same methods on kit inside `withEchoHeroCombat`                                                                                                                                                                                                                                              |
| Combo / ComboStrikeTracker      | `Hero.onAttackComplete` and `shoot()`                              | **Not called** on EchoBoss melee                                                                                                                                                                                                                                                             |
| Berserk / Preparation damage    | `Char.attack` reads buffs on **attacker**                          | Body must hold the buff; kit-only Berserk does not multiply body swings                                                                                                                                                                                                                      |
| Combat DoTs from glyphs         | On the hero                                                        | Whitelist moved kit → body (`Burning`, `Chill`, `Frost`, `Ooze`, …)                                                                                                                                                                                                                          |
| Surprise                        | Hero stealth                                                       | `canSurpriseAttack` from kit; `Mob.surprisedBy` still vs the player                                                                                                                                                                                                                          |
| `Property.INORGANIC`            | Organic                                                            | Not set (only `Property.BOSS`)                                                                                                                                                                                                                                                               |
| VFX                             | Sprite on scene                                                    | [`Char.canWorldFx`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/Char.java) / [`EchoActionContext.canWorldFx`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action/EchoActionContext.java); kit borrows body sprite during procs |

---

## 5. AI Behavior

Each **HUNTING** turn in [`EchoBoss.act()`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/EchoBoss.java):

1. Skip if `busy` (throw/zap VFX) or `paralysed`.
2. Update FOV; last-seen / door-stall / blind-defense bookkeeping.
3. `fightRecorder.trackTurn()`.
4. Sense → [`EchoPolicyMatcher.choose`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy/EchoPolicyMatcher.java) → [`EchoRoleExecutor.execute`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy/EchoRoleExecutor.java).
5. If nothing spent: [`EchoUntouchable`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy/EchoUntouchable.java) floor (shield → run → prep → last ranged). Never idle.
6. Else `super.act()` (standard hunting melee).

Sleeping / wandering still uses default Mob AI. `MELEE` (`*melee`) is an explicit fallthrough.

Movement is hazard-aware (`EchoAoeDots`, plant cover on kite). `Mob.FLEEING` is not used.

---

## 6. Visual / Sprite

[`EchoBossSprite`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/EchoBossSprite.java) uses the class spritesheet and armor tier (`linkVisuals` / `EchoBoss.sprite()`). Animations match [`HeroSprite`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/HeroSprite.java): attack, zap, fly (leap/lunge), read. Invisible echoes fully hide (not 0.4α). No blood burst.

Intro/defeat lines are `Messages.get(EchoBoss.class, …)` from [`EchoBossSpawner`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/boss/EchoBossSpawner.java).

---

## 7. Special Class Mechanics

| Class        | Hero                                          | EchoBoss                                                                                                                                                                                |
| ------------ | --------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Warrior**  | Combo (Gladiator), Berserk, parry, armor ults | Armor ults via policy. Combo **not** incremented on melee. Berserk attaches on **kit** `defenseProc` — shield/damageFactor do not sit on the body.                                      |
| **Mage**     | Staff zap, wands, Elemental Blast             | Staff + wand adapters; armor ult when policy asks                                                                                                                                       |
| **Rogue**    | Cloak, Preparation, Smoke Bomb                | [`EchoCloakAdapter`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action/EchoCloakAdapter.java) (Preparation on **body**); Smoke Bomb via armor adapter |
| **Huntress** | Spirit Bow, missiles, Natures Power           | Bow/throw adapters; kiting via `KEEP_DISTANCE`                                                                                                                                          |
| **Duelist**  | Weapon abilities, ComboStrikeTracker          | Abilities via adapter. ComboStrikeTracker **not** incremented on melee                                                                                                                  |
| **Cleric**   | Tome spells, trinity                          | Adapter for the wired spells; Guiding Light vs player uses `attackerIsCleric` on EchoBoss                                                                                               |

Death loot is regional ([`EchoBossRegionalDeath`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/boss/EchoBossRegionalDeath.java)), not class flavor drops.

---

## 8. Capture vs Runtime

| Data                              | Used at fight time?       | Notes                                                                 |
| --------------------------------- | ------------------------- | --------------------------------------------------------------------- |
| `heroClass`, `lvl`, `ht`          | Sprite, scaling, messages | ✅                                                                    |
| Full `echoData`                   | Restored kit              | Required; spawn throws without it                                     |
| Talents / subclass / armorAbility | Kit queries + adapters    | Combo/Berserk identity still split — [§7](#7-special-class-mechanics) |
| Buffs at capture                  | `live()` then restore     | Stale fight buffs cleared; kit chargers re-scheduled                  |
| Backpack                          | Policy + adapters         | ✅ for supported classes                                              |
| Policy JSON                       | Matcher / executor        | Required; unsupported → no echo spawn                                 |
| Version gate                      | Storage / lookup          | Incompatible echoes skipped                                           |
| Game seed                         | Stored                    | Not used as a separate AI RNG                                         |

---

## 9. Infrastructure

### Implemented

| Piece                        | Key types                                                                                                                                                                                                                                                                                   |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Echo DTO / snapshot          | [`Echo`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/Echo.java), [`EchoHeroSnapshot`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/EchoHeroSnapshot.java)                                                                  |
| Capture                      | [`EchoCaptureTrigger`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/EchoCaptureTrigger.java) on all regional bosses + echo death                                                                                                                            |
| Lookup / pending             | [`EchoReplacementDecider`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/EchoReplacementDecider.java), [`Dungeon.prefetchEchoBossForDepth`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Dungeon.java), `pendingEcho` + `pendingEchoPolicy` |
| Spawn                        | [`EchoBossSpawner`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/boss/EchoBossSpawner.java) on regional boss floors                                                                                                                                         |
| Regional rewards / lock time | [`EchoBossRegionalDeath`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/boss/EchoBossRegionalDeath.java)                                                                                                                                                     |
| Policy                       | [`heroechoes/policy`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy)                                                                                                                                                                                  |
| Adapters                     | [`heroechoes/action`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action)                                                                                                                                                                                  |
| Online                       | [`heroechoes/online`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/online)                                                                                                                                                                                  |
| Leaderboard                  | [`EchoFightRecorder`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/boss/EchoFightRecorder.java)                                                                                                                                                             |
| UI                           | `WndEchoes`, `WndEchoDetail`                                                                                                                                                                                                                                                                |

`Dungeon.isEchoBossActive()` is pending echo **and** policy (whole sealed arena).

### Remaining gaps

| Gap                                          | Why it still matters                                                                                                                                                                      |
| -------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Gladiator Combo / Duelist ComboStrikeTracker | Incremented only in `Hero.onAttackComplete` / `shoot`                                                                                                                                     |
| Berserk (and similar kit-only identity)      | Applied on kit; `Char.attack` / shielding read the body                                                                                                                                   |
| `EchoCombatBuffTransfer` whitelist           | Moves glyph DoTs/debuffs, not Combo / Berserk / Barrier / SnipersMark / MonkEnergy                                                                                                        |
| Artifact / spell adapters                    | Cloak, chains, horn, tome, class armor, duelist weapons, stones, bombs, wands, potions, scrolls — not hourglass, Dried Rose, sandals, toolkit, talisman, waterskin, alchemy `Spell`, food |
| Echo-fight stun/evasion rules                | Capped stuns, shared immunity, first guaranteed stun-hit, door half-evasion — specified, not implemented                                                                                  |

---

## Remaining work (priority)

### P0 — Subclass identity on the body

1. Increment Gladiator `Combo` and Duelist `ComboStrikeTracker` on successful EchoBoss melee/ranged hits (same conditions as `Hero.onAttackComplete` / `shoot`).
2. Keep Berserk (and its shield) on the **body**, or make `Char.attack` / shielding consult the kit. Same question for SnipersMark follow-up shots.
3. Tests: Gladiator melee raises combo on the body; Berserker taken damage multiplies the next body swing; glyph AntiEntropy still burns the body (existing transfer test).

### P1 — Echo-fight stun / evasion

Follow [hard-stun-and-evasion](../../../hero-echoes/docs/features/echo-policy/hard-stun-and-evasion.md).

### P2 — Adapter coverage

Hourglass, Dried Rose, sandals, toolkit, and other active artifacts the arsenal map marks 🟢. Fail closed until an adapter exists.

### P3 — Transfer list (only if P0 needs it)

Widen [`EchoCombatBuffTransfer`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action/EchoCombatBuffTransfer.java) only for buffs that must live on the body; do not move kit identity (Hunger, chargers).

---

## Architecture (what we built)

Stay with `EchoBoss extends Mob`. Do not extend `Hero`.

```
HUNTING act
  → EchoPolicyStatusBuilder.build
  → EchoPolicyMatcher.choose
  → EchoRoleExecutor.execute  (adapters on body, items from kit)
  → else EchoUntouchable floor
  → else Mob hunting (melee)
```

Combat stats/procs: `EchoBoss` overrides → `withEchoHeroCombat` → kit `Hero` methods → HP + combat-proc buffs onto the body.

World VFX: sprite-parent gate, never `Dungeon.hero` identity.

---

## Tests that lock current behavior

| Area                            | Examples                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| ------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Combat delegate                 | [`EchoBossAiAndMechanicsTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/boss/EchoBossAiAndMechanicsTest.java), [`EchoBossHeadlessKitCombatTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/EchoBossHeadlessKitCombatTest.java), [`EchoBossCombatBuffTransferTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/EchoBossCombatBuffTransferTest.java), [`EchoBossCombatSyncTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/EchoBossCombatSyncTest.java) |
| Movement / kiting / untouchable | [`EchoBossMovementTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/EchoBossMovementTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| Policy execute                  | [`EchoRoleExecutorTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy/EchoRoleExecutorTest.java), [`EchoRoleExecutorSupportMatrixTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy/EchoRoleExecutorSupportMatrixTest.java)                                                                                                                                                                                                                                                                                           |
| Depths / spawn                  | [`EchoBossDepthRoutingTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/boss/EchoBossDepthRoutingTest.java), [`EchoBossSpawnerTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/boss/EchoBossSpawnerTest.java)                                                                                                                                                                                                                                                                                                                   |
| Regional death                  | [`EchoBossRegionalDeathTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/boss/EchoBossRegionalDeathTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| Sprite                          | [`EchoBossSpriteTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/sprites/EchoBossSpriteTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |

**Thin coverage:** Gladiator Combo and Berserk on the **body** during EchoBoss melee.

```bash
./gradlew :core:test -q -PerrorProneOff --tests "com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss*"
./gradlew :core:test -q -PerrorProneOff --tests "com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.*"
```

---

## Key file map

| Purpose           | Path                                                                                                                                  |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| Player hero       | [`Hero.java`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java)                                |
| Boss mob          | [`EchoBoss.java`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/EchoBoss.java)                        |
| Kit restore       | [`EchoHeroSnapshot.java`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/EchoHeroSnapshot.java)         |
| Policy execute    | [`EchoRoleExecutor.java`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy/EchoRoleExecutor.java)  |
| Adapters          | [`heroechoes/action`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action)                            |
| Spawn             | [`EchoBossSpawner.java`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/boss/EchoBossSpawner.java)      |
| Sprite            | [`EchoBossSprite.java`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/EchoBossSprite.java)                |
| Depths            | [`EchoReplacementDecider.java`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/EchoReplacementDecider.java) |
| Game architecture | [`architecture.md`](../architecture.md)                                                                                               |

---

**Bottom line:** Echo fights already use hero-shaped numbers, procs, and a playbook brain. Next combat-parity work is **subclass identity on the on-stage body** (Combo / Berserk / related), then the specified stun/evasion rules, then leftover artifact adapters.
