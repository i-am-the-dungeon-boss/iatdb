> **Related:** [PLAN.md](../../PLAN.md) · [online-integration.md](online-integration.md) · [README](README.md)
>
> **Superseded AI notes:** Flat-action stubs (`decideAction`, `wantsToHeal`, `PolicyInterpreter`, armor-ability cooldown) are **removed**. Hunting AI is `EchoPolicyStatusBuilder` → `EchoPolicyMatcher` → `EchoRoleExecutor`. Prefer [online-integration.md](online-integration.md) for the current pipeline; sections below that mention those stubs are historical.

# Hero vs EchoBoss — Gap Analysis & Implementation Plan

This compares the player `Hero` (`core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java`) with the boss `EchoBoss` (`core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/EchoBoss.java`) for the hero-echoes feature. The original design intent is in `PLAN.md` (Task 3: "transform a static Hero data object into a challenging, intelligent Mob").

---

## Executive Summary

**What works today:** Echo capture, local storage, boss replacement, boss HP scaling, combat stat delegation, role-based policy AI (match/execute), class-based sprite tint, settings, intro messaging, and leaderboard plumbing.

**What does not work yet:** Full hero _feel_ in combat — many weapon/armor procs, talents on hit, subclass mechanics, real armor/weapon abilities, and some ranged paths still fall through to mob AI when policy cannot resolve them.

**Architectural note:** `EchoBoss` is a `Mob` holding a detached `Hero echoHero`. Combat rolls partially delegate to `echoHero`, but proc/speed/defense paths run on the `Mob` entity, so enchantments, glyphs, and talent triggers largely never fire.

---

## Feature Parity Checklist

| Category                      | Hero                                                 | EchoBoss                                                        | Status                                    |
| ----------------------------- | ---------------------------------------------------- | --------------------------------------------------------------- | ----------------------------------------- |
| **HP**                        | `Hero.updateHT()` + rings/elixirs                    | `scaledHT()` × 1.3 × depth bonus; boss HP separate from echo    | ✅ Implemented (intentionally buffed)     |
| **Attack accuracy**           | `Hero.attackSkill()` — weapon, rings, talents        | Delegates to `echoHero.attackSkill()`                           | ✅ Partial (rolls work if bundle present) |
| **Evasion**                   | `Hero.defenseSkill()` — armor, rings, talents, parry | `Mob.defenseSkill` flat field; init bug sets `attackSkill`      | ❌ Missing / buggy                        |
| **Damage**                    | `Hero.damageRoll()` — weapon, rings, talents         | Delegates to `echoHero.damageRoll()`                            | ✅ Partial                                |
| **Damage reduction**          | `Hero.drRoll()` — armor + weapon DR                  | Delegates to `echoHero.drRoll()`                                | ✅ Partial                                |
| **Attack speed**              | `Hero.attackDelay()` — weapon, furor, augments       | `Mob.attackDelay()` fixed ~1.0                                  | ❌ Missing                                |
| **Move speed**                | `Hero.speed()` — haste ring, armor, momentum         | `Mob.speed()` base                                              | ❌ Missing                                |
| **Weapon enchants (on hit)**  | `Hero.attackProc()` → `wep.proc()`                   | `Mob.attackProc()` default                                      | ❌ Missing                                |
| **Armor glyphs (on defend)**  | `Hero.defenseProc()` → `armor.proc()`                | `Mob.defenseProc()` default                                     | ❌ Missing                                |
| **Talent combat procs**       | `Talent.onAttackProc`, parry, barrier, etc.          | None on Mob                                                     | ❌ Missing                                |
| **Subclass mechanics**        | Gladiator Combo, Berserker rage, Sniper mark, etc.   | None                                                            | ❌ Missing                                |
| **Armor abilities**           | Full `ArmorAbility` per class                        | Warrior Combo buff + Rogue Invis only, every 50 turns           | 🟡 Stub                                   |
| **Weapon abilities**          | Duelist/Mage/etc. weapon skills                      | None                                                            | ❌ Missing                                |
| **Ranged / missiles**         | `Hero.shoot()`                                       | Mob melee only                                                  | ❌ Missing                                |
| **Wands / scrolls**           | Player-driven                                        | None                                                            | ❌ Missing                                |
| **Healing potions**           | Player uses from inventory                           | `decideAction`/`wantsToHeal` exist, **not called from `act()`** | 🟡 Stub                                   |
| **Equipment display**         | `HeroSprite` layers (weapon, armor)                  | `EchoBossSprite` — class body + tint only                       | 🟡 Partial                                |
| **Talents stored**            | Full tree in bundle                                  | Restored on `echoHero`, not applied to boss Char                | 🟡 Capture only                           |
| **Inventory stored**          | Full `Belongings` bundle                             | Same bundle; boss cannot use items                              | 🟡 Capture only                           |
| **AI**                        | Player input                                         | Standard `Mob` HUNTING; no class logic at runtime               | ❌ Minimal                                |
| **Boss depths 5/10/15/20/25** | N/A                                                  | Only depth 5 routes to `EchoBossLevel`                          | 🟡 Depth 5 only                           |
| **Echo on boss kill**         | N/A                                                  | `EchoCaptureTrigger` on `Goo.die()` only                        | 🟡 Sewer boss only                        |
| **Leaderboard**               | N/A                                                  | Records boss **defeat** only; no combat tracking hooks          | 🟡 Partial                                |
| **Visual distinction**        | Full-color hero                                      | `EchoBossSprite.DISTINCT_TINT = 0.55f`                          | ✅ Done                                   |

