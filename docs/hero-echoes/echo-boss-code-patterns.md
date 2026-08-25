> **Related:** [README.md](README.md) · [echo-boss-gap-analysis.md](echo-boss-gap-analysis.md) · [NAMING.md](NAMING.md) · [architecture.md](../architecture.md) · [echo-policy (backend)](../../../hero-echoes/docs/features/echo-policy/README.md)

# EchoBoss vs Hero — Code Pattern Analysis

## What this document is

[echo-boss-gap-analysis.md](echo-boss-gap-analysis.md) answers **"does the echo do what the hero does?"** (feature parity).

This document answers a different question: **"is the echo _built_ the way the hero is built?"** — the shape of the code, where the two shapes agree, where they diverge, which divergences are load-bearing and which are accidents, and what to change so the feature is cheap to extend and hard to break.

Scope: everything under [`heroechoes/`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes), plus [`EchoBoss`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/EchoBoss.java), [`EchoBossSprite`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/EchoBossSprite.java), and the `*EchoBridge` shims that sit beside base-game classes.

Rough size, for calibration:

| Surface                                    |     LOC |
| ------------------------------------------ | ------- |
| `heroechoes/**` (main)                     | ~13,300 |
| `EchoBoss` + `EchoBossSprite`              |  ~1,490 |
| `*EchoBridge` shims in base-game packages  |    ~910 |
| Echo tests                                 | ~26,500 across 121 files |

---

## 1. The two architectures, side by side

The hero is an **input-driven turn machine**. The echo is a **policy-driven turn machine**. Almost every difference below follows from that one fact — the question is which ones follow *necessarily*.

