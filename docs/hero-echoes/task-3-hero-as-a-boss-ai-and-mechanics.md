### Task 3: Hero-as-a-Boss AI & Mechanics

**Goal**: Turn a hero echo into a formidable `Mob` (`EchoBoss`) with combat parity and **policy-driven AI** from Hero Echoes.

#### Scope

- Combat stat delegation (damage, evasion, procs, speed) — see [gap analysis](echo-boss-gap-analysis.md)
- **Role-based policy blob** from Hero Echoes (capabilities / reactions / recipes / …) — see [online-integration.md](online-integration.md)
- Supported **`echo_policy` required** at prefetch/boss init — missing/unsupported → no spawn
- Local solo may persist `EchoPolicy.fallback()` as an explicit minimal playbook (not a silent boss-init safety net)

#### Files/Systems to Touch

- `EchoBoss.java`, `EchoBossSprite.java` — existing
- `EchoPolicy.java` — parse & hold server policy JSON
- `EchoPolicyMatcher` / `EchoRoleExecutor` / `EchoInventory` — match + execute roles

#### Initialization from Echo

- Map echo stats to mob stats; apply boss multipliers if needed (e.g., +30% HP, +10% damage) for parity with other bosses.
- Equip items from the echo using normal equip pathways if feasible, otherwise simulate their effects in computed stats.
- Set class-specific flags to drive behavior selections.

#### AI Design

**Primary path — role-based policy (online echoes):**

1. On each `act()`, build condition context (HP ratio, distance, inventory, class, LOS).
2. Match roles from `echo_policy` (capabilities, reactions, recipes, selection, matchups).
3. Execute via role executor (items / movement from capability definitions).
4. Validate against actual inventory before executing.

Policy fetched once with echo; persisted in level save. No per-turn network.

**No silent fallback at boss init:**

Missing or unsupported policy fails at prefetch / lookup. Solo local saves may write `EchoPolicy.fallback()` intentionally. Unresolved roles during a fight fall through to standard mob hunting AI.

**Combat parity (independent of policy):**

- Delegate `defenseSkill`, `attackProc`, `defenseProc`, `attackDelay`, `speed` to `echoHero` where applicable — see gap analysis P0–P1.

Heuristic tuning notes (until matcher lands):

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
