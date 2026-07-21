# Prepare release artifacts

One command builds the community-alpha binaries and copies them into `dist/<version>/`.

```bash
./gradlew prepareRelease
```

Windows:

```powershell
.\gradlew.bat prepareRelease
```

## Output

| File | Platform |
| --- | --- |
| `iatdb-<version>-android.apk` | Android (signed with release keystore) |
| `iatdb-<version>-desktop.jar` | Desktop (needs a Java runtime) |
| `SHA256SUMS.txt` | Hashes + version metadata |

Example for `0.0.1`: `dist/0.0.1/`

## Optional: native desktop package (no Java install)

Slow; downloads a Temurin JDK via the desktop runtime plugin.

```bash
./gradlew prepareRelease -PwithJpackage
```

Adds `iatdb-<version>-desktop-windows.zip` (or `macos` / `linux`) from `desktop/build/jpackage`.

## Requirements

- Android SDK (`local.properties` `sdk.dir` or `ANDROID_HOME`)
- [`android/keystore.properties`](android-signing.md) + release keystore
- JDK for Gradle / desktop JAR

## Not included (by design for alpha)

- iOS / TestFlight
- Play Store AAB upload