| Layer                    | Hero                                                                                                                    | EchoBoss                                                                                    | Same shape?                          |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- | ------------------------------------ |
| **Turn entry**           | [`Hero.act()`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java)                  | `EchoBoss.act()`                                                                              | ✅ both are `Actor.act()` overrides  |
| **Wait-for-input gate**  | `ready` / `busy()`                                                                                                      | `busy` / `busy()` / `cancelBusy()`                                                            | ✅ deliberately mirrored             |
| **Intent**               | `curAction` — a typed `HeroAction.Move` / `.Attack` / …                                                                 | `EchoPolicyChoice` — role string + layer string                                               | ⚠️ same role, very different typing  |
| **Intent source**        | player taps → `CellSelector` → `handle(cell)`                                                                          | server JSON playbook → `EchoPolicyMatcher`                                                    | ✅ divergence is the whole point     |
| **Sense**                | implicit (the player's eyes)                                                                                            | `EchoPolicyStatusBuilder` → `EchoPolicyStatus`                                                | ✅ necessary addition                |
| **Dispatch to effect**   | `instanceof HeroAction.X` in `act()`, then **virtual dispatch** into the item                                            | centralized `instanceof` chains in `Echo*Handlers`                                            | ❌ opposite direction                |
| **Item behaviour**       | `Item.execute` / `Scroll.doRead` / `ClericSpell.onCast` / `ArmorAbility.activate` / `MeleeWeapon.duelistAbility`         | reimplemented per type in `EchoScrollHandlers` / `EchoClericHandlers` / `EchoArmorHandlers` / `EchoDuelistAdapter` | ❌ duplicated                        |
| **Who owns stats**       | the `Hero` itself                                                                                                       | a second, off-stage `Hero` (the "phantom kit") wrapped by `withEchoHeroCombat`                | ❌ unique to the echo                |
| **Sprite**               | `HeroSprite`, owned by the `Hero`                                                                                       | `EchoBossSprite` on the body, **mirrored** onto the kit                                       | ⚠️ two `Char`s share one sprite      |
| **Turn cost**            | `spend()` / `spendAndNext()` inside each action                                                                          | either `runChoice` spends, or the adapter's `ctx.complete()` does                              | ❌ two protocols                     |
| **Persistence**          | full `Hero.storeInBundle`                                                                                               | `Echo` bundle + policy JSON + 2 scratch ints                                                   | ⚠️ most AI state is not saved        |

### The echo's turn, end to end

```
EchoBoss.act()
  ├─ mirrorKitSprite()            re-assert the kit's sprite mirror
  ├─ scheduleEchoKitBuffs()       put kit buffs back on the Actor clock
  ├─ if (busy) return false       wait for the VFX callback   ← Hero.ready analogue
  ├─ paralysed / state != HUNTING → super.act()
  ├─ updateFieldOfView + note door pursuit / blind-defense
  ├─ EchoPolicyStatusBuilder.build(...)          PHASE 1  SENSE
  ├─ forceStalledDoor(status)     spend DOOR_BREAK on a door that keeps shutting (§2.7)
  ├─ EchoPolicyMatcher.choose(...)               PHASE 2  MATCH
  │     reactions → recipes → positioning → matchups → default
  ├─ EchoRoleExecutor.execute(...)               PHASE 3  EXECUTE
  │     role → capability JSON → itemId string → instanceof chain → adapter
  ├─ forcedUntouchableAct(...)                   PHASE 4  JAVA FLOOR
  └─ super.act()                                 PHASE 5  fall through to Mob hunting AI
```

Phases 1–3 are the designed pipeline; 4 and 5 are floors under it. That layering is sound and worth keeping — every problem below lives *inside* phase 3 or in the state the phases share.

---

## 2. Where the design is consistent — keep these

These are deliberate, documented, and paying for themselves. They should survive any refactor.

**2.1 The busy/ready handshake is a faithful port.**
`EchoBoss.busy()` / `isBusy()` / `spendAndNext()` mirror `Hero.busy()` / `ready` / `spendAndNext()` one-for-one, including "return `false` from `act()` while waiting for a missile callback". Anyone who understands the hero's turn pump understands the echo's. `vfxOwnsTurn` is the only addition, and it exists for a real reason: `runChoice` must not double-spend a turn that a VFX callback already paid for.

**2.2 Combat delegation through one funnel.**
Every combat override — `damageRoll`, `attackSkill`, `defenseSkill`, `drRoll`, `attackDelay`, `speed`, `attackProc`, `defenseProc` — goes through `withEchoHeroCombat`. One place saves/restores kit `pos`, `paralysed`, `alignment`, `HP`, `HT`, `fieldOfView`, mirrors HP back to the body, and moves new combat buffs via [`EchoCombatBuffTransfer`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action/EchoCombatBuffTransfer.java). Single choke point, symmetric save/restore in a `finally`. This is the best-engineered part of the feature.

**2.3 `EchoRole` as the single vocabulary.**
[`EchoRole`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy/EchoRole.java) collapses "is this a damage role / blob role / prep role / does it drink or throw a potion / which virtual tag backs it" into one enum carrying a `Kind` and a `prepRank`. `EchoUntouchable.PREP_ORDER` is *derived* from `prepRank()`, so membership and ordering cannot disagree. `byId()` is the single string→enum boundary. This is the right pattern — and it is the template for fixing everything in §3.

**2.4 The Java floor under the wire playbook.**
[`EchoUntouchable`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy/EchoUntouchable.java) guarantees the rung (shield → run → prep → fight) while JSON only chooses *which* item. A stale or hostile playbook cannot make the echo stand still. Good trust boundary, well documented — including the non-obvious reason the shield rung prefers "shield up and keep swinging" over running.

**2.5 `EchoActionContext` narrows the kit.**
Making the kit `Hero` private behind `gear()` (inventory) and `stats()` (calculator), with `showEnchant` / `attackFx` / `zapFx` so drawing on the kit is not expressible at the call site, is the right instinct. [`EchoKitEscapeTest`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/action/EchoKitEscapeTest.java) backstops the residual. See §3.3 for why it is only half-enforced.

**2.6 Comment quality.**
Nearly every non-obvious branch carries a *why* plus the incident that caused it (ANDROID-20/21, Family A, ANDROID-1T). That is unusually good and is the main reason this analysis was possible at all.

**2.7 One stated home for the rules the playbook must not vote on.**
[`EchoPolicySafety`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/heroechoes/policy/EchoPolicySafety.java) is the client half of the wire split. The playbook is generated once, off the device, from a kit card, so it owns *with what* — item lists, their order, the numbers behind them — and cannot own anything that depends on the board this turn. Membership test: **would a wrong answer be the playbook's fault or the board's?** If the board's, it belongs here, and its backend counterpart stays a knob or an item list, never a reaction. Current members:

| Rule | Client | Backend |
| ---- | ------ | ------- |
| An item whose blast covers the echo at this range is dropped | `EchoPolicySafety.withoutSelfBlast` | item list only |
| Stun items are dropped while the hero is stun-immune | `EchoPolicySafety.withoutParalyticGas` | item list only |
| *When* a door that keeps shutting in the echo's face comes down | `EchoBoss.forceStalledDoor` | `tuning.door_force_turns`, `capabilities.DOOR_BREAK` |

The value of naming the category is that the next such rule joins the table instead of becoming a third one-off `if` in whichever class noticed the problem.

---

## 3. Where it is inconsistent — ranked by bug risk

### 3.1 The match/execute seam loses the target — and it has already cost behaviour ❗

`EchoPolicyChoice` carries `useRole`, `layer`, `recipeId`, `itemId`. **It does not carry a target cell.** So `EchoRoleExecutor` re-derives aim from scratch, with one hard-coded special case:

```java
// EchoRoleExecutor.execute
if (doorBreak) { cell = boss.doorStallCell(); }
else           { cell = EchoTargetPicker.pick(boss, status, itemId, isSplashAimHazard(cap)); }
```

`EchoTargetPicker.pick` only ever aims at the hero (or a neighbour, for splash). Consequences visible in the current tree:

- **`CLEAR_PLANT` aims at the wrong cell.** *(Fixed — see §5.1.)* `EchoPolicyStatusBuilder.sensePlantBlocked` computes the blocking plant's cell, stores it via `EchoBoss.setPlantBlockerCell`, and `isPlantBlockerAimable` even verifies a projectile reaches it. Then `EchoRoleExecutor` never reads `plantBlockerCell()` — the role fires its item at the hero instead of at the plant. Sense knows; execute cannot see.
- **`CLEAR_LOS` is dead in a different way.** `EchoBoss.pathBlockerCell` / `losBlockerCell` and `EchoPolicyHazards.PATH_BLOCKED` / `LOS_BLOCKED` are declared and **never written or read anywhere in `core/src`**. `virtualRoleFeasible` falls through to `default: return true`, so `CLEAR_LOS` reports *always ready*, and a playbook naming it gets an aimless shot at the hero. *(Fixed — see §5.1 and §5.6.)*
- ~~`door_break` works only because it got a bespoke `if` in the executor.~~ *(Gone — the executor's special case is deleted. `EchoBoss.forceStalledDoor` decides when a door has stalled the echo, builds a `DOOR_BREAK` plan carrying the door cell, and runs it through the ordinary executor; the reaction and the `door_stalling` predicate are retired. The knock count is `tuning.door_force_turns`, and the item is resolved through `EchoPolicySafety.withoutSelfBlast` so a point-blank blast tool is never the answer — see §2.7.)*

**Root cause:** `EchoPolicyChoice` is a role *name*, not a resolved *plan*. The sense phase is the only phase that knows the geometry, and its only channel to the execute phase is mutable fields on `EchoBoss`.

### 3.2 Five parallel type-switches replace five virtual dispatch points ❗

The base game extends behaviour by overriding a method on the item. The echo extends behaviour by adding an `if (x instanceof Y)` to a central file:

| Base-game extension point                    | Echo replacement                    | `instanceof` count |  LOC |
| -------------------------------------------- | ----------------------------------- | ------------------ | ---- |
| `ArmorAbility.activate(armor, hero, target)` | `EchoArmorHandlers.activate`        |                 30 | 1,962 |
| `MeleeWeapon.duelistAbility(hero, target)`   | `EchoDuelistAdapter.dispatchAbility`|                 32 |   799 |
| `ClericSpell.onCast(tome, hero)`             | `EchoClericHandlers.cast`           |                 50 |   910 |
| `Scroll.doRead()`                            | `EchoScrollHandlers.read`           |                 15 |   433 |
| `Item.execute` / throw / zap                 | `EchoRoleExecutor.executeNonPotion` |                 15 |   419 |

Two things follow.

**Adding one item means editing a central file that everything else routes through.** New duelist weapon → edit `EchoDuelistAdapter`. New cleric spell → edit `EchoClericHandlers`. The compiler never tells you that you missed one; the role simply logs `unsupported item class=…` and the turn falls through to melee.

**The formulas are copied, so upstream drift is silent.** `Rapier.duelistAbility` computes `augment.damageFactor(5 + Math.round(1.5f*buffedLvl()))`; `EchoDuelistAdapter` re-encodes the same literal `5` and `1.5f` in its own branch — for roughly 25 weapons. The same pattern at the armor-ability layer:

|                                    | `HeroicLeap.activate` (base) | `EchoArmorHandlers.heroicLeap` |
| ---------------------------------- | ---------------------------- | ------------------------------ |
| `Dungeon.level.occupyCell`         | ✅                           | ❌ missing                     |
| `GameScene.updateFog()`            | ✅                           | ❌ missing (player-camera work) |
| `WandOfBlastWave.BlastWave.blast`  | ✅                           | ❌ missing                     |
| `PixelScene.shake(2, 0.5f)`        | ✅                           | ❌ missing                     |
| body-slam / impact-wave maths      | ✅                           | duplicated verbatim            |

Some omissions are intentional (`updateFog` is the player's camera); `BlastWave.blast` and `occupyCell` look like drift, not decisions. Nothing in the codebase distinguishes the two cases, and a rebase onto a newer upstream will flag none of it.

### 3.3 The phantom kit is a `Hero` that is not on stage, and the type system cannot say so ❗

`EchoBoss` holds an `echoHero` restored by `EchoHeroSnapshot.restoreHero`. It is a full `Hero`, therefore a `Char`, therefore accepted by every world-facing API. All mitigations are conventions:

- `EchoActionContext.stats()` is *named* to discourage misuse, but its type is `Hero` — `chargeUse(Hero)` and `Enchanting.show(Char, Item)` take the same reference indistinguishably.
- `EchoKitEscapeTest` is a **regex scan over source text**, matching `Enchanting.show(kit`, `MagicMissile.boltFromChar(kit`, `new Flare(...).show(kit`, and `kit.sprite`. Anything spelled differently — a local named `h`, a helper taking `Hero`, a new VFX entry point — slips through.
- The kit's sprite is **mirrored** from the body (`mirrorKitSprite()` on every `act()`, on every `getEchoHero()`, and on `EchoBossSprite.link`) precisely because the older borrow-per-call design shipped ANDROID-20/21.

The contagion reaches base-game code. These files now carry echo-shaped defensive branches because a `Hero` can exist off-stage:

`Char.java` · `Hero.java` · `Belongings.java` · `Item.java` · `Vampiric.java` · `Metabolism.java` · `WandOfLightning.java` · `Wand.java` · `Paralysis.java` · `Frost.java` · `MagicalSleep.java` · `TimeStasis.java` · `Affection.java` · `CloakOfShadows.java` · `SpiritBow.java` · `MissileWeapon.java` · `TippedDart.java` · `HeavyBoomerang.java` · …

That is the real cost of the kit design: it is not contained inside `heroechoes/`.

**Fair counterpoint:** the kit exists because base-game combat math genuinely *demands* a `Hero` (talents, `Belongings`, `chargeUse`, `buffedLvl`). Replacing it wholesale is not on the table. The realistic move is §5.3 — make the two roles (calculator vs. actor) different **types**, not different method names on the same type.

### 3.4 Two turn-accounting protocols, chosen per adapter ⚠️

The echo spends a turn in one of two ways, and which one applies depends on which adapter ran:

| Path                                                                    | Who spends           | Adapters                                                                                                                   |
| ----------------------------------------------------------------------- | -------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| **VFX-owned** — `ctx.busy()` → async callback → `ctx.complete(delay)`   | the adapter          | `EchoThrowAdapter`, `EchoWandAdapter`, `EchoArmorAbilityAdapter`, `EchoDuelistAdapter`, `SpiritBowEchoBridge`               |
| **Caller-owned** — return `true`, `runChoice` spends `TICK` or `1f/speed()` | `EchoBoss.runChoice` | `EchoPotionAdapter.drink` / `.breathe`, `EchoScrollAdapter`, `EchoCloakAdapter`, `EchoHornAdapter`, `EchoInventoryStoneAdapter`, all `*move*` virtuals |

The bridge between them is one mutable boolean on the boss, `vfxOwnsTurn`, set by `busy()` and cleared by `cancelBusy()`. There is no type-level record of which protocol an adapter follows, so a new adapter that busies but forgets to `complete()` **hangs the actor clock**, and one that both busies and returns `true` without the flag can double-spend. `EchoArmorAbilityAdapter` documents a third variant on top ("soft refuses cancel busy and still return `true`; hard refuses return `false` before busy") — three distinct outcomes encoded in one `boolean` return.

Compare the hero: `spend()` / `spendAndNext()` is always called by the code that performed the action. One protocol.

### 3.5 The "adapter / handler / bridge" taxonomy is not a taxonomy ⚠️

Three suffixes are in play and none predicts what a file does:

| Suffix         | Intended meaning                                | Actual usage                                                                                                                             |
| -------------- | ----------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `*Adapter`     | executor entry point: validate, then delegate    | sometimes that (`EchoScrollAdapter`); sometimes the whole 799-line implementation (`EchoDuelistAdapter`); sometimes a one-line forward (`EchoCloakAdapter`, `EchoHornAdapter`) |
| `*Handlers`    | the per-type effect bodies                       | `EchoArmorHandlers`, `EchoClericHandlers`, `EchoScrollHandlers` — internally consistent, but only three exist                              |
| `*EchoBridge`  | same-package shim to reach protected members     | mostly that; `EtherealChainsEchoBridge` and `CloakOfShadowsEchoBridge` hold the full effect implementation                                 |

`EchoCloakAdapter` and `EchoHornAdapter` are *pure forwarders* — each is a single `return SomethingEchoBridge.x(...)`. That is exactly what [`no-pass-through-wrappers.mdc`](../../.cursor/rules/no-pass-through-wrappers.mdc) forbids. Meanwhile `EchoChainsAdapter` forwards to a bridge that holds the logic, and `EchoArmorAbilityAdapter` forwards to a handlers file. One word, three structures.

### 3.6 Bridge signatures have no convention ⚠️

The `*EchoBridge` shims take four different first parameters:

```java
ItemEchoBridge.castVisual(Item, CharSprite, int, int, Callback)
CloakOfShadowsEchoBridge.toggleStealth(EchoBoss, CloakOfShadows)
EtherealChainsEchoBridge.cast(EchoBoss, EtherealChains, int)
InventoryScrollEchoBridge.read(InventoryScroll, EchoActionContext)          // ctx last
InventoryStoneEchoBridge.applyEcho(EchoActionContext, InventoryStone, Item) // ctx first
InventoryStoneEchoBridge.firstUsable(InventoryStone, Hero kit)              // raw kit!
MeleeWeaponEchoBridge.beforeAbilityUsed(MeleeWeapon, EchoActionContext, Char)
ClericSpellEchoBridge.firstUsableItem(InventoryClericSpell, Hero kit)       // raw kit!
DragonsBreathEchoBridge.applyCone(int, int)                                 // no context at all
```

Two of them take the bare kit `Hero` — precisely the reference §3.3 is trying to keep contained. `EchoActionContext` appears as first arg, last arg, and not at all.

### 3.7 Item identity is a stringly-typed class name ⚠️

`EchoInventory.itemId(item)` returns `item.getClass().getSimpleName()`. That string is simultaneously the wire contract with the backend playbook, the key in `capabilities.items[]`, and a switch value in client code:

```java
if ("StoneOfBlink".equals(itemId)) { return pickBlinkAway(boss); }   // EchoTargetPicker
"HolyTome".equals(id)                                                // EchoRoleExecutor
{"PotionOfParalyticGas","PotionOfFrost","PotionOfSnapFreeze","FlashBangBomb"}  // EchoPolicyHazards
```

Renaming or moving any of those classes compiles cleanly, passes most tests, and silently disarms a role. There is no `Bundle.addAlias`-style safety net here, and no test asserting that every id the backend can emit resolves to a class in this build. Roles have a backend mirror (`src/lib/echo/roles.ts`); item ids have no equivalent guard.

### 3.8 `EchoBoss` is the AI's shared scratchpad ⚠️

> **Partly fixed.** The three pure mailbox fields — `plantBlockerCell`, `pathBlockerCell`, `losBlockerCell` — are deleted. The sense phase keeps them as locals and puts the cell on the plan (§5.1), which is the only thing execute ever wanted them for. The rest below still stands.

Besides its actor state, `EchoBoss` carries: `recipeSteps`, `doorStallCell`, `doorStallTurns`, `blindDefenseShotsLeft`, `lastAttackerPos`, ~~`plantBlockerCell`~~, ~~`pathBlockerCell`~~, ~~`losBlockerCell`~~, `avoidPredictedGas`, `avoidHarmfulPlants`, `busy`, `vfxOwnsTurn`, `disengageTurns`, `selfShieldPeak`, `preppedThisWindow` — most written by `EchoPolicyStatusBuilder` and read by `EchoRoleExecutor`, with `EchoBoss` acting only as the mailbox between them.

`avoidPredictedGas` is the sharpest case: a flag toggled around a call and restored in a `finally`, so that `cellIsPathable` answers differently for the same cell depending on who is asking:

```java
public boolean policyCellPathable(int cell, boolean avoidGrowthRing) {
    boolean saved = avoidPredictedGas;
    avoidPredictedGas = avoidGrowthRing;
    try { return cellIsPathable(cell); } finally { avoidPredictedGas = saved; }
}
```

Compare the hero, whose per-turn intent lives in one typed object (`curAction`) nulled on `ready()`. The echo has no equivalent reset point — which leads straight to §3.9.

### 3.9 Persistence is asymmetric and mostly undocumented ⚠️

`EchoBoss.storeInBundle` saves four things: `echo`, `echo_policy`, `disengage_turns`, `self_shield_peak`. The last two carry an explicit rationale ("a reload must not save-scum a fresh window"). By that same reasoning, these are *not* saved and can be reset by reloading mid-fight:

| Not bundled                          | Save-scummable effect                     |
| ------------------------------------ | ----------------------------------------- |
| `recipeSteps`                        | every multi-step recipe restarts at step 0 |
| `blindDefenseShotsLeft`              | two fresh blind shots per reload           |
| `doorStallCell` / `doorStallTurns`   | door-force pressure resets (count from `tuning.door_force_turns`) |
| `lastAttackerPos`                    | focus memory wiped                         |
| `preppedThisWindow`                  | documented as acceptable ("worst case one extra prep") |

Only one of the five has a stated rationale. The fight is on a sealed floor, so reload-scumming is exactly the exploit the two saved fields exist to prevent — this reads as an oversight rather than a decision.

### 3.10 `EchoBossSprite` re-encodes the `HeroSprite` frame table ⚠️

`EchoBossSprite extends MobSprite` but re-declares `idle` / `run` / `die` / `attack` / `zap` / `operate` / `fly` / `read` with the same frame indices, the same `FRAME_WIDTH` / `FRAME_HEIGHT` / `RUN_FRAMERATE` constants, and the same `HeroSprite.tiers()` film that `HeroSprite.updateArmor()` uses. Five comments in the file literally say "Match `HeroSprite`". Upstream changing a frame index updates the hero and silently desyncs the echo.

### 3.11 Indentation splits along an invisible line ℹ️

New echo files in **base-game packages** use 4 spaces; files in `heroechoes/**` use tabs like the rest of the project:

| Spaces                                                                       | Tabs                                                          |
| ---------------------------------------------------------------------------- | ------------------------------------------------------------- |
| `actors/mobs/EchoBoss.java`, `sprites/EchoBossSprite.java`, `heroechoes/Echo.java` | everything else in `heroechoes/**`, all `*EchoBridge`, all base-game files |

Per the master-style rule in [`java-best-practices.mdc`](../../.cursor/rules/java-best-practices.mdc), files sitting *in base-game packages* are the ones that most need to match master's tabs. It is currently exactly backwards.

### 3.12 Smaller items ℹ️

- `EchoBoss.setBlindDefenseShotsLeftForTests(int)` is a test-only mutator on production API. `replacePolicy` and `stopAllHunting` are debug/sandbox entry points on the same class.
- `EchoBoss` declares private `IntAction` / `ValueAction` interfaces (RoboVM has no `java.util.function`) — correct per [`cross-platform.mdc`](../../.cursor/rules/cross-platform.mdc) — while `EchoActionContext.HostileEffect` and `EchoKitBorrow.DeferredCapture` solve the same problem separately. Four ad-hoc functional interfaces, no shared home.
- `EchoActionContext` exposes `attackFx` / `zapFx` "so drawing on the kit is not expressible at the call site", yet `EchoArmorHandlers` reaches `body.sprite.*` directly ~20 times behind hand-written `canWorldFx` guards. The safe API exists and is routinely bypassed.
- Refusal semantics differ per adapter: `EchoActionSupport.refuse(ctx)` cancels and returns `false`; `EchoArmorAbilityAdapter` cancels and returns `true`; `EchoWandAdapter` returns `false` *before* busy for a `tryToZap` failure but calls `refuse()` for a self-aimed shot. Three refusal shapes, one `boolean`.
- `EchoBossLevel` is an empty save-compat stub (`extends SewerBossLevel`), correctly documented — worth an explicit `Bundle.addAlias` note so nobody deletes it during a cleanup pass.

---

## 4. The bug classes this shape produces

Every incident referenced in the code comments falls into one of four buckets, and each maps to a §3 item:

| Bug class                       | Example from the comments                                                   | Root cause                                          |
| ------------------------------- | --------------------------------------------------------------------------- | --------------------------------------------------- |
| **Kit escapes onto the stage**  | ANDROID-20 / ANDROID-21, plus a third copy in the runestone bridge          | §3.3 — the kit is a `Hero`; the type system can't object |
| **Headless-kit NPEs**           | "Family A / ANDROID-1T", `Vampiric`, `Metabolism`, `WandOfLightning` guards | §3.3 — a `Char` with no scene sprite                 |
| **Turn lost or double-spent**   | `vfxOwnsTurn`, `cancelBusy`, "do not busy before validating"                | §3.4 — two turn protocols                            |
| **Role sensed but mis-executed**| `CLEAR_PLANT` aim, `CLEAR_LOS` always-ready                                 | §3.1 — the choice carries no target                  |

Adding one item or role today can touch: the backend playbook generator, `EchoRole`, `EchoPolicyHazards`, `EchoPolicyStatusBuilder.virtualRoleFeasible`, `EchoRoleExecutor.executeNonPotion`, `EchoTargetPicker`, an `Echo*Handlers` file, possibly a new `*EchoBridge`, and the item-id string in three places.

`EchoRole`'s own header comment says this was already the problem it was created to solve — *"adding a role meant editing six files and a typo in any of them failed silently"*. That fix worked, for roles. The same medicine has not been applied to items, targets, or turn costs.

---

## 5. Re-architecture proposal

Ordered by value per unit of risk. Each stage is independently shippable, and each has a natural characterization-test entry point per [`test-driven-development.mdc`](../../.cursor/rules/test-driven-development.mdc) — characterize current behaviour first, then red → implement → green.

### 5.1 Make the plan carry the target — *highest value, lowest risk*

> **Landed.** `EchoPolicyChoice` is now `EchoPlan`: it carries the typed `EchoRole` and the `targetCell` the sense phase resolved, the sense phase records that cell per role on `EchoPolicyStatus`, and `EchoRoleExecutor` aims at it instead of re-deriving one. The resolved `Item` and `TurnCost` fields of the sketch below belong to §5.2/§5.4/§5.5 and are still open.

Replace `EchoPolicyChoice` (role + layer strings) with a resolved **`EchoPlan`** produced by the sense/match phase:

```java
public final class EchoPlan {
    public final EchoRole role;        // enum, not String
    public final String   layer;
    public final String   recipeId;
    public final Item     item;        // resolved, not an id string
    public final int      targetCell;  // -1 = none; set by whoever knows the geometry
    public final TurnCost cost;        // MOVE | TICK | VFX_OWNED
}
```

`EchoPolicyStatusBuilder` already computes `plantBlockerCell`, `losBlockerCell`, and blink landings — it hands them to the plan directly instead of stashing them on `EchoBoss`. `EchoRoleExecutor` stops re-deriving aim.

Effects: `CLEAR_PLANT` starts working; `CLEAR_LOS` becomes implementable instead of silently always-ready; `EchoTargetPicker` becomes a helper the sense phase calls rather than a second source of truth; six of the sixteen scratch fields leave `EchoBoss`.

> **Landed alongside it (§3.8).** `plantBlockerCell`, `pathBlockerCell` and `losBlockerCell` are gone from `EchoBoss`. The sense phase now holds them as locals and hands them to the plan, so the three fields that existed only as a mailbox between sense and execute no longer exist at all. `los_blocked` and `path_blocked` are sensed for the first time — the backend has always been allowed to key reactions on both (`base-policy.ts`, `policy.ts` `path_blocked_pierce`), and neither was ever written, so `CLEAR_LOS` reported permanently ready and `PATH_THROUGH` never fired.

### 5.2 One turn-cost protocol

> **Also landed alongside it:** `CLOSE_IN` now answers `virtualRoleFeasible` with `EchoBoss.hasStepCloser`, the mirror of `KEEP_DISTANCE`'s `hasStepAwayFrom`. A virtual movement role reported ready on item resolution alone, so a blob walling off the route let the positioning layer win the turn and then refuse it.
>
> **Partly landed.** Every rung of `EchoBoss.act()` that claims a turn now pays for it through one private `spendTurn(EchoPlan, posBefore)` — the playbook, the door force, the paralysis skip, the AoE last resort and the untouchable floor. Only `super.act()` (stock hunting AI, which does its own accounting) and the deferred `spendAndNext` VFX callback sit outside it. That closed a live bug: the AoE last-resort escape returned `true` without spending, so the actor re-entered `act()` at the same game time and the boss appeared frozen. `EchoTurnOutcome` below is still open — the "return `true` but soft-refuse" fallthrough is still a bare boolean.

Fold `busy` / `vfxOwnsTurn` / "return `true` but soft-refuse" into a single returned value:

```java
public enum EchoTurnOutcome { SPENT_TICK, SPENT_MOVE, SPENT_BY_VFX, REFUSED_FALLTHROUGH, REFUSED_TRY_NEXT }
```

Adapters return it; `runChoice` is the only place that reads it. A missing return becomes a compile error instead of a hung actor clock. The `Hero.busy()` mirror stays — this changes *who declares the turn paid*, not the ready/busy pump itself.

### 5.3 Split the kit's two roles into two types

The kit is used for exactly two things. Give them two names the compiler understands:

- **`KitStats`** — a wrapper (**not** a subtype of `Char`) exposing only what base-game math needs: `hasTalent`, `pointsInTalent`, `buffedLvl`, `STR`, `heroClass`, `armorAbility`, `belongings`, `chargeUse`. It holds the `Hero` internally and never returns it.
- **`Hero asHeroFor(...)`** — one narrow, audited escape hatch for APIs whose signature genuinely requires `Hero` (`chargeUse(Hero)`, `tryToZap(Hero, int)`, `isEquipped(Hero)`), with the raw reference reachable from nowhere else.

`EchoKitEscapeTest`'s regex scan then becomes a *backstop* for the escape hatch rather than the primary defence — structural enforcement instead of textual. Bridges that currently take `Hero kit` (`InventoryStoneEchoBridge.firstUsable`, `ClericSpellEchoBridge.firstUsableItem`) take `KitStats` and stop being holes.

This is the largest stage, and it can be done incrementally: introduce `KitStats`, migrate one bridge at a time, keep `stats()` as a deprecated shim until the last caller is gone.

### 5.4 Turn the five type-switches back into dispatch

Two viable shapes:

**(a) Echo entry points on the base classes.** Add an echo-aware method beside the hero one, so the *item* owns both behaviours:

```java
// MeleeWeapon
protected void duelistAbility(Hero hero, Integer target);              // upstream, untouched
protected boolean echoAbility(EchoActionContext ctx, Integer target);  // fork
```

Cost: ~25 weapon files, ~19 ability files, ~20 spell files — but each edit is small, it puts the code *where the formulas already live* (so `Rapier`'s `5 + 1.5f*buffedLvl()` is written once), and a missing override is a missing method rather than a silent `instanceof` gap. This is the shape the base game uses and the one an upstream rebase will survive.

**(b) A registry keyed by class.** `Map<Class<?>, EchoEffect>` populated once, with a test asserting that every `ArmorAbility` / `ClericSpell` / duelist `MeleeWeapon` subclass in the build has an entry. Keeps all echo code inside `heroechoes/`, and converts "silently unsupported" into a **failing test at build time** — most of the value of (a) for a fraction of the diff.

**Recommendation: (b) first**, then (a) opportunistically for the two families where formula duplication actually bites (duelist weapons, armor abilities). Either way, pair it with **shared-formula extraction**: the numeric literals in `EchoDuelistAdapter.dispatchAbility` should call the weapon's own accessor instead of restating them.

### 5.5 Type the item id

Introduce `EchoItemId` alongside `EchoRole`: one place mapping wire string ↔ `Class<? extends Item>`, `byId()` returning `null` for unknown ids, and a test asserting every id the backend playbook can emit resolves in this build. Then kill the three hard-coded string comparisons (`"StoneOfBlink"`, `"HolyTome"`, `SETUP_CC_STUN_ITEMS`) by hanging those facts off the enum — exactly what `EchoRole` did for role facts.

### 5.6 Housekeeping (cheap, do alongside anything)

- ~~Delete `pathBlockerCell` / `losBlockerCell` / `PATH_BLOCKED` / `LOS_BLOCKED`, **or** implement `CLEAR_LOS`.~~ **Done, the implement way** — deleting was not open: the backend already emits reactions keyed on both statuses. `EchoPolicyStatusBuilder.senseLosBlocked` flags burnable grass (never a wall) eating the sightline and aims `CLEAR_LOS` at it; `sensePathBlocked` flags a non-hero character standing in the shot, which `PATH_THROUGH` pierces by aiming at the hero's own cell.
- Inline `EchoCloakAdapter` and `EchoHornAdapter` into their bridges (or the executor) per `no-pass-through-wrappers`.
- Fix bridge signatures to one convention — `(EchoActionContext ctx, <SubjectItem> item, …)`, ctx always first, never a bare `Hero`.
- Retab `EchoBoss.java`, `EchoBossSprite.java`, and `Echo.java` to match their base-game packages.
- Bundle `recipeSteps`, `blindDefenseShotsLeft`, and the door-stall counters — or write down why each is safe to reset, the way `preppedThisWindow` does.
- Have `EchoBossSprite` derive its film from `HeroSprite` (one shared `updateArmor` frame table) instead of copying it.
- Move `setBlindDefenseShotsLeftForTests` behind a package-private test seam.
- Give `IntAction` / `ValueAction` / `HostileEffect` / `DeferredCapture` one home, e.g. `heroechoes/RoboVMFunctions.java`.

### 5.7 Enforcement to add

Structural checks in the spirit of `RoboVMUnsupportedApisTest` and `EchoKitEscapeTest`:

| Check                                                                                                    | Catches                              |
| -------------------------------------------------------------------------------------------------------- | ------------------------------------ |
| Every `ArmorAbility` / `ClericSpell` / duelist `MeleeWeapon` subclass has an echo effect (entry or override) | §3.2 silent gaps                     |
| Every `EchoRole` constant is reachable — sensed in `virtualRoleFeasible` **and** executable in the executor  | `CLEAR_LOS`-shaped dead roles        |
| Every `EchoItemId` resolves to a loadable `Item` class                                                     | §3.7 renames                         |
| Every adapter returns an `EchoTurnOutcome` (no raw `boolean`)                                              | §3.4 hung / double-spent turns       |
| Golden test: fixed policy + fixed seed → fixed turn transcript                                            | drift from any of the above          |

---

## 6. Summary judgment

The **pipeline** (sense → match → execute → Java floor) is a good architecture, and the **role vocabulary** (`EchoRole`) is a good pattern that has already proved itself in this codebase. The trouble is that the pattern was applied to roles and stopped there: **targets, items, and turn costs are still stringly-typed, re-derived, or passed through mutable fields on `EchoBoss`** — and item behaviour is duplicated *out* of the base game into five central type-switches instead of dispatched *into* it.

If you do only one thing: **§5.1 — make the plan carry its target.** It fixes a live behavioural bug, deletes a third of `EchoBoss`'s scratch state, removes the `door_break` special case, and makes every future targeted role a data change rather than another `if`.

If you do two: add **§5.4(b)**, the registry with a completeness test. That converts the largest silent-failure class in the feature into a build error.
