# Naming Convention — Echo

**Rule:** Use **Echo** for feature code and docs. **Hero Echoes** for the online service brand. Keep **hero\_** on wire fields that describe hero payload (`hero_class`, `echo_data_base64`).

---

## Product

| Concept          | Name                                                  |
| ---------------- | ----------------------------------------------------- |
| Game             | I am the Dungeon Boss                                 |
| Online service   | Hero Echoes                                           |
| Backend repo     | `hero-echoes-backend`                                 |
| Game docs folder | `docs/hero-echoes/`                                   |
| Java package     | `com.shatteredpixel.shatteredpixeldungeon.heroechoes` |

---

## Player-facing

| Concept       | Name                                                  |
| ------------- | ----------------------------------------------------- |
| Mob name      | Hero Echo                                             |
| Mob i18n keys | `actors.mobs.echoboss.*`                              |
| Lore term     | echo (e.g. "echo of the hero who claimed this floor") |

---

## Game code (Java)

| Name                          | Role                        |
| ----------------------------- | --------------------------- |
| `Echo`                        | Captured hero state         |
| `EchoStorage`                 | Local `echoes/` persistence |
| `EchoCaptureTrigger`          | Boss-kill capture hook      |
| `EchoBoss`                    | Echo spawned as floor boss  |
| `EchoReplacementDecider`      | Boss-floor routing          |
| `EchoFightRecorder`           | Fight outcome → leaderboard |
| `EchoLeaderboardStorage`      | Local leaderboard file      |
| `WndEchoes` / `WndEchoDetail` | Echo browser UI             |

**Policy / online:**

| Name                      | Role                                 |
| ------------------------- | ------------------------------------ |
| `EchoClient`              | HTTP client for Hero Echoes API      |
| `EchoPolicy`              | Parsed role-based `echo_policy` blob |
| `EchoPolicyStatusBuilder` | Char/Level → per-turn status         |
| `EchoPolicyMatcher`       | policy + status → chosen role        |
| `EchoRoleResolver`        | role capability → item id            |
| `EchoTargetPicker`        | legal aim cell (no UI CellSelector)  |
| `EchoRoleExecutor`        | drink / throw / zap / move / wait    |

---

## API / JSON

| Field / path             | Notes                                       |
| ------------------------ | ------------------------------------------- |
| `GET /v1/echoes/{depth}` | Fetch echo + stored `echo_policy`           |
| `POST /v1/echoes/policy` | Solo: generate `echo_policy` for local echo |
| `POST /v1/echoes`        | Upload after boss kill                      |
| `echo_id`                | Client-generated id (`{depth}-{ms}`)        |
| `echo_data_base64`       | Serialized hero bundle                      |
| `echo_policy`            | Combat plan on fetch (server-generated)     |
| `boss_win`               | Echo won the fight                          |

---

## Do not rename

SPD core types unrelated to this feature: `HeroicLeap`, warrior "Heroic" abilities, Sniper "snapshot" shot, etc.