---

## 1. Stats (HP, Armor, Evasion, Accuracy, Damage, Speed)

### Hero (`Hero.java`)

- **HP:** `updateHT()` — level, `HTBoost`, `RingOfMight`, elixirs (`Hero.java` ~250–265).
- **Accuracy:** `attackSkill(Char)` — base skill, `RingOfAccuracy`, weapon factor, Precise Assault / Liquid Agility, Scimitar dance (~486–539).
- **Evasion:** `defenseSkill(Char)` — parry, guard, rings, Liquid Agility, armor evasion factor (~543–580).
- **DR:** `drRoll()` — armor DR, weapon defense factor, Hold Fast (~613–635).
- **Damage:** `damageRoll()` — weapon, Ring of Force, Physical Empower, Weapon Recharging (~639–671).
- **Speed:** `speed()` — haste ring, armor, Momentum, Natures Power (~684–710).
- **Attack delay:** `attackDelay()` — weapon delay, furor, augments (~752–781).

### EchoBoss (`EchoBoss.java`)

```96:111:core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/EchoBoss.java
    public void initFromEcho(Echo echo, int depth) {
        // ...
            echoHero.restoreFromBundle(echo.echoData);
            HP = HT = scaledHT(echo, depth);
            defenseSkill = echoHero.attackSkill(null);  // BUG: should be defenseSkill
```

**Implemented:**

- `scaledHT()` — `BOSS_HP_MULTIPLIER` (1.3) + 2% per depth (`EchoBoss.java` ~90–94).
- Overrides: `damageRoll()`, `attackSkill()`, `drRoll()` delegate to `echoHero` (~167–188).

**Missing / broken:**

- `defenseSkill` Mob field wrongly set to **attack** skill (line 104).
- No override of `defenseSkill(Char)` — uses `Mob.defenseSkill` (~678–698 in `Mob.java`), ignoring armor/talents on `echoHero`.
- No `speed()` or `attackDelay()` overrides — boss moves and attacks at generic mob pace.
- Ring/talent bonuses in delegated methods **only work when `echo.echoData` is present**; test fixtures often use `echoData = null` (`EchoTestSupport.warriorEcho`).

**Restoration gap:** `EchoBoss` does not call `echoHero.live()`; `EchoHeroLoader.load()` does (~19 in `EchoHeroLoader.java`). Without `live()`, restored buffs from `Char.restoreFromBundle` may include stale combat buffs from capture time.

---

## 2. Equipment & Inventory

### Capture (`Echo.fromHero`, `Hero.storeInBundle`)

Full hero state is serialized:

- Class, subclass, `armorAbility`, talents (`Talent.storeTalentsInBundle`)
- Equipped: weapon, armor, artifact, misc, ring, second weapon
- Full backpack (`Belongings.storeInBundle` — `Belongings.java` ~176–186)

**Tests:** `EchoAndStorageTest`, `EchoViewerTest.formatDetailsIncludesBackpackFromHeroBundle` — confirms bundle round-trip for UI.

### Runtime (EchoBoss)

- Equipment affects **delegated rolls only** (damage/accuracy/DR) when `echoHero` exists.
- Boss **cannot equip, swap, or consume** inventory items in combat.
- No logic to read `echoHero.belongings.getItem(PotionOfHealing.class)` despite heal AI stubs.

