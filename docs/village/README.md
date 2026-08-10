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

**The village is an ordinary `Level` at depth 0, but it is not part of any run.** It is a real
level — same tilemap, same `GameScene`, same hero, same movement and NPC interaction as anywhere
else in the game — living in a reserved save slot of its own, always under the solo namespace. A
*run* only begins when the player walks into the dungeon entrance and picks a mode.

The constraint that forces this is concrete: run saves are namespaced by play mode.
`GamesInProgress.gameFolder(slot)` appends `EchoPlayModePaths.gameFolderSuffix()` —
`-solo` / `-ranked` / `-debug` (`core/src/main/java/…/GamesInProgress.java:102`,
`heroechoes/EchoPlayModePaths.java:33-43`). Echo storage and the leaderboard file are namespaced
the same way, and `Dungeon.init()` reads the mode when deciding seed, challenges and easy-mode
eligibility (`Dungeon.java:243-265`). So a run genuinely cannot start before the mode is known,
without either guessing the folder or migrating save data mid-run.

Two ways out were considered. Building a bespoke village *scene* outside the `Level`/`Hero`
machinery avoids the save question entirely, but re-implements movement, rendering and NPC
interaction — a large amount of new surface for a hub with no mechanics. The shipped approach
instead keeps the village a real level and sidesteps the namespacing with two cheap invariants:

- **A reserved save slot.** `VillageGateway.VILLAGE_SLOT = GamesInProgress.MAX_SLOTS + 1`, past
  every run slot. `GamesInProgress.firstEmpty()` and `checkAll()` only scan `1..MAX_SLOTS`, so
  the village can never collide with a run or appear in the save list.
- **Always solo storage.** The village is stored under `-solo` because it *is* a private, solo
  place. Nothing about it is mode-specific, so nothing needs migrating when a run commits to
  ranked.

The ordering that results:

```
TitleScene ──"Enter the Village"──► depth 0, slot VILLAGE_SLOT, solo namespace
                 │
                 ├── house door ──► depth 0 / branch 1 (HouseLevel, always solo)
                 │
                 └── dungeon entrance ──► WndDungeonMode [Solo | Ranked | Not yet]
                          → save the village (still solo)
                          → GamesInProgress.selectEchoPlayMode(mode)   ← namespace decided here
                          → HeroSelectScene / StartScene
                          → InterlevelScene(DESCEND) → Dungeon.init() → depth 1
```

The village hero is a throwaway town avatar: choosing a mode at the mouth starts a genuinely
fresh run, so nothing carries from the village into the dungeon and no save is ever migrated.

This still leaves task 2 room: the village is persistent and run-independent, which is what a
shared village and a chat channel need. The one thing it does *not* give for free is a village
that exists while the player is descending — see task 2's open questions.
