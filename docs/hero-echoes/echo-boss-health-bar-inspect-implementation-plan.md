# EchoBoss health-bar inspect — implementation plan

## Context

Tapping the boss health bar (or examining the EchoBoss body) today opens
`[WndInfoMob](iatdb/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndInfoMob.java)`,
which shows sprite + name + HP + buffs + `mob.info()` — but **not the echo's equipped
kit**. A player fighting another player's echo cannot see what weapon, armour, artifact,
misc or ring they are up against, even though the boss's whole combat identity comes from
that gear.

`[docs/hero-echoes/echo-boss-health-bar-inspect.md](iatdb/docs/hero-echoes/echo-boss-health-bar-inspect.md)`
already catalogues every existing UI pattern for this. This plan turns that catalogue into
a concrete design.

The hard constraint: **the live player must not be touched.** The one existing echo-kit
viewer,
`[WndEchoDetail](iatdb/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndEchoDetail.java)`,
works by temporarily assigning `Dungeon.hero = viewHero` so that
`[InventorySlot](iatdb/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/InventorySlot.java)`
resolves its equipped tint. That trick is safe in the main menu and **unsafe mid-fight** —
the actor clock, the inventory pane and every `isEquipped(Dungeon.hero)` call assume the
real hero. The new window must therefore read the echo's gear without writing to, or
reading from, player state.

**Outcome:** tapping the boss bar opens a two-tab window — the existing mob info, plus a
5-column grid of the echo's equipped kit whose slots inspect (`WndInfoItem`) and nothing
else.

---

## Design decisions (already settled)

| Question         | Decision                                                                                                    |
| ---------------- | ----------------------------------------------------------------------------------------------------------- |
| Plan location    | New sibling doc `docs/hero-echoes/echo-boss-inspect-plan.md`; pattern dump stays a pure "what exists" file. |
| Slot ownership   | **New render+inspect-only slot widget. Zero edits to `Hero`, `Belongings`, `InventorySlot`, `Dungeon`.**    |
| Window shape     | Tabbed: tab 1 = today's mob info, tab 2 = equipped kit.                                                     |

---

## 1. `ui/EchoKitSlot` — the render + inspect-only slot

New file:
`core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/EchoKitSlot.java`

Placed in the `ui` package so `ItemSlot`'s `protected` members (`item`, `extra`, `status`,
`level`, `sprite`) are reachable without widening anything.

It extends
`[ItemSlot](iatdb/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/ItemSlot.java)`
— **not** `InventorySlot` — because `InventorySlot.item()` unconditionally reads
`Dungeon.hero.belongings` for its equipped tint and its `lostInventory()` gating
(`InventorySlot.java:82-111`). Subclassing it cannot avoid those reads; its `bg` field is
private, so the paint cannot be overridden either.

```java
public class EchoKitSlot extends ItemSlot {

    public static final int NO_TINT = -1;

    private static final int NORMAL   = 0x9953564D;   // same values as InventorySlot
    private static final int EQUIPPED = 0x9991938C;

    private ColorBlock bg;
    private Hero owner;          // the echo hero — never Dungeon.hero
    private boolean equipped;

    public EchoKitSlot(Item item, Hero owner, boolean equipped) {
        super();                 // no-arg ctor: does NOT call item(...)
        this.owner = owner;
        this.equipped = equipped;
        item(item);              // safe: fields are initialised
    }
    ...
}
```

**Constructor ordering matters.** `ItemSlot(Item)` calls `item(item)` from the super
constructor, before subclass fields exist. Using the no-arg `ItemSlot()` super and calling
`item(item)` last is what makes an owner-aware subclass possible at all.

### What it does

- Owns its own `ColorBlock bg` (copied from `InventorySlot`, ~20 lines). Background is
  `EQUIPPED` for real kit items and `NORMAL` for placeholders — a **constant per slot**,
  passed in, never derived from any hero. Cursed / unidentified tints come from the item's
  own fields (`item.cursed`, `item.cursedKnown`, `item.isIdentified()`), which are
  hero-independent.
- **No `lostInventory()` gating.** That is player state and is simply not implemented.
- Overrides `updateText()` to re-colour the STR-requirement label against `owner`:
  `ItemSlot.updateText()` colours it red when `str > Dungeon.hero.STR()`
  (`ItemSlot.java:260`). For a boss's kit that reads as "the player can't lift it", which
  is misleading — the echo always can. `super.updateText()` then
  `extra.hardlight(strengthColor(item, owner))` corrects it.
- Exposes the pure, headless-testable seam:

