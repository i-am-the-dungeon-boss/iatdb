# Village — Ground Level Docs

Design docs for the **ground level**: the outdoor hub the player lands on before entering the
dungeon. It holds the player's **house** (always solo, even later), a **village** with NPCs, and
the **dungeon entrance**, which is where the player commits to **Solo** or **Ranked**.

These are *plans*, not a record of shipped code. Nothing in this folder is implemented yet.

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

The village is **not a dungeon depth of a run**. It is a separate, persistent, *mode-independent*
place, and a run only begins when the player walks into the dungeon entrance and picks a mode.

The reason is concrete: run saves are namespaced by play mode.
`GamesInProgress.gameFolder(slot)` appends `EchoPlayModePaths.gameFolderSuffix()` —
`-solo` / `-ranked` / `-debug` (`core/src/main/java/…/GamesInProgress.java:102`,
`heroechoes/EchoPlayModePaths.java:33-43`). Echo storage and the leaderboard file are namespaced
the same way. So a run cannot start before the mode is known without either guessing the folder
or migrating save data mid-run — and `Dungeon.init()` also reads the mode when deciding seed,
challenges and easy-mode eligibility (`Dungeon.java:243-265`).

Making the village a depth-0 floor *inside* a run would therefore force the mode choice back to
the title screen, which is exactly what this feature is trying to move into the world. Keeping
the village outside the run keeps the ordering honest:

```
TitleScene → VillageScene (persistent, no mode) → house / village / NPCs
                  └── dungeon entrance → WndOptions[Solo | Ranked]
                          → GamesInProgress.selectEchoPlayMode(mode)
                          → HeroSelectScene → InterlevelScene(DESCEND) → Dungeon.init() → depth 1
```

It is also what makes task 2 possible: a persistent, run-independent village is the thing that
can later carry other players and a chat channel. A per-run depth-0 floor could not.
