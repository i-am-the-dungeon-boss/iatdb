# iatdb — Agent notes

Shattered Pixel Dungeon mod (desktop / Android / iOS via RoboVM). Shared gameplay in `core` + `SPD-classes`.

## Branches

| Branch   | Role                                                                                                        |
| -------- | ----------------------------------------------------------------------------------------------------------- |
| `master` | Base open-source game (upstream Shattered). Style, modules, and portable API reference. No unit-test suite. |
| `main`   | **Active mod branch** — continue all feature work here (Hero Echoes, TDD, fork helpers).                    |

When matching “master style,” compare against the `master` branch. Implement and test on `main`.

## Commands

```bash
./gradlew :core:test
./gradlew :core:test --tests "com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoAndStorageTest"
./gradlew :core:test --tests "com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoAndStorageTest.fromHeroCapturesRequiredFields"
./gradlew :desktop:run
```

Windows: `gradlew.bat`. Prefer targeted `:core:test` unless a full build is requested.

## Modules

| Path                         | Role                                                 |
| ---------------------------- | ---------------------------------------------------- |
| `core/`                      | Game logic, assets, tests under `core/src/test/java` |
| `SPD-classes/`               | Engine (noosa, `Bundle`, `FileUtils`, utils)         |
| `desktop/` `android/` `ios/` | Platform launchers                                   |
| `services/`                  | Optional update/news backends                        |
| `.cursor/rules/`             | Agent rules                                          |
| `.cursor/skills/`            | Workflows (e.g. `echo-combat-vfx-test`)              |

## Package map (`…shatteredpixeldungeon`)

| Package                    | Contents                                                        |
| -------------------------- | --------------------------------------------------------------- |
| `actors/`                  | `Hero`, `Mob`, buffs, turns                                     |
| `heroechoes/`              | Echo DTO, storage, capture, play modes                          |
| `heroechoes/boss/`         | Spawner, regional death, fight recorder, leaderboard            |
| `heroechoes/policy/`       | Offline fight brain: policy match/execute, inventory, targeting |
| `heroechoes/online/`       | HTTP client, auth, lookup, sync, wire codec                     |
| `heroechoes/debug/`        | Debug arena kits / arsenal / snapshot weaken                    |
| `items/`                   | Weapons, potions, `UseContext`, …                               |
| `levels/`                  | Generation, terrain, `EchoBossLevel`                            |
| `scenes/` `ui/` `windows/` | Screens and widgets                                             |
| `messages/`                | i18n (`Messages.get`)                                           |
| `sprites/` `effects/`      | Visuals / VFX (`EchoBossSprite`)                                |
| `utils/`                   | Game-side helpers                                               |

Engine: `com.watabou.utils`, `com.watabou.noosa` in `SPD-classes`.

`EchoBoss` stays in `actors/mobs/` (Mob on the Actor clock).

## Hard constraints

- **TDD** for production behavior (AssertJ + `@DisplayName`)
- **Three platforms** — RoboVM-safe JDK subset; Android-safe `org.json`
- **Echo world VFX** — `canWorldFx`, not `heroFX` alone
- Match **`master`** style for base-game code; build features on **`main`**; fail clearly on required restore/wire data
