#Requires -Version 5.1
<#
.SYNOPSIS
  End-to-end community alpha release: build artifacts, tag, publish GitHub Release.

.DESCRIPTION
  Reads appVersionName / appVersionCode from the root build.gradle, runs
  prepareRelease (optionally with -PwithJpackage), then creates an annotated
  git tag and a GitHub Release with APK, JAR, SHA256SUMS, and generated notes.

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

    # Reuse existing dist/<version>/ artifacts; skip Gradle.
    [switch] $SkipBuild,

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
## $VersionName — I am the Dungeon Boss

Unofficial Shattered Pixel Dungeon fork / mod (GPLv3). Not affiliated with Shattered Pixel or Watabou.

### Install
- **Android:** download the APK and sideload (enable install from unknown sources).
$desktopInstall

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
Write-Host "  dryRun      = $DryRun"
Write-Host ""

$porcelain = git status --porcelain
if ($porcelain -and -not $AllowDirty) {
    Write-Host $porcelain
    throw 'Working tree has uncommitted changes. Commit/stash them, or pass -AllowDirty.'
}

if (-not $SkipBuild) {
    $gradlew = Get-GradleWrapper $root
    $gradleArgs = @('prepareRelease')
    if ($WithJpackage) {
        $gradleArgs += '-PwithJpackage'
    }
    Write-Host ">> $gradlew $($gradleArgs -join ' ')"
    if (-not $DryRun) {
        & $gradlew @gradleArgs
        if ($LASTEXITCODE -ne 0) {
            throw "prepareRelease failed with exit code $LASTEXITCODE"
        }
    }
} else {
    Write-Host ">> Skipping build (-SkipBuild)"
}

if (-not (Test-Path -LiteralPath $distDir)) {
    if ($DryRun) {
        Write-Host ">> Dry run: dist/${versionName} missing (would be created by prepareRelease)."
        Write-Host ">> Would tag ${tagName}, push to ${Remote}, and gh release create with APK+JAR(+optional zip)."
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

$assets = [System.Collections.Generic.List[string]]::new()
$assets.Add($apk.FullName) | Out-Null
$assets.Add($jar.FullName) | Out-Null
$assets.Add($sums.FullName) | Out-Null
$jpackageZips = @(Get-ChildItem -LiteralPath $distDir -Filter "iatdb-$versionName-desktop-*.zip" -File -ErrorAction SilentlyContinue)
foreach ($z in $jpackageZips) {
    $assets.Add($z.FullName) | Out-Null
}
$includesJpackage = $jpackageZips.Count -gt 0

$commitSha = (git rev-parse HEAD).Trim()
$notesPath = if ($NotesFile) {
    if (-not (Test-Path -LiteralPath $NotesFile)) {
        throw "Notes file not found: $NotesFile"
    }
    (Resolve-Path -LiteralPath $NotesFile).Path
} else {
    $generated = Join-Path $distDir 'RELEASE_NOTES.md'
    $body = Get-ReleaseNoteBody `
        -VersionName $versionName `
        -VersionCode $versionCode `
        -TagName $tagName `
        -CommitSha $commitSha `
        -AssetNames @($assets | ForEach-Object { Split-Path $_ -Leaf }) `
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
$title = "$versionName — I am the Dungeon Boss"
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
