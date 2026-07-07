### Task 3: Hero-as-a-Boss AI & Mechanics

**Goal**: Turn a hero echo into a formidable `Mob` (`EchoBoss`) with combat parity and **policy-driven AI** from Hero Echoes.

#### Scope

- Combat stat delegation (damage, evasion, procs, speed) — see [gap analysis](echo-boss-gap-analysis.md)
- **Policy interpreter:** evaluate server `echo_policy` each turn → action (see [online-integration.md](online-integration.md))
- Bundled **fallback policy** when offline or policy missing (minimal `MELEE_CHASE`)
- Hard-coded class AI in client is **fallback only**; primary tuning is server-generated

#### Files/Systems to Touch

- `EchoBoss.java`, `EchoBossSprite.java` — existing
- **New:** `EchoPolicy.java`, `PolicyInterpreter.java` — parse & evaluate server rules
- Pathfinding, item use via existing action APIs

#### Initialization from Echo

- Map echo stats to mob stats; apply boss multipliers if needed (e.g., +30% HP, +10% damage) for parity with other bosses.
- Equip items from the echo using normal equip pathways if feasible, otherwise simulate their effects in computed stats.
- Set class-specific flags to drive behavior selections.

#### AI Design

**Primary path — policy interpreter (online echoes):**

1. On each `act()`, build condition context (HP ratio, distance, inventory, class, LOS).
2. Evaluate `echo_policy.rules` by priority; first match wins.
3. Execute action: `MELEE_CHASE`, `KEEP_DISTANCE`, `USE_ITEM`, `ZAP`, `WAIT`.
4. Validate action against actual inventory before executing.

Policy fetched once with echo; persisted in level save. No per-turn network.

**Fallback path — local policy / stubs:**

When no server policy (offline, fetch failed, unsupported schema):

1. Use bundled minimal policy JSON in client assets, **or**
2. Existing `decideAction()` heuristics (chase, heal thresholds)

**Combat parity (independent of policy):**

- Delegate `defenseSkill`, `attackProc`, `defenseProc`, `attackDelay`, `speed` to `echoHero` where applicable — see gap analysis P0–P1.

Legacy detailed class AI notes (for fallback tuning only):

   - Healing: if HP < 35% and has healing potion, use it if not threatened by imminent lethal damage.
   - Disengage: if ranged class and melee-threatened, kite to maintain optimal range.
   - Scroll usage: when badly outnumbered or debuffed, read a suitable scroll (e.g., teleport, rage) if present.
   - Wand usage: prefer wand when LOS and range favorable; otherwise close distance.
   - Class-specific:
     - Warrior: prioritize melee, pop healing slightly earlier, use charges aggressively.
     - Mage: maintain distance, heavy wand/staff usage, teleport if cornered.
     - Rogue: use positioning advantages, backstab-style bonuses if available.
     - Huntress: ranged priority, traps if represented in echo.

3. Cooldown and decision framework:
   - Implement small internal cooldowns per item/ability to avoid spammy behavior.
   - Use a simple scoring system to select action each turn (e.g., evaluate attack score, heal score, kite score).

#### Balancing

- Normalize damage/defense to boss-tier expectations; add a difficulty scaler per depth.
- Cap consumables used during the fight (e.g., max 2 healing potions) to avoid unwinnable scenarios.

#### Edge Cases

- Missing or invalid items in echo: ignore and proceed with defaults.
- Blind/confusion effects: respect status effects to maintain consistency with other mobs.

#### Testing Checklist

- Verify basic chase/attack works and fight concludes without exceptions.
- Simulate echos of different classes; observe class-appropriate behaviors.
- Ensure item usage triggers under intended thresholds and respects cooldowns.
- Performance check: ensure no per-turn allocations that would cause GC spikes.

#### Acceptance Criteria

- `EchoBoss` runs server policy when `echo_policy` present.
- Fallback policy works offline.
- Combat delegation fixes from gap analysis P0–P1 applied.
- No crashes from missing policy data; performance stable.