```java
/** DEGRADED / MASTERED / NO_TINT for item's STR label as seen by owner. */
public static int strengthColor(Item item, Hero owner)
```

- `onClick()` / `onLongClick()` → `GameScene.show(new WndInfoItem(item))`, disabled for
  placeholders. There is **no** equip / use / drop path in the class at all —
  `[WndInfoItem](iatdb/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndInfoItem.java)`
  is hero-free, unlike `WndUseItem` which executes actions on `Dungeon.hero`.

### Accepted residual `Dungeon.hero` reads

These are **reads only, no mutation**, inside item description text we do not control:
`Item.info()` / `Armor.info()` / ring `info()` reference `Dungeon.hero` to phrase
descriptions. That is the same behaviour as inspecting any item on the floor, and it is
the useful framing ("could *I* use this?"). Documented, not fixed.

---

## 2. `windows/WndEchoBossInfo` — the tabbed window

New file:
`core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndEchoBossInfo.java`,
extending
`[WndTabbed](iatdb/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndTabbed.java)`,
modelled directly on `WndEchoDetail`'s two-`IconTab` structure — but **without** its
`Dungeon.hero` swap and without its `hide()` restore, because there is nothing to restore.

### Static API (the testable surface)

```java
public static boolean hasInspectableKit(Mob mob);   // EchoBoss && getEchoHero() != null
public static Window  windowFor(Mob mob);           // routing factory
public static Item[]  equippedKit(Hero echoHero);   // 5 or 6 entries, placeholders for gaps
```

`equippedKit` mirrors `WndEchoDetail.InventoryTab` (`WndEchoDetail.java:243-251`) —
weapon / armor / artifact / misc / ring, each falling back to
`[WndBag.Placeholder](iatdb/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndBag.java)`
(`WEAPON_HOLDER`, `ARMOR_HOLDER`, `ARTIFACT_HOLDER`, `SOMETHING`, `RING_HOLDER`), plus
`secondWep` appended only when non-null. It reads the **raw** `Belongings` fields, per the
comment in
`[Belongings](iatdb/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Belongings.java)`
— the accessor forms gate on `LostInventory`.

Per `fail-dont-fake`: when `getEchoHero()` is null, `windowFor` returns a plain
`WndInfoMob` rather than a hollow empty grid. In practice `EchoBoss.initFromEcho` already
throws when the hero cannot be restored (`EchoBoss.java:205-207`), so this is a guard, not
a code path with a fabricated result.

### Tabs

- **Info tab** (`Icons.INFO`) — reuses `WndInfoMob`'s existing `MobTitle` component
  (sprite + name + `HealthBar` + `BuffIndicator`) plus a `RenderedTextBlock` of
  `mob.info()`. Requires widening `WndInfoMob.MobTitle` from `private static class` to
  package-private `static class` — a one-keyword change, same package, no behaviour
  change. `EchoBoss.sprite()` already calls `linkVisuals` without `link()`
  (`EchoBoss.java:162-167`), so the sprite renders correctly here — covered by
  `EchoBossSpriteTest`.
- **Kit tab** (`Icons.BACKPACK`) — 5-column grid of `EchoKitSlot`s from `equippedKit(...)`,
  reusing the existing
  `[WndEchoDetail.fittingInventorySlotSize(int)](iatdb/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndEchoDetail.java)`
  helper for slot sizing rather than inventing a second sizing rule.