---

## 3. Abilities & Talents

### Hero

- **Talents:** Stored and queried via `hasTalent()` / `pointsInTalent()` (~353+).
- **Armor ability:** `armorAbility` — class-specific (Warrior Heroic Leap, Mage Elemental Blast, Rogue Smoke Bomb, etc.).
- **Weapon abilities:** Duelist stances, charged shots, monk energy, etc.

### EchoBoss

```216:225:core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/EchoBoss.java
    private void useArmorAbility() {
        if (echoHero.heroClass == HeroClass.WARRIOR) {
            Buff.affect(this, Combo.class).hit(Dungeon.hero);
        }
        if (echoHero.heroClass == HeroClass.ROGUE) {
            Buff.affect(this, Invisibility.class, 3f);
        }
    }
```

- Talents restored on `echoHero` but **never drive boss behavior** (no `attackProc` delegation).
- Armor ability: 2 of 6 classes, simplified, applied to **Mob** not through real ability classes.
- No weapon abilities, no talent cooldown tracking, no energy/charge systems.

---

## 4. Combat Mechanics

| Mechanic             | Hero                                                            | EchoBoss                                                          |
| -------------------- | --------------------------------------------------------------- | ----------------------------------------------------------------- |
| Melee attack flow    | `Hero.act()` → attack animation → `attackProc`                  | `Mob.Hunting` → `doAttack` → `Char.attack` → **Mob** `attackProc` |
| Weapon enchant procs | `wep.proc(this, enemy, damage)` in `Hero.attackProc` (~1447)    | Not called                                                        |
| Armor glyph procs    | `armor.proc(enemy, this, damage)` in `Hero.defenseProc` (~1507) | Not called                                                        |
| On-hit talents       | `Talent.onAttackProc` (~1445)                                   | Not called                                                        |
| Surprise / prep      | Hero stealth mechanics                                          | `Mob.surprisedBy` still works for **player** attacking boss       |
| `Property.INORGANIC` | Hero is organic                                                 | Boss has `INORGANIC` (~59) — immune to some effects (bleed, etc.) |

**Implication:** A captured Huntress with +10 bow and flaming enchant will roll bow damage but won't apply enchant procs or Sniper marks.

---

## 5. AI Behavior

### Current runtime (`EchoBoss.act()`)

```202:214:core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/EchoBoss.java
    protected boolean act() {
        if (abilityCooldown > 0) abilityCooldown--;
        if (abilityCooldown <= 0 && Dungeon.hero != null && echoHero != null) {
            useArmorAbility();
            abilityCooldown = ABILITY_COOLDOWN_TURNS;
        }
        return super.act();  // standard Mob AI
    }
```

- Inherits `Mob.act()` → `HUNTING` → path to player → melee (`Mob.java` ~220–264, ~1213–1265).
- **`decideAction()` / `wantsToHeal()` are never called** — only unit-tested (`EchoBossAiAndMechanicsTest`).
- Mage "retreat when threatened" exists in `decideAction` but is dead code.
- No wand/scroll logic, no kiting for Huntress/Mage, no Duelist stance switching.

### PLAN.md intent vs reality

| Planned AI                           | Status                                   |
| ------------------------------------ | ---------------------------------------- |
| Chase and attack                     | ✅ Via `Mob`                             |
| Heal at low HP                       | 🟡 Logic only                            |
| Class-specific positioning           | 🟡 Mage branch in `decideAction`, unused |
| Item usage (potions, scrolls, wands) | ❌                                       |
| Armor / weapon abilities             | 🟡 Minimal stub                          |

---

## 6. Visual / Sprite Representation

### `EchoBossSprite.java`

- Sets class texture (Warrior/Mage/Rogue/Huntress/Duelist/Cleric) in `link()` from `echoHero.heroClass`.
- Applies `DISTINCT_TINT = 0.55f` so boss is visually distinct.
- **Missing vs `HeroSprite`:** weapon overlay, armor tier appearance, ability VFX, sprint animation, attack animations tied to weapon type.

**Test:** `UiUxAndPolishTest.distinctVisualTintApplied`.

---

## 7. Special Class Mechanics

