# Prepare release artifacts

## End-to-end (recommended)

One command runs all unit tests (`gradlew test`), builds the store binaries, creates an annotated git tag `v<appVersionName>`, pushes it, and publishes a [GitHub Release][github-releases] whose notes link to Google Play and Steam. Binaries stay in `dist/` for you to upload. Failed tests abort the release before packaging.

```powershell
# Play AAB (full game) + Steam demo zip (Windows)
.\scripts\release.ps1
```

`release.ps1` is the orchestrator. It calls [`Build-PlayAab.ps1`](../../scripts/release/Build-PlayAab.ps1) (`prepareRelease`, no `-Pdemo`), [`Build-SteamDemo.ps1`](../../scripts/release/Build-SteamDemo.ps1) (`packageSteamDemo -Pdemo`), and [`Publish-GitHubRelease.ps1`](../../scripts/release/Publish-GitHubRelease.ps1) (notes only).

| Flag                 | Meaning                                                          |
| -------------------- | ---------------------------------------------------------------- |
| `-SkipBuild`         | Reuse existing `dist/<version>/` (publish only; tests still run) |
| `-SkipTests`         | Skip the pre-release `gradlew test` gate (not recommended)       |
| `-DryRun`            | Print steps; no tests/build/tag/`gh release create`              |
| `-Draft`             | Create a draft GitHub Release                                    |
| `-AllowDirty`        | Allow uncommitted local changes                                  |
| `-NotesFile path`    | Use custom release notes instead of the template                 |
| `-Tag name`          | Override tag (default `v` + `appVersionName`)                    |
| `-SkipVercelPromote` | Skip `vercel promote` for hero-echoes after release              |
| `-HeroEchoesRoot`    | Path to hero-echoes (default sibling `../hero-echoes`)           |

Requires: `git`, authenticated `gh`, Android SDK + release keystore (same as below). For Vercel promote: Vercel CLI (`vercel` or `npx vercel`) authenticated, or `VERCEL_TOKEN`.

**Hero Echoes / Vercel:** bump and push `package.json` on hero-echoes `main` before
or during release prep — builds run, but `github.autoAlias: false` keeps production
unassigned. After `gh release create` succeeds, `release.ps1` invokes sibling
hero-echoes [`scripts/promote-vercel.ps1`](../../../hero-echoes/scripts/promote-vercel.ps1)
(`pnpm run promote:vercel`). Use `-SkipVercelPromote` to skip.

Lint (optional; needs [PSScriptAnalyzer](https://github.com/PowerShell/PSScriptAnalyzer)):

```powershell
Invoke-ScriptAnalyzer -Path .\scripts\release.ps1 -Settings .\scripts\PSScriptAnalyzerSettings.psd1
```

Version identity is read from root [`build.gradle`](../../build.gradle) (`appVersionName` / `appVersionCode`). Bump those (and keep hero-echoes `package.json` in sync on `main`) **before** running the script. Do **not** promote Vercel yourself during prep — `release.ps1` calls hero-echoes `promote-vercel.ps1` after the GitHub Release succeeds.

## Build only

```bash
./gradlew prepareRelease
```

Windows:

```powershell
.\gradlew.bat prepareRelease
```

### Output

| File                                     | Where                                                         |
| ---------------------------------------- | ------------------------------------------------------------- |
| `iatdb-<version>-android.aab`            | Google Play (full game, no `-Pdemo`). Upload in Play Console. |
| `iatdb-<version>-steam-demo-windows.zip` | Steam demo (`-Pdemo`)                                         |
| `SHA256SUMS.txt`                         | Hashes + version metadata. Not uploaded to GitHub.            |

Example for `0.0.1`: `dist/0.0.1/`

Linux/SteamOS zip is paused. This release builds the Windows demo locally and does not download a Linux zip. GitHub Release notes link to Play and Steam; they do not attach these files.

```bash
./gradlew prepareRelease
./gradlew packageSteamDemo -Pdemo
```

`-Pdemo` hides the title-menu Support button and the victory support link, and stops the demo at the sewers boss. `prepareRelease` refuses `-Pdemo`.

### Paused

Kept in [`scripts/release/paused.ps1`](../../scripts/release/paused.ps1), not called: Android APK, desktop JAR, macOS zip, unsigned iOS IPA, and uploading those files to the GitHub Release. Linux/SteamOS zip is paused in [`Build-SteamDemo.ps1`](../../scripts/release/Build-SteamDemo.ps1) (`iatdb-<version>-steam-demo-linux.zip`); [`.github/workflows/steam-demo.yml`](../../.github/workflows/steam-demo.yml) can still build it by hand. iOS workflow [`.github/workflows/ios-unsigned.yml`](../../.github/workflows/ios-unsigned.yml) is still in the repo.

## Requirements

- Android SDK (`local.properties` `sdk.dir` or `ANDROID_HOME`)
- [`android/keystore.properties`](android-signing.md) + release keystore
- JDK for Gradle
- For e2e publish: [GitHub CLI](https://cli.github.com/) (`gh auth login`)
- Linux/SteamOS zip is paused, so this release does not need `steam-demo.yml`
- **Sentry uploads are mandatory on release:** `prepareRelease`, `packageSteamDemo`, `bundleRelease`,
  `:desktop:release`, and `release.ps1` all require
  `SENTRY_AUTH_TOKEN` (process env or root `.env`). There is no skip flag.
  Uploads source context to `android` and `java` (desktop), plus Android ProGuard maps.
  - Token: https://sentry.io/settings/dungeonboss/auth-tokens/
  - GitHub Actions secret: `SENTRY_AUTH_TOKEN` (required by `steam-demo.yml`)

## Not included (by design for beta)

- Signed iOS / TestFlight (paused)
- Play Console upload (the AAB is built; you upload it)
- Steamworks upload (the demo zips are built; you upload them)
- itch.io mirror (optional later via `butler`)

<!-- Project link refs: keep identical to docs/project-link-refs.md -->

[github-releases]: https://github.com/i-am-the-dungeon-boss/iatdb/releases
