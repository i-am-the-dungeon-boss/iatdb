# Android release signing

Release APKs/AABs for **I am the Dungeon Boss** must be signed with a **dedicated release keystore** — a PKCS12 file that holds the private key Android uses to prove the app came from you.

## Why it matters

- Sideload updates only install over an existing app if the **package name and signing key match**.
- Lose the keystore (or its passwords) → you cannot update that install; players must uninstall and lose local data, or you ship under a new package name.
- Debug builds use a throwaway debug key. Never distribute those as “the” release.

## Local setup (already done on this machine)

1. `android/iatdb-release.keystore` — private key material (**gitignored**)
2. `android/keystore.properties` — passwords + alias (**gitignored**)
3. `android/build.gradle` — `signingConfigs.release` applied when `keystore.properties` exists

Template: [`android/keystore.properties.example`](../../android/keystore.properties.example)

## Backup (do this now)

Copy both files to offline/password-manager storage you control:

- `android/iatdb-release.keystore`
- `android/keystore.properties`

Do **not** commit them, put them in chat, or share the passwords publicly.

## Build a signed release APK

```bash
./gradlew :android:assembleRelease
```

Output under `android/build/outputs/apk/release/`.

## Version identity (related)

| Field | Current | Scheme |
| --- | --- | --- |
| `appPackageName` | `com.marwanelzainy.iatdb` | Permanent once people install |
| `appVersionName` | `0.0.1` | `0.0.N` alpha · `0.1.N` beta · `1.0.N` first release |
| `appVersionCode` | `896` | Integer; bump every published binary |