| Class        | Hero capability                            | EchoBoss                              |
| ------------ | ------------------------------------------ | ------------------------------------- |
| **Warrior**  | Combo, parry, Heroic Leap / Endure         | Combo buff on cooldown only; no parry |
| **Mage**     | Staff bolts, Elemental Blast, wand synergy | No ranged; no mage ability            |
| **Rogue**    | Smoke bomb, invis, preparation             | 3-turn invis buff only                |
| **Huntress** | Spirit bow, thrown weapons, Natures Power  | Melee only; no ranged AI              |
| **Duelist**  | Stances, weapon abilities, combo strike    | None                                  |
| **Cleric**   | Holy weapon, trinity, guiding light        | Class drop on death only              |

**Death drops** by class (~244–265) are flavor loot, not combat behavior.

---

## 8. Echo Capture vs Runtime Restoration Gaps

| Data captured                  | Used at boss runtime?                 | Gap                                       |
| ------------------------------ | ------------------------------------- | ----------------------------------------- |
| `heroClass`, `lvl`, `hp`, `ht` | Metadata, scaling, sprite             | ✅                                        |
| Full `echoData` bundle         | Restored to `echoHero`                | ✅ when present                           |
| Talents                        | On `echoHero` only                    | Not wired to Mob combat                   |
| Buffs at capture time          | Restored via `Char.restoreFromBundle` | May be inappropriate; no `live()` cleanup |
| Backpack consumables           | UI viewer only                        | Boss cannot use                           |
| `subClass`                     | Stored                                | Never consulted by AI                     |
| `armorAbility`                 | Stored                                | Minimal stub                              |
| Version compatibility          | `isCompatibleWith()`                  | ✅ storage skips incompatible             |
| Game seed                      | Stored                                | Not used for AI RNG                       |

**Test gap:** Most boss tests use `warriorEcho()` with `echoData = null`, so combat delegation is untested with real equipment bundles.

---

## 9. Infrastructure: Implemented vs Stubbed vs Missing

### ✅ Implemented

| Component             | Key classes                                                   |
| --------------------- | ------------------------------------------------------------- |
| Echo create/save/load | `Echo`, `EchoStorage`, `EchoCaptureTrigger`                   |
| Boss depth decision   | `EchoReplacementDecider`                                      |
| Depth-5 routing       | `Dungeon.levelClassForDepth`, `EchoBossLevel`, `EchoBossRoom` |
| UI viewer             | `WndEchoes`, `WndEchoDetail`, `EchoHeroLoader`                |
| Leaderboard file I/O  | `EchoLeaderboardStorage`, `EchoFightResult`                   |
| Goo echo hook         | `Goo.die()` → `EchoCaptureTrigger.onBossDefeated()`           |

### 🟡 Stubbed (logic exists, not integrated)

| Component       | Location                                                                                                              |
| --------------- | --------------------------------------------------------------------------------------------------------------------- |
| Heal decision   | `EchoBoss.decideAction`, `wantsToHeal`, `consumeHealingPotion`                                                        |
| Armor abilities | `useArmorAbility()` — Warrior/Rogue only                                                                              |
| Intro message   | `EchoMessages.introBannerText` — hardcoded English, not `Messages.get`                                                |
| Fight recorder  | Created in `initFromEcho`; `trackDamage*` / `trackTurn` never called; `recordBossVictory` never called from game code |

### ❌ Missing

| Component                           | Notes                                                                             |
| ----------------------------------- | --------------------------------------------------------------------------------- |
| Boss replacement depths 10/15/20/25 | `Dungeon.levelClassForDepth` only special-cases depth 5                           |
| Heroic boss rooms for other regions | Only `EchoBossRoom` (sewer)                                                       |
| Echo triggers on other bosses       | Only `Goo.java`                                                                   |
| Combat proc delegation              | `attackProc`, `defenseProc`, `defenseSkill`, `speed`, `attackDelay`               |
| Ranged AI                           | No `shoot()` / missile path                                                       |
| Consumable AI                       | Potions, scrolls, wands                                                           |
| Full class AI                       | Per `PLAN.md` Phase 2                                                             |
| Online Hero Echoes integration      | [online-integration.md](online-integration.md) — `EchoClient`, policy interpreter |
| Echo policy interpreter             | `EchoPolicy`, `PolicyInterpreter` — not started                                   |
| Combat integration tests            | No test asserts damage/accuracy from real hero bundle                             |

---

## Priority-Ordered Implementation Tasks

### P0 — Correctness bugs (small, high impact)

