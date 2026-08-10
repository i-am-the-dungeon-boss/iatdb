### Task 1: Ground Level — House, Village, Dungeon Entrance

**Goal**: Replace "press Solo/Ranked on the title screen" with "land in a village, walk to the
dungeon, and choose your mode at its mouth." The player starts in their own house (always solo),
steps outside into a village with NPCs, and enters the dungeon through a prompt that commits the
run to Solo or Ranked.

#### Scope and Key Concepts

- **Ground level** = a persistent, hand-authored outdoor level that is *not* part of a dungeon run
  (see [README.md](README.md#the-one-architectural-decision-everything-hangs-on) for why).
- **House** — a small interior, reached through a door in the village. Always solo: it stays
  single-player even after task 2 makes the village shared. It is the player's private space and
  the natural home for storage/stash and cosmetic progression later.
- **Village** — the outdoor area. Peaceful: no mobs, no respawner, no hunger pressure, no combat.
- **Dungeon entrance** — a `Terrain.ENTRANCE` tile whose activation is intercepted to show a
  Solo/Ranked prompt instead of descending directly.
- Nothing here changes the existing run: once the player is on depth 1, the game is byte-for-byte
  the game it is today.

#### Files/Systems to Touch

New, under `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/`:

| File                                       | Role                                                              |
| ------------------------------------------ | ----------------------------------------------------------------- |
| `village/VillageLevel.java`                 | Hand-built outdoor level (`extends Level`)                        |
| `village/HouseLevel.java`                   | Hand-built house interior (`extends Level`)                       |
| `village/VillageScene.java`                 | `GameScene`-like scene that hosts a village level                 |
| `village/VillageSession.java`               | Load/save of village state + the town avatar, mode-independent    |
| `village/DungeonGateway.java`               | The Solo/Ranked prompt and hand-off into a run                    |
| `village/npcs/*.java`                       | Village NPCs (see below)                                          |
| `windows/WndDungeonMode.java`               | `WndOptions` subclass: Solo / Ranked / Cancel                     |

Existing, touched:

| File                              | Change                                                                        |
| --------------------------------- | ----------------------------------------------------------------------------- |
| `scenes/TitleScene.java:146-180`  | "Ranked"/"Solo" buttons → a single **Enter Village** button                   |
| `scenes/TitleScene.java:533-553`  | `beginEchoRun(mode)` stays, but is called from `DungeonGateway`, not the title |
| `scenes/HeroSelectScene.java`     | Reached from the village gateway; unchanged otherwise                          |
| `levels/Level.java`               | Possibly relax `Level` assumptions that presume a run (see Risks)              |
| `messages/…/village.properties`   | New message bundle                                                             |

Deliberately **not** touched: `Dungeon.init()` (`Dungeon.java:293`) keeps `depth = 1`, and
`Dungeon.levelClassForDepth` (`Dungeon.java:398`) gains no `case 0`. The village never becomes a
dungeon depth.

#### Layout

Two levels, connected by a door-style transition pair, mirroring how `DeadEndLevel.java:78-88`
builds a 6-arg `LevelTransition` for a branch.

```
VillageLevel  (outdoor, ~40x40)            HouseLevel (interior, ~11x9)
┌──────────────────────────────┐            ┌───────────────┐
│  ~ ~ ~ water / shore         │            │  bed  chest   │
│      ▓ house ▓  ← door ──────┼──────────► │               │
│      ▓▓▓▓▓▓▓▓▓               │            │   alchemy pot │
│   shop   smith   well        │            │      ▲ door   │
│      ░ ░ ░ village green ░   │            └───────┼───────┘
│         ▼ DUNGEON ENTRANCE   │ ◄──────────────────┘
└──────────────────────────────┘
```

- `VillageLevel.build()`: `setSize(40, 40)`, `Arrays.fill(map, Terrain.WALL)` then carve with
  `levels/painters/Painter.fill(...)`, exactly as `levels/DebugArenaLevel.java` does. The outer
  ring stays `WALL` so the level is closed; visually it is dressed as cliff/treeline.
- Terrain vocabulary available today (`levels/Terrain.java:29-73`) is enough for a village:
  `GRASS` / `HIGH_GRASS` / `FURROWED_GRASS` for the green, `WATER` for the shore, `EMPTY_SP` for
  paved paths, `WALL` + `WALL_DECO` for building shells, `DOOR` / `OPEN_DOOR`, `WELL` and
  `EMPTY_WELL`, `STATUE` / `STATUE_SP`, `BOOKSHELF`, `ALCHEMY`, `PEDESTAL`, `EMBERS` for a
  firepit, and `CUSTOM_DECO` / `CUSTOM_DECO_EMPTY` for anything that needs a bespoke visual.
- **There is no `Terrain.SIGN` and no `Sign.java` in this fork** — signposts must be NPCs or
  `CustomTilemap` + `desc()` tooltips, not the vanilla sign feature.
- `HouseLevel` is a single room: walls, one `DOOR` back to the village, `ALCHEMY` pot, a bed
  rendered as a `CustomTilemap`, and a chest/stash pedestal reserved for later.

#### Assets — reuse only, no new art in task 1

Everything below already ships in `core/src/main/assets/`:

- **Tiles**: `environment/tiles_city.png` is the closest to a built-up settlement and should be
  the village tileset (`Level.tilesTex()`); `environment/tiles_sewers.png` is the fallback if the
  city palette reads too grim. `environment/water*.png` for the shore (`Level.waterTex()`),
  `environment/terrain_features.png` for grass/plants.
- **Custom tiles**: `environment/custom_tiles/city_quest.png` and `city_boss.png` carry
  hand-authored city structures — house fronts, arches, paving — to be sampled by
  `CustomTilemap` overlays for the house exterior, shop awning and the dungeon mouth. Precedent
  for the technique: `levels/LastLevel.java:262-390` (`CustomFloor`, `CenterPieceVisuals`,
  `CenterPieceWalls`) and the boss levels.
- **NPC sprites**: `sprites/shopkeeper.png`, `sprites/blacksmith.png`, `sprites/wandmaker.png`,
  `sprites/ghost.png`, `sprites/ratking.png`, `sprites/sheep.png` — all existing NPCs with
  existing sprite classes and dialogue plumbing.
- **Sky/backdrop**: `interfaces/surface.png` already powers `scenes/SurfaceScene.java`'s sky,
  clouds and grass patches. `VillageScene` can reuse those visual classes for an outdoor feel
  above the tilemap. (`SurfaceScene` itself is the vanilla *win screen* — it has no tiles and no
  hero actor, so it is an art reference, not a base class.)
- **UI**: `interfaces/chrome.png`, `icons.png` — `WndOptions` picks these up for free.

#### The Solo / Ranked prompt

Interaction point: the dungeon entrance tile. The idiom already used in this codebase is to
intercept the transition rather than to hook the tile —
`levels/SewerLevel.java:148-175` overrides `activateTransition(Hero, LevelTransition)`, shows a
window, and returns `false` to cancel the move.

```java
// VillageLevel
@Override
public boolean activateTransition(Hero hero, LevelTransition transition) {
    if (transition.type != LevelTransition.Type.REGULAR_ENTRANCE) {
        return super.activateTransition(hero, transition);
    }
    Game.runOnRenderThread(() -> GameScene.show(new WndDungeonMode()));
    return false; // never descend directly; the window drives the hand-off
}
```

`WndDungeonMode extends WndOptions` (`windows/WndOptions.java:35`, `onSelect(int)`), offering
**Solo**, **Ranked** and **Not yet**. Copy can reuse the existing mode blurbs at
`scenes/HeroSelectScene.java:559-590` / `scenes/StartScene.java:184` rather than inventing new
strings.

`onSelect` delegates to `DungeonGateway.beginRun(mode)`, which performs exactly the sequence
`TitleScene.beginEchoRun(mode)` performs today (`TitleScene.java:533-553`) — and must keep
performing it in this order, because each step gates the next:

1. `EchoBackendProbe.isOnlineReady()` — Ranked requires the backend; if it is down, disable the
   Ranked option via `WndOptions`' `enabled(int)` hook and say why.
2. `EchoPlayerAuthGate.ensureReadyThen(...)` — Ranked requires an authenticated player.
3. `GamesInProgress.selectEchoPlayMode(mode)` (`GamesInProgress.java:57`) — **this is the moment
   the save namespace is decided**; everything after it reads the `-solo` / `-ranked` folder.
4. `GamesInProgress.selectedClass = null`, then `HeroSelectScene` (or `StartScene` if saved runs
   exist for that mode) — hero choice, challenges and seed options follow the mode's own gates
   (`HeroSelectScene.gameOptionsAllowed(mode)` is SOLO-only, `:592`).
5. The existing start button then does `Dungeon.echoPlayMode = …`, `Dungeon.initSeed()`,
   `InterlevelScene.mode = DESCEND` → `Dungeon.init()` → depth 1. Untouched.

Returning from a run (death or victory) drops the player back to `VillageScene`, replacing the
current `RankingsScene`-then-title flow's terminus.

#### Village state and persistence

`VillageSession` owns a small `Bundle`, saved to `village/village.dat` via `FileUtils` — outside
every mode-suffixed folder, because the village belongs to the player, not to a run:

- town avatar: hero class skin used for the village sprite, position on the level, facing.
- flags: whether the house door has been opened, which NPCs have been spoken to, tutorial state.
- **No inventory, no HP, no hunger, no depth.** The town avatar is a puppet, not a `Hero` with a
  run behind it. Keeping it dumb is what stops village state from leaking into run balance.

Village and house levels are regenerated deterministically from code each time (they are
hand-authored, not seeded), so only the flags above need storing. If the house later gains a
stash, that stash is a separate bundle with its own versioning.

#### Making it peaceful

Both levels override the three spawn hooks to nothing, the way `levels/DeadEndLevel.java:94-103`
and `levels/LastLevel.java:157-168` do:

- `createMob()` → `null`
- `createMobs()` → empty
- `addRespawner()` → `null`

and mark the whole map `visited`/`mapped` in `create()` (as `DebugArenaLevel` does) so there is
no fog to explore in a hub. NPCs are added explicitly in `createMobs()`, not by the spawner.

#### NPCs (task 1 set, all reusing existing classes)

| NPC                              | Role in the village                                      |
| -------------------------------- | -------------------------------------------------------- |
| `Shopkeeper` (`ShopRoom` stock)  | Sells starting consumables; existing buy/sell `WndOptions` at `Shopkeeper.java:241-290` |
| `Blacksmith`                     | Flavour now; reforge hook later                          |
| `Wandmaker`                      | Flavour now                                              |
| `Sheep`                          | Harmless ambient life on the green                       |

`actors/mobs/npcs/RatKing.java:120-170` is the canonical example of an NPC opening a `WndOptions`
from `interact(Char)` on the render thread. `levels/LastShopLevel.java` is the best structural
model overall: a complete hand-built, mob-free floor with a shop on it.

#### Implementation Steps

1. **`VillageLevel` skeleton, no content.** Build a closed 40x40 grass field with one entrance
   tile and a spawn point. Test: level builds, is fully passable where intended, has exactly one
   `REGULAR_ENTRANCE` transition, spawns no mobs and no respawner.
2. **`VillageScene` + `VillageSession`.** Enter it from a temporary debug button; walk the town
   avatar around; save and reload position. Test: round-trip of the village bundle.
3. **`HouseLevel` + the door pair.** Village door → house, house door → village, position
   preserved on both sides. Test: transition targets resolve both ways.
4. **`WndDungeonMode` + `DungeonGateway`.** Prompt appears on the entrance, `Not yet` cancels the
   move, Solo/Ranked reach `HeroSelectScene` with `GamesInProgress.selectedEchoPlayMode` set.
   Test (pure, no UI): `DungeonGateway.resolveMode(...)` and the online/auth gating decide the
   right target and the right enabled options for backend-up / backend-down / unauthenticated.
5. **Title screen rewiring.** One **Enter Village** button; the direct Solo/Ranked buttons go
   away. Keep the debug entry (`btnDebug` / `beginDebugRun`, `TitleScene.java:183-190`) as-is.
6. **Return path.** Death and victory land back in `VillageScene`.
7. **Dress the village.** `CustomTilemap` overlays from `city_quest.png` / `city_boss.png`, NPCs,
   water, the green, and the `surface.png` sky in `VillageScene`.
8. **Polish.** Message bundle, tile `name()`/`desc()` tooltips for the house and dungeon mouth,
   music/ambience selection.

Steps 1–4 are the feature; 5–8 make it the front door. Per `AGENTS.md`, each step is TDD —
AssertJ, `@DisplayName`, tests in `core/src/test/java` — and the pure-helper shape of
`Dungeon.levelClassForDepth` is the model to copy: put routing and gating decisions in static
helpers that a test can call without a scene.

#### Risks and open questions

- **`Level` outside a run.** `Level` and `GameScene` reach for `Dungeon.hero`, `Dungeon.depth`
  and `Statistics` in places. Two options: (a) `VillageScene` sets up a minimal `Dungeon` context
  (`depth = 0`, a puppet hero) purely as scaffolding without ever calling `Dungeon.init()`, or
  (b) a slimmer scene that reuses the tilemap/actor rendering but not the run machinery. **(a) is
  the recommendation** — far less new code, and it keeps NPC interaction, movement and the
  tilemap working unchanged. The cost is discipline: never save that scaffolding as a run.
- **Save-slot interaction.** The village is per-player, but runs are per-slot *and* per-mode. If
  the player has an in-progress Solo run and picks Ranked at the entrance, they must reach the
  Ranked slot list, not resume the Solo one — `GamesInProgress.selectEchoPlayMode` before
  `StartScene` already handles this, but it needs an explicit test.
- **`Statistics.deepestFloor`** is only bumped for `depth > deepestFloor` (`Dungeon.java:373-374`),
  so a depth-0 concept would be harmless there — but this design avoids depth 0 in runs anyway.
- **Three-platform safety.** Per `AGENTS.md`, everything must stay inside the RoboVM-safe JDK
  subset and Android-safe `org.json`; no new dependencies for this task.
- **Existing saves.** Players mid-run when this ships must still be able to resume. The title
  screen should keep a "Continue" path straight into the run, bypassing the village, until the
  run ends.

#### Build & verify

Per `AGENTS.md`, and per the `gradle-worktree` rule never run two Gradle builds in one checkout:

```
./gradlew :core:test -q -PerrorProneOff
./gradlew :core:spotlessApply
./gradlew :core:spotlessCheck :core:test    # before PR
./gradlew :desktop:run                      # in a separate worktree
```
