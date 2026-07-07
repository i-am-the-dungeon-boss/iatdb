### Task 2: Boss Replacement Logic

**Goal**: Intercept standard boss-level generation at depths 5/10/15/20/25 and spawn a hero-based boss (from a echo) when available, with robust fallbacks.

#### Scope

- Modify level creation flow to check for available echos.
- If found, load `EchoBoss` instead of the default boss.
- Preserve the original level layout unless a special arena is required.

#### Files/Systems to Touch

- `EchoReplacementDecider` / `Dungeon.levelClassForDepth` — existing hook
- **New:** `EchoClient.fetchEcho` — primary path when online
- `EchoStorage` — local fallback
- Level bundle: persist `echo_policy` for save/load mid-fight

#### Decision Flow

1. When entering a boss depth (online mode on):
   - **`EchoClient.fetchEcho(depth, gameVersion)`** → echo + `echo_policy`
   - On success → spawn `EchoBoss` with policy attached
2. On fetch failure → `EchoStorage.loadForDepth(depth)` (local)
3. If none found → default regional boss

See [online-integration.md](online-integration.md).

#### Implementation Steps

1. Add replacement hook in `Dungeon.newLevel()`:

   - Detect boss depths via existing switch/case.
   - Resolve echo: online fetch first (ranked mode), else `EchoStorage.loadForDepth(depth)`.
   - Store echo **and** `EchoPolicy` on `Dungeon` / level for `EchoBoss` to read.

2. Adapt boss levels:

   - Update boss-level class to accept an optional `Echo` in constructor or via a setter before `build()`.
   - During mob spawning, if echo is present: instantiate `EchoBoss` with the echo; otherwise spawn the default boss.
   - Ensure boss room generation still works with a humanoid boss footprint.

3. Fallbacks and safety:

   - If echo fails to deserialize, log a warning and spawn the default boss.
   - Do not alter non-boss floors.

4. Save compatibility:
   - Persist `echo_policy` in level bundle alongside echo reference so reload does not re-fetch.

#### Edge Cases

- No valid spawn cell for humanoid: reuse default boss spawn logic or pick center of boss room.
- Echo from incompatible version: skip and fallback.
- Feature toggle off mid-run: respect current floor choice; apply toggle on next boss depth.

#### Testing Checklist

- With echos present for depth 5, confirm `EchoBoss` spawns instead of Goo.
- Without echos, confirm default boss spawns.
- With corrupted echo file, verify fallback occurs without crash.
- Reloading a save on a boss floor preserves the chosen boss type.

#### Acceptance Criteria

- On boss floors, the game prefers echo-based bosses when available.
- Fallback to default boss is seamless and reliable.
- Save/load preserves boss selection.