1. **Fix `defenseSkill` initialization** in `EchoBoss.initFromEcho()` — use `echoHero.defenseSkill(enemy)` or stored defense skill, not `attackSkill(null)`.
2. **Call `echoHero.live()`** after `restoreFromBundle` (mirror `EchoHeroLoader`) to clear inappropriate buffs.
3. **Override `defenseSkill(Char)`** to delegate to `echoHero.defenseSkill(enemy)` when `echoHero != null`.

**Tests to add:** Boss with bundled warrior + armor asserts evasion > base mob; defense skill ≠ attack skill.

---

### P1 — Core combat parity (make hits feel like the hero)

4. **Override `attackProc`** — delegate to `echoHero.attackProc(enemy, damage)` (may need temporary `echoHero.pos` sync or a dedicated `HeroCombatDelegate` helper).
5. **Override `defenseProc`** — delegate to `echoHero.defenseProc(enemy, damage)`.
6. **Override `attackDelay()`** — `echoHero.attackDelay()`.
7. **Override `speed()`** — `echoHero.speed()` (account for mob ascension modifier if desired).

**Reference pattern:** `ShadowClone.ShadowAlly` normalizes hero damage (~206–209 in `ShadowClone.java`) — useful if direct delegation causes actor/sprite issues.

**Tests:** Echo with known weapon + enchant; assert proc buff appears on target. Echo with glyph; assert glyph proc on boss hit.

---

### P2 — Wire existing AI stubs

8. **Integrate `decideAction()` into `act()`** before `super.act()`:
   - `HEAL` → find `PotionOfHealing` in `echoHero.belongings`, consume, heal `EchoBoss.HP`, `consumeHealingPotion()`.
   - `MOVE` → step away from adjacent hero (Mage kiting).
   - `ATTACK` → fall through to mob hunt.
9. **Detect `hasHealingPotion` / `meleeThreatened`** from real game state, not test-only parameters.

**Tests (extend `EchoBossAiAndMechanicsTest`):** Integration test that `act()` at low HP reduces potion count and raises HP (may need lightweight dungeon fixture).

---

### P3 — Armor & weapon abilities

10. **Replace `useArmorAbility()` stub** with calls to real `ArmorAbility` implementations, using `echoHero` as the logical caster (similar to how allies cast abilities).
11. **Subclass-aware behavior** — read `echoHero.subClass` for Combo (Gladiator), Berserker thresholds, etc.
12. **Weapon ability cooldowns** — track per-ability, not one global 50-turn timer.

**Reference:** `Hero.java` attack flow (~1384–1497) for on-attack talent/subclass triggers.

---

### P4 — Ranged & class positioning

13. **Ranged weapon detection** — if `echoHero` has equipped `MissileWeapon` or Huntress bow, override `doAttack` / add `Hunting` subclass (see `Elemental.java` ~303–350 for ranged mob pattern).
14. **Class AI profiles** — Mage/Huntress keep distance; Warrior/Duelist aggressive; Rogue uses invis/smoke when available.

---

### P5 — Consumables & wands

15. **Scroll usage AI** — e.g. teleport when cornered, mirror image when threatened (read from backpack).
16. **Wand usage** — select best wand from inventory, zap when line-of-sight and out of melee (high complexity; prioritize boss-tier wands).

---

### P6 — Visual & UX polish

17. **Enhance `EchoBossSprite`** — weapon/armor layers from `echoHero.belongings` (or extend `HeroSprite` with tint).
18. **i18n intro banner** — use `Messages.get(EchoMessages.INTRO_BANNER, ...)` instead of hardcoded string in `EchoMessages.introBannerText`.
19. **Review `Property.INORGANIC`** — may be wrong for a hero echo; consider removing for bleed/some talent interactions.

---

### P7 — Feature expansion

20. **All boss depths** — extend `Dungeon.levelClassForDepth` for 10/15/20/25; add `EchoBossRoom` variants per region (or generic heroic boss room).
21. **Echo triggers** — hook `EchoCaptureTrigger.onBossDefeated()` into all regional bosses.
22. **Leaderboard combat hooks** — call `fightRecorder.trackDamageDealt/Taken/trackTurn` from `damage()` override; `recordBossVictory` on player death vs heroic boss.
23. **Online integration** — `EchoClient`, `GET /v1/echoes/{depth}` + `echo_policy`, upload/sync — [online-integration.md](online-integration.md).
24. **Policy interpreter** — evaluate server rules in `EchoBoss.act()`; fallback policy offline.

---

## Recommended Architecture (practical)

Avoid making `EchoBoss extends Hero` (alignment, player input, and actor priority conflicts). Prefer a **combat delegate**:

