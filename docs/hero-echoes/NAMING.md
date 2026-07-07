# Naming Convention — Echo

**Rule:** Use **Echo** for feature code and docs. **Hero Echoes** for the online service brand. Keep **hero_** on wire fields that describe hero payload (`hero_class`, `echo_data_base64`).

---

## Product

| Concept          | Name                                                  |
| ---------------- | ----------------------------------------------------- |
| Game             | I Am The Dungeon Boss                                 |
| Online service   | Hero Echoes                                           |
| Backend repo     | `hero-echoes-backend`                                 |
| Game docs folder | `docs/hero-echoes/`                                   |
| Java package     | `com.shatteredpixel.shatteredpixeldungeon.heroechoes` |

---

## Player-facing

| Concept       | Name                                      |
| ------------- | ----------------------------------------- |
| Mob name      | Fallen Hero                               |
| Mob i18n keys | `actors.mobs.echoboss.*`                  |
| Lore term     | echo (e.g. "echo of a fallen adventurer") |

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

**Planned:**

| Name                    | Role                            |
| ----------------------- | ------------------------------- |
| `EchoClient`            | HTTP client for Hero Echoes API |
| `EchoPolicy`            | Parsed `echo_policy` from fetch |
| `EchoPolicyInterpreter` | Evaluates rules each turn       |

---

## API / JSON

| Field / path             | Notes                                   |
| ------------------------ | --------------------------------------- |
| `GET /v1/echoes/{depth}` | Fetch echo + `echo_policy`              |
| `POST /v1/echoes`        | Upload after boss kill                  |
| `echo_id`                | Client-generated id (`{depth}-{ms}`)    |
| `echo_data_base64`       | Serialized hero bundle                  |
| `echo_policy`            | Combat plan on fetch (server-generated) |
| `boss_win`               | Echo won the fight                      |

---

## Do not rename

SPD core types unrelated to this feature: `HeroicLeap`, warrior "Heroic" abilities, Sniper "snapshot" shot, etc.