Equipped-only means ≤ 6 slots, so no backpack and no scroll pane. Sizing: build both tabs,
`resize(WIDTH, max(infoHeight, kitHeight))`, then `layoutTabs(); select(0);` — the same
sequence `WndEchoDetail` uses. Width starts at 120 (matching `WndInfoMob`'s `WIDTH_MIN`)
and widens toward 220 if the info text is tall, mirroring
`[WndTitledMessage](iatdb/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndTitledMessage.java)`'s
widen loop.

**No new i18n keys.** Icon tabs carry no text and the grid uses the `WndEchoDetail`
placeholder convention, so `messages/windows/windows.properties` is untouched.

---

## 3. Routing — two call sites, one factory

Exactly two places construct `WndInfoMob`:

- `[BossHealthBar.java:115](iatdb/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/BossHealthBar.java)` — the invisible overlay button covering the whole bar
- `[GameScene.java:1998](iatdb/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java)` — `examineObject`

Both become `GameScene.show(WndEchoBossInfo.windowFor(mob))`. Nothing else in
`BossHealthBar` changes — `hoverText()`, `assignBoss`, `nameLabelFor` and the layout are
untouched.

---

## 4. Files touched

**New**

- `core/src/main/java/…/ui/EchoKitSlot.java`
- `core/src/main/java/…/windows/WndEchoBossInfo.java`
- `core/src/test/java/…/heroechoes/EchoBossInspectTest.java`
- `docs/hero-echoes/echo-boss-inspect-plan.md`

**Modified**

- `…/ui/BossHealthBar.java` — one line (line 115) + import
- `…/scenes/GameScene.java` — one line (line 1998) + import
- `…/windows/WndInfoMob.java` — `private static class MobTitle` → `static class MobTitle`
- `docs/hero-echoes/echo-boss-health-bar-inspect.md` — add the plan to the `Related:` line
- `docs/hero-echoes/README.md` — add the plan doc to the index table

**Untouched, deliberately** — `Hero`, `Belongings`, `Dungeon`, `InventorySlot`,
`InventoryPane`, `WndBag`, `WndUseItem`, `WndHero`.

---

## 5. TDD sequence

Per the mandatory TDD rule, each step is red before it is green. Widget classes need GL and
cannot be constructed headless, so every assertion targets the pure static seams above —
the same approach `EchoViewerTest` takes with `fittingInventoryLayout`.

New test class `core/src/test/java/…/heroechoes/EchoBossInspectTest.java`
(AssertJ + `@DisplayName` + `@ExtendWith(GdxTestExtension.class)`, using the existing
`EchoTestSupport.createBoss` / `createBossWithPolicy` / `warriorHero` helpers):

1. `hasInspectableKit` is true for an `EchoBoss` with a restored hero.
2. `hasInspectableKit` is false for a plain `Mob` and for `null`.
3. `equippedKit` returns weapon / armor / artifact / misc / ring in that order, with a
   `WndBag.Placeholder` for each empty slot.
4. `equippedKit` appends `secondWep` only when the echo has one (5 vs 6 entries).
5. **`equippedKit` reads the echo, not the player** — give `Dungeon.hero` a different
   weapon and armour, assert the returned items are the echo's.
6. **`equippedKit` does not disturb the player** — assert `Dungeon.hero` identity and its
   `belongings` fields are unchanged across the call (this is the regression test for the
   `WndEchoDetail`-style swap never being reintroduced).
7. `EchoKitSlot.strengthColor` returns `NO_TINT` when the echo's STR meets the item's
   requirement and `ItemSlot.DEGRADED` when it does not — **while `Dungeon.hero` has the
   opposite STR**, proving the label is owner-driven.

Run: `./gradlew :core:test -q -PerrorProneOff --tests "*EchoBossInspectTest"`, then the
`heroechoes` suite, then `:core:test`.

---

## 6. Verification

1. `./gradlew :core:test -q -PerrorProneOff` — full core suite green, including the
   existing `UiUxAndPolishTest`, `EchoBossSpriteTest`, `EchoViewerTest`,
   `EchoBossSpawnerTest`.
2. `./gradlew :desktop:run`, reach an EchoBoss floor (debug arena / `WndGameDebugTools` if
   quicker than a real descent), then:
   - Tap the boss bar → tabbed window opens, info tab looks exactly like today's
     `WndInfoMob`.
   - Switch to the kit tab → 5 slots (6 with a second weapon), placeholders for empties,
     sprites and upgrade levels drawn.
   - Tap a kit item → `WndInfoItem` with **no action buttons**; close it, the kit tab is
     still there.
   - Close the window, open the player's own inventory pane → **player gear unchanged,
     nothing equipped/unequipped, no quickslot change.**
   - Examine the boss body via the cell menu → same window as the bar tap.
   - Tap a non-echo boss's bar (e.g. Goo) → unchanged `WndInfoMob`.
3. Before PR: `./gradlew :core:spotlessCheck :core:test`.

---

## Flagged for your call (not doing unless you say so)

- **`keep-module-docs` rule.** Neither `windows/docs/` nor `ui/docs/` exists today. The
  rule says a missing module doc should be created via `explain-game-module` — but that
  means writing full explainers for two large base-game packages (~60 window classes,
  ~40 widgets) as a side effect of adding two files. This plan documents the feature in
  `docs/hero-echoes/` instead. Say the word if you want the module explainers written.
- **`WndEchoDetail` cleanup.** Once `EchoKitSlot` exists, `WndEchoDetail` could adopt it
  and delete its `Dungeon.hero` swap and the `hide()` restore. That is a real improvement
  but is out of scope for this feature and would need its own characterisation tests.