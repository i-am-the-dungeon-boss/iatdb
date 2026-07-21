# Prepare release artifacts

## End-to-end (recommended)

One command builds binaries, creates an annotated git tag `v<appVersionName>`, pushes it, and publishes a [GitHub Release][github-releases] with APK, JAR, `SHA256SUMS.txt`, and generated notes (includes `internal version number: <appVersionCode>`).

```powershell
# APK + desktop JAR
.\scripts\release.ps1

# Also include native desktop zip for this OS (slow; downloads a JDK)
.\scripts\release.ps1 -WithJpackage
```

| Flag              | Meaning                                           |
| ----------------- | ------------------------------------------------- |
| `-WithJpackage`   | Passes `-PwithJpackage` to Gradle                 |
| `-SkipBuild`      | Reuse existing `dist/<version>/` (publish only)   |
| `-DryRun`         | Print steps; no tag push / no `gh release create` |
| `-Draft`          | Create a draft GitHub Release                     |
| `-AllowDirty`     | Allow uncommitted local changes                   |
| `-NotesFile path` | Use custom release notes instead of the template  |
| `-Tag name`       | Override tag (default `v` + `appVersionName`)     |

Requires: `git`, authenticated `gh`, Android SDK + release keystore (same as below).

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

| File                          | Platform                               |
| ----------------------------- | -------------------------------------- |
| `iatdb-<version>-android.apk` | Android (signed with release keystore) |
| `iatdb-<version>-desktop.jar` | Desktop (needs a Java runtime)         |
| `SHA256SUMS.txt`              | Hashes + version metadata              |

Example for `0.0.1`: `dist/0.0.1/`

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

## Not included (by design for alpha)

- iOS / TestFlight
- Play Store AAB upload
- itch.io mirror (optional later via `butler`)

<!-- Project link refs: keep identical to docs/project-link-refs.md -->

[github-releases]: https://github.com/i-am-the-dungeon-boss/iatdb/releases
