#Requires -Version 5.1
Set-StrictMode -Version Latest

function Assert-CommitOnRemote([string] $CommitSha, [string] $Workflow) {
    git fetch --quiet origin 2>$null
    $remoteContains = @(git branch -r --contains $CommitSha 2>$null)
    if (-not $remoteContains -or $remoteContains.Count -eq 0) {
        throw "Commit $CommitSha is not on the remote. Push your branch before release so $Workflow can build the unsigned IPA."
    }
}

function Get-IosUnsignedWorkflowRef([string] $CommitSha) {
    $ref = (git rev-parse --abbrev-ref HEAD).Trim()
    if ($ref -eq 'HEAD') { $ref = $CommitSha }
    return $ref
}

function Find-UnsignedIosIpaRunId([string] $CommitSha, [string] $Workflow) {
    $json = gh run list --workflow $Workflow --limit 20 --json databaseId,headSha,status,conclusion
    if ($LASTEXITCODE -ne 0) { throw 'gh run list failed' }
    $runs = @($json | ConvertFrom-Json)

    $success = $runs |
        Where-Object { $_.headSha -eq $CommitSha -and $_.status -eq 'completed' -and $_.conclusion -eq 'success' } |
        Select-Object -First 1
    if ($success) { return [string]$success.databaseId }

    $activeStatuses = @('queued', 'pending', 'requested', 'waiting', 'in_progress')
    $active = $runs |
        Where-Object { $_.headSha -eq $CommitSha -and ($activeStatuses -contains $_.status) } |
        Select-Object -First 1
    if ($active) { return [string]$active.databaseId }

    return $null
}

function Wait-UnsignedIosIpaRunId([string] $CommitSha, [string] $Workflow) {
    Write-Host ">> Waiting for $Workflow run at $CommitSha ..."
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 5
        $runId = Find-UnsignedIosIpaRunId -CommitSha $CommitSha -Workflow $Workflow
        if ($runId) { return $runId }
    }
    throw "No $Workflow run found for commit $CommitSha"
}

<#
.SYNOPSIS
  Ensure an ios-unsigned.yml run exists for $CommitSha (reuse or dispatch). Returns run id.
#>
function Start-UnsignedIosIpaViaActions([string] $CommitSha) {
    $workflow = 'ios-unsigned.yml'
    Assert-CommitOnRemote -CommitSha $CommitSha -Workflow $workflow

    $existing = Find-UnsignedIosIpaRunId -CommitSha $CommitSha -Workflow $workflow
    if ($existing) {
        Write-Host ">> Reusing existing $workflow run $existing for $CommitSha"
        return $existing
    }

    $ref = Get-IosUnsignedWorkflowRef -CommitSha $CommitSha
    Write-Host ">> gh workflow run $workflow --ref $ref"
    Invoke-Checked { gh workflow run $workflow --ref $ref } "Failed to dispatch $workflow (is the workflow on the remote ref?)"

    return (Wait-UnsignedIosIpaRunId -CommitSha $CommitSha -Workflow $workflow)
}

<#
.SYNOPSIS
  Wait for a started ios-unsigned run and copy the IPA into dist/.
#>
function Complete-UnsignedIosIpaViaActions(
    [string] $DistDir,
    [string] $VersionName,
    [string] $RunId
) {
    $expectedName = "iatdb-$VersionName-ios-unsigned.ipa"
    $expectedPath = Join-Path $DistDir $expectedName
    $artifactName = "iatdb-$VersionName-ios-unsigned"

    Write-Host ">> gh run watch $RunId"
    Invoke-Checked { gh run watch $RunId --exit-status } "ios-unsigned workflow run $RunId failed"

    $downloadDir = Join-Path $DistDir '_ios-unsigned-download'
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
        $downloaded = Get-ChildItem -LiteralPath $downloadDir -Recurse -Filter '*.ipa' -File |
            Select-Object -First 1
    }
    if (-not $downloaded) { throw "Downloaded artifact did not contain $expectedName" }

    Copy-Item -LiteralPath $downloaded.FullName -Destination $expectedPath -Force
    Remove-Item -LiteralPath $downloadDir -Recurse -Force
    return $expectedPath
}

<#
.SYNOPSIS
  Start (or reuse) ios-unsigned.yml for this commit and download the IPA into dist/.
#>
function Get-UnsignedIosIpaViaActions([string] $DistDir, [string] $VersionName, [string] $CommitSha) {
    $runId = Start-UnsignedIosIpaViaActions -CommitSha $CommitSha
    return (Complete-UnsignedIosIpaViaActions -DistDir $DistDir -VersionName $VersionName -RunId $runId)
}
