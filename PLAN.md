# Technical Implementation Plan: Hero-as-Boss Feature

## 1. Feature Overview

When a player defeats a region boss (depths 5, 10, 15, 20, 25), the game captures a **hero echo**. Other players can fight that hero as a **dungeon boss** instead of the default floor boss — driven by a server-generated **echo policy** fetched once per fight.

**Hero Echoes** is the online service (separate proprietary repo: `hero-echoes-backend`). The game client (this repo, GPL) implements capture, sync, spawning, combat execution, and a generic policy interpreter.

See [docs/hero-echoes/README.md](docs/hero-echoes/README.md) and [online-integration.md](docs/hero-echoes/online-integration.md).

---

## 2. Core Components

| Task | Goal                                       | Doc                                                                  |
| ---- | ------------------------------------------ | -------------------------------------------------------------------- |
| 1    | Echo capture, local storage, online upload | [task-1](docs/hero-echoes/task-1-echo-capture-and-storage.md)        |
| 2    | Boss replacement, online fetch + fallback  | [task-2](docs/hero-echoes/task-2-boss-replacement-logic.md)          |
| 3    | Combat parity + **policy interpreter**     | [task-3](docs/hero-echoes/task-3-hero-as-a-boss-ai-and-mechanics.md) |
| 4    | UI/UX, settings, messaging                 | [task-4](docs/hero-echoes/task-4-ui-ux-and-polish.md)                |
| 5    | Leaderboard local + online sync            | [task-5](docs/hero-echoes/task-5-leaderboard-feature.md)             |
| —    | **Online integration**                     | [online-integration.md](docs/hero-echoes/online-integration.md)      |

---

## 3. Architecture: Client vs Server

```
[Game client — GPL, this repo]
  Echo, EchoStorage, EchoClient
  EchoReplacementDecider → GET /v1/echoes/{depth}
  EchoBoss + EchoPolicyMatcher / EchoRoleExecutor
  EchoLeaderboardStorage + optional fetch

[Hero Echoes service — proprietary, hero-echoes-backend]
  POST/GET /v1/echoes (+ echo_policy on fetch)
  POST/GET /v1/leaderboard/*
  Policy generator, pool, admin, web UI
```

**One request per echo fight:** echo fetch includes `echo_policy`. No per-turn API calls.

---

## 4. Phased Implementation

### Phase 1: Local loop ✅ (mostly done)

- Hero echo capture & local `echoes/` storage
- Depth-5 boss replacement, `EchoBoss`, settings, intro messaging
- Leaderboard plumbing (local)

### Phase 2: Combat parity (in progress)

- Combat delegation (evasion, procs, speed, attack delay)
- Policy-driven item/role use (HEAL, cleanses, throws, movement); more parity still open
- See [gap analysis](docs/hero-echoes/echo-boss-gap-analysis.md) (AI sections partly historical)

### Phase 3: Online integration ✅ (core done)

1. **`EchoClient`** — `GET/POST /v1/echoes`, leaderboard endpoints
2. **`EchoWireCodec`** — `echo_data_base64` encoding
3. **`EchoPolicy` + matcher/executor** — role-based policy runtime
4. **`CompositeEchoLookup`** — ranked online first, local/solo paths
5. Async upload after boss kill; async result POST after echo fight
6. Backend deployed separately (`hero-echoes`)

### Phase 4: Expansion & polish

- All boss depths (10/15/20/25) + echo capture on all regional bosses
- Full UI/UX, i18n, echo viewer polish
- Optional in-game fetch of web leaderboard data

---

## 5. API contract (summary)

Full spec: `hero-echoes-backend/docs/features/`

| Method | Path                               | Auth    | Purpose                           |
| ------ | ---------------------------------- | ------- | --------------------------------- |
| `GET`  | `/v1/echoes/{depth}?game_version=` | —       | Fetch echo + **echo_policy**      |
| `POST` | `/v1/echoes`                       | API key | Upload echo after boss kill       |
| `POST` | `/v1/leaderboard/results`          | API key | Record echo fight outcome         |
| `GET`  | `/v1/leaderboard/{depth}`          | —       | Ranked results (optional in-game) |

---

## 6. Offline behavior

The game remains playable offline:

- No online → local echo pool or default boss
- Upload/leaderboard sync failures are non-blocking
- Local storage always written before online sync attempts

Online mode is required for **community echoes** from the global pool; local-only echoes remain possible via `echoes/` directory.
