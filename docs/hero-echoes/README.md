# Hero Echoes — Game Client Docs

Design and implementation docs for the **Hero Echoes** feature in *I Am The Dungeon Boss* (GPL game client fork).

The **Hero Echoes service** (API, echo policy generator, web UI, admin) lives in the separate **`hero-echoes-backend`** repo and is proprietary. This folder documents **game client** responsibilities only.

---

## Documents

| Doc                                                                                    | Topic                                                                   |
| -------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| [online-integration.md](online-integration.md)                                         | **Hero Echoes online service** — API client, echo policy, repo boundary |
| [task-1-echo-capture-and-storage.md](task-1-echo-capture-and-storage.md)             | Echo capture, local storage, upload                                 |
| [task-2-boss-replacement-logic.md](task-2-boss-replacement-logic.md)                   | Boss floor routing, fetch + fallback                                    |
| [task-3-hero-as-a-boss-ai-and-mechanics.md](task-3-hero-as-a-boss-ai-and-mechanics.md) | Combat parity + **policy interpreter**                                  |
| [task-4-ui-ux-and-polish.md](task-4-ui-ux-and-polish.md)                               | Messaging, settings, visuals                                            |
| [task-5-leaderboard-feature.md](task-5-leaderboard-feature.md)                         | Fight results, local + online leaderboard                               |
| [NAMING.md](NAMING.md)                                                                 | Echo naming convention (code, API, UI)                                  |
| [echo-boss-gap-analysis.md](echo-boss-gap-analysis.md)                                 | Echo vs EchoBoss parity checklist                                       |

Top-level plan: [PLAN.md](../../PLAN.md)

---

## External spec (backend repo)

Full API and web UI specs:

```
hero-echoes-backend/docs/BACKEND-SPEC.md
hero-echoes-backend/docs/features/
```

Key endpoints the game client uses:

| Method | Path                                  | When                               |
| ------ | ------------------------------------- | ---------------------------------- |
| `GET`  | `/v1/echoes/{depth}?game_version=` | Enter boss floor (online)          |
| `POST` | `/v1/echoes`                       | After boss kill (online)           |
| `POST` | `/v1/leaderboard/results`             | After echo fight (online)          |
| `GET`  | `/v1/leaderboard/{depth}`             | Optional in-game leaderboard fetch |

Echo fetch returns **`echo_policy`** alongside echo fields — one request per fight.

---

## Client vs server split

| In game client (GPL)                               | In Hero Echoes service (proprietary)   |
| -------------------------------------------------- | -------------------------------------- |
| Echo capture & `echo_data_base64` encode       | Echo pool, retention, random fetch |
| Spawn echo boss from echo                      | Echo **policy generator**              |
| Generic **policy interpreter** in `EchoBoss.act()` | Policy rules tuned per echo        |
| Combat execution (move, attack, use item)          | Leaderboard storage & ranking          |
| Local fallback storage                             | Web leaderboard pages, admin           |
| Thin `EchoClient` HTTP layer                       | Auth, CORS, ops                        |

---

## Implementation status (summary)

| Area                         | Status                     |
| ---------------------------- | -------------------------- |
| Local echos & storage    | Done                       |
| Boss replacement (depth 5)   | Done                       |
| EchoBoss combat / AI stubs   | Partial — see gap analysis |
| **EchoClient / online sync** | **Not started**            |
| **Echo policy interpreter**  | **Not started**            |
| Leaderboard local + UI       | Partial                    |
| All boss depths / triggers   | Partial                    |
