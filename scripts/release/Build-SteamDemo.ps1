#Requires -Version 5.1
Set-StrictMode -Version Latest

function Get-SteamHostSlug([bool] $OnWindows, [bool] $OnMacOs) {
    if ($OnWindows) { return 'windows' }
    if ($OnMacOs) { return 'macos' }
    return 'linux'
}

<#
.SYNOPSIS
  Dispatch steam-demo.yml for the OS this machine will not build.
  Linux covers SteamOS. Returns a hashtable of platform -> run id.
#>
function Start-SteamDemoBuilds(
    [string] $DistDir,
    [string] $VersionName,
    [string] $CommitSha,
    [bool] $OnWindows,
    [bool] $OnMacOs,
    [bool] $DryRun,
    [bool] $SkipBuild
) {
    $runs = @{}
    if ($DryRun -or $SkipBuild) { return ,$runs }
    $hostSlug = Get-SteamHostSlug -OnWindows $OnWindows -OnMacOs $OnMacOs
    # Linux/SteamOS zip is paused. Restore: foreach ($platform in @('windows', 'linux')) {
    foreach ($platform in @('windows')) {
        if ($platform -eq $hostSlug) { continue }
        $expected = Join-Path $DistDir "iatdb-$VersionName-steam-demo-$platform.zip"
        if (Test-Path -LiteralPath $expected) { continue }
        $runs[$platform] = Start-SteamDemoViaActions -CommitSha $CommitSha -Platform $platform
    }
    return ,$runs
}

function Build-SteamDemoLocal([string] $Gradlew, [bool] $OnWindows, [bool] $OnMacOs) {
    # macOS depot is paused. Linux/SteamOS zip is paused. Restore: if (-not $OnMacOs) {
    if ($OnWindows) {
        Invoke-ReleaseGradle -Gradlew $Gradlew -GradleArgs @('packageSteamDemo', '-Pdemo')
    }
}

function Complete-SteamDemoBuilds([hashtable] $Runs, [string] $DistDir, [string] $VersionName) {
    if (-not $Runs) { return }
    foreach ($platform in @($Runs.Keys)) {
        Complete-SteamDemoViaActions -DistDir $DistDir -VersionName $VersionName -Platform $platform -RunId $Runs[$platform] | Out-Null
    }
}

function Require-SteamDemoZips(
    [string] $DistDir,
    [string] $VersionName,
    [string] $CommitSha,
    [bool] $DryRun
) {
    # Linux/SteamOS zip is paused. Restore: foreach ($platform in @('windows', 'linux')) {
    foreach ($platform in @('windows')) {
        $zipPath = Join-Path $DistDir "iatdb-$VersionName-steam-demo-$platform.zip"
        if (Test-Path -LiteralPath $zipPath) { continue }
        if ($DryRun) {
            throw "Missing $zipPath. Without -DryRun, release.ps1 builds it locally or via steam-demo.yml."
        }
        Get-SteamDemoViaActions -DistDir $DistDir -VersionName $VersionName -CommitSha $CommitSha -Platform $platform | Out-Null
    }
}
