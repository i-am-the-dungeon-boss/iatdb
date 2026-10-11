#Requires -Version 5.1
<#
.SYNOPSIS
  Orchestrate a store release: Play AAB, Steam demo, GitHub Release notes.

.DESCRIPTION
  Reads appVersionName / appVersionCode from the root build.gradle, runs all
  unit tests (`gradlew test`), then Build-PlayAab (prepareRelease, no -Pdemo)
  and Build-SteamDemo (-Pdemo, Windows only). Linux/SteamOS zip is paused.
  Creates an
  annotated git tag and a GitHub Release whose notes link to Google Play and
  Steam. Binaries stay in dist/ for Play Console and Steamworks.

  Paused paths (iOS IPA, APK/JAR assets, -PwithJpackage) are in
  scripts/release/paused.ps1. That file is not dot-sourced.

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

    # Re-release the current version without bumping: move an existing tag to
    # HEAD, force-push it, and overwrite the existing GitHub Release's notes and
    # assets. Without this, an existing tag on another commit aborts the release.
    [switch] $Force,

    # Override path to the hero-echoes repo (default: sibling ../hero-echoes or HERO_ECHOES_ROOT).
    [string] $HeroEchoesRoot = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

#region Load the step scripts. Paused iOS, APK, and asset-upload paths stay in paused.ps1.
$releaseLib = Join-Path $PSScriptRoot 'release'
. (Join-Path $releaseLib '_common.ps1')
. (Join-Path $releaseLib 'Fetch-SteamDemo.ps1')
. (Join-Path $releaseLib 'Build-PlayAab.ps1')
. (Join-Path $releaseLib 'Build-SteamDemo.ps1')
. (Join-Path $releaseLib 'New-ReleaseNotes.ps1')
. (Join-Path $releaseLib 'Publish-GitHubRelease.ps1')
. (Join-Path $releaseLib 'Publish-HeroEchoesVercelProduction.ps1')
. (Join-Path $releaseLib 'Update-Sha256Sums.ps1')
#endregion

#region Read the version from build.gradle, then refuse a dirty tree.
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
  play        = AAB full game
  steam       = demo windows
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
#endregion

#region Start other-OS Steam demo builds so they overlap tests. Linux/SteamOS is paused, so this is a no-op on Windows.
$steamArgs = @{
    DistDir     = $distDir
    VersionName = $versionName
    CommitSha   = $commitSha
    OnWindows   = [bool]$onWindows
    OnMacOs     = [bool]$onMacOs
    DryRun      = [bool]$DryRun
    SkipBuild   = [bool]$SkipBuild
}
$steamRuns = Start-SteamDemoBuilds @steamArgs
#endregion

#region Run unit tests before .env, then build the Play AAB and this machine's Steam demo.
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
        Build-PlayAab -Gradlew $gradlew
        Build-SteamDemoLocal -Gradlew $gradlew -OnWindows $onWindows -OnMacOs $onMacOs
    }
} finally {
    Restore-DaemonJvmCriteria $daemonJvmBackup
}
#endregion

#region Download the other OS Steam zip, then require the AAB and both demo zips.
Complete-SteamDemoBuilds -Runs $steamRuns -DistDir $distDir -VersionName $versionName

if (-not (Test-Path -LiteralPath $distDir)) {
    if ($DryRun) {
        Write-Host ">> Dry run: dist/${versionName} missing (would be created by prepareRelease)."
        Write-Host ">> Would build the Steam demo for the other OS via steam-demo.yml, tag ${tagName}, push to ${Remote}, and gh release create (notes only)."
        Write-Host ''
        Write-Host 'Dry run complete - no tag push or GitHub Release created.'
        exit 0
    }
    throw ('Missing dist folder: {0}. Run without -SkipBuild, or build first.' -f $distDir)
}

Require-PlayAab -DistDir $distDir -VersionName $versionName | Out-Null
Require-SteamDemoZips -DistDir $distDir -VersionName $versionName -CommitSha $commitSha -DryRun:$DryRun
Update-Sha256Sums -DistDir $distDir -VersionName $versionName -VersionCode $versionCode

Write-Host ''
Get-ChildItem -LiteralPath $distDir -File | ForEach-Object { Write-Host "  - $($_.Name)" }
Write-Host ''
#endregion

#region Point v<version> at this commit and push the tag.
$forceTagPush = $false
git show-ref --verify --quiet "refs/tags/$tagName"
if ($LASTEXITCODE -eq 0) {
    $tagCommit = (git rev-list -n 1 $tagName).Trim()
    if ($tagCommit -ne $commitSha) {
        if (-not $Force) {
            throw "Tag $tagName already exists on $tagCommit but HEAD is $commitSha"
        }
        $forceTagPush = $true
        Write-Host ">> git tag -f $tagName (was $tagCommit)"
        if (-not $DryRun) {
            Invoke-Checked {
                git tag -f -a $tagName -m "IATDB ${versionName} (versionCode ${versionCode})"
            } 'git tag -f failed'
        }
    } else {
        Write-Host ">> Tag $tagName already points at HEAD"
    }
} else {
    Write-Host ">> git tag -a $tagName"
    if (-not $DryRun) {
        Invoke-Checked {
            git tag -a $tagName -m "IATDB ${versionName} (versionCode ${versionCode})"
        } 'git tag failed'
    }
}

$pushArgs = if ($forceTagPush) { @('push', '--force', $Remote, $tagName) } else { @('push', $Remote, $tagName) }
Write-Host ">> git $($pushArgs -join ' ')"
if (-not $DryRun) {
    Invoke-Checked { git @pushArgs } 'git push tag failed'
}
#endregion

#region Publish GitHub Release notes that link to Play and Steam. No binaries attached.
$githubArgs = @{
    TagName      = $tagName
    VersionName  = $versionName
    VersionCode  = $versionCode
    CommitSha    = $commitSha
    DistDir      = $distDir
    NotesFile    = $NotesFile
    Draft        = [bool]$Draft
    DryRun       = [bool]$DryRun
    Force        = [bool]$Force
    ProjectLinks = $projectLinks
}
Publish-GitHubRelease @githubArgs
#endregion

#region Promote hero-echoes production only after the GitHub Release exists.
if ($SkipVercelPromote) {
    Write-Host '>> Skipping Hero Echoes Vercel promote (-SkipVercelPromote)'
} else {
    $vercelArgs = @{
        IatdbRoot      = $root
        HeroEchoesRoot = $HeroEchoesRoot
        Remote         = $Remote
    }
    if ($DryRun) { $vercelArgs['DryRun'] = $true }
    Publish-HeroEchoesVercelProduction @vercelArgs
}
#endregion

Write-Host ''
if ($DryRun) {
    Write-Host 'Dry run complete - no tag push, GitHub Release, or Vercel promote.'
} else {
    Write-Host "Published: $($projectLinks.GithubRepoUrl)/releases/tag/$tagName"
}
