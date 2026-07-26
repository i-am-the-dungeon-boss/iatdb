#Requires -Version 5.1
<#
.SYNOPSIS
  End-to-end community alpha release: build artifacts, tag, publish GitHub Release.

.DESCRIPTION
  Reads appVersionName / appVersionCode from the root build.gradle, runs all
  unit tests (`gradlew test`), then prepareRelease with -PwithJpackage (native
  desktop zip for this OS), ensures an unsigned iOS IPA is present (built on
  macOS, or fetched via GitHub Actions on other OSes). On Windows/Linux,
  ios-unsigned.yml is dispatched (or a same-commit run reused) before
  tests/prepareRelease so CI overlaps the local build; the IPA is downloaded
  after dist/ is ready. Then creates an annotated git tag and a GitHub Release
  with APK, JAR, IPA, jpackage zip, SHA256SUMS, and generated notes.

.EXAMPLE
  .\scripts\release.ps1

.EXAMPLE
  .\scripts\release.ps1 -SkipBuild -DryRun
#>
[CmdletBinding()]
param(
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
    [string] $Tag = '',

    # Skip vercel promote for hero-echoes after the GitHub Release.
    [switch] $SkipVercelPromote,

    # Override path to the hero-echoes repo (default: sibling ../hero-echoes or HERO_ECHOES_ROOT).
    [string] $HeroEchoesRoot = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$releaseLib = Join-Path $PSScriptRoot 'release'
. (Join-Path $releaseLib '_common.ps1')
. (Join-Path $releaseLib 'Fetch-UnsignedIosIpa.ps1')
. (Join-Path $releaseLib 'New-ReleaseNotes.ps1')
. (Join-Path $releaseLib 'Publish-HeroEchoesVercelProduction.ps1')
. (Join-Path $releaseLib 'Update-Sha256Sums.ps1')

function Assert-Command([string] $Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found on PATH: $Name"
    }
}

function Read-KeyValueFile([string] $Path) {
    $map = @{}
    if (-not (Test-Path -LiteralPath $Path)) { return $map }
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) { return }
        $eq = $line.IndexOf('=')
        if ($eq -le 0) { return }
        $key = $line.Substring(0, $eq).Trim()
        $value = $line.Substring($eq + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $map[$key] = $value
    }
    return $map
}

function Import-DotEnv([string] $Path) {
    foreach ($entry in (Read-KeyValueFile $Path).GetEnumerator()) {
        $existing = [Environment]::GetEnvironmentVariable($entry.Key, 'Process')
        if ([string]::IsNullOrWhiteSpace($existing)) {
            Set-Item -Path "Env:$($entry.Key)" -Value $entry.Value
        }
    }
}

function Get-AppVersion([string] $BuildGradlePath) {
    $text = Get-Content -LiteralPath $BuildGradlePath -Raw
    if ($text -notmatch "appVersionName\s*=\s*'([^']+)'") {
        throw "Could not parse appVersionName from $BuildGradlePath"
    }
    $name = $Matches[1]
    if ($text -notmatch 'appVersionCode\s*=\s*(\d+)') {
        throw "Could not parse appVersionCode from $BuildGradlePath"
    }
    return @{ Name = $name; Code = [int]$Matches[1] }
}

