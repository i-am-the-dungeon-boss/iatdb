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

$releaseLib = Join-Path $PSScriptRoot 'release'
. (Join-Path $releaseLib '_common.ps1')
. (Join-Path $releaseLib 'Fetch-UnsignedIosIpa.ps1')
. (Join-Path $releaseLib 'New-ReleaseNotes.ps1')
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
  withJpackage= $WithJpackage
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
$gradlew = Join-Path $root $(if ($onWindows) { 'gradlew.bat' } else { 'gradlew' })
if (-not (Test-Path -LiteralPath $gradlew)) { throw "gradlew not found at $gradlew" }

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
    $gradleArgs = @('prepareRelease')
    if ($WithJpackage) { $gradleArgs += '-PwithJpackage' }
    Invoke-ReleaseGradle -Gradlew $gradlew -GradleArgs $gradleArgs
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

Write-Host ''
if ($DryRun) {
    Write-Host 'Dry run complete - no tag push or GitHub Release created.'
} else {
    Write-Host "Published: $($projectLinks.GithubRepoUrl)/releases/tag/$tagName"
}
