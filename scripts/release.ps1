#Requires -Version 5.1
<#
.SYNOPSIS
  End-to-end community alpha release: build artifacts, tag, publish GitHub Release.

.DESCRIPTION
  Reads appVersionName / appVersionCode from the root build.gradle, runs all
  unit tests (`gradlew test`), then prepareRelease (optionally with
  -PwithJpackage), ensures an unsigned iOS IPA is present (built on macOS, or
  fetched via GitHub Actions on other OSes), then creates an annotated git tag
  and a GitHub Release with APK, JAR, IPA, SHA256SUMS, and generated notes.

.EXAMPLE
  .\scripts\release.ps1

.EXAMPLE
  .\scripts\release.ps1 -WithJpackage

.EXAMPLE
  .\scripts\release.ps1 -SkipBuild -DryRun
#>
[CmdletBinding()]
param(
    # Also build the native desktop zip (slow; downloads a JDK).
    [switch] $WithJpackage,

    # Reuse existing dist/<version>/ artifacts; skip prepareRelease (tests still run).
    [switch] $SkipBuild,

    # Skip the pre-release `gradlew test` gate (not recommended).
    [switch] $SkipTests,

    # Print actions without tagging, pushing, or calling gh release create.
    [switch] $DryRun,

    # Allow release when the working tree has uncommitted changes.
    [switch] $AllowDirty,

    # Create a draft GitHub Release (not published).
    [switch] $Draft,

    # Override release notes body (otherwise a template is generated).
    [string] $NotesFile = '',

    # Git remote used to push the tag (default: origin).
    [string] $Remote = 'origin',

    # Tag name override (default: v<appVersionName>).
    [string] $Tag = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-RepoRoot {
    $here = $PSScriptRoot
    if (-not $here) { $here = Split-Path -Parent $MyInvocation.MyCommand.Path }
    return (Resolve-Path (Join-Path $here '..')).Path
}

function Get-AppVersion {
    param([string] $BuildGradlePath)

    $text = Get-Content -LiteralPath $BuildGradlePath -Raw
    if ($text -notmatch "appVersionName\s*=\s*'([^']+)'") {
        throw "Could not parse appVersionName from $BuildGradlePath"
    }
    $versionName = $Matches[1]
    if ($text -notmatch 'appVersionCode\s*=\s*(\d+)') {
        throw "Could not parse appVersionCode from $BuildGradlePath"
    }
    $versionCode = [int]$Matches[1]
    return @{
        Name = $versionName
        Code = $versionCode
    }
}

function Assert-Command {
    param([string] $Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found on PATH: $Name"
    }
}

function Get-GradleWrapper {
    param([string] $Root)
    $onWindows = ($env:OS -match 'Windows') -or ($env:WinDir)
    $bat = Join-Path $Root 'gradlew.bat'
    $sh = Join-Path $Root 'gradlew'
    if ($onWindows) {
        if (-not (Test-Path -LiteralPath $bat)) {
            throw "gradlew.bat not found at $bat"
        }
        return $bat
    }
    if (-not (Test-Path -LiteralPath $sh)) {
        throw "gradlew not found at $sh"
    }
    return $sh
}

function Import-DotEnv {
    param([string] $Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) { return }
        $eq = $line.IndexOf('=')
        if ($eq -le 0) { return }
        $key = $line.Substring(0, $eq).Trim()
        $value = $line.Substring($eq + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        # Do not overwrite variables already set in the process environment.
        $existing = [Environment]::GetEnvironmentVariable($key, 'Process')
        if ([string]::IsNullOrWhiteSpace($existing)) {
            Set-Item -Path "Env:$key" -Value $value
        }
    }
}

function Assert-SentryAuthToken {
    if ([string]::IsNullOrWhiteSpace($env:SENTRY_AUTH_TOKEN)) {
        throw @'
Missing SENTRY_AUTH_TOKEN.

Every release uploads Sentry source context (android / java / ios). Set the token in your
environment or root .env (never commit it):
  https://sentry.io/settings/dungeonboss/auth-tokens/
'@
    }
    Write-Host '>> SENTRY_AUTH_TOKEN present — Sentry source uploads required.'
}

function Test-JdkHasJlink {
    param([string] $JavaHome)
    if ([string]::IsNullOrWhiteSpace($JavaHome)) { return $false }
    if (-not (Test-Path -LiteralPath $JavaHome)) { return $false }
    return (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\jlink.exe')) -or
        (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/jlink'))
}

function Resolve-ReleaseJavaHome {
    # Android JdkImageTransform needs a real JDK with jlink. The Cursor/VS Code
    # Red Hat Java extension embeds a stripped Temurin JRE (no jlink) that Gradle
    # may otherwise pick as the Daemon JVM.
    $candidates = [System.Collections.Generic.List[string]]::new()
    foreach ($key in @('JAVA_HOME', 'JDK_HOME')) {
        $value = [Environment]::GetEnvironmentVariable($key, 'Process')
        if (-not [string]::IsNullOrWhiteSpace($value)) { $candidates.Add($value.Trim()) }
    }
    $userGradleHome = Join-Path $env:USERPROFILE '.gradle\gradle.properties'
    if (Test-Path -LiteralPath $userGradleHome) {
        $text = Get-Content -LiteralPath $userGradleHome -Raw
        if ($text -match 'org\.gradle\.java\.home\s*=\s*(\S+)') {
            $candidates.Add(($Matches[1] -replace '/', '\').Trim())
        }
    }
    $candidates.Add((Join-Path $env:USERPROFILE '.gradle\jdks\eclipse_adoptium-21-amd64-windows.2'))
    $candidates.Add('C:\Program Files\Android\Android Studio\jbr')
    $candidates.Add('C:\Program Files\Java\jdk-17')
    $candidates.Add('C:\Program Files\Eclipse Adoptium\jdk-21.0.7+6-hotspot')

    $seen = @{}
    foreach ($raw in $candidates) {
        if ([string]::IsNullOrWhiteSpace($raw)) { continue }
        $home = $raw.Trim().Trim('"').Trim("'")
        $key = $home.ToLowerInvariant()
        if ($seen.ContainsKey($key)) { continue }
        $seen[$key] = $true
        # Never accept the Red Hat / JustJ embedded JRE from the Java extension.
        if ($home -match '[\\/]\.cursor[\\/]extensions[\\/]redhat\.java') { continue }
        if ($home -match '[\\/]org\.eclipse\.justj') { continue }
        if (Test-JdkHasJlink $home) {
            return (Resolve-Path -LiteralPath $home).Path
        }
    }

    throw @'
No JDK with jlink.exe found for release builds.

Android compileRelease needs jlink (JdkImageTransform). The Cursor Red Hat Java
extension JRE is not a full JDK — do not use it for Gradle.

Install Temurin 21 (or Android Studio JBR), set JAVA_HOME to that JDK, then retry.
'@
}

function Assert-ReleaseJavaHome {
    $javaHome = Resolve-ReleaseJavaHome
    $env:JAVA_HOME = $javaHome
    # Daemon JVM criteria (gradle/gradle-daemon-jvm.properties) overrides
    # org.gradle.java.home and can still select the IDE's stripped JRE.
    $env:ORG_GRADLE_JAVA_HOME = $javaHome
    Write-Host ">> JAVA_HOME=$javaHome (jlink OK)"
    return $javaHome
}

function Suspend-DaemonJvmCriteria {
    param([string] $Root)
    $path = Join-Path $Root 'gradle\gradle-daemon-jvm.properties'
    if (-not (Test-Path -LiteralPath $path)) {
        return $null
    }
    $backup = "$path.release-bak"
    Move-Item -LiteralPath $path -Destination $backup -Force
    Write-Host '>> Suspended gradle-daemon-jvm.properties (use JAVA_HOME / org.gradle.java.home)'
    return $backup
}

function Restore-DaemonJvmCriteria {
    param([string] $BackupPath)
    if ([string]::IsNullOrWhiteSpace($BackupPath)) { return }
    if (-not (Test-Path -LiteralPath $BackupPath)) { return }
    $original = $BackupPath -replace '\.release-bak$', ''
    Move-Item -LiteralPath $BackupPath -Destination $original -Force
    Write-Host '>> Restored gradle-daemon-jvm.properties'
}

function Invoke-ReleaseGradle {
    param(
        [string] $Gradlew,
        [string[]] $GradleArgs
    )
    Write-Host ">> $Gradlew $($GradleArgs -join ' ')"
    & $Gradlew @GradleArgs
    if ($LASTEXITCODE -ne 0) {
        throw ("Gradle {0} failed with exit code {1}" -f ($GradleArgs -join ' '), $LASTEXITCODE)
    }
}

function Get-ProjectLinks {
    param([string] $Root)

    $path = Join-Path $Root 'services\src\main\resources\project-links.properties'
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Missing project-links.properties at $path"
    }

    $map = @{}
    Get-Content -LiteralPath $path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) { return }
        $eq = $line.IndexOf('=')
        if ($eq -le 0) { return }
        $key = $line.Substring(0, $eq).Trim()
        $value = $line.Substring($eq + 1).Trim()
        $map[$key] = $value
    }

    foreach ($required in @('github.owner.repo', 'developer.email')) {
        if (-not $map.ContainsKey($required) -or [string]::IsNullOrWhiteSpace($map[$required])) {
            throw "Missing required property '$required' in $path"
        }
    }

    $ownerRepo = $map['github.owner.repo']
    return @{
        GithubOwnerRepo = $ownerRepo
        GithubRepoUrl   = "https://github.com/$ownerRepo"
        DeveloperEmail  = $map['developer.email']
    }
}

function Update-Sha256Sums {
    param(
        [string] $DistDir,
        [string] $VersionName,
        [int] $VersionCode
    )

    $manifest = Join-Path $DistDir 'SHA256SUMS.txt'
    $lines = @(
        "appVersionName=$VersionName",
        "appVersionCode=$VersionCode",
        "builtAt=$(Get-Date -Format o)",
        ''
    )
    Get-ChildItem -LiteralPath $DistDir -File |
        Where-Object { $_.Name -ne 'SHA256SUMS.txt' -and $_.Name -ne 'RELEASE_NOTES.md' } |
        Sort-Object Name |
        ForEach-Object {
            $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            $lines += "$hash  $($_.Name)"
        }
    Set-Content -LiteralPath $manifest -Value ($lines -join "`n") -Encoding utf8
}

function Get-UnsignedIosIpaViaActions {
    param(
        [string] $DistDir,
        [string] $VersionName,
        [string] $CommitSha
    )

    $expectedName = "iatdb-$VersionName-ios-unsigned.ipa"
    $expectedPath = Join-Path $DistDir $expectedName
    $artifactName = "iatdb-$VersionName-ios-unsigned"
    $workflow = 'ios-unsigned.yml'

    $ref = (git rev-parse --abbrev-ref HEAD).Trim()
    if ($ref -eq 'HEAD') {
        $ref = $CommitSha
    }

    git fetch --quiet origin 2>$null
    $remoteContains = @(git branch -r --contains $CommitSha 2>$null)
    if (-not $remoteContains -or $remoteContains.Count -eq 0) {
        throw "Commit $CommitSha is not on the remote. Push your branch before release so $workflow can build the unsigned IPA."
    }

    Write-Host ">> gh workflow run $workflow --ref $ref"
    gh workflow run $workflow --ref $ref
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to dispatch $workflow (is the workflow on the remote ref?)"
    }

    Write-Host ">> Waiting for $workflow run at $CommitSha ..."
    $runId = $null
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 5
        $json = gh run list --workflow $workflow --limit 10 --json databaseId,headSha,status,conclusion
        if ($LASTEXITCODE -ne 0) {
            throw 'gh run list failed'
        }
        $runs = $json | ConvertFrom-Json
        $match = $runs | Where-Object { $_.headSha -eq $CommitSha } | Select-Object -First 1
        if ($match) {
            $runId = [string]$match.databaseId
            break
        }
    }
    if (-not $runId) {
        throw "No $workflow run found for commit $CommitSha"
    }

    Write-Host ">> gh run watch $runId"
    gh run watch $runId --exit-status
    if ($LASTEXITCODE -ne 0) {
        throw "ios-unsigned workflow run $runId failed"
    }

    $downloadDir = Join-Path $DistDir '_ios-unsigned-download'
    if (Test-Path -LiteralPath $downloadDir) {
        Remove-Item -LiteralPath $downloadDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $downloadDir | Out-Null

    Write-Host ">> gh run download $runId -n $artifactName"
    gh run download $runId -n $artifactName -D $downloadDir
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to download artifact $artifactName from run $runId"
    }

    $downloaded = Get-ChildItem -LiteralPath $downloadDir -Recurse -Filter $expectedName -File | Select-Object -First 1
    if (-not $downloaded) {
        $downloaded = Get-ChildItem -LiteralPath $downloadDir -Recurse -Filter '*.ipa' -File | Select-Object -First 1
    }
    if (-not $downloaded) {
        throw "Downloaded artifact did not contain $expectedName"
    }
    Copy-Item -LiteralPath $downloaded.FullName -Destination $expectedPath -Force
    Remove-Item -LiteralPath $downloadDir -Recurse -Force
    return $expectedPath
}

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
    $repoUrl = $ProjectLinks.GithubRepoUrl
    $email = $ProjectLinks.DeveloperEmail
    $assetLines = ($AssetNames | ForEach-Object { "- $tick$_$tick" }) -join "`n"
    if ($IncludesJpackage) {
        $desktopInstall = @"
- **Desktop JAR:** needs Java 11+ (${tick}java -jar ...${tick}).
- **Desktop native zip:** unpack and run; no separate Java install (this host OS only).
"@
    } else {
        $desktopInstall = "- **Desktop:** run the JAR with Java 11+ (${tick}java -jar ...${tick})."
    }

    return @"
