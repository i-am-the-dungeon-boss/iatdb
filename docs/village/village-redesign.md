# Village redesign — design sketch

Status: **design agreed, not implemented.** This captures the layout decisions made during
sketching so implementation can start from a settled target. Current shipped layout is
[`village-map.png`](village-map.png), rendered by
[`VillageMapRenderer.java`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/tools/VillageMapRenderer.java).

## Why

The village today is a 33×33 field of grass with a carpet path down column 8, a few
scattered props (well, firepit, market stall, two statues) and four villagers. It reads as
an empty lawn rather than an inhabited place, the map border is visible black, and the five
echo `DEPTH_POSTS` sit in an unremarkable row along the waterfront. The redesign makes the
village denser, makes its edges read as endless, and gives the echo bosses a real monument.

## Village layout

```
                    forest belt (N)
              ┌──────────────────────────┐
              │        DUNGEON gate      │   → WndDungeonMode
              │            │             │
   GUILD ─────┤            │             ├───── SMITHY
 (crier,      │            │             │    (forge glow)
  leaderboard)│      ECHO ALTAR          │
              │      (see below)         │
  ← wood-path ┼──────  ●  ───────────────┼ wood-path →
              │                          │
    ELDER ────┤            │             ├───── SHOP
              │          TAVERN          │    (awning, coin sign)
              │        (world chat)      │
              └────────────┬─────────────┘
                    sand ──┴── DOCK ──── boat
                        sea (S)
```

| Element | Placement | Notes |
| --- | --- | --- |
| Dungeon gate | North, centre | Stone gatehouse. Moves from the current bottom-of-map position. |
| Echo Altar | Centre | See below. Replaces the plain plaza. |
| Guild | NW | Title crier + leaderboard board. |
| Smithy | NE | Forge glow as a light source. |
| Elder | W | Small cottage. |
| Shop | E | Replaces the player house, which is dropped entirely. |
| Tavern | S, on the shore | World-chat gathering spot; dock runs off its porch. |
| Dock | S, into the water | Planks + moored boat. |
| Wood-paths | E and W | Dirt paths that trail off into the forest. Decorative, non-walkable at the fade. |

No village gate — the east/west dirt paths fading into the trees carry the "there is more
world out there" read instead.

### Endless edges

Two changes together:

1. **Forest belt** on the north, east and west edges and **sea** on the south, so no map
   border is ever the last thing you see.
2. **Camera pan clamp** so the player cannot scroll past that belt. This does not exist
   today — `com.watabou.noosa.Camera` has no bounds field and no clamping anywhere in the
   repo, and edge-scroll in `PixelScene`/`CellSelector` is unbounded. This is new code.

## The Echo Altar

One connected disc at the centre of the village, split into four quarters with a raised
circular dais at the middle.

```
            ╭─────────────────────╮
        ╱   SEWERS   │   PRISON    ╲
      ╱     echo     │    echo       ╲
     │      1–5      │    6–10        │
     ├──────────  ╭─────╮  ──────────┤   ← carpet cross = walkway
     │            │HALLS│             │
     │            │21–25│             │
     │            ╰─────╯             │
      ╲     CITY     │    CAVES      ╱
        ╲    echo    │    echo      ╱
            ╰──── 16–20 │ 11–15 ────╯
```

Quarters run clockwise by depth from the NW: Sewers → Prison → Caves → City, with Halls
raised at the centre on the Dwarf King's throne.

**Only echo bosses stand here.** The actual dungeon bosses (Goo, Tengu, DM-300, Dwarf King,
Yog-Dzewa) never appear in the village. Each quarter's occupant is the reigning player echo
for that depth band; the region identity is carried entirely by the ground under them.
Honorable mentions stay in their existing cluster around the well.

### Construction

| Piece | Mechanism |
| --- | --- |
| Four quarter wedges | One `CustomTilemap` each, `texture` set to that region's sheet — `TILES_SEWERS`, `TILES_PRISON`, `TILES_CAVES`, `TILES_CITY`. |
| Halls centre | A fifth `CustomTilemap` on `TILES_HALLS`. |
| Throne | A sixth `CustomTilemap` on `Assets.Environment.CITY_BOSS`, painted over `Terrain.CUSTOM_DECO` — the same recipe as [`CityBossLevel.CustomGroundVisuals`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/CityBossLevel.java). Throne tiles are `13*8+1..3`, `14*8+1..3`, `15*8+1..3`; shadow is `13*8+5` on the wall layer. |
| Raised dais | Ring drawn via `customWalls` so it reads as elevated, with step treads on all four approaches. |
| Walkway | Carpet cross through the disc — north to the dungeon gate, south to the tavern and dock, east/west to the wood-paths. |
| Occupants | The existing `VillageEcho` pattern: `PASSIVE` + `IMMOVABLE` NPC, `level.mobs.add(body)` then `GameScene.addSprite(body)`, as in [`VillageFigures.java`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageFigures.java). |

Base terrain under the entire disc stays a walkable floor type. Every region texture is a
purely visual overlay on top of it.

## Implementation notes

- `VillageLevel.tilesTex()` returns `TILES_CITY` today and the village uses **no**
  `CustomTilemap` at all. Six of them is a new pattern for this level — mechanically fine,
  since `CustomTilemap.texture` is per-instance, but no existing level in the repo mixes
  region tilesets like this.
- All five `tiles_*.png` share **one 16-column layout, and tile index N is the same terrain on
  every sheet** — `DungeonTerrainTilemap.getTileVisual()` computes indices from
  `DungeonTileSheet` constants with no region branching at all. So `DungeonTileSheet.FLOOR`
  is plain floor on the sewers sheet, the caves sheet and the rest alike; no per-sheet index
  hunting is needed. (`custom_tiles/city_boss.png` is the exception: it is 8 columns, not 16.)
- The five `DEPTH_POSTS` move from `PROMENADE_X`/`PROMENADE_Y` onto the altar quarters.
  `MENTION_POSTS` keep their arrangement but follow the well, which moves off the middle of
  town to (7,13) to make room for the altar.

### Tests this will break

These assert exact coordinates and layout invariants, so they need updating alongside:

- [`VillageLevelBuildTest.java`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageLevelBuildTest.java) — map size, exit position, arrival cell one step short of the mouth, path reachability.
- [`VillageFigurePlacementTest.java`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageFigurePlacementTest.java) — every post unique, walkable, and not blocking the path.
- [`VillageFiguresTest.java`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageFiguresTest.java) — reconciliation against the post arrays.

Per the repo's TDD rule, characterize-then-change: update these tests to the new layout
first, watch them fail, then build.

## Open

- Exact disc radius in tiles, and whether the altar footprint forces the 33×33 `SIZE` up.
- Whether the dungeon gate moving north changes `arrivalCell()`/`dungeonEntrance()` semantics.
- Camera clamp design — where the bounds live and how they are set per level.
