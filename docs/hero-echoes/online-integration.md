# Online Integration (Hero Echoes)

How the game client integrates with the **Hero Echoes** online service.

**Status:** Spec defined; client not implemented  
**Backend spec:** `hero-echoes-backend/docs/` (sibling repo)  
**Related tasks:** [Task 1](task-1-echo-capture-and-storage.md), [Task 2](task-2-boss-replacement-logic.md), [Task 3](task-3-hero-as-a-boss-ai-and-mechanics.md), [Task 5](task-5-leaderboard-feature.md)

---

## Overview

Hero Echoes is a private backend that:

1. Collects hero echos after boss kills
2. Serves random compatible echoes when entering a boss floor
3. Returns a stored **`echo_policy`** (combat plan) with each ranked fetch; solo asks `POST /v1/echoes/policy` for a generated plan — **one HTTP call per fight**
4. Stores and ranks fight outcomes on a leaderboard

The game client ships a **thin HTTP client** and a **role-based policy matcher/executor**. Proprietary tuning lives on the server.

---

## Configuration

| Setting          | Purpose                           |
| ---------------- | --------------------------------- |
| Backend base URL | e.g. `https://echoes.example.com` |
| API key          | `X-API-Key` header on `POST` only |

Store in `SPDSettings` or build-time config — never hard-code secrets in public builds.

---

## Planned client modules

| Class / package            | Responsibility                                        |
| -------------------------- | ----------------------------------------------------- |
| `heroechoes.EchoClient`    | HTTP: fetch echo, upload echo, post/fetch leaderboard |
| `heroechoes.EchoPolicy`    | Parse `echo_policy` JSON from fetch response          |
| `EchoPolicyMatcher` / …    | Match + execute roles each hunting turn               |
| `heroechoes.EchoWireCodec` | `Echo` ↔ JSON (`echo_data_base64`)                    |

Existing classes extended, not replaced:

- `EchoStorage` — local fallback; optional demote when online-only path chosen
- `EchoReplacementDecider` — try `EchoClient.fetchEcho(depth)` before local pool
- `EchoBoss` — policy match/execute in `act()`; unresolved → mob hunting AI
- `EchoLeaderboardStorage` — append locally; `EchoClient.postResult()` when online

---

## Flow: Enter boss floor (online)

```
1. Online mode off → local echo or default boss (today's behavior)
2. GET /v1/echoes/{depth}?game_version={version}
3. On 200:
   - Decode echo_data_base64 → Echo.echoData
   - Store EchoPolicy on level / EchoBoss
   - Route to EchoBossLevel, spawn echo
4. During fight:
   - EchoPolicyMatcher + EchoRoleExecutor pick/execute each hunting turn (no network)
   - Persist echo_policy in level save bundle
5. On failure / 404 → local echo or default floor boss
6. Missing or unsupported echo_policy → NOT_FOUND (no spawn; fail early at prefetch)
```

---

## Flow: After boss kill (online)

```
1. Echo.fromHero(...) — always
2. EchoStorage.save(...) — always (local cache / offline)
3. EchoClient.uploadEcho(...) — non-blocking POST /v1/echoes
   - Failures: log only; never block gameplay
```

---

## Flow: After echo fight (online)

```
1. EchoFightResult from EchoFightRecorder — always
2. EchoLeaderboardStorage.append(...) — always
3. EchoClient.postLeaderboardResult(...) — non-blocking POST /v1/leaderboard/results
```

---

## Wire format: echo upload

Maps to `Echo` + backend [echo-pool spec](../../../hero-echoes/docs/features/echo-pool.md):

```json
{
  "echo_id": "5-1780785524009",
  "depth": 5,
  "game_version": 1200,
  "hero_class": "WARRIOR",
  "lvl": 6,
  "hp": 40,
  "ht": 45,
  "game_seed": 12345,
  "timestamp": 1780785524009,
  "echo_data_base64": "<base64 hero bundle>",
  "source_client": "iatdb-desktop-1.0"
}
```

**Encoding `echo_data_base64`:**

1. `Echo.toFileBundle()` or hero `Bundle`
2. Write to bytes (same as local `.dat` files)
3. Base64-encode for JSON

---

## Wire format: echo fetch response

Same echo fields plus **`echo_policy`** (required for online echo fights):

```json
{
  "echo_id": "...",
  "depth": 5,
  "game_version": 1200,
  "hero_class": "MAGE",
  "echo_data_base64": "...",
  "echo_policy": {
    "policy_schema_version": "0.0.1",
    "capabilities": { "...": "..." },
    "reactions": [ "..." ],
    "tuning": { "aggression": 0.7 }
  }
}
```

Policy schema and decision logic: [`hero-echoes/docs/features/echo-policy/`](../../../hero-echoes/docs/features/echo-policy/) — the single doc set covering both sides.

---

## Echo policy (client)

Documented in full — pipeline, obligations, ownership split, error handling and license boundary —
in the single echo-policy doc set:
[`hero-echoes/docs/features/echo-policy/client.md`](../../../hero-echoes/docs/features/echo-policy/client.md).

---

## Error handling (non-policy)

| Situation                | Client behavior                 |
| ------------------------ | ------------------------------- |
| Network timeout on fetch | Local echo → default boss       |
| 404 no echo              | Default boss                    |
| Upload POST fails        | Log; local echo already saved   |
| Leaderboard POST fails   | Log; local record already saved |

All online calls **non-blocking** on background thread where possible.

---

## Implementation checklist

- [x] `EchoClient` with configurable base URL + API key
- [x] `EchoWireCodec` (Bundle ↔ base64 ↔ JSON)
- [x] Ranked fetch via `CompositeEchoLookup` before local pool
- [x] Role-based `EchoPolicy` + `EchoPolicyMatcher` / `EchoRoleExecutor`
- [x] Persist policy in level bundle (save/load mid-fight)
- [x] Upload echo after boss kill (async)
- [x] Post leaderboard result after echo fight (async)
- [ ] Optional: fetch web leaderboard in `WndLeaderboard`
- [x] Tests: mock HTTP + matcher/executor/status unit tests

---

## Testing

- Integration: mock server or testcontainers against the Hero Echoes test API
- Do not require a live production server in CI
- Policy-side testing: see [echo-policy/client.md](../../../hero-echoes/docs/features/echo-policy/client.md#testing)

Run game tests: `./gradlew :core:test -q -PerrorProneOff --tests "com.shatteredpixel.shatteredpixeldungeon.heroechoes.*"`