## $VersionName - I am the Dungeon Boss

Unofficial Shattered Pixel Dungeon fork / mod (GPLv3). Not affiliated with Shattered Pixel or Watabou.

### Install
- **Android:** download the APK and sideload (enable install from unknown sources).
$desktopInstall
- **iOS:** unsigned IPA is included for archive/CI only - it will **not** install on devices without resigning.

### Artifacts
$assetLines

### Source
Tag ${tick}${TagName}${tick} at commit ${tick}${CommitSha}${tick}
$repoUrl/tree/$TagName

### Feedback
- GitHub Issues: $repoUrl/issues
- Email: $email

### Known issues
- Alpha build - expect bugs; please report crashes with platform + steps.

---
internal version number: $VersionCode
"@
}

$root = Get-RepoRoot
Set-Location -LiteralPath $root

Assert-Command git
Assert-Command gh

$projectLinks = Get-ProjectLinks $root
$versions = Get-AppVersion (Join-Path $root 'build.gradle')
$versionName = $versions.Name
$versionCode = $versions.Code
$tagName = if ($Tag) { $Tag } else { "v$versionName" }
$distDir = Join-Path $root "dist\$versionName"

Write-Host "IATDB release"
Write-Host "  versionName = $versionName"
Write-Host "  versionCode = $versionCode"
Write-Host "  tag         = $tagName"
Write-Host "  github      = $($projectLinks.GithubOwnerRepo)"
Write-Host "  withJpackage= $WithJpackage"
Write-Host "  skipBuild   = $SkipBuild"
Write-Host "  skipTests   = $SkipTests"
Write-Host "  dryRun      = $DryRun"
Write-Host ""

