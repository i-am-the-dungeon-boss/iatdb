#Requires -Version 5.1
Set-StrictMode -Version Latest

# Paused install notes (sideload artifacts):
# - **Android:** download the APK and sideload (enable install from unknown sources).
# - **Desktop JAR:** needs Java 11+ (`java -jar ...`).
# - **Desktop native zip:** unpack and run; no separate Java install (this host OS only).
# - **iOS:** install the unsigned IPA with Sideloadly (resigns with your free or paid Apple ID). Follow its guide: https://sideloadly.io/

function Get-ReleaseNoteBody {
    param(
        [string] $VersionName,
        [int] $VersionCode,
        [string] $TagName,
        [string] $CommitSha,
        [hashtable] $ProjectLinks
    )

    $tick = [char]96

    return @"
## $VersionName - I am the Dungeon Boss

Unofficial Shattered Pixel Dungeon fork / mod (GPLv3). Not affiliated with Shattered Pixel or Watabou.

### Play
- **Google Play** (full game): https://play.google.com/store/apps/details?id=com.dungeonboss.iatdb
- **Steam** (demo for Windows): https://store.steampowered.com/app/5020980

### Source
Tag ${tick}${TagName}${tick} at commit ${tick}${CommitSha}${tick}
$($ProjectLinks.GithubRepoUrl)/tree/$TagName

### Feedback
- GitHub Issues: $($ProjectLinks.GithubRepoUrl)/issues
- Email: $($ProjectLinks.DeveloperEmail)

### Known issues
- Beta build - expect bugs; please report crashes with platform + steps.

---
internal version number: $VersionCode
"@
}