function Get-ProjectLinks([string] $Root) {
    $path = Join-Path $Root 'services\src\main\resources\project-links.properties'
    $map = Read-KeyValueFile $path
    if ($map.Count -eq 0) { throw "Missing project-links.properties at $path" }
    foreach ($required in @('github.owner.repo', 'developer.email')) {
        if ([string]::IsNullOrWhiteSpace($map[$required])) {
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

function Test-JdkHasJlink([string] $JavaHome) {
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
        $jdkHome = $raw.Trim().Trim('"').Trim("'")
        $key = $jdkHome.ToLowerInvariant()
        if ($seen.ContainsKey($key)) { continue }
        $seen[$key] = $true
        # Never accept the Red Hat / JustJ embedded JRE from the Java extension.
        if ($jdkHome -match '[\\/]\.cursor[\\/]extensions[\\/]redhat\.java') { continue }
        if ($jdkHome -match '[\\/]org\.eclipse\.justj') { continue }
        if (Test-JdkHasJlink $jdkHome) {
            return (Resolve-Path -LiteralPath $jdkHome).Path
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

function Suspend-DaemonJvmCriteria([string] $Root) {
    $path = Join-Path $Root 'gradle\gradle-daemon-jvm.properties'
    if (-not (Test-Path -LiteralPath $path)) {
        return $null
    }
    $backup = "$path.release-bak"
    Move-Item -LiteralPath $path -Destination $backup -Force
    Write-Host '>> Suspended gradle-daemon-jvm.properties (use JAVA_HOME / org.gradle.java.home)'
    return $backup
}

function Restore-DaemonJvmCriteria([string] $BackupPath) {
    if ([string]::IsNullOrWhiteSpace($BackupPath)) { return }
    if (-not (Test-Path -LiteralPath $BackupPath)) { return }
    $original = $BackupPath -replace '\.release-bak$', ''
    Move-Item -LiteralPath $BackupPath -Destination $original -Force
    Write-Host '>> Restored gradle-daemon-jvm.properties'
}

function Invoke-ReleaseGradle([string] $Gradlew, [string[]] $GradleArgs) {
    Write-Host ">> $Gradlew $($GradleArgs -join ' ')"
    if ($DryRun) { return }
    & $Gradlew @GradleArgs
    if ($LASTEXITCODE -ne 0) {
        throw ("Gradle {0} failed with exit code {1}" -f ($GradleArgs -join ' '), $LASTEXITCODE)
    }
}

function Get-RequiredDistFile([string] $DistDir, [string] $Filter) {
    $item = Get-ChildItem -LiteralPath $DistDir -Filter $Filter -File -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if (-not $item) { throw "Missing $Filter under $DistDir" }
    return $item
}

# --- main ---

$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location -LiteralPath $root

Assert-Command git
Assert-Command gh

$projectLinks = Get-ProjectLinks $root
$versions = Get-AppVersion (Join-Path $root 'build.gradle')
$versionName = $versions.Name
$versionCode = $versions.Code
$tagName = if ($Tag) { $Tag } else { "v$versionName" }
$distDir = Join-Path $root "dist\$versionName"

Write-Host @"
IATDB release
  versionName = $versionName
  versionCode = $versionCode
  tag         = $tagName
  github      = $($projectLinks.GithubOwnerRepo)
  withJpackage= True
  skipBuild   = $SkipBuild
  skipTests   = $SkipTests
  dryRun      = $DryRun
"@

$porcelain = git status --porcelain
if ($porcelain -and -not $AllowDirty) {
    Write-Host $porcelain
    throw 'Working tree has uncommitted changes. Commit/stash them, or pass -AllowDirty.'
}

$commitSha = (git rev-parse HEAD).Trim()

$onWindows = ($env:OS -match 'Windows') -or $env:WinDir
# prepareRelease only builds the IPA when :ios is included (macOS; see settings.gradle).
$onMacOs = $false
if (-not $onWindows) {
    if (Get-Variable -Name IsMacOS -Scope Global -ErrorAction SilentlyContinue) {
        $onMacOs = [bool]$IsMacOS
    } elseif (Get-Command uname -ErrorAction SilentlyContinue) {
        $onMacOs = (uname) -match 'Darwin'
    }
}
$gradlew = Join-Path $root $(if ($onWindows) { 'gradlew.bat' } else { 'gradlew' })
if (-not (Test-Path -LiteralPath $gradlew)) { throw "gradlew not found at $gradlew" }

# Overlap cold macOS CI with local tests + prepareRelease when IPA won't be built here.
$ipaExpectedPath = Join-Path $distDir "iatdb-$versionName-ios-unsigned.ipa"
$willBuildIpaLocally = $onMacOs -and -not $SkipBuild
$iosRunId = $null
if (-not $DryRun -and -not $willBuildIpaLocally -and -not (Test-Path -LiteralPath $ipaExpectedPath)) {
    $iosRunId = Start-UnsignedIosIpaViaActions -CommitSha $commitSha
}

# Load .env before Gradle so SENTRY_AUTH_TOKEN is available for source uploads during prepareRelease.
# Unit tests that need unset backend keys run first; keep backend secrets out of the test JVM by
# loading .env only after tests below.
Assert-ReleaseJavaHome | Out-Null
$daemonJvmBackup = $null
try {
    if (-not $DryRun -and (-not $SkipTests -or -not $SkipBuild)) {
        $daemonJvmBackup = Suspend-DaemonJvmCriteria $root
        Write-Host ">> $gradlew --stop"
        & $gradlew --stop | Out-Host
    }

    if ($SkipTests) {
        Write-Host '>> Skipping tests (-SkipTests)'
    } else {
        try {
            Invoke-ReleaseGradle -Gradlew $gradlew -GradleArgs @('test')
        } catch {
            throw ("Unit tests failed — release aborted. {0}" -f $_.Exception.Message)
        }
        if (-not $DryRun) { Write-Host '>> All unit tests passed.' }
    }

    # Load .env only after tests — Echo backend keys would otherwise poison
    # "backend unavailable" unit tests that expect those vars unset.
    Import-DotEnv (Join-Path $root '.env')
    if ([string]::IsNullOrWhiteSpace($env:SENTRY_AUTH_TOKEN)) {
        throw @'
Missing SENTRY_AUTH_TOKEN.

Every release uploads Sentry source context (android / java / ios). Set the token in your
environment or root .env (never commit it):
  https://sentry.io/settings/dungeonboss/auth-tokens/
'@
    }
    Write-Host '>> SENTRY_AUTH_TOKEN present — Sentry source uploads required.'

    if ($SkipBuild) {
        Write-Host '>> Skipping build (-SkipBuild)'
    } else {
        $gradleArgs = @('prepareRelease', '-PwithJpackage')
        Invoke-ReleaseGradle -Gradlew $gradlew -GradleArgs $gradleArgs
    }
} finally {
    Restore-DaemonJvmCriteria $daemonJvmBackup
}

if (-not (Test-Path -LiteralPath $distDir)) {
    if ($DryRun) {
        Write-Host ">> Dry run: dist/${versionName} missing (would be created by prepareRelease)."
        Write-Host ">> Would ensure unsigned IPA, tag ${tagName}, push to ${Remote}, and gh release create."
        Write-Host ''
        Write-Host 'Dry run complete - no tag push or GitHub Release created.'
        exit 0
    }
    throw ('Missing dist folder: {0}. Run without -SkipBuild, or build first.' -f $distDir)
}

$apk = Get-RequiredDistFile $distDir "iatdb-$versionName-android.apk"
$jar = Get-RequiredDistFile $distDir "iatdb-$versionName-desktop.jar"
$sums = Get-RequiredDistFile $distDir 'SHA256SUMS.txt'

$iosIpa = Get-ChildItem -LiteralPath $distDir -Filter "iatdb-$versionName-ios-unsigned.ipa" -File -ErrorAction SilentlyContinue |
    Select-Object -First 1
if (-not $iosIpa) {
    if ($DryRun) {
        throw @"
Missing unsigned iOS IPA under $distDir (expected iatdb-$versionName-ios-unsigned.ipa).
The IPA is a required GitHub Release asset. For -DryRun, place it in dist/ first (macOS prepareRelease,
or a prior ios-unsigned.yml artifact). Without -DryRun, release.ps1 fetches it via ios-unsigned.yml.
"@
    }
    if ($iosRunId) {
        Complete-UnsignedIosIpaViaActions -DistDir $distDir -VersionName $versionName -RunId $iosRunId | Out-Null
    } else {
        Get-UnsignedIosIpaViaActions -DistDir $distDir -VersionName $versionName -CommitSha $commitSha | Out-Null
    }
    Update-Sha256Sums -DistDir $distDir -VersionName $versionName -VersionCode $versionCode
    $iosIpa = Get-Item -LiteralPath (Join-Path $distDir "iatdb-$versionName-ios-unsigned.ipa")
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
foreach ($z in $jpackageZips) { $assets.Add($z.FullName) | Out-Null }
$includesJpackage = $jpackageZips.Count -gt 0

if ($NotesFile) {
    if (-not (Test-Path -LiteralPath $NotesFile)) { throw "Notes file not found: $NotesFile" }
    $notesPath = (Resolve-Path -LiteralPath $NotesFile).Path
} else {
    $notesPath = Join-Path $distDir 'RELEASE_NOTES.md'
    $body = Get-ReleaseNoteBody `
        -VersionName $versionName `
        -VersionCode $versionCode `
        -TagName $tagName `
        -CommitSha $commitSha `
        -AssetNames @($assets | ForEach-Object { Split-Path $_ -Leaf }) `
        -IncludesJpackage $includesJpackage `
        -ProjectLinks $projectLinks
    Set-Content -LiteralPath $notesPath -Value $body -Encoding utf8
}

Write-Host ''
Write-Host 'Artifacts:'
foreach ($a in $assets) { Write-Host "  - $(Split-Path $a -Leaf)" }
Write-Host "Notes: $notesPath"
Write-Host ''

git show-ref --verify --quiet "refs/tags/$tagName"
if ($LASTEXITCODE -eq 0) {
    $tagCommit = (git rev-list -n 1 $tagName).Trim()
    if ($tagCommit -ne $commitSha) {
        throw "Tag $tagName already exists on $tagCommit but HEAD is $commitSha"
    }
    Write-Host ">> Tag $tagName already points at HEAD"
} else {
    Write-Host ">> git tag -a $tagName"
    if (-not $DryRun) {
        Invoke-Checked {
            git tag -a $tagName -m "IATDB ${versionName} (versionCode ${versionCode})"
        } 'git tag failed'
    }
}

Write-Host ">> git push $Remote $tagName"
if (-not $DryRun) {
    Invoke-Checked { git push $Remote $tagName } 'git push tag failed'
}

# Version first: GitHub's release list truncates titles on the left.
$ghArgs = @(
    'release', 'create', $tagName,
    '--title', "$versionName - I am the Dungeon Boss",
    '--notes-file', $notesPath
)
if ($Draft) { $ghArgs += '--draft' }
$ghArgs += @($assets)

Write-Host ">> gh $($ghArgs -join ' ')"
if (-not $DryRun) {
    & gh @ghArgs
    if ($LASTEXITCODE -ne 0) {
        throw "gh release create failed with exit code $LASTEXITCODE"
    }
}

# Invoke hero-echoes promote-vercel.ps1 only after the GitHub Release succeeds
# so /v1/game-version does not advertise a version whose release assets are missing.
if ($SkipVercelPromote) {
    Write-Host '>> Skipping Hero Echoes Vercel promote (-SkipVercelPromote)'
} else {
    Publish-HeroEchoesVercelProduction `
        -IatdbRoot $root `
        -HeroEchoesRoot $HeroEchoesRoot `
        -Remote $Remote `
        -DryRun:$DryRun
}

Write-Host ''
if ($DryRun) {
    Write-Host 'Dry run complete - no tag push, GitHub Release, or Vercel promote.'
} else {
    Write-Host "Published: $($projectLinks.GithubRepoUrl)/releases/tag/$tagName"
}