$porcelain = git status --porcelain
if ($porcelain -and -not $AllowDirty) {
    Write-Host $porcelain
    throw 'Working tree has uncommitted changes. Commit/stash them, or pass -AllowDirty.'
}

$commitSha = (git rev-parse HEAD).Trim()

Import-DotEnv (Join-Path $root '.env')
Assert-SentryAuthToken
Assert-ReleaseJavaHome | Out-Null

$gradlew = Get-GradleWrapper $root
$daemonJvmBackup = $null

try {
    if (-not $DryRun -and (-not $SkipTests -or -not $SkipBuild)) {
        $daemonJvmBackup = Suspend-DaemonJvmCriteria $root
        Write-Host ">> $gradlew --stop"
        & $gradlew --stop | Out-Host
    }

    if (-not $SkipTests) {
        if ($DryRun) {
            Write-Host ">> $gradlew test"
        } else {
            try {
                Invoke-ReleaseGradle -Gradlew $gradlew -GradleArgs @('test')
            } catch {
                throw ("Unit tests failed — release aborted. {0}" -f $_.Exception.Message)
            }
            Write-Host '>> All unit tests passed.'
        }
    } else {
        Write-Host '>> Skipping tests (-SkipTests)'
    }

    if (-not $SkipBuild) {
        $gradleArgs = @('prepareRelease')
        if ($WithJpackage) {
            $gradleArgs += '-PwithJpackage'
        }
        if ($DryRun) {
            Write-Host ">> $gradlew $($gradleArgs -join ' ')"
        } else {
            Invoke-ReleaseGradle -Gradlew $gradlew -GradleArgs $gradleArgs
        }
    } else {
        Write-Host '>> Skipping build (-SkipBuild)'
    }
} finally {
    Restore-DaemonJvmCriteria $daemonJvmBackup
}

