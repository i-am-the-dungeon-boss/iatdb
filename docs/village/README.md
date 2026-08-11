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

**The village shares nothing with the dungeon.** It is not a dungeon depth, not a `Level`, and the
figure the player walks around the village is not a `Hero`. While the player is in the village
there is no dungeon at all: `Dungeon`, the hero, and the run save are only created when the player
answers the prompt at the dungeon entrance and picks a mode.

This costs real code — the village brings its own map data, tilemap renderer, avatar, movement and
save file rather than reusing the dungeon's. It buys a boundary that holds:

- **Permadeath and persistence are different lifetimes.** The run hero dies; the village persists.
  Sharing one object forces death to be special-cased forever.
- **Ranked integrity.** Nothing can cross from the village into a run, because there is nothing to
  cross with — a run starts fresh from hero select. No rule needs enforcing.
- **No shared bookkeeping to get wrong.** An earlier draft made the village depth 0 of a real run
  in a reserved save slot; it crashed because a hero position belonging to one ground-level map was
  applied to the other through shared depth/branch state. That entire class of bug is gone.
- **Multiplayer later.** Task 2 needs a hub that exists independently of any run, and a presence
  payload with no run state in it. That is exactly what this is.

```
TitleScene ──"Enter the Village"──► VillageScene (own map, avatar, save file)
                 │                    no Dungeon, no Hero, no run save exists yet
                 │
                 ├── front door ──► the house (always solo)
                 │
                 └── dungeon entrance ──► WndDungeonMode [Solo | Ranked | Not yet]
                          → VillageSave.save()
                          → GamesInProgress.selectEchoPlayMode(mode)  ← the run is created here
                          → HeroSelectScene / StartScene → the game as it has always been
```

The one thing the village still borrows from the dungeon side is *art*: `DungeonTileSheet`'s tile
indices, which are plain constants describing where sprites sit on a tileset and carry no run
state.
