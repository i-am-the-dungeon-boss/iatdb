#Requires -Version 5.1
Set-StrictMode -Version Latest

function Get-ReleaseNoteBody {
    param(
        [string] $VersionName,
        [int] $VersionCode,
        [string] $TagName,
        [string] $CommitSha,
        [string[]] $AssetNames,
        [bool] $IncludesJpackage,
        [hashtable] $ProjectLinks
    )

    $tick = [char]96
    $assetLines = ($AssetNames | ForEach-Object { "- $tick$_$tick" }) -join "`n"
    $desktopInstall = if ($IncludesJpackage) {
        @"
- **Desktop JAR:** needs Java 11+ (${tick}java -jar ...${tick}).
- **Desktop native zip:** unpack and run; no separate Java install (this host OS only).
"@
    } else {
        "- **Desktop:** run the JAR with Java 11+ (${tick}java -jar ...${tick})."
    }

    return @"
## $VersionName - I am the Dungeon Boss

Unofficial Shattered Pixel Dungeon fork / mod (GPLv3). Not affiliated with Shattered Pixel or Watabou.

### Install
- **Android:** download the APK and sideload (enable install from unknown sources).
$desktopInstall
- **iOS:** install the unsigned IPA with Sideloadly (resigns with your free or paid Apple ID). Follow its guide: https://sideloadly.io/ (Download → Load IPA → Apple ID → Sideload).

### Artifacts
$assetLines

### Source
Tag ${tick}${TagName}${tick} at commit ${tick}${CommitSha}${tick}
$($ProjectLinks.GithubRepoUrl)/tree/$TagName

### Feedback
- GitHub Issues: $($ProjectLinks.GithubRepoUrl)/issues
- Email: $($ProjectLinks.DeveloperEmail)

### Known issues
- Alpha build - expect bugs; please report crashes with platform + steps.

---
internal version number: $VersionCode
"@
}