if (-not (Test-Path -LiteralPath $distDir)) {
    if ($DryRun) {
        Write-Host ">> Dry run: dist/${versionName} missing (would be created by prepareRelease)."
        Write-Host ">> Would ensure unsigned IPA, tag ${tagName}, push to ${Remote}, and gh release create."
        Write-Host ""
        Write-Host "Dry run complete - no tag push or GitHub Release created."
        exit 0
    }
    throw ('Missing dist folder: {0}. Run without -SkipBuild, or build first.' -f $distDir)
}

$apk = Get-ChildItem -LiteralPath $distDir -Filter "iatdb-$versionName-android.apk" -File -ErrorAction SilentlyContinue
$jar = Get-ChildItem -LiteralPath $distDir -Filter "iatdb-$versionName-desktop.jar" -File -ErrorAction SilentlyContinue
$sums = Get-ChildItem -LiteralPath $distDir -Filter 'SHA256SUMS.txt' -File -ErrorAction SilentlyContinue
if (-not $apk) { throw "Missing Android APK under $distDir" }
if (-not $jar) { throw "Missing desktop JAR under $distDir" }
if (-not $sums) { throw "Missing SHA256SUMS.txt under $distDir" }

$iosIpa = Get-ChildItem -LiteralPath $distDir -Filter "iatdb-$versionName-ios-unsigned.ipa" -File -ErrorAction SilentlyContinue
if (-not $iosIpa) {
    if ($DryRun) {
        throw @"
Missing unsigned iOS IPA under $distDir (expected iatdb-$versionName-ios-unsigned.ipa).
The IPA is a required GitHub Release asset. For -DryRun, place it in dist/ first (macOS prepareRelease,
or a prior ios-unsigned.yml artifact). Without -DryRun, release.ps1 fetches it via ios-unsigned.yml.
"@
    }
    $ipaPath = Get-UnsignedIosIpaViaActions -DistDir $distDir -VersionName $versionName -CommitSha $commitSha
    Update-Sha256Sums -DistDir $distDir -VersionName $versionName -VersionCode $versionCode
    $iosIpa = Get-Item -LiteralPath $ipaPath
    $sums = Get-Item -LiteralPath (Join-Path $distDir 'SHA256SUMS.txt')
}
if (-not $iosIpa) {
    throw "Missing unsigned iOS IPA under $distDir (expected iatdb-$versionName-ios-unsigned.ipa)"
}

