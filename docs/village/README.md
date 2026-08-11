# Village — Ground Level Docs

Design docs for the **ground level**: the outdoor hub the player lands on before entering the
dungeon. It holds the player's **house** (always solo, even later), a **village** with NPCs, and
the **dungeon entrance**, which is where the player commits to **Solo** or **Ranked**.

Task 1 is **implemented**. Task 2 is still a forward plan.

---

## Documents

| Doc                                                                                | Topic                                                                     |
| ---------------------------------------------------------------------------------- | ------------------------------------------------------------------------- |
| [task-1-ground-level-village.md](task-1-ground-level-village.md)                   | The single-player ground level: layout, levels, transitions, mode prompt  |
| [task-2-multiplayer-village-and-world-chat.md](task-2-multiplayer-village-and-world-chat.md) | Forward path: shared village presence + world chat                        |

Related: [`../hero-echoes/`](../hero-echoes/) (play modes, online client, auth),
[`../../PLAN.md`](../../PLAN.md).

---

## The one architectural decision everything hangs on

The boundary is drawn through **state and lifetime, not through machinery**.

**Separate:** the figure standing in the village is not the run hero, and the village is not part
of a run. Nothing crosses from the village into the dungeon, and the village outlives every run's
death. The dungeon run is *created* only when the player answers the Solo/Ranked prompt at the
dungeon entrance.

**Shared:** everything the player touches. The village is a `Level`, played in `GameScene`, with
the dungeon's own controls, camera, cell selector, fog of war and autotiled visuals. Reimplementing
those produces a hub that plays and looks subtly wrong, so the village extends the dungeon's
presentation rather than duplicating it. Its field of view is 12 against the dungeon's 8 — a little
further, because the village is safe and should read as open, but not so far that the map is simply
handed over.

Two invariants keep the separate half honest, given that `GamesInProgress.gameFolder` is keyed by
play mode (`-solo` / `-ranked` / `-debug`) and `Dungeon.init()` reads the mode when choosing seed
and challenges:

- **A reserved save slot.** `VillageGateway.VILLAGE_SLOT = GamesInProgress.MAX_SLOTS + 1`, past
  every run slot. `firstEmpty()` and `checkAll()` only scan `1..MAX_SLOTS`, so the village can
  never collide with a run or appear in the save list.
- **Always-solo storage.** The village is stored under `-solo` because it is a private, solo place.
  Nothing about it is mode-specific, so nothing needs migrating when a run commits to ranked.
- **Never resumed.** The ground level is discarded and rebuilt on every entry. Saved levels carry
  their own dimensions in their bundle, so a stored village would outlive any change to the map —
  and did, crashing once the house geometry changed underneath an existing save.

The town avatar is disposable: choosing a mode at the mouth starts a genuinely fresh run from hero
select, so nothing carries down and no save is ever migrated.

```
TitleScene ──"Enter the Village"──► depth 0, slot VILLAGE_SLOT, solo namespace
                 │
                 ├── front door ──► depth 0 / branch 1 (the house, always solo)
                 │
                 └── dungeon entrance ──► WndDungeonMode [Solo | Ranked | Not yet]
                          → save the village (still solo)
                          → GamesInProgress.selectEchoPlayMode(mode)  ← the run is created here
                          → HeroSelectScene / StartScene → depth 1
```
