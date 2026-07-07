### Task 1: Hero Echo & Storage

**Goal**: Capture a complete echo of the player hero immediately after a boss is defeated and persist it for later use as a replacement boss.

#### Scope and Key Concepts

- Echo must include core stats, class, talents, equipment, curated inventory, and appearance.
- Triggered exactly once per boss kill, for depths 5/10/15/20/25.
- Phase 1 (Local): ✅ Save to `echoes/` under the game directory — implemented (`EchoStorage`).
- Phase 2 (Online): Upload/download via **Hero Echoes** REST API — see [online-integration.md](online-integration.md).
  - `POST /v1/echoes` — upload after boss kill (async, non-blocking)
  - `GET /v1/echoes/{depth}?game_version=` — fetch echo + `echo_policy` on boss floor entry
  - Wire encoding: hero `Bundle` → bytes → `echo_data_base64` — see [EchoWireCodec plan](online-integration.md#planned-client-modules)

#### Files/Systems to Touch

- `Hero.java`, `Echo.java`, `EchoStorage.java` — existing
- **New:** `EchoClient.java`, `EchoWireCodec.java` — [online-integration.md](online-integration.md)
- `EchoCaptureTrigger` — after local save, queue online upload

#### Data Model

- Echo file format: JSON or `Bundle`-backed map serialized to a string; filename convention: `depth-<N>-<timestamp>.json`.
- Required fields:
  - Hero meta: class, level, experience, HP/HT, STR, accuracy/evasion modifiers, alignment flags.
  - Talents/skills: list of learned talents with levels.
  - Equipment: weapon, armor, two rings, artifact (serialized via existing item `Bundle` routines).
  - Inventory subset: potions (healing, strength), scrolls (teleportation, upgrade), wands; limit to a small capped count per category.
  - Appearance: sprite key or data to reconstruct visuals (hair/armor tint if applicable).
  - Context: depth, game seed, difficulty modifiers (if any), timestamp, game version.

#### Implementation Steps

1. Extend `Hero` serialization:

   - Audit `Hero.storeInBundle(Bundle)` and `Hero.restoreFromBundle(Bundle)` to ensure all required fields above are present.
   - For talents and items, leverage existing `Bundlable` implementations on those classes to avoid bespoke serializers.

2. Echo builder:

   - Create a static utility `Echo` (POJO + to/from `Bundle`).
   - Method: `Echo fromHero(Hero hero, int depth)` which copies the curated subset of data into a `Bundle`.
   - Method: `Bundle toBundle()` and `static Echo fromBundle(Bundle)`.

3. Trigger on boss death:

   - In the boss `Mob` class for each relevant floor (starting with Goo), override `die(Object cause)` and after calling `super.die(cause)` schedule a task to: build echo, then persist.
   - Guard conditions: only when hero is alive, feature toggle is enabled, and the kill depth is one of the boss depths.

4. Persistence (local):

   - Create helper `EchoStorage` with methods:
     - `static File getEchoesDir()` that resolves a writable directory in the save location; ensure it exists.
     - `static void save(Echo echo)` writes JSON string to depth-based file; consider also saving the most recent per-depth as `latest-depth-<N>.json`.
     - `static Optional<Echo> loadForDepth(int depth)` returns the echo for that boss depth, or `Optional.empty()`.

5. Versioning:

   - Include `gameVersion` in the echo; on load, accept same-major versions only, otherwise skip echo to avoid crashes on schema drift.

6. **Online upload (Phase 2):**

   - After successful local save, call `EchoClient.uploadEcho(echo)` on a background thread.
   - Map `Echo` fields to JSON; encode `echoData` as `echo_data_base64`.
   - Failures: log only; never block gameplay.

7. Error handling:
   - Wrap all I/O in try/catch; log and continue gameplay if save fails.
   - Apply size caps (max 1 echo per boss depth); saving replaces the previous echo for that depth.

#### Edge Cases

- Boss dies to environmental effect: still trigger echo if hero is on the floor and alive.
- Hero at 1 HP, temporary buffs active: echo permanent state only; exclude temporary buffs/potions timers.
- Corrupted items or mod content: rely on item `Bundle` to skip unknown fields gracefully.

#### Testing Checklist

- Defeat Goo with various classes; verify files appear in `echoes/` and contain expected fields.
- Load echos with a small harness to verify `Bundle` round-trip.
- Simulate I/O failures (read-only directory) and confirm no crash.
- Online upload succeeds against test server (mock or `hero-echoes-backend` integration).

#### Acceptance Criteria

- A echo is saved locally exactly once after each boss kill on supported depths.
- Echo includes hero stats, equipment, curated inventory, talents, and appearance.
- Saving failures do not impact gameplay; informative logs are produced.
- Local save always succeeds independently of online upload.
- Utility can load a random echo for a given depth (local fallback).
