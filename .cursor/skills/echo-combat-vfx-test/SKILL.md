---
name: echo-combat-vfx-test
description: >-
  Sets up EchoBoss combat and world-VFX unit tests with EchoTestSupport fixtures
  (linked sprites, Actor registration, FOV, InstantProjectileGroup). Use when
  writing or fixing Echo/Hero combat, role executor, wand/missile, attack/jump/zap,
  or UseContext canWorldFx tests.
---

# Echo combat / world VFX tests

## Setup

1. `@ExtendWith(GdxTestExtension.class)` on the test class.
2. Reset state in `@BeforeEach` with `EchoTestSupport.resetWorkflowState()` when filesystem / Dungeon / lookup seams matter.
3. Build hero + boss with real kit data (`warriorEchoWithData` / `createBossWithPolicy`), not hollow echoes.

## Live fight fixture

```java
EchoTestSupport.installEchoBossLevel(hero, boss, 2);
Assertions.assertThat(hero.sprite.ch).isSameAs(hero);
Assertions.assertThat(boss.getEchoHero().sprite).isNull(); // phantom stays headless
```

- Player and boss have **linked** stub sprites (`sprite.ch` set).
- Both chars registered with `Actor`; `heroFOV` covers the fight.
- Force the branch under test (e.g. `boss.getEchoHero().invisible = 1` for a guaranteed hit).

## World VFX assertions

When the path can show attack / jump / zap / missiles:

```java
EchoTestSupport.InstantProjectileGroup fx =
		EchoTestSupport.attachInstantProjectileParent(boss);
// … invoke the production path with UseContext.echo(…) …
Assertions.assertThat(fx.magicMissileRecycles).isGreaterThan(0);
// or: stubSpriteAttackCalls / Jump / Operate / Zap / stubSpritePlacedCell
```

Headless (no parent) must still apply gameplay synchronously.

## Do not

- Gate the test on “spent turn” alone while `enemy.sprite == null` (skips crashing VFX).
- Assert only Hero/`heroFX` paths when Echo is in scope — see project rule `echo-world-vfx`.

## Run

```bash
./gradlew :core:test --tests "com.shatteredpixel.shatteredpixeldungeon.heroechoes.<YourTest>"
```
