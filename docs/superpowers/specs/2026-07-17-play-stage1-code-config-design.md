# Design: Google Play Stage 1 — Code/Config Pass

**Date:** 2026-07-17  
**Repo:** `iatdb`  
**Stage:** Coming soon / pre-registration ([docs/release/01-coming-soon.md](../../release/01-coming-soon.md))  
**Approach:** Minimal Play-safe config (no assets, no Billing)

## Goal

Make Android **release** builds safe for Google Play identity and payments policy: unique package ID, no upstream update/news services, no Patreon/external payment surfaces, correct developer contact email. Defer branding assets and Play Billing.

## Decisions (locked)

| Decision                                | Choice                                      |
| --------------------------------------- | ------------------------------------------- |
| `appPackageName` / Play `applicationId` | `com.marwanelzainy.iatdb`                   |
| Release update checker + news           | Disable both → `debugUpdates` + `debugNews` |
| Branding assets                         | Deferred until before Play upload           |
| Supporter / Patreon                     | Hide in Stage 1; Play Billing in Stage 2    |
| Developer contact email                 | `marwan.elzainy@gmail.com`                  |

## Out of scope

- App icons and title-banner art (follow-up before upload)
- Google Play Billing / supporter product UI (Stage 2)
- Full About/credits rewrite and all PENDING-LINKS cleanup
- Signing keys, AAB generation, Play Console (non-code or later)
- Hero Echoes online Android wiring
- New Gradle product flavors

## Architecture

No new modules. Stage 1 is configuration + UI gating on existing surfaces:

```text
build.gradle          → applicationId identity
android/build.gradle  → release service stubs (no network news/updates)
TitleScene            → hide Support; reflow buttons
WornKey               → stop WndSupportPrompt
WndVictoryCongrats    → hide Support button; reflow close
DesktopLauncher       → contact email (Android already correct)
docs/release/*        → decisions + follow-ups remain source of truth
```

Release builds keep the News button if present; with `debugNews` the service is a no-op (same pattern as debug). Update checks similarly no-op via `debugUpdates`.

## File-level changes

### 1. Package identity

**Modify:** `build.gradle`

- Set `appPackageName = 'com.marwanelzainy.iatdb'`
- Leave `appName`, `appVersionCode`, `appVersionName` unchanged this pass

`android` `applicationId` already uses `appPackageName`. Debug suffix `.indev` stays.

### 2. Disable release update/news services

**Modify:** `android/build.gradle` (release `dependencies`)

- `githubUpdates` → `debugUpdates`
- `shatteredNews` → `debugNews`

Desktop release wiring is out of scope unless it shares the same anti-pattern and is trivial; this pass prioritizes Android Play.

### 3. Hide Patreon / external payment UI

**Modify:** `TitleScene.java`

- Do not add / do not show `btnSupport`
- Anchor `btnRankings` (and subsequent layout) under `btnSolo` / ranked row so landscape and portrait have no empty Support row

**Modify:** `WornKey.java`

- Remove the `WndSupportPrompt` pickup path (or permanently no-op it). Do not open Patreon UI after Goo key pickup.

**Modify:** `WndVictoryCongrats.java`

- Remove or hide the Support button that opens `SupporterScene`
- Keep Close (and any other non-payment actions); adjust layout so Close is full-width or correctly centered

**Leave in place (dead for Stage 1):** `SupporterScene.java`, `WndSupportPrompt.java` — Stage 2 will replace with Play Billing. No requirement to delete classes this pass.

### 4. Contact email

**Modify:** `DesktopLauncher.java` — replace `Marwan.Elzainy@example.com` with `marwan.elzainy@gmail.com`

**Verify only:** `AndroidMissingNativesHandler.java` already uses `marwan.elzainy@gmail.com` — no change if still true.

**Modify:** `docs/PENDING-LINKS.md` — mark LINK-25 resolved / updated to the real address.

### 5. Docs

**Modify:** `docs/release/01-coming-soon.md`

- Check off completed code tasks as they land
- Keep FOLLOW-UP BEFORE UPLOAD tags on asset items
- Keep Decisions table as source of truth

## Testing

TDD applies to behavior that can be unit-tested without full Play upload.

### Tests to add

1. **WornKey does not show support prompt**
   - Behavior: picking up / support-nag path does not schedule `WndSupportPrompt` (or equivalent observable: nag gate never opens support UI).
   - Prefer a focused unit/integration test using existing `GdxTestExtension` / game setup helpers if WornKey prompt is reachable; otherwise a small extract (e.g. `SupportPrompts.enabled()` returning false) tested in isolation, with WornKey calling that gate.

2. **Package name constant / build identity (optional lightweight)**
   - Only if the project already reads package from a testable Java constant; do **not** invent a Gradle test harness. Prefer documenting verification via `./gradlew :android:tasks` / assemble and inspecting `applicationId` if no constant exists.

### Manual verification (required before merge claim)

- Assemble Android release (or at least configure release dependencies) and confirm release classpath uses debug update/news modules
- Launch title scene: no Support button; layout not broken
- Defeat Goo / WornKey pickup: no Patreon prompt
- Victory congrats: no Support → Patreon path

### Non-goals for tests

- Play Console upload, AAB signing, store listing
- Visual pixel tests for title layout
- Billing

## Error handling / edge cases

- Players with old saves: hiding Support is harmless; no migration
- Changing `appPackageName` creates a **new** Android app identity vs any prior sideloads using the upstream ID — expected for first Play publish
- Stage 2 must reintroduce a Support entry point wired to Play Billing (documented in `02-early-access.md`)

## Follow-up before Play upload (not this pass)

- Replace icons and title banner
- About/credits and remaining PENDING-LINKS on public surfaces
- Privacy policy URL + Data Safety (non-code)
- Signed AAB + Play App Signing

## Success criteria

- [ ] `applicationId` is `com.marwanelzainy.iatdb` (debug: `…iatdb.indev`)
- [ ] Android release does not depend on `githubUpdates` or `shatteredNews`
- [ ] No in-app path opens Patreon or shows Support → Patreon UI
- [ ] Crash/contact strings use `marwan.elzainy@gmail.com`
- [ ] Release checklist documents remaining asset follow-up
- [ ] Agreed tests pass under `./gradlew :core:test`
