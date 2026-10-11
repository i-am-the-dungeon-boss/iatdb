#Requires -Version 5.1
Set-StrictMode -Version Latest

function Assert-SteamDemoCommitOnRemote([string] $CommitSha) {
    git fetch --quiet origin 2>$null
    $remoteContains = @(git branch -r --contains $CommitSha 2>$null)
    if (-not $remoteContains -or $remoteContains.Count -eq 0) {
        throw "Commit $CommitSha is not on the remote. Push your branch before release so steam-demo.yml can build."
    }
}

function Get-SteamDemoWorkflowRef([string] $CommitSha) {
    $ref = (git rev-parse --abbrev-ref HEAD).Trim()
    if ($ref -eq 'HEAD') { $ref = $CommitSha }
    return $ref
}

function Find-SteamDemoRunId([string] $CommitSha, [string] $Platform) {
    $workflow = 'steam-demo.yml'
    $want = "Steam demo $Platform"
    $json = gh run list --workflow $workflow --limit 20 --json databaseId,headSha,status,conclusion,displayTitle,name
    if ($LASTEXITCODE -ne 0) { throw 'gh run list failed' }
    $runs = @($json | ConvertFrom-Json) | Where-Object {
        $_.headSha -eq $CommitSha -and ($_.displayTitle -eq $want -or $_.name -eq $want)
    }

    $success = $runs |
        Where-Object { $_.status -eq 'completed' -and $_.conclusion -eq 'success' } |
        Select-Object -First 1
    if ($success) { return [string]$success.databaseId }

    $activeStatuses = @('queued', 'pending', 'requested', 'waiting', 'in_progress')
    $active = $runs |
        Where-Object { $activeStatuses -contains $_.status } |
        Select-Object -First 1
    if ($active) { return [string]$active.databaseId }

    return $null
}

function Wait-SteamDemoRunId([string] $CommitSha, [string] $Platform) {
    $workflow = 'steam-demo.yml'
    Write-Host ">> Waiting for $workflow ($Platform) at $CommitSha ..."
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 5
        $runId = Find-SteamDemoRunId -CommitSha $CommitSha -Platform $Platform
        if ($runId) { return $runId }
    }
    throw "No $workflow run found for $Platform at commit $CommitSha"
}

<#
.SYNOPSIS
  Ensure a steam-demo.yml run exists for this commit and platform. Returns run id.
#>
function Start-SteamDemoViaActions([string] $CommitSha, [string] $Platform) {
    $workflow = 'steam-demo.yml'
    Assert-SteamDemoCommitOnRemote -CommitSha $CommitSha

    $existing = Find-SteamDemoRunId -CommitSha $CommitSha -Platform $Platform
    if ($existing) {
        Write-Host ">> Reusing existing $workflow run $existing for $Platform at $CommitSha"
        return $existing
    }

    $ref = Get-SteamDemoWorkflowRef -CommitSha $CommitSha
    Write-Host ">> gh workflow run $workflow --ref $ref -f platform=$Platform"
    Invoke-Checked {
        gh workflow run $workflow --ref $ref -f "platform=$Platform"
    } "Failed to dispatch $workflow (is the workflow on the remote ref?)"

    return (Wait-SteamDemoRunId -CommitSha $CommitSha -Platform $Platform)
}

<#
.SYNOPSIS
  Wait for a started steam-demo run and copy the zip into dist/.
#>
function Complete-SteamDemoViaActions(
    [string] $DistDir,
    [string] $VersionName,
    [string] $Platform,
    [string] $RunId
) {
    $expectedName = "iatdb-$VersionName-steam-demo-$Platform.zip"
    $expectedPath = Join-Path $DistDir $expectedName
    $artifactName = "iatdb-$Platform-steam-demo"

    Write-Host ">> gh run watch $RunId"
    Invoke-Checked { gh run watch $RunId --exit-status } "steam-demo workflow run $RunId failed"

    $downloadDir = Join-Path $DistDir "_steam-demo-$Platform-download"
    if (Test-Path -LiteralPath $downloadDir) {
        Remove-Item -LiteralPath $downloadDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $downloadDir | Out-Null

    Write-Host ">> gh run download $RunId -n $artifactName"
    Invoke-Checked {
        gh run download $RunId -n $artifactName -D $downloadDir
    } "Failed to download artifact $artifactName from run $RunId"

    $downloaded = Get-ChildItem -LiteralPath $downloadDir -Recurse -Filter $expectedName -File |
        Select-Object -First 1
    if (-not $downloaded) {
        $downloaded = Get-ChildItem -LiteralPath $downloadDir -Recurse -Filter '*-steam-demo-*.zip' -File |
            Select-Object -First 1
    }
    if (-not $downloaded) { throw "Downloaded artifact did not contain $expectedName" }

    Copy-Item -LiteralPath $downloaded.FullName -Destination $expectedPath -Force
    Remove-Item -LiteralPath $downloadDir -Recurse -Force
    return $expectedPath
}

<#
.SYNOPSIS
  Start (or reuse) steam-demo.yml for this commit and download the zip into dist/.
#>
function Get-SteamDemoViaActions(
    [string] $DistDir,
    [string] $VersionName,
    [string] $CommitSha,
    [string] $Platform
) {
    $runId = Start-SteamDemoViaActions -CommitSha $CommitSha -Platform $Platform
    return (Complete-SteamDemoViaActions -DistDir $DistDir -VersionName $VersionName -Platform $Platform -RunId $runId)
}
