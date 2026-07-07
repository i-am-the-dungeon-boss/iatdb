### Task 5: Leaderboard Feature

**Goal**: Track echo boss fight performance and present rankings locally and via Hero Echoes.

#### Scope

- Instrument boss fights to emit results data — partially done (`EchoFightRecorder`)
- Persist locally; sync to backend when online
- In-game UI (`WndLeaderboard`); web UI lives in `hero-echoes-backend`

See [online-integration.md](online-integration.md) and backend [leaderboard.md](../../../hero-echoes-backend/docs/features/leaderboard.md).

#### Files/Systems to Touch

- `EchoFightRecorder`, `EchoFightResult`, `EchoLeaderboardStorage` — existing
- **New:** `EchoClient.postLeaderboardResult`, optional `EchoClient.fetchLeaderboard`
- UI: `WndLeaderboard`, entry from menu/settings

#### Data Model

- `EchoFightResult` — maps to `POST /v1/leaderboard/results` body:
  - `echo_id`, `boss_win`, `depth`, `game_version`, `player_class`
  - `damage_dealt`, `damage_taken`, `turns`, `timestamp`
- Local cap: 200 entries (`EchoLeaderboardStorage`)
- Server cap: global 1000 (backend prunes)

#### Implementation Steps

1. Instrumentation — extend combat hooks for damage/turn tracking (see gap analysis P7).

2. Local storage — `EchoLeaderboardStorage.append` (existing).

3. Online sync:
   - After fight: `EchoClient.postLeaderboardResult(record)` — async, non-blocking
   - Optional UI refresh: `GET /v1/leaderboard/{depth}?limit=50`

4. UI:
   - `WndLeaderboard` — show local entries; optional "fetch global" when online
   - Sort matches server: boss wins first, then damage, then recency

#### Edge Cases

- Corrupted local file: rebuild minimal structure
- Online fetch fails: show local data only
- Incompatible schema versions: filter on load

#### Testing Checklist

- Boss win/loss recorded with sane metrics
- POST payload matches backend spec (mock server)
- UI empty state and sort order

#### Acceptance Criteria

- Outcomes recorded locally always
- Online POST when enabled; failures non-blocking
- Players can view leaderboard in-game
