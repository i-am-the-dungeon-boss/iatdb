# Prepare release artifacts

## End-to-end (recommended)

One command runs all unit tests (`gradlew test`), builds binaries, ensures an unsigned iOS IPA is present, creates an annotated git tag `v<appVersionName>`, pushes it, and publishes a [GitHub Release][github-releases] with APK, JAR, unsigned IPA, `SHA256SUMS.txt`, and generated notes (includes `internal version number: <appVersionCode>`). Failed tests abort the release before packaging.

```powershell
# APK + desktop JAR + unsigned IPA
.\scripts\release.ps1

# Also include native desktop zip for this OS (slow; downloads a JDK)
.\scripts\release.ps1 -WithJpackage
```

| Flag              | Meaning                                                          |
| ----------------- | ---------------------------------------------------------------- |
| `-WithJpackage`   | Passes `-PwithJpackage` to Gradle                                |
| `-SkipBuild`      | Reuse existing `dist/<version>/` (publish only; tests still run) |
| `-SkipTests`      | Skip the pre-release `gradlew test` gate (not recommended)       |
| `-DryRun`         | Print steps; no tests/build/tag/`gh release create`              |
| `-Draft`          | Create a draft GitHub Release                                    |
| `-AllowDirty`     | Allow uncommitted local changes                                  |
| `-NotesFile path` | Use custom release notes instead of the template                 |
| `-Tag name`       | Override tag (default `v` + `appVersionName`)                    |

Requires: `git`, authenticated `gh`, Android SDK + release keystore (same as below). Push your release commit before running so the iOS workflow can build it when you are not on macOS.

Lint (optional; needs [PSScriptAnalyzer](https://github.com/PowerShell/PSScriptAnalyzer)):

```powershell
Invoke-ScriptAnalyzer -Path .\scripts\release.ps1 -Settings .\scripts\PSScriptAnalyzerSettings.psd1
```

Version identity is read from root [`build.gradle`](../../build.gradle) (`appVersionName` / `appVersionCode`). Bump those (and keep hero-echoes `package.json` in sync) **before** running the script.

## Build only

```bash
./gradlew prepareRelease
```

Windows:

```powershell
.\gradlew.bat prepareRelease
```

### Output

| File                               | Platform                                             |
| ---------------------------------- | ---------------------------------------------------- |
| `iatdb-<version>-android.apk`      | Android (signed with release keystore)               |
| `iatdb-<version>-desktop.jar`      | Desktop (needs a Java runtime)                       |
| `iatdb-<version>-ios-unsigned.ipa` | iOS (macOS builds only; unsigned - will not install) |
| `SHA256SUMS.txt`                   | Hashes + version metadata                            |

Example for `0.0.1`: `dist/0.0.1/`

The unsigned IPA is a **required** GitHub Release asset (same as APK + JAR). On macOS,
`prepareRelease` builds it locally. On Windows/Linux, Gradle omits it; [`scripts/release.ps1`](../../scripts/release.ps1)
fetches it by manually dispatching [`.github/workflows/ios-unsigned.yml`](../../.github/workflows/ios-unsigned.yml)
(`workflow_dispatch` only — not on tag pushes).

### Optional: native desktop package (no Java install)

Slow; downloads a Temurin JDK via the desktop runtime plugin.

```bash
./gradlew prepareRelease -PwithJpackage
```

Adds `iatdb-<version>-desktop-windows.zip` (or `macos` / `linux`) from `desktop/build/jpackage`.

## Requirements

- Android SDK (`local.properties` `sdk.dir` or `ANDROID_HOME`)
- [`android/keystore.properties`](android-signing.md) + release keystore
- JDK for Gradle / desktop JAR
- For e2e publish: [GitHub CLI](https://cli.github.com/) (`gh auth login`)
- For local IPA in `prepareRelease`: macOS + Xcode
- For IPA via Actions (Windows release): commit pushed to GitHub
- **Sentry uploads are mandatory on release:** `prepareRelease`, `assembleRelease`,
  `:desktop:release`, `:ios:createIPA`, and `release.ps1` all require
  `SENTRY_AUTH_TOKEN` (process env or root `.env`). There is no skip flag.
  Uploads source context to `android`, `java` (desktop), and `ios`, plus Android ProGuard maps.
  - Token: https://sentry.io/settings/dungeonboss/auth-tokens/
  - GitHub Actions secret: `SENTRY_AUTH_TOKEN` (required by `ios-unsigned.yml`)

## Not included (by design for alpha)

- Signed iOS / TestFlight
- Play Store AAB upload
- itch.io mirror (optional later via `butler`)

<!-- Project link refs: keep identical to docs/project-link-refs.md -->

[github-releases]: https://github.com/i-am-the-dungeon-boss/iatdb/releases