```java
// Conceptual — not in codebase yet
class EchoCombatDelegate {
    static int damageRoll(EchoBoss boss) { return boss.getEchoHero().damageRoll(); }
    static int attackProc(EchoBoss boss, Char enemy, int dmg) { ... }
    // etc.
}
```

Override all combat-relevant `Mob` methods on `EchoBoss` to forward to `echoHero`, applying results to the **Mob's** HP/buffs where appropriate. Keep `echoHero` off the actor list but treat it as the stat/ability source of truth.

For AI, use a **`PolicyInterpreter`** (evaluates server `echo_policy` each turn) plus bundled fallback policy. Optional `EchoBossBrain` for local fallback only:

- `evaluate(context)` → `IntendedAction` from policy rules
- `executeHeal()`, `executeAbility()`, `executeRanged()` — map policy actions to game mechanics

Server owns policy generation; client owns generic interpreter. See [online-integration.md](online-integration.md).

---

## Tests That Define Expected Behavior Today

| Test class                    | What it asserts                                                      |
| ----------------------------- | -------------------------------------------------------------------- |
| `EndToEndWorkflowTest`        | Echo → depth-5 heroic level → boss HP > echo HT                      |
| `EchoAndStorageTest`          | Echo metadata, bundle round-trip, storage, version gate              |
| `AutoEchoCaptureTest`         | Capture guards and file write                                        |
| `BossReplacementLogicTest`    | `shouldUseEchoBoss` decision matrix                                  |
| `Depth5EchoBossSelectionTest` | Routing, pending echo, save/restore mid-run                          |
| `LevelRoutingTest`            | Depths 1–4 sewer, 5 default boss without echo                        |
| `EchoBossAiAndMechanicsTest`  | HP scaling, heal **decision** logic, potion cap — **not runtime AI** |
| `UiUxAndPolishTest`           | Tint, intro key                                                      |
| `EchoViewerTest`              | UI labels, backpack from bundle                                      |
| `LeaderboardFeatureTest`      | Recorder/storage API (manual calls, not combat)                      |

**Not yet tested:** Real combat stats from bundled hero, proc effects, ability usage, ranged attacks, item consumption in `act()`, depths beyond 5.

---

## Suggested TDD Sequence (per workspace rules)

1. RED: Test bundled warrior with tier-4 armor → boss `defenseSkill(hero)` reflects armor evasion.
2. GREEN: Fix init + `defenseSkill(Char)` override.
3. RED: Test weapon enchant proc on boss attack.
4. GREEN: `attackProc` delegation.
5. RED: Test low-HP boss consumes healing potion in `act()`.
6. GREEN: Wire `decideAction` + inventory scan.

Run: `./gradlew :core:test --tests "com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoBossAiAndMechanicsTest"`

---

## Key File Reference Map

| Purpose             | Path                                                              |
| ------------------- | ----------------------------------------------------------------- |
| Player hero         | `core/src/main/java/.../actors/hero/Hero.java`                    |
| Boss mob            | `core/src/main/java/.../actors/mobs/EchoBoss.java`                |
| Echo model          | `core/src/main/java/.../heroechoes/Echo.java`                     |
| Echo trigger        | `core/src/main/java/.../heroechoes/EchoCaptureTrigger.java`       |
| Level routing       | `core/src/main/java/.../Dungeon.java` (~364–431)                  |
| Boss room spawn     | `core/src/main/java/.../levels/rooms/sewerboss/EchoBossRoom.java` |
| Sprite              | `core/src/main/java/.../sprites/EchoBossSprite.java`              |
| Hero restore for UI | `core/src/main/java/.../windows/EchoHeroLoader.java`              |
| Mob AI base         | `core/src/main/java/.../actors/mobs/Mob.java`                     |
| Design plan         | `PLAN.md`                                                         |
| Online integration  | `docs/hero-echoes/online-integration.md`                          |
| Backend API spec    | `hero-echoes-backend/docs/` (sibling repo)                        |
| Tests               | `core/src/test/java/.../heroechoes/*Test.java`                    |

---

**Bottom line:** Infrastructure and "boss shows up with roughly correct melee numbers" are in place. Next work: **combat delegation** (P0–P1), **policy interpreter + EchoClient** (Phase 3 online), then depth expansion and leaderboard combat tracking. Server-generated `echo_policy` replaces hard-coded class AI as the primary combat brain for online echoes.
