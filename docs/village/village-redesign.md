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
| Basins | A basin of `Terrain.WATER` in each quarter, **cut to its own shape** — a sluice for the sewers, a squared tank for the prison, a wandering stream for the caves, a formal pool for the city — with an [`AltarPool`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/village/AltarPool.java) laid over it drawing that region's own water image (`water0..3.png`). A level has one water texture, so four different waters cannot be terrain — the region's colour has to come from the overlay. The cost is that these basins do not ripple, the overlay drawing above the scrolling bed. |
| Basin shores | [`AltarShore`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/village/AltarShore.java), one per basin, laid over the water it edges. Necessary because the plaza is floored with `EMPTY_SP`, which is **not** in `DungeonTileSheet.waterStitcheable`: the terrain layer stitches every basin cell against nothing, lands on the bare `WATER` index, and `DungeonTerrainTilemap.needsRender` then skips it — leaving water with no shoreline at all, a square hole cut in the floor. The four-bit stitch is worked out from the basin's own shape instead, and drawn from the region's sheet so the lip matches the water. A corollary: **no basin cell may have water on all four sides**, or it lands back on that skipped index and the pool renders with a hole in it. |
| Dock | [`VillageDock`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageDock.java), east of the road down through town, planked with the **sewers** sheet's `FLOOR_SP`. The city sheet has no wood on it at all — its worked floor is the walkway's red carpet, so a dock in the level's own tiles reads as a rug on the water. |
| Throne | A sixth `CustomTilemap` on `Assets.Environment.CITY_BOSS`, art borrowed from [`CityBossLevel.CustomGroundVisuals`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/CityBossLevel.java). Throne tiles are `13*8+1..3`, `14*8+1..3`, `15*8+1..3`; shadow is `13*8+5` on the wall layer. The seat sits on the dead centre of the disc and stays **walkable** — unlike the arena's `Terrain.CUSTOM_DECO`, because the deepest echo's post *is* the chair: the reigning Halls champion is found sitting in it, not standing in front of it. |
| Raised dais | The halls sheet's worked floor over a 5×5 centre, with a step tread where each arm of the walkway meets it. *(The sketch's elevation ring on `customWalls` is not built — it needs art the halls sheet does not contain.)* |
| Walkway | Carpet cross through the disc — north to the dungeon gate, south to the tavern and dock, east/west to the wood-paths. |
| Occupants | The existing `VillageEcho` pattern: `PASSIVE` + `IMMOVABLE` NPC, `level.mobs.add(body)` then `GameScene.addSprite(body)`, as in [`VillageFigures.java`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/village/VillageFigures.java). |

Base terrain under the entire disc stays a walkable floor type. Every region texture is a
purely visual overlay on top of it.

## Implementation notes

- `VillageLevel.tilesTex()` returns `TILES_CITY` and the village previously used **no**
  `CustomTilemap` at all. Six of them is a new pattern for this level — mechanically fine,
  since `CustomTilemap.texture` is per-instance, but no existing level in the repo mixes
  region tilesets like this.
- **Nothing on the altar may call `create()` outside the game.** It resolves the texture
  through the texture cache and so needs a GL context the headless test harness does not
  have. Every altar piece therefore implements
  [`AltarOverlay`](../../core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/village/AltarOverlay.java)
  — a pure `tileData()` plus the sheet it is read from — which is what both the tests and
  [`VillageMapRenderer`](../../core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/tools/VillageMapRenderer.java)
  use. `create()` only feeds that array to the tilemap.
- **Each quarter is floored by colour, not by `FLOOR` everywhere.** The plain floors of the
  sewers and the prison are within a few values of the same grey, and the caves' is dark
  enough (43,40,38) to read as a hole in the plaza; `FLOOR_ALT_1` and `FLOOR_ALT_2` are
  pixel-identical to `FLOOR` on all five sheets, so alt-tile variance buys nothing. Sewers
  and caves take their sheet's `FLOOR_SP`, prison and city their `FLOOR` — the city cannot
  take `FLOOR_SP`, because on that sheet it is the red carpet the walkway is made of.
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
