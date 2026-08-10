### Task 1: Ground Level — House, Village, Dungeon Entrance

**Status**: implemented.

**Goal**: Replace "press Solo/Ranked on the title screen" with "land in a village, walk to the
dungeon, and choose your mode at its mouth." The player lands in a village with their own house
(always solo), and enters the dungeon through a prompt that commits the run to Solo or Ranked.

#### Scope and Key Concepts

- **Ground level** = depth 0. A real `Level`, in a save slot of its own, outside any run
  (see [README.md](README.md#the-one-architectural-decision-everything-hangs-on) for why).
- **House** — the interior behind the village's front door, on depth 0 / branch 1. Always solo:
  nothing is ever generated in it, and it is meant to stay single-player even if the village
  later becomes shared.
- **Village** — the outdoor area. Peaceful by construction: no spawns, no respawner, no hunger,
  and the map starts fully revealed.
- **Dungeon entrance** — a `Terrain.EXIT` tile whose transition is intercepted to raise the
  Solo/Ranked prompt instead of descending.
- The run itself is unchanged: once the player is on depth 1, the game is the game it was.

#### What shipped

New:

| File                                              | Role                                                       |
| ------------------------------------------------- | ---------------------------------------------------------- |
| `levels/VillageLevel.java`                        | Hand-built outdoor level; owns the mode-prompt interception |
| `levels/HouseLevel.java`                          | Hand-built house interior, always solo                     |
| `village/VillageGateway.java`                     | The village↔run seam: slot, namespace, entry, hand-off     |
| `windows/WndDungeonMode.java`                     | Solo / Ranked / Not yet prompt                             |
| `actors/mobs/npcs/Villager.java`                  | Conversational villager, three kinds, existing sprites     |
| `core/src/test/…/village/VillageGroundLevelTest`  | Routing, slot, namespace, prompt mapping                   |
| `core/src/test/…/village/VillageLevelBuildTest`   | Real map builds, transitions, walkability, peacefulness    |

Changed:

| File                                    | Change                                                                              |
| --------------------------------------- | ----------------------------------------------------------------------------------- |
| `Dungeon.levelClassForDepth`            | Depth 0 → `VillageLevel`, depth 0 / branch 1 → `HouseLevel`, ahead of the debug arena |
| `Dungeon.init`                          | New `applyVillageStartIfNeeded()` beside the existing debug-start hook               |
| `actors/buffs/Hunger.act`               | No hunger accrues on the ground level                                               |
| `scenes/TitleScene`                     | Solo + Ranked buttons replaced by one **Enter the Village** button                   |
| `scenes/TitleSceneOnlineOnlyTest`       | Re-pinned to the new contract (still online- and auth-gated)                         |
| `messages/{scenes,levels,windows,actors}` | New strings                                                                       |

#### Layout

```
VillageLevel  (33x33, depth 0)                 HouseLevel (13x11, depth 0 / branch 1)
┌───────────────────────────────┐               ┌─────────────────┐
│ ~ ~ ~ ~ water ~ ~ ~ ~ ~ ~ ~ ~ │               │ hearth  shelf   │
│ ▓▓▓▓▓▓▓▓▓  ← house     ▓▓▓▓▓  │               │           pot   │
│ ▓       ▓        stall ▓   ▓  │               │ chest           │
│ ▓▓▓█▓▓▓▓▓              ▓▓▓▓▓  │               │        ▲        │
│    ║ ← front door  ○ well     │               └────────█────────┘
│    ║        ☼ firepit         │                    door
│    ║   ░ ░ high grass ░ ░     │
│  ☗ ║ ☗  ← statues              │
│    ▼ DUNGEON ENTRANCE          │
└───────────────────────────────┘
```

- `VillageLevel.build()` fills the map with `Terrain.WALL`, carves the interior to `GRASS` with
  `Painter.fill`, then lays the shore, the house shell, the market stall, and a paved
  (`EMPTY_SP`) path from the front door down to the dungeon mouth. Same shape as
  `levels/DebugArenaLevel.java`.
- Terrain is all stock (`levels/Terrain.java`): `GRASS` / `HIGH_GRASS`, `WATER`, `EMPTY_SP`,
  `WALL` / `WALL_DECO`, `DOOR`, `WELL`, `EMBERS`, `STATUE`, `ENTRANCE`, `EXIT`. This fork has no
  `Terrain.SIGN` and no `Sign.java`, so flavour text is carried by NPCs and by
  `tileName`/`tileDesc` overrides.
- Three transitions on the village: a `REGULAR_ENTRANCE` where the hero arrives (just outside the
  front door), a `REGULAR_EXIT` at the dungeon mouth pointed at depth 1, and a `BRANCH_ENTRANCE`
  on the front door pointed at depth 0 / branch 1. The house carries the matching `BRANCH_EXIT`.
  Same-depth / different-branch transitions are exactly how the mining and vault branches already
  work, so nothing new was needed in `InterlevelScene`.

#### Assets — existing art only

- Tiles: `Assets.Environment.TILES_CITY` and `WATER_CITY` for both levels.
- NPCs: `BlacksmithSprite`, `WandmakerSprite`, `GhostSprite` for the three villager kinds, plus a
  real `Shopkeeper` at the market stall (its sell/talk/buyback window works unmodified).
- Music: `Assets.Music.CITY_1` outdoors, `CITY_2` indoors.
- No new art was added.

#### The Solo / Ranked prompt

`VillageLevel.activateTransition` intercepts, following `levels/SewerLevel.java:148-175`:

- `REGULAR_EXIT` (the dungeon mouth) → show `WndDungeonMode`, return `false` so no descent
  happens until the prompt is answered.
- `REGULAR_ENTRANCE` (the arrival tile) → a message: there is nothing above the village.

`WndDungeonMode` offers **Solo**, **Ranked** and **Not yet**. Ranked is disabled, with the reason
appended to the window body, when `EchoBackendProbe.isOnlineReady()` is false. Selecting a mode
calls `VillageGateway.beginRun(mode)`, ranked first passing through
`EchoPlayerAuthGate.ensureReadyThen`.

`beginRun` does what `TitleScene.beginEchoRun` used to, in an order that matters:

1. **Save the village first**, while the namespace is still solo and the slot is still the
   village's. Selecting a mode repoints `gameFolder`, so saving afterwards would file the village
   under `-ranked`.
2. Clear the town avatar (`Dungeon.hero = null`, `Mob.clearHeldAllies()`) — the run starts fresh.
3. `GamesInProgress.selectEchoPlayMode(mode)` — the namespace is decided here.
4. `selectedClass = null`, pick a run slot, then `HeroSelectScene` (nothing saved) or
   `StartScene` (existing runs in that mode).

Because the run is genuinely fresh, mode-specific gates (`gameOptionsAllowed`, challenges, seed,
easy mode) all apply normally and nothing has to be migrated.

#### Entering and persisting the village

`VillageGateway.enterVillage()` forces the solo namespace and `VILLAGE_SLOT`, then either
resumes (`InterlevelScene.Mode.CONTINUE`) if a village save exists, or builds a fresh one
(`DESCEND` with `startingInVillage` set, which makes `Dungeon.init()` land on depth 0 instead of
depth 1). The `startingInVillage` flag is transient and never bundled.

The town avatar is a normal `Hero` of the last class the player selected, defaulting to Warrior.
It is deliberately disposable: nothing it carries reaches a run.

#### Peacefulness

Both levels override `createMob()` → `null`, `createMobs()` (village adds only NPCs, house adds
nothing), and `addRespawner()` → `null`, and mark the map `visited`/`mapped` in `create()`. The
one change outside the village package is `Hunger.act`, which now returns early on depth 0 — the
same early-return already used for locked floors and `VaultLevel`.

#### Tests

`VillageGroundLevelTest` covers the pure decisions: depth-0 routing (including that the debug
arena never replaces the village), untouched dungeon routing, the ground-level predicates, the
reserved slot never colliding with a run slot, the solo storage namespace, and the prompt's
option→mode mapping.

`VillageLevelBuildTest` builds both levels for real and asserts size and revealed state, that all
three transitions are wired to the right depth/branch and sit on the right terrain, that the
dungeon mouth is reachable from the front door by flood fill, and that neither level spawns
anything.

#### Known gaps / follow-ups

- **Returning from a run** still goes wherever it went before (rankings, then title) rather than
  back to the village. Re-entering is one button, so this is cosmetic, but it is the obvious next
  polish step.
- **The house is furnished but inert.** The chest is scenery; there is no stash yet.
- **No custom art.** The village reads as city tiles, not as a settlement. `CustomTilemap`
  overlays sampling `custom_tiles/city_quest.png` would do a lot here, following
  `levels/LastLevel.java:262-390`.
- **Offline play is still blocked** at the title screen, unchanged from before. Solo could now
  work offline, since only ranked needs the backend, but that is a product decision and was left
  alone deliberately.
- **The village avatar's class is cosmetic-only** and resets to whatever was last picked.

#### Build & verify

```
./gradlew :core:test -q -PerrorProneOff
./gradlew :core:spotlessApply
./gradlew :core:spotlessCheck :core:test    # before PR
./gradlew :desktop:run                      # in a separate worktree
```
