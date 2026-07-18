# Play Stage 1 Code/Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Android release builds Play-safe for Stage 1 (unique package ID, stub news/updates, hide Patreon surfaces, correct contact email).

**Architecture:** Introduce a single gate `SupportPrompts.externalSupportEnabled()` (false for Stage 1). Title/WornKey/victory UI consult it. Gradle flips package ID and Android release service modules.

**Tech Stack:** Java, Gradle Android, JUnit 5, AssertJ, existing `GdxTestExtension` where needed.

## Global Constraints

- Package ID: `com.marwanelzainy.iatdb`
- Contact email: `marwan.elzainy@gmail.com`
- No Play Billing this pass
- No branding assets this pass
- TDD for behavior; config-only edits need no unit tests

---

### Task 1: SupportPrompts gate (TDD)

**Files:**

- Create: `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/SupportPrompts.java`
- Test: `core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/ui/SupportPromptsTest.java`

**Interfaces:**

- Produces: `SupportPrompts.externalSupportEnabled()` → `boolean` (Stage 1: always `false`)

- [x] **Step 1: Write failing test** — `@DisplayName("disables external support surfaces for Play Stage 1")` asserts `externalSupportEnabled()` is false
- [x] **Step 2: Run test** — expect compile/fail (class missing)
- [x] **Step 3: Implement** `SupportPrompts` returning `false`
- [x] **Step 4: Run test** — expect PASS

### Task 2: Wire call sites behind the gate

**Files:**

- Modify: `TitleScene.java`, `WornKey.java`, `WndVictoryCongrats.java`

- [x] Gate title Support button creation/layout
- [x] Gate WornKey `WndSupportPrompt`
- [x] Gate victory Support button; full-width Close when gated off
- [x] Re-run `SupportPromptsTest` + assemble mental check

### Task 3: Gradle identity + Android release services

**Files:**

- Modify: `build.gradle`, `android/build.gradle`

- [x] `appPackageName = 'com.marwanelzainy.iatdb'`
- [x] Release: `debugUpdates` + `debugNews`

### Task 4: Email + docs

**Files:**

- Modify: `DesktopLauncher.java`, `docs/PENDING-LINKS.md`, `docs/release/01-coming-soon.md`

- [x] Desktop email → `marwan.elzainy@gmail.com`
- [x] Mark LINK-25 updated
- [x] Check off completed Stage 1 code tasks; keep asset FOLLOW-UPs

### Task 5: Verify

- [x] `./gradlew :core:test --tests "com.shatteredpixel.shatteredpixeldungeon.ui.SupportPromptsTest"`
- [x] Confirm Android release deps in `android/build.gradle`
