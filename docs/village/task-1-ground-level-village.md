### Task 1: Ground Level — House, Village, Dungeon Entrance

**Status**: implemented.

**Goal**: Replace "press Solo/Ranked on the title screen" with "land in a village, walk to the
dungeon, and choose your mode at its mouth." The player lands in a village with their own house
(always solo), and enters the dungeon through a prompt that commits the run to Solo or Ranked.

#### Scope and Key Concepts

- **Ground level** = the village and the house. Wholly separate from the dungeon: its own map
  data, renderer, avatar and save file (see
  [README.md](README.md#the-one-architectural-decision-everything-hangs-on)).
- **House** — the interior behind the front door. Always solo, and never populated.
- **Village** — the outdoor area, with a shopkeeper and villagers. There is no combat, no hunger
  and no danger, because none of those systems exist here at all.
- **Dungeon entrance** — walking onto it raises the Solo/Ranked prompt. **The dungeon is created
  when that prompt is answered**, and not before.

#### What shipped

All new, under `core/src/main/java/…/village/`:

| File                     | Role                                                                    |
| ------------------------ | ----------------------------------------------------------------------- |
| `VillageTerrain.java`     | The village's own terrain vocabulary and passability                    |
| `VillageMap.java`         | Plain tile data, both hand-authored layouts, and breadth-first routing  |
| `VillageTilemap.java`     | Flat tile renderer over watabou's `Tilemap`                             |
| `VillageAvatar.java`      | The player's figure: a `MovieClip` that walks a route                   |
| `VillageNpc.java`         | A villager: sprite, cell, and something to say                          |
| `VillageScene.java`       | The scene: camera, input, dialogue, area transitions                    |
| `VillageSave.java`        | Position and appearance, in its own file                                |
| `VillageGateway.java`     | The one-way door from village into a run                                |
| `windows/WndDungeonMode`  | Solo / Ranked / Not yet prompt                                          |
| `test/…/VillageMapTest`   | Layout, closure, reachability, routing, prompt mapping                  |

Changed outside the village package — deliberately almost nothing:

| File                              | Change                                                             |
| --------------------------------- | ------------------------------------------------------------------ |
| `scenes/TitleScene`               | Solo + Ranked buttons replaced by one **Enter the Village** button  |
| `scenes/TitleSceneOnlineOnlyTest` | Re-pinned to the new contract (still online- and auth-gated)        |
| `messages/misc`, `messages/scenes` | New strings                                                        |

`Dungeon`, `Level`, `Hero`, `Hunger` and the level-routing switch are **untouched**. A dungeon run
is byte-for-byte the run it was before this feature.

#### Layout

```
Village (33x33)                                House (13x12)
┌───────────────────────────────┐               ┌─────────────────┐
│ ~ ~ ~ ~ water ~ ~ ~ ~ ~ ~ ~ ~ │               │ hearth  shelf   │
│ ▓▓▓▓▓▓▓▓▓  ← house     ▓▓▓▓▓  │               │ chest      pot  │
│ ▓       ▓        stall ▓   ▓  │               │                 │
│ ▓▓▓█▓▓▓▓▓              ▓▓▓▓▓  │               │        █        │← door row
│    ║ ← front door  ○ well     │               └─────────────────┘← solid ring
│    ║        ☼ firepit         │
│    ║   ░ ░ tall grass ░ ░     │
│  ☗ ║ ☗  ← statues              │
│    ▼ DUNGEON ENTRANCE          │
└───────────────────────────────┘
```

Both maps keep a solid outer ring, and the house's door row sits one row in from the bottom so
that ring is never broken.

#### Rendering, and what it costs

The dungeon's tilemaps autotile walls into raised, three-dimensional visuals, but that logic reads
the global dungeon level, so it cannot be reused across the boundary. `VillageTilemap` draws flat
tiles straight from the city tileset instead. **The village therefore reads as a flat top-down
settlement rather than a dungeon interior.** That is the visible price of the separation; adding a
village-owned autotiler later would close the gap without touching the boundary.

Art is all existing: `tiles_city.png`, the shopkeeper / blacksmith / wandmaker / ghost sprites for
villagers, the hero class spritesheets for the avatar, and the city music tracks.

#### Movement and interaction

Tap a cell: `VillageMap.route` runs a breadth-first search over passable cells and the avatar walks
it, one orthogonal step at a time, interpolating between cells. Tap a villager: a titled message.
Walk onto the front door: the scene switches to the other area. Walk onto the dungeon entrance:
`WndDungeonMode`.

There is no pathfinder shared with the dungeon, no turn scheduler and no actor system — the village
has nothing to schedule.

#### Starting a run

`VillageGateway.beginRun(mode)` saves the village, then does exactly what the title screen used to:
`GamesInProgress.selectEchoPlayMode(mode)` — which is what decides the run's save namespace, since
`gameFolder` is keyed by play mode — then `selectedClass = null`, a run slot, and
`HeroSelectScene` or `StartScene`. Ranked passes through `EchoPlayerAuthGate.ensureReadyThen`
first, and is disabled in the prompt when `EchoBackendProbe.isOnlineReady()` is false.

#### Persistence

`VillageSave` writes `village.dat`: which area you were in, which cell, and which hero class the
avatar wears. Outside `GamesInProgress` and outside every play-mode folder, because the village
belongs to the player and outlives every run. The maps themselves are rebuilt from code, so
nothing else needs storing.

#### Tests

`VillageMapTest` builds both maps and asserts: declared sizes; that neither map's outer ring is
walkable (the invariant that keeps the map closed); that the arrival point, front door and dungeon
entrance are walkable and correctly typed; that the dungeon entrance and front door are reachable
by route from where the player arrives; that routes are strictly orthogonal and never wrap a row
edge; that there is no route into solid rock; and the prompt's option→mode mapping.

#### Known gaps / follow-ups

- **Not yet run in the live game.** The maps, routing and mode mapping are covered by tests, but
  the scene itself — rendering, camera, input, dialogue — has only been compiled, not played.
- **Flat visuals**, as above.
- **Returning from a run** still goes to rankings and the title screen rather than back to the
  village.
- **The shopkeeper cannot trade.** Under a strict split there is no shared currency or inventory,
  so they are currently flavour only. Either keep them flavour or give the village a solo-only
  economy — a design decision, not an oversight.
- **The house is furnished but inert**; the chest is scenery.

#### Build & verify

```
./gradlew :core:test -q -PerrorProneOff
./gradlew :core:spotlessCheck :core:test    # before PR
./gradlew :desktop:run                      # in a separate worktree
```