$assets = [System.Collections.Generic.List[string]]::new()
$assets.Add($apk.FullName) | Out-Null
$assets.Add($jar.FullName) | Out-Null
$assets.Add($iosIpa.FullName) | Out-Null
$assets.Add($sums.FullName) | Out-Null
$jpackageZips = @(Get-ChildItem -LiteralPath $distDir -Filter "iatdb-$versionName-desktop-*.zip" -File -ErrorAction SilentlyContinue)
foreach ($z in $jpackageZips) {
    $assets.Add($z.FullName) | Out-Null
}
$includesJpackage = $jpackageZips.Count -gt 0

$notesPath = if ($NotesFile) {
    if (-not (Test-Path -LiteralPath $NotesFile)) {
        throw "Notes file not found: $NotesFile"
    }
    (Resolve-Path -LiteralPath $NotesFile).Path
} else {
    $generated = Join-Path $distDir 'RELEASE_NOTES.md'
    $assetNamesForNotes = @($assets | ForEach-Object { Split-Path $_ -Leaf })
    $body = Get-ReleaseNoteBody `
        -VersionName $versionName `
        -VersionCode $versionCode `
        -TagName $tagName `
        -CommitSha $commitSha `
        -AssetNames $assetNamesForNotes `
        -IncludesJpackage $includesJpackage `
        -ProjectLinks $projectLinks
    Set-Content -LiteralPath $generated -Value $body -Encoding utf8
    $generated
}

Write-Host ""
Write-Host "Artifacts:"
foreach ($a in $assets) {
    Write-Host "  - $(Split-Path $a -Leaf)"
}
Write-Host "Notes: $notesPath"
Write-Host ""

git show-ref --verify --quiet "refs/tags/$tagName"
$tagExists = ($LASTEXITCODE -eq 0)
if ($tagExists) {
    $tagCommit = (git rev-list -n 1 $tagName).Trim()
    if ($tagCommit -ne $commitSha) {
        throw "Tag $tagName already exists on $tagCommit but HEAD is $commitSha"
    }
    Write-Host ">> Tag $tagName already points at HEAD"
} else {
    Write-Host ">> git tag -a $tagName"
    if (-not $DryRun) {
        git tag -a $tagName -m "IATDB ${versionName} (versionCode ${versionCode})"
        if ($LASTEXITCODE -ne 0) { throw "git tag failed" }
    }
}

Write-Host ">> git push $Remote $tagName"
if (-not $DryRun) {
    git push $Remote $tagName
    if ($LASTEXITCODE -ne 0) { throw "git push tag failed" }
}

# Version first: GitHub's release list truncates titles on the left.
$title = "$versionName - I am the Dungeon Boss"
$ghArgs = @(
    'release', 'create', $tagName,
    '--title', $title,
    '--notes-file', $notesPath
)
if ($Draft) {
    $ghArgs += '--draft'
}
$ghArgs += @($assets)

Write-Host ">> gh $($ghArgs -join ' ')"
if (-not $DryRun) {
    & gh @ghArgs
    if ($LASTEXITCODE -ne 0) {
        throw "gh release create failed with exit code $LASTEXITCODE"
    }
}

Write-Host ""
if ($DryRun) {
    Write-Host "Dry run complete - no tag push or GitHub Release created."
} else {
    Write-Host "Published: $($projectLinks.GithubRepoUrl)/releases/tag/$tagName"
}
